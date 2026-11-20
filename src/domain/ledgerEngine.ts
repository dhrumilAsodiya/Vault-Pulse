import { isSameLocalDay } from './formatters';
import { extractAvailableBalance } from './smsParser';
import {
  AccountProfile,
  CaptureProcessResult,
  CategoryId,
  KeywordRule,
  MerchantAccountOverride,
  ParsedFinancialEvent,
  SmsInboxMessage,
  SyncValidationResult,
  Transaction,
  TransactionType,
} from './types';

export const DEDUP_WINDOW_MS = 3 * 60 * 1000; // ±3 minutes

/**
 * Matches merchant/raw text against user-editable local keyword rules.
 */
export function categorizeByRules(
  merchant: string,
  rules: KeywordRule[],
  txType: TransactionType,
  rawText: string = ''
): CategoryId {
  if (txType === 'SELF_TRANSFER') return 'Transfer';
  const haystack = `${merchant} ${rawText}`.toLowerCase();

  for (const rule of rules) {
    for (const kw of rule.keywords) {
      const trimmed = kw.trim().toLowerCase();
      if (trimmed && haystack.includes(trimmed)) {
        return rule.category;
      }
    }
  }

  return txType === 'CREDIT' ? 'Income / Salary' : 'Other';
}

/**
 * Resolves which AccountProfile should own an incoming parsed SMS or UPI notification.
 */
export function resolveAccountForEvent(
  event: ParsedFinancialEvent,
  accounts: AccountProfile[],
  overrides: MerchantAccountOverride[]
): AccountProfile {
  // Check explicit auto-reassignment rules first
  const lowerMerchant = event.merchant.toLowerCase();
  for (const override of overrides) {
    if (lowerMerchant.includes(override.merchantPattern.toLowerCase())) {
      const matched = accounts.find((a) => a.id === override.accountId);
      if (matched) return matched;
    }
  }

  // Match by masked 4 digits (e.g. 4892, 9104, 1278)
  if (event.maskedDigits) {
    const byDigits = accounts.find((a) => a.maskedDigits === event.maskedDigits);
    if (byDigits) return byDigits;
  }

  // Match by bank senderId prefix (e.g. SBIINB -> SBI, HDFCBK -> HDFC)
  const senderUpper = event.senderOrApp.toUpperCase();
  const bySender = accounts.find(
    (a) => a.senderId && senderUpper.includes(a.senderId.toUpperCase())
  );
  if (bySender) return bySender;

  // UPI notifications and unknown bank events route to Default Primary Profile
  return (
    accounts.find((a) => a.isPrimaryUpi) ??
    accounts.find((a) => a.accountType === 'BANK') ??
    accounts[0]
  );
}

/**
 * Checks for an existing transaction with same amount and type within ±3 minutes.
 */
export function findDuplicateTransaction(
  existing: Transaction[],
  amount: number,
  type: TransactionType,
  timestamp: number
): Transaction | undefined {
  return existing.find(
    (tx) =>
      tx.type === type &&
      Math.abs(tx.amount - amount) < 0.005 &&
      Math.abs(tx.timestamp - timestamp) <= DEDUP_WINDOW_MS
  );
}

/**
 * Inserts or merges a parsed SMS/UPI event into state with deduplication, keyword categorization,
 * and balance update (snap to Avl Bal if present, else delta).
 */
