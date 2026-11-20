import test from 'node:test';
import assert from 'node:assert/strict';
import { parseBankSms, parseUpiNotification } from './smsParser';
import {
  addManualTransaction,
  computeLedgerSummary,
  processParsedEvent,
  reassignTransactionAccount,
  validateAndSyncBankSms,
} from './ledgerEngine';
import { createSeedState } from './seedData';

test('SMS parser blocks OTP and failed/declined/unsuccessful/requested keywords', () => {
  const blockedSamples = [
    'Your OTP for Rs 2,500 transaction at Amazon is 492184.',
    'Payment of INR 489.00 at Swiggy failed due to bank timeout.',
    'Card XX1278 transaction of Rs.1200 was declined.',
    'UPI transfer of Rs 500 was unsuccessful.',
    'PhonePe: Rahul requested Rs 800 from you.',
  ];

  for (const body of blockedSamples) {
    const res = parseBankSms({
      id: 's1',
      senderId: 'HDFCBK',
      body,
      timestamp: Date.now(),
      read: false,
    });
    assert.ok(res.blockedBy, `Expected "${body}" to be blocked`);
    assert.equal(res.event, undefined);
  }
});

test('SMS parser extracts DEBIT, amount, masked digits, merchant, and Avl Bal', () => {
  const res = parseBankSms({
    id: 's2',
    senderId: 'AD-SBIINB',
    body: 'A/c XX4892 debited by Rs.1,450.00 at Blue Tokai Coffee on 24-Oct. Avl Bal: INR 2,44,370.50',
    timestamp: Date.now(),
    read: false,
  });

  assert.ok(res.event);
  assert.equal(res.event.type, 'DEBIT');
  assert.equal(res.event.amount, 1450);
  assert.equal(res.event.maskedDigits, '4892');
  assert.equal(res.event.merchant, 'Blue Tokai Coffee');
  assert.equal(res.event.avlBal, 244370.5);
});

test('UPI NotificationListener routes whitelisted packages to Default Primary Profile and rejects unknown packages', () => {
  const rejected = parseUpiNotification({
    id: 'u1',
    packageName: 'com.whatsapp',
    appName: 'WhatsApp',
    title: 'Message',
    body: 'Paid Rs 500 to Bob',
    timestamp: Date.now(),
  });
  assert.equal(rejected.event, undefined);

  const accepted = parseUpiNotification({
    id: 'u2',
    packageName: 'com.google.android.apps.nbu.paisa.user',
    appName: 'Google Pay',
    title: 'Paid ₹320.00',
    body: 'Paid to Zepto Grocery via UPI',
    timestamp: Date.now(),
  });

  assert.ok(accepted.event);
  assert.equal(accepted.event.type, 'DEBIT');
  assert.equal(accepted.event.amount, 320);

  const seed = createSeedState();
  const processed = processParsedEvent({
    event: accepted.event,
    accounts: seed.accounts,
    transactions: seed.transactions,
    keywordRules: seed.keywordRules,
    merchantAccountOverrides: seed.merchantAccountOverrides,
  });

  assert.equal(processed.result.status, 'INSERTED');
  assert.equal(processed.result.transaction?.accountId, 'acc_hdfc'); // Default Primary UPI
  assert.equal(processed.result.transaction?.category, 'Groceries'); // Keyword rule match
});

test('Deduplication merges same amount & type within ±3 minutes instead of creating duplicate', () => {
  const now = Date.now();
  const seed = createSeedState(now);
  const initialCount = seed.transactions.length;

  // Existing seed tx_1 is DEBIT ₹489 on HDFC around now - 25m
  const existingTx1 = seed.transactions.find((t) => t.id === 'tx_1')!;
  const upiRes = parseUpiNotification({
    id: 'u_dup',
    packageName: 'com.phonepe.app',
    appName: 'PhonePe',
    title: 'Paid ₹489',
    body: 'Paid to Swiggy Food Delivery',
    timestamp: existingTx1.timestamp + 60_000, // +1 min (< 3 min window)
  });

  assert.ok(upiRes.event);
  const merged = processParsedEvent({
    event: upiRes.event,
    accounts: seed.accounts,
    transactions: seed.transactions,
    keywordRules: seed.keywordRules,
    merchantAccountOverrides: seed.merchantAccountOverrides,
  });

  assert.equal(merged.result.status, 'MERGED');
  assert.equal(merged.transactions.length, initialCount);

  // Outside ±3 minutes creates a separate entry
  const separateRes = parseUpiNotification({
    id: 'u_new',
    packageName: 'com.phonepe.app',
    appName: 'PhonePe',
    title: 'Paid ₹489',
    body: 'Paid to Swiggy Food Delivery',
    timestamp: existingTx1.timestamp + 5 * 60_000, // +5 min (> 3 min window)
  });
  assert.ok(separateRes.event);
  const inserted = processParsedEvent({
    event: separateRes.event,
    accounts: seed.accounts,
    transactions: seed.transactions,
    keywordRules: seed.keywordRules,
    merchantAccountOverrides: seed.merchantAccountOverrides,
  });

  assert.equal(inserted.result.status, 'INSERTED');
  assert.equal(inserted.transactions.length, initialCount + 1);
});

