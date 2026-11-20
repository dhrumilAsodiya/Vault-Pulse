/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useMemo, useState } from 'react';
import { AddTransactionModal } from './components/AddTransactionModal';
import { AnalyticsView } from './components/AnalyticsView';
import { AppShell, NavTab } from './components/AppShell';
import {
  CallBankModal,
  ReconcileCashModal,
  SyncResultModal,
} from './components/BankSyncModals';
import { BiometricLockScreen } from './components/BiometricLockScreen';
import { CaptureSimulatorModal } from './components/CaptureSimulatorModal';
import { HomeView } from './components/HomeView';
import { LedgerView } from './components/LedgerView';
import { ReassignAccountModal } from './components/ReassignAccountModal';
import { SettingsView } from './components/SettingsView';
import {
  decryptVaultBackup,
  EncryptedBackupEnvelope,
  encryptVaultBackup,
} from './domain/cryptoBackup';
import {
  addManualTransaction,
  computeLedgerSummary,
  computeNetLiquidity,
  processParsedEvent,
  reassignTransactionAccount,
  validateAndSyncBankSms,
} from './domain/ledgerEngine';
import { createSeedState } from './domain/seedData';
import { parseBankSms, parseUpiNotification } from './domain/smsParser';
import {
  AccountProfile,
  CaptureProcessResult,
  CategoryId,
  SmsInboxMessage,
  SyncValidationResult,
  Transaction,
  TransactionType,
  VaultState,
} from './domain/types';

const STORAGE_KEY = 'vaultpulse_state_v1';

function loadInitialVaultState(): VaultState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as VaultState;
      if (Array.isArray(parsed.accounts) && Array.isArray(parsed.transactions)) {
        return parsed;
      }
    }
  } catch {
    // Fallback to seed state if localStorage is unavailable or corrupt
  }
  return createSeedState();
}