export function processParsedEvent(params: {
  event: ParsedFinancialEvent;
  accounts: AccountProfile[];
  transactions: Transaction[];
  keywordRules: KeywordRule[];
  merchantAccountOverrides: MerchantAccountOverride[];
}): {
  accounts: AccountProfile[];
  transactions: Transaction[];
  result: CaptureProcessResult;
} {
  const { event, accounts, transactions, keywordRules, merchantAccountOverrides } = params;
  const targetAccount = resolveAccountForEvent(event, accounts, merchantAccountOverrides);
  const existingDup = findDuplicateTransaction(
    transactions,
    event.amount,
    event.type,
    event.timestamp
  );

  if (existingDup) {
    const betterMerchant =
      existingDup.merchant.includes('UPI') || existingDup.merchant.includes('Transfer')
        ? event.merchant
        : existingDup.merchant;

    const mergedCategory =
      existingDup.category === 'Other'
        ? categorizeByRules(betterMerchant, keywordRules, event.type, event.rawBody)
        : existingDup.category;

    const mergedTx: Transaction = {
      ...existingDup,
      merchant: betterMerchant,
      category: mergedCategory,
      avlBalSnapshot: event.avlBal ?? existingDup.avlBalSnapshot,
      rawRef: existingDup.rawRef
        ? `${existingDup.rawRef} | ${event.senderOrApp}`
        : event.senderOrApp,
      statusLabel:
        existingDup.source === 'UPI_LISTENER' || event.source === 'SMS_AUTO'
          ? 'SMS + UPI Merged'
          : existingDup.statusLabel,
    };

    const nextTransactions = transactions.map((tx) =>
      tx.id === existingDup.id ? mergedTx : tx
    );

    // If the duplicate brings an authoritative Avl Bal, snap that account without double-counting delta
    const nextAccounts =
      event.avlBal !== undefined
        ? accounts.map((acc) =>
            acc.id === mergedTx.accountId
              ? {
                  ...acc,
                  balance: event.avlBal!,
                  lastUpdatedTimestamp: Math.max(acc.lastUpdatedTimestamp, event.timestamp),
                }
              : acc
          )
        : accounts;

    return {
      accounts: nextAccounts,
      transactions: nextTransactions,
      result: {
        status: 'MERGED',
        reason: `Deduplicated within ±3 min window against "${existingDup.merchant}" and merged metadata.`,
        transaction: mergedTx,
      },
    };
  }

  const category = categorizeByRules(
    event.merchant,
    keywordRules,
    event.type,
    event.rawBody
  );

  const newTx: Transaction = {
    id: `tx_${event.timestamp}_${Math.random().toString(36).slice(2, 7)}`,
    type: event.type,
    amount: event.amount,
    merchant: event.merchant,
    category,
    accountId: targetAccount.id,
    timestamp: event.timestamp,
    source: event.source,
    statusLabel: event.source === 'SMS_AUTO' ? 'SMS Auto' : `${event.senderOrApp} UPI`,
    avlBalSnapshot: event.avlBal,
    rawRef: event.senderOrApp,
  };

  const nextAccounts = accounts.map((acc) => {
    if (acc.id !== targetAccount.id) return acc;
    const deltaBalance =
      event.type === 'CREDIT' ? acc.balance + event.amount : acc.balance - event.amount;
    const nextBalance = event.avlBal !== undefined ? event.avlBal : Number(deltaBalance.toFixed(2));
    return {
      ...acc,
      balance: nextBalance,
      lastUpdatedTimestamp: Math.max(acc.lastUpdatedTimestamp, event.timestamp),
      lastSyncedSmsTimestamp:
        event.avlBal !== undefined && event.source === 'SMS_AUTO'
          ? Math.max(acc.lastSyncedSmsTimestamp, event.timestamp)
          : acc.lastSyncedSmsTimestamp,
    };
  });

  return {
    accounts: nextAccounts,
    transactions: [newTx, ...transactions].sort((a, b) => b.timestamp - a.timestamp),
    result: {
      status: 'INSERTED',
      reason: `Captured via ${newTx.statusLabel} into ${targetAccount.shortName} (${category}).`,
      transaction: newTx,
    },
  };
}

/**
 * Adds a manual transaction (Expense, Income, or Self-Transfer) and updates running account balances.
 */
