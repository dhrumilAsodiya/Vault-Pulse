export type TransactionType = 'DEBIT' | 'CREDIT' | 'SELF_TRANSFER';

export type TransactionSource = 'MANUAL' | 'SMS_AUTO' | 'UPI_LISTENER' | 'BANK_SYNC';

export type CategoryId =
  | 'Food & Dining'
  | 'Fuel & Transport'
  | 'Shopping & Retail'
  | 'Groceries'
  | 'Subscriptions'
  | 'Utilities'
  | 'Entertainment'
  | 'Income / Salary'
  | 'Transfer'
  | 'Other';

export interface AccountProfile {
  id: string;
  name: string;
  shortName: string;
  accountType: 'BANK' | 'CASH';
  maskedDigits: string; // e.g. '4892' or '' for Cash Wallet
  tagLabel: string; // e.g. 'State Bank', 'Primary UPI', 'Discretionary', 'Offline Safe'
  balance: number;
  isPrimaryUpi: boolean;
  upiAlias?: string;
  senderId?: string; // e.g. 'SBIINB', 'HDFCBK', 'ICICIB'
  missedCallNumber?: string; // e.g. '+91 92237 66666'
  lastSyncedSmsTimestamp: number;
  lastUpdatedTimestamp: number;
  pendingNewSmsCount: number;
  accentColor: 'indigo' | 'teal' | 'emerald' | 'amber';
}

export interface Transaction {
  id: string;
  type: TransactionType;
  amount: number;
  merchant: string;
  category: CategoryId;
  accountId: string;
  toAccountId?: string; // used for SELF_TRANSFER
  timestamp: number;
  source: TransactionSource;
  statusLabel?: string; // e.g. 'SMS Auto', 'Settled', 'Recurring', 'UPI Capture'
  avlBalSnapshot?: number;
  taxDeductible?: boolean;
  autoDeductCashStash?: boolean;
  note?: string;
  rawRef?: string;
}

export interface KeywordRule {
  id: string;
  keywords: string[]; // e.g. ['Swiggy', 'Zomato', 'UberEats']
  category: CategoryId;
}

export interface MerchantAccountOverride {
  merchantPattern: string;
  accountId: string;
}

export interface SmsInboxMessage {
  id: string;
  senderId: string;
  body: string;
  timestamp: number;
  read: boolean;
}

export interface UpiNotificationPayload {
  id: string;
  packageName: string;
  appName: string;
  title: string;
  body: string;
  timestamp: number;
}

export interface ParsedFinancialEvent {
  type: 'DEBIT' | 'CREDIT';
  amount: number;
  merchant: string;
  maskedDigits?: string;
  avlBal?: number;
  senderOrApp: string;
  source: 'SMS_AUTO' | 'UPI_LISTENER';
  timestamp: number;
  rawBody: string;
}

export interface CaptureProcessResult {
  status: 'BLOCKED' | 'IGNORED' | 'MERGED' | 'INSERTED';
  reason: string;
  transaction?: Transaction;
}

export interface SyncValidationResult {
  ok: boolean;
  reason?: 'NO_SMS_FOUND' | 'CALL_BANK_FIRST';
  message: string;
  newBalance?: number;
  syncedTimestamp?: number;
  smsMessage?: SmsInboxMessage;
}

export interface VaultState {
  accounts: AccountProfile[];
  transactions: Transaction[];
  keywordRules: KeywordRule[];
  merchantAccountOverrides: MerchantAccountOverride[];
  smsInbox: SmsInboxMessage[];
  autoParseUpiSms: boolean;
  autoBackupEnabled: boolean;
  biometricLockEnabled: boolean;
  monthlyBudgetLimit: number;
}
