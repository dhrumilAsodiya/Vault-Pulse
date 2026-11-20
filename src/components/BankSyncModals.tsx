import React, { useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Phone,
  RefreshCw,
  Scale,
  X,
} from 'lucide-react';
import { formatINR } from '../domain/formatters';
import { AccountProfile, SyncValidationResult } from '../domain/types';

interface CallBankModalProps {
  account: AccountProfile | null;
  onClose: () => void;
  onSimulateMissedCallSms: (account: AccountProfile, updatedBalance: number) => void;
}

export function CallBankModal({
  account,
  onClose,
  onSimulateMissedCallSms,
}: CallBankModalProps) {
  const [balanceInput, setBalanceInput] = useState('');

  if (!account) return null;

  const targetBal =
    balanceInput.trim() !== '' && Number.isFinite(Number(balanceInput))
      ? Number(balanceInput)
      : account.balance;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="call-bank-title"
    >
      <div className="w-full max-w-md rounded-3xl bg-[#1a1c1e] border border-white/[0.08] p-6 space-y-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-[#2dd4bf]/15 text-[#2dd4bf] flex items-center justify-center">
              <Phone className="w-5 h-5" />
            </div>
            <div>
              <h2 id="call-bank-title" className="text-lg font-bold text-white">
                Step 1: Call {account.shortName}
              </h2>
              <p className="text-xs text-white/55">
                Launches Intent.ACTION_DIAL (Dual SIM safe)
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Call Bank modal"
            className="w-9 h-9 rounded-full bg-[#2d3036] text-white/80 flex items-center justify-center"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="rounded-2xl bg-[#222428] border border-white/[0.06] p-4 space-y-2">
          <div className="text-xs text-white/55">Missed-Call Balance Number</div>
          <div className="text-xl font-bold text-[#818cf8] tnum">
            {account.missedCallNumber || 'Not configured'}
          </div>
          <div className="text-xs text-white/50">
            Sender ID expected: <span className="font-mono text-white/80">{account.senderId}</span>
          </div>
        </div>

        <div>
          <label
            htmlFor="simulated-bank-bal"
            className="block text-xs font-semibold text-white/65 mb-1.5"
          >
            Bank SMS Avl Bal response (optional override for testing)
          </label>
          <input
            id="simulated-bank-bal"
            type="number"
            step="0.01"
            value={balanceInput}
            onChange={(e) => setBalanceInput(e.target.value)}
            placeholder={`Default: ${account.balance.toFixed(2)}`}
            className="w-full h-11 px-3.5 rounded-2xl bg-[#222428] border border-white/[0.06] text-sm text-white tnum focus:outline-none focus:border-[#2dd4bf]"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <a
            href={`tel:${(account.missedCallNumber ?? '').replace(/\s+/g, '')}`}
            className="h-11 rounded-2xl bg-[#2d3036] hover:bg-[#38393c] text-xs font-semibold text-white flex items-center justify-center gap-2"
          >
            <Phone className="w-4 h-4 text-[#2dd4bf]" />
            <span>Open System Dialer</span>
          </a>

          <button
            type="button"
            onClick={() => {
              onSimulateMissedCallSms(account, targetBal);
              onClose();
            }}
            className="h-11 rounded-2xl bg-[#4de082] hover:bg-[#6dfe9c] text-[#00210c] text-xs font-bold flex items-center justify-center gap-2"
          >
            <CheckCircle2 className="w-4 h-4" />
            <span>Deliver Bank SMS</span>
          </button>
        </div>
      </div>
    </div>
  );
}

interface SyncResultModalProps {
  account: AccountProfile | null;
  result: SyncValidationResult | null;
  onClose: () => void;
  onSwitchToCallBank: (account: AccountProfile) => void;
}