export function addManualTransaction(params: {
  accounts: AccountProfile[];
  transactions: Transaction[];
  input: {
    type: TransactionType;
    amount: number;
    merchant: string;
    category: CategoryId;
    accountId: string;
    toAccountId?: string;
    timestamp: number;
    taxDeductible?: boolean;
    autoDeductCashStash?: boolean;
    note?: string;
  };
}): {
  accounts: AccountProfile[];
  transactions: Transaction[];
  transaction: Transaction;
} {
  const { accounts, transactions, input } = params;
  const newTx: Transaction = {
    id: `tx_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    type: input.type,
    amount: input.amount,
    merchant: input.merchant.trim(),
    category: input.type === 'SELF_TRANSFER' ? 'Transfer' : input.category,
    accountId: input.accountId,
    toAccountId: input.type === 'SELF_TRANSFER' ? input.toAccountId : undefined,
    timestamp: input.timestamp,
    source: 'MANUAL',
    statusLabel: input.type === 'SELF_TRANSFER' ? 'Transfer' : 'Manual Entry',
    taxDeductible: input.taxDeductible,
    autoDeductCashStash: input.autoDeductCashStash,
    note: input.note,
  };

  const nextAccounts = accounts.map((acc) => {
    if (input.type === 'DEBIT') {
      if (acc.id === input.accountId) {
        return {
          ...acc,
          balance: Number((acc.balance - input.amount).toFixed(2)),
          lastUpdatedTimestamp: input.timestamp,
        };
      }
      if (input.autoDeductCashStash && acc.accountType === 'CASH' && acc.id !== input.accountId) {
        return acc;
      }
      return acc;
    }

    if (input.type === 'CREDIT') {
      if (acc.id === input.accountId) {
        return {
          ...acc,
          balance: Number((acc.balance + input.amount).toFixed(2)),
          lastUpdatedTimestamp: input.timestamp,
        };
      }
      return acc;
    }

    // SELF_TRANSFER
    if (acc.id === input.accountId) {
      return {
        ...acc,
        balance: Number((acc.balance - input.amount).toFixed(2)),
        lastUpdatedTimestamp: input.timestamp,
      };
    }
    if (input.toAccountId && acc.id === input.toAccountId) {
      return {
        ...acc,
        balance: Number((acc.balance + input.amount).toFixed(2)),
        lastUpdatedTimestamp: input.timestamp,
      };
    }
    return acc;
  });

  return {
    accounts: nextAccounts,
    transactions: [newTx, ...transactions].sort((a, b) => b.timestamp - a.timestamp),
    transaction: newTx,
  };
}

/**
 * 1-tap account reassignment on any Ledger item.
 * Reverses the balance delta on the old account and applies it to the new account.
 */
export function reassignTransactionAccount(params: {
  transactionId: string;
  newAccountId: string;
  autoReassignFuture: boolean;
  accounts: AccountProfile[];
  transactions: Transaction[];
  merchantAccountOverrides: MerchantAccountOverride[];
}): {
  accounts: AccountProfile[];
  transactions: Transaction[];
  merchantAccountOverrides: MerchantAccountOverride[];
} {
  const {
    transactionId,
    newAccountId,
    autoReassignFuture,
    accounts,
    transactions,
    merchantAccountOverrides,
  } = params;

  const targetTx = transactions.find((t) => t.id === transactionId);
  if (!targetTx || targetTx.accountId === newAccountId) {
    return { accounts, transactions, merchantAccountOverrides };
  }

  const oldAccountId = targetTx.accountId;

  const nextAccounts = accounts.map((acc) => {
    if (acc.id === oldAccountId) {
      // Reverse original impact
      const reversed =
        targetTx.type === 'CREDIT'
          ? acc.balance - targetTx.amount
          : acc.balance + targetTx.amount;
      return { ...acc, balance: Number(reversed.toFixed(2)) };
    }
    if (acc.id === newAccountId) {
      // Apply impact to newly selected account
      const applied =
        targetTx.type === 'CREDIT'
          ? acc.balance + targetTx.amount
          : acc.balance - targetTx.amount;
      return { ...acc, balance: Number(applied.toFixed(2)) };
    }
    return acc;
  });

  const nextTransactions = transactions.map((tx) =>
    tx.id === transactionId ? { ...tx, accountId: newAccountId } : tx
  );

  let nextOverrides = merchantAccountOverrides;
  if (autoReassignFuture && targetTx.merchant.trim().length > 0) {
    const token = targetTx.merchant.split(/\s+/)[0].toLowerCase();
    const filtered = merchantAccountOverrides.filter(
      (o) => o.merchantPattern.toLowerCase() !== token
    );
    nextOverrides = [...filtered, { merchantPattern: token, accountId: newAccountId }];
  }

  return {
    accounts: nextAccounts,
    transactions: nextTransactions,
    merchantAccountOverrides: nextOverrides,
  };
}

/**
 * Step 2 ("Sync SMS") Checkpoint Ledger validation:
 * - Queries SMS inbox for latest balance/mini-statement SMS from the bank's sender ID
 * - Verifies SMS date is TODAY and sms.timestamp > profile.lastSyncedSmsTimestamp
 * - If not, instructs user to call the bank again first.
 */
export function validateAndSyncBankSms(
  profile: AccountProfile,
  smsInbox: SmsInboxMessage[],
  now: number = Date.now()
): SyncValidationResult {
  if (!profile.senderId) {
    return {
      ok: false,
      reason: 'NO_SMS_FOUND',
      message: `${profile.name} is an offline wallet and does not sync via bank SMS.`,
    };
  }

  const matchingSms = smsInbox
    .filter((sms) => sms.senderId.toUpperCase().includes(profile.senderId!.toUpperCase()))
    .sort((a, b) => b.timestamp - a.timestamp);

  if (matchingSms.length === 0) {
    return {
      ok: false,
      reason: 'CALL_BANK_FIRST',
      message: `No balance SMS found from ${profile.senderId}. Tap "Call Bank" first to request a fresh balance SMS, then tap "Sync SMS".`,
    };
  }

  const latest = matchingSms[0];
  const isToday = isSameLocalDay(latest.timestamp, now);
  const isNewerThanLastSync = latest.timestamp > profile.lastSyncedSmsTimestamp;

  if (!isToday || !isNewerThanLastSync) {
    return {
      ok: false,
      reason: 'CALL_BANK_FIRST',
      message: `Latest ${profile.shortName} SMS is stale (${
        !isToday ? 'not from today' : 'already synced'
      }). Please tap "Call Bank" first to receive a fresh balance SMS today, then run "Sync SMS" again.`,
    };
  }

  const parsedBalance = extractAvailableBalance(latest.body);
  if (parsedBalance === null) {
    return {
      ok: false,
      reason: 'CALL_BANK_FIRST',
      message: `Latest SMS from ${profile.senderId} did not include a readable Avl Bal figure. Please call the bank again first.`,
    };
  }

  return {
    ok: true,
    message: `Checkpoint synced ${profile.shortName} to ${parsedBalance.toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
    })}.`,
    newBalance: parsedBalance,
    syncedTimestamp: latest.timestamp,
    smsMessage: latest,
  };
}

/**
 * Computes Ledger summary totals strictly excluding SELF_TRANSFER transactions.
 */
export function computeLedgerSummary(transactions: Transaction[]): {
  spent: number;
  income: number;
  savedPercent: number;
} {
  let spent = 0;
  let income = 0;

  for (const tx of transactions) {
    if (tx.type === 'SELF_TRANSFER') continue;
    if (tx.type === 'DEBIT') spent += tx.amount;
    if (tx.type === 'CREDIT') income += tx.amount;
  }

  const savedPercent =
    income > 0 ? Math.max(0, Math.round(((income - spent) / income) * 100)) : 0;

  return {
    spent: Number(spent.toFixed(2)),
    income: Number(income.toFixed(2)),
    savedPercent,
  };
}

export function computeNetLiquidity(accounts: AccountProfile[]): number {
  return Number(accounts.reduce((sum, acc) => sum + acc.balance, 0).toFixed(2));
}