test('Self-Transfer is excluded from spent/income totals and 1-tap reassignment conserves net liquidity', () => {
  const seed = createSeedState();
  const beforeSummary = computeLedgerSummary(seed.transactions);

  const afterTransfer = addManualTransaction({
    accounts: seed.accounts,
    transactions: seed.transactions,
    input: {
      type: 'SELF_TRANSFER',
      amount: 5000,
      merchant: 'Self-Transfer (SBI → Cash)',
      category: 'Transfer',
      accountId: 'acc_sbi',
      toAccountId: 'acc_cash',
      timestamp: Date.now(),
    },
  });

  const afterSummary = computeLedgerSummary(afterTransfer.transactions);
  assert.equal(afterSummary.spent, beforeSummary.spent);
  assert.equal(afterSummary.income, beforeSummary.income);

  // 1-tap reassignment of tx_1 (₹489 DEBIT) from HDFC to SBI
  const hdfcBefore = seed.accounts.find((a) => a.id === 'acc_hdfc')!.balance;
  const sbiBefore = seed.accounts.find((a) => a.id === 'acc_sbi')!.balance;

  const reassigned = reassignTransactionAccount({
    transactionId: 'tx_1',
    newAccountId: 'acc_sbi',
    autoReassignFuture: true,
    accounts: seed.accounts,
    transactions: seed.transactions,
    merchantAccountOverrides: seed.merchantAccountOverrides,
  });

  const hdfcAfter = reassigned.accounts.find((a) => a.id === 'acc_hdfc')!.balance;
  const sbiAfter = reassigned.accounts.find((a) => a.id === 'acc_sbi')!.balance;
  assert.equal(hdfcAfter, Number((hdfcBefore + 489).toFixed(2)));
  assert.equal(sbiAfter, Number((sbiBefore - 489).toFixed(2)));
  assert.equal(reassigned.merchantAccountOverrides.length, 1);
});

test('2-Step Bank Sync requires SMS from TODAY with timestamp > lastSyncedSmsTimestamp', () => {
  const now = Date.now();
  const seed = createSeedState(now);

  // SBI has already synced at sbiLastSync -> no newer SMS -> must fail with CALL_BANK_FIRST
  const sbiProfile = seed.accounts.find((a) => a.id === 'acc_sbi')!;
  const sbiFail = validateAndSyncBankSms(sbiProfile, seed.smsInbox, now);
  assert.equal(sbiFail.ok, false);
  assert.equal(sbiFail.reason, 'CALL_BANK_FIRST');

  // ICICI has an SMS from yesterday -> must fail with CALL_BANK_FIRST
  const iciciProfile = seed.accounts.find((a) => a.id === 'acc_icici')!;
  const iciciFail = validateAndSyncBankSms(iciciProfile, seed.smsInbox, now);
  assert.equal(iciciFail.ok, false);
  assert.equal(iciciFail.reason, 'CALL_BANK_FIRST');

  // HDFC has a fresh unread SMS from today newer than hdfcLastSync -> must succeed
  const hdfcProfile = seed.accounts.find((a) => a.id === 'acc_hdfc')!;
  const hdfcPass = validateAndSyncBankSms(hdfcProfile, seed.smsInbox, now);
  assert.equal(hdfcPass.ok, true);
  assert.equal(hdfcPass.newBalance, 188430);
});