export function SyncResultModal({
  account,
  result,
  onClose,
  onSwitchToCallBank,
}: SyncResultModalProps) {
  if (!account || !result) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="sync-result-title"
    >
      <div className="w-full max-w-md rounded-3xl bg-[#1a1c1e] border border-white/[0.08] p-6 space-y-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div
              className={`w-11 h-11 rounded-xl flex items-center justify-center ${
                result.ok
                  ? 'bg-[#4de082]/15 text-[#4de082]'
                  : 'bg-[#fbbf24]/15 text-[#fbbf24]'
              }`}
            >
              {result.ok ? (
                <RefreshCw className="w-5 h-5" />
              ) : (
                <AlertTriangle className="w-5 h-5" />
              )}
            </div>
            <div>
              <h2 id="sync-result-title" className="text-lg font-bold text-white">
                {result.ok ? 'Checkpoint Synced' : 'Call Bank First'}
              </h2>
              <p className="text-xs text-white/55">{account.name}</p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close Sync Status modal"
            className="w-9 h-9 rounded-full bg-[#2d3036] text-white/80 flex items-center justify-center"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-sm leading-relaxed text-white/80">{result.message}</p>

        {result.smsMessage && (
          <div className="rounded-2xl bg-[#222428] border border-white/[0.06] p-3.5 text-xs font-mono text-white/75">
            {result.smsMessage.body}
          </div>
        )}

        <div className="flex justify-end gap-3">
          {!result.ok && account.accountType === 'BANK' && (
            <button
              type="button"
              onClick={() => {
                onClose();
                onSwitchToCallBank(account);
              }}
              className="h-11 px-4 rounded-2xl bg-[#2dd4bf] text-[#003731] text-xs font-bold flex items-center gap-2"
            >
              <Phone className="w-4 h-4" />
              <span>Call Bank Now</span>
            </button>
          )}
          <button
            type="button"
            onClick={onClose}
            className="h-11 px-5 rounded-2xl bg-[#2d3036] hover:bg-[#38393c] text-xs font-semibold text-white"
          >
            Dismiss
          </button>
        </div>
      </div>
    </div>
  );
}

interface ReconcileCashModalProps {
  account: AccountProfile | null;
  onClose: () => void;
  onReconcile: (accountId: string, newBalance: number) => void;
}

export function ReconcileCashModal({
  account,
  onClose,
  onReconcile,
}: ReconcileCashModalProps) {
  const [cashDraft, setCashDraft] = useState(account ? account.balance.toFixed(2) : '');

  if (!account) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="reconcile-cash-title"
    >
      <div className="w-full max-w-md rounded-3xl bg-[#1a1c1e] border border-white/[0.08] p-6 space-y-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-[#2dd4bf]/15 text-[#2dd4bf] flex items-center justify-center">
              <Scale className="w-5 h-5" />
            </div>
            <div>
              <h2 id="reconcile-cash-title" className="text-lg font-bold text-white">
                Reconcile Cash Wallet
              </h2>
              <p className="text-xs text-white/55">
                Current recorded: {formatINR(account.balance)}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Reconcile modal"
            className="w-9 h-9 rounded-full bg-[#2d3036] text-white/80 flex items-center justify-center"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            const val = Number(cashDraft);
            if (Number.isFinite(val) && val >= 0) {
              onReconcile(account.id, val);
              onClose();
            }
          }}
          className="space-y-4"
        >
          <div>
            <label
              htmlFor="cash-reconcile-input"
              className="block text-xs font-semibold text-white/65 mb-1.5"
            >
              Actual Physical Cash Count (₹)
            </label>
            <input
              id="cash-reconcile-input"
              type="number"
              step="0.01"
              min="0"
              required
              value={cashDraft}
              onChange={(e) => setCashDraft(e.target.value)}
              className="w-full h-12 px-4 rounded-2xl bg-[#222428] border border-white/[0.06] text-lg font-bold text-white tnum focus:outline-none focus:border-[#4de082]"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={onClose}
              className="h-11 rounded-2xl bg-[#222428] text-xs font-semibold text-white"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="h-11 rounded-2xl bg-[#4de082] text-[#00210c] text-xs font-bold"
            >
              Update Cash Balance
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