export default function App() {
  const [vault, setVault] = useState<VaultState>(loadInitialVaultState);
  const [activeTab, setActiveTab] = useState<NavTab>('home');

  // Biometric lock state (bypasses automatically if ?autostart=1)
  const [isLocked, setIsLocked] = useState<boolean>(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('autostart') === '1') return false;
    return loadInitialVaultState().biometricLockEnabled;
  });

  // Modal states
  const [addTxConfig, setAddTxConfig] = useState<{
    open: boolean;
    initialAccountId?: string;
    initialType?: TransactionType;
  }>({ open: false });

  const [reassignTx, setReassignTx] = useState<Transaction | null>(null);
  const [callBankAccount, setCallBankAccount] = useState<AccountProfile | null>(null);
  const [syncDialog, setSyncDialog] = useState<{
    account: AccountProfile | null;
    result: SyncValidationResult | null;
  }>({ account: null, result: null });
  const [reconcileAccount, setReconcileAccount] = useState<AccountProfile | null>(null);
  const [captureSimOpen, setCaptureSimOpen] = useState(false);

  // Persist to localStorage whenever vault changes
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(vault));
    } catch {
      // Ignore quota errors in restricted sandboxes
    }
  }, [vault]);

  const netLiquidity = useMemo(
    () => computeNetLiquidity(vault.accounts),
    [vault.accounts]
  );

  const ledgerSummary = useMemo(
    () => computeLedgerSummary(vault.transactions),
    [vault.transactions]
  );

  const unreadSmsCount = useMemo(
    () => vault.smsInbox.filter((s) => !s.read).length,
    [vault.smsInbox]
  );

  // Handlers
  const handleSaveManualTransaction = (input: {
    type: TransactionType;
    amount: number;
    merchant: string;
    category: CategoryId;
    accountId: string;
    toAccountId?: string;
    timestamp: number;
    taxDeductible: boolean;
    autoDeductCashStash: boolean;
  }) => {
    setVault((prev) => {
      const next = addManualTransaction({
        accounts: prev.accounts,
        transactions: prev.transactions,
        input,
      });
      return {
        ...prev,
        accounts: next.accounts,
        transactions: next.transactions,
      };
    });
  };

  const handleConfirmReassign = (params: {
    transactionId: string;
    newAccountId: string;
    autoReassignFuture: boolean;
  }) => {
    setVault((prev) => {
      const next = reassignTransactionAccount({
        ...params,
        accounts: prev.accounts,
        transactions: prev.transactions,
        merchantAccountOverrides: prev.merchantAccountOverrides,
      });
      return {
        ...prev,
        accounts: next.accounts,
        transactions: next.transactions,
        merchantAccountOverrides: next.merchantAccountOverrides,
      };
    });
  };

  const handleSimulateMissedCallSms = (
    account: AccountProfile,
    updatedBalance: number
  ) => {
    const now = Date.now();
    const newSms: SmsInboxMessage = {
      id: `sms_${now}`,
      senderId: `VM-${account.senderId ?? 'BANK'}`,
      body: `${account.shortName}: Balance for A/c XX${account.maskedDigits} as on today is Avl Bal: INR ${updatedBalance.toLocaleString(
        'en-IN',
        { minimumFractionDigits: 2, maximumFractionDigits: 2 }
      )}.`,
      timestamp: now,
      read: false,
    };

    setVault((prev) => ({
      ...prev,
      smsInbox: [newSms, ...prev.smsInbox],
      accounts: prev.accounts.map((acc) =>
        acc.id === account.id
          ? { ...acc, pendingNewSmsCount: acc.pendingNewSmsCount + 1 }
          : acc
      ),
    }));
  };

  const handleSyncSms = (account: AccountProfile) => {
    const now = Date.now();
    const result = validateAndSyncBankSms(account, vault.smsInbox, now);

    if (result.ok && result.newBalance !== undefined && result.syncedTimestamp !== undefined) {
      setVault((prev) => ({
        ...prev,
        accounts: prev.accounts.map((acc) =>
          acc.id === account.id
            ? {
                ...acc,
                balance: result.newBalance!,
                lastSyncedSmsTimestamp: result.syncedTimestamp!,
                lastUpdatedTimestamp: now,
                pendingNewSmsCount: 0,
              }
            : acc
        ),
        smsInbox: prev.smsInbox.map((sms) =>
          sms.id === result.smsMessage?.id ? { ...sms, read: true } : sms
        ),
      }));
    }

    setSyncDialog({ account, result });
  };

  const handleReconcileCash = (accountId: string, newBalance: number) => {
    const now = Date.now();
    setVault((prev) => ({
      ...prev,
      accounts: prev.accounts.map((acc) =>
        acc.id === accountId
          ? { ...acc, balance: Number(newBalance.toFixed(2)), lastUpdatedTimestamp: now }
          : acc
      ),
    }));
  };

  const handleSimulateSmsCapture = (
    senderId: string,
    body: string
  ): CaptureProcessResult => {
    const now = Date.now();
    const sms: SmsInboxMessage = {
      id: `sms_${now}`,
      senderId,
      body,
      timestamp: now,
      read: true,
    };

    const parsed = parseBankSms(sms);
    if (parsed.blockedBy) {
      setVault((prev) => ({ ...prev, smsInbox: [sms, ...prev.smsInbox] }));
      return {
        status: 'BLOCKED',
        reason: `Blocked by negative keyword "${parsed.blockedBy}" (OTP/Failed filter).`,
      };
    }

    if (!parsed.event) {
      setVault((prev) => ({ ...prev, smsInbox: [sms, ...prev.smsInbox] }));
      return {
        status: 'IGNORED',
        reason: parsed.ignoredReason ?? 'Could not parse SMS transaction.',
      };
    }

    const processed = processParsedEvent({
      event: parsed.event,
      accounts: vault.accounts,
      transactions: vault.transactions,
      keywordRules: vault.keywordRules,
      merchantAccountOverrides: vault.merchantAccountOverrides,
    });

    setVault((prev) => ({
      ...prev,
      smsInbox: [sms, ...prev.smsInbox],
      accounts: processed.accounts,
      transactions: processed.transactions,
    }));

    return processed.result;
  };

  const handleSimulateUpiCapture = (
    packageName: string,
    title: string,
    body: string
  ): CaptureProcessResult => {
    const now = Date.now();
    const parsed = parseUpiNotification({
      id: `upi_${now}`,
      packageName,
      appName: packageName,
      title,
      body,
      timestamp: now,
    });

    if (parsed.blockedBy) {
      return {
        status: 'BLOCKED',
        reason: `Blocked by negative keyword "${parsed.blockedBy}".`,
      };
    }

    if (!parsed.event) {
      return {
        status: 'IGNORED',
        reason: parsed.ignoredReason ?? 'Ignored UPI notification.',
      };
    }

    const processed = processParsedEvent({
      event: parsed.event,
      accounts: vault.accounts,
      transactions: vault.transactions,
      keywordRules: vault.keywordRules,
      merchantAccountOverrides: vault.merchantAccountOverrides,
    });

    setVault((prev) => ({
      ...prev,
      accounts: processed.accounts,
      transactions: processed.transactions,
    }));

    return processed.result;
  };

  const handleExportCsvReport = () => {
    const headers = ['Date', 'Type', 'Merchant', 'Category', 'Account', 'Amount_INR', 'Source'];
    const accMap = new Map(vault.accounts.map((a) => [a.id, a.shortName]));
    const rows = vault.transactions.map((tx) => [
      new Date(tx.timestamp).toISOString(),
      tx.type,
      `"${tx.merchant.replace(/"/g, '""')}"`,
      `"${tx.category}"`,
      `"${accMap.get(tx.accountId) ?? tx.accountId}"`,
      tx.amount.toFixed(2),
      tx.source,
    ]);
    const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `vaultpulse-ledger-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleExportEncryptedBackup = async (passphrase: string) => {
    const envelope = await encryptVaultBackup(vault, passphrase);
    const blob = new Blob([JSON.stringify(envelope, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `vaultpulse-backup-${new Date().toISOString().slice(0, 10)}.enc.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImportEncryptedBackup = async (file: File, passphrase: string) => {
    const text = await file.text();
    const envelope = JSON.parse(text) as EncryptedBackupEnvelope;
    const restored = await decryptVaultBackup(envelope, passphrase);
    setVault(restored);
  };

  if (isLocked) {
    return <BiometricLockScreen onUnlock={() => setIsLocked(false)} />;
  }

  return (
    <AppShell
      activeTab={activeTab}
      onTabChange={setActiveTab}
      unreadSmsCount={unreadSmsCount}
      onOpenCaptureSimulator={() => setCaptureSimOpen(true)}
      onOpenAddTransaction={() => setAddTxConfig({ open: true, initialType: 'DEBIT' })}
      onLockVault={() => setIsLocked(true)}
    >
      {activeTab === 'home' && (
        <HomeView
          accounts={vault.accounts}
          netLiquidity={netLiquidity}
          monthlyIncome={ledgerSummary.income}
          monthlySpent={ledgerSummary.spent}
          onCallBank={(acc) => setCallBankAccount(acc)}
          onSyncSms={handleSyncSms}
          onReconcileCash={(acc) => setReconcileAccount(acc)}
          onLogCashExpense={(acc) =>
            setAddTxConfig({
              open: true,
              initialAccountId: acc.id,
              initialType: 'DEBIT',
            })
          }
          onOpenAddTransaction={() =>
            setAddTxConfig({ open: true, initialType: 'DEBIT' })
          }
          onOpenManageAccounts={() => setActiveTab('settings')}
        />
      )}

      {activeTab === 'ledger' && (
        <LedgerView
          transactions={vault.transactions}
          accounts={vault.accounts}
          onOpenReassign={(tx) => setReassignTx(tx)}
          onOpenAddTransaction={() =>
            setAddTxConfig({ open: true, initialType: 'DEBIT' })
          }
        />
      )}

      {activeTab === 'analytics' && (
        <AnalyticsView
          transactions={vault.transactions}
          accounts={vault.accounts}
          monthlyBudgetLimit={vault.monthlyBudgetLimit}
          onExportCsvReport={handleExportCsvReport}
        />
      )}

      {activeTab === 'settings' && (
        <SettingsView
          accounts={vault.accounts}
          keywordRules={vault.keywordRules}
          autoParseUpiSms={vault.autoParseUpiSms}
          autoBackupEnabled={vault.autoBackupEnabled}
          biometricLockEnabled={vault.biometricLockEnabled}
          onSetPrimaryUpiAccount={(accountId) =>
            setVault((prev) => ({
              ...prev,
              accounts: prev.accounts.map((acc) => ({
                ...acc,
                isPrimaryUpi: acc.id === accountId,
              })),
            }))
          }
          onToggleAutoParseUpi={(enabled) =>
            setVault((prev) => ({ ...prev, autoParseUpiSms: enabled }))
          }
          onToggleAutoBackup={(enabled) =>
            setVault((prev) => ({ ...prev, autoBackupEnabled: enabled }))
          }
          onToggleBiometricLock={(enabled) =>
            setVault((prev) => ({ ...prev, biometricLockEnabled: enabled }))
          }
          onUpdateDialCode={(accountId, newNumber) =>
            setVault((prev) => ({
              ...prev,
              accounts: prev.accounts.map((acc) =>
                acc.id === accountId ? { ...acc, missedCallNumber: newNumber } : acc
              ),
            }))
          }
          onAddCustomBank={(input) =>
            setVault((prev) => ({
              ...prev,
              accounts: [
                ...prev.accounts,
                {
                  id: `acc_${Date.now()}`,
                  name: `${input.name} Bank Account`,
                  shortName: input.name,
                  accountType: 'BANK',
                  maskedDigits: input.maskedDigits,
                  tagLabel: 'Custom Bank',
                  balance: 0,
                  isPrimaryUpi: false,
                  senderId: input.senderId,
                  missedCallNumber: input.missedCallNumber,
                  lastSyncedSmsTimestamp: 0,
                  lastUpdatedTimestamp: Date.now(),
                  pendingNewSmsCount: 0,
                  accentColor: 'indigo',
                },
              ],
            }))
          }
          onCallBank={(acc) => setCallBankAccount(acc)}
          onAddKeywordRule={(keywords, category) =>
            setVault((prev) => ({
              ...prev,
              keywordRules: [
                ...prev.keywordRules,
                { id: `rule_${Date.now()}`, keywords, category },
              ],
            }))
          }
          onUpdateKeywordRule={(ruleId, keywords, category) =>
            setVault((prev) => ({
              ...prev,
              keywordRules: prev.keywordRules.map((r) =>
                r.id === ruleId ? { ...r, keywords, category } : r
              ),
            }))
          }
          onDeleteKeywordRule={(ruleId) =>
            setVault((prev) => ({
              ...prev,
              keywordRules: prev.keywordRules.filter((r) => r.id !== ruleId),
            }))
          }
          onExportEncryptedBackup={handleExportEncryptedBackup}
          onImportEncryptedBackup={handleImportEncryptedBackup}
          onOpenCaptureSimulator={() => setCaptureSimOpen(true)}
        />
      )}

      {/* Modals */}
      <AddTransactionModal
        isOpen={addTxConfig.open}
        accounts={vault.accounts}
        initialAccountId={addTxConfig.initialAccountId}
        initialType={addTxConfig.initialType}
        onClose={() => setAddTxConfig({ open: false })}
        onSave={handleSaveManualTransaction}
      />

      <ReassignAccountModal
        transaction={reassignTx}
        accounts={vault.accounts}
        onClose={() => setReassignTx(null)}
        onConfirm={handleConfirmReassign}
      />

      <CallBankModal
        account={callBankAccount}
        onClose={() => setCallBankAccount(null)}
        onSimulateMissedCallSms={handleSimulateMissedCallSms}
      />

      <SyncResultModal
        account={syncDialog.account}
        result={syncDialog.result}
        onClose={() => setSyncDialog({ account: null, result: null })}
        onSwitchToCallBank={(acc) => setCallBankAccount(acc)}
      />

      <ReconcileCashModal
        account={reconcileAccount}
        onClose={() => setReconcileAccount(null)}
        onReconcile={handleReconcileCash}
      />

      <CaptureSimulatorModal
        isOpen={captureSimOpen}
        smsInbox={vault.smsInbox}
        onClose={() => setCaptureSimOpen(false)}
        onSimulateSms={handleSimulateSmsCapture}
        onSimulateUpi={handleSimulateUpiCapture}
      />
    </AppShell>
  );
}
