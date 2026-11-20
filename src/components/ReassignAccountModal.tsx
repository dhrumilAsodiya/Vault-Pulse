import React, { useEffect, useState } from 'react';
import { ArrowLeftRight, Check, Wand2, X } from 'lucide-react';
import { formatINR } from '../domain/formatters';
import { AccountProfile, Transaction } from '../domain/types';
import { AccountIcon } from './CategoryIcon';

interface ReassignAccountModalProps {
  transaction: Transaction | null;
  accounts: AccountProfile[];
  onClose: () => void;
  onConfirm: (params: {
    transactionId: string;
    newAccountId: string;
    autoReassignFuture: boolean;
  }) => void;
}

export function ReassignAccountModal({
  transaction,
  accounts,
  onClose,
  onConfirm,
}: ReassignAccountModalProps) {
  const [selectedAccountId, setSelectedAccountId] = useState<string>('');
  const [autoReassignFuture, setAutoReassignFuture] = useState<boolean>(true);

  useEffect(() => {
    if (transaction) {
      setSelectedAccountId(transaction.accountId);
      setAutoReassignFuture(true);
    }
  }, [transaction]);

  if (!transaction) return null;

  const merchantToken = transaction.merchant.split(/\s+/)[0] || transaction.merchant;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="reassign-title"
    >
      <div className="w-full sm:max-w-md rounded-t-3xl sm:rounded-3xl bg-[#1a1c1e] border border-white/[0.08] p-5 sm:p-6 space-y-5">
        <div className="w-10 h-1 rounded-full bg-white/20 mx-auto sm:hidden" />

        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 id="reassign-title" className="text-lg font-bold text-white">
              Reassign Account
            </h2>
            <p className="text-xs text-white/55 mt-0.5">
              {transaction.merchant} ·{' '}
              <span className="tnum font-semibold text-white/80">
                {formatINR(transaction.amount)}
              </span>
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Reassign Account dialog"
            className="w-9 h-9 rounded-full bg-[#2d3036] hover:bg-[#38393c] text-white/80 flex items-center justify-center"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Account List */}
        <div className="space-y-2.5" role="radiogroup" aria-label="Select target account">
          {accounts.map((acc) => {
            const isCurrent = acc.id === transaction.accountId;
            const isSelected = acc.id === selectedAccountId;

            return (
              <button
                key={acc.id}
                type="button"
                role="radio"
                aria-checked={isSelected}
                onClick={() => setSelectedAccountId(acc.id)}
                className={`w-full rounded-2xl p-3.5 flex items-center justify-between gap-3 border transition-colors text-left ${
                  isSelected
                    ? 'bg-[#222428] border-[#4de082]/60'
                    : 'bg-[#222428] border-white/[0.05] hover:bg-[#2d3036]'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <AccountIcon
                    accentColor={acc.accentColor}
                    accountType={acc.accountType}
                  />
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-white truncate">
                        {acc.shortName}{' '}
                        {acc.maskedDigits ? `•••• ${acc.maskedDigits}` : ''}
                      </span>
                      {isCurrent && (
                        <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-[#2d3036] text-white/70">
                          Current
                        </span>
                      )}
                      {isSelected && !isCurrent && (
                        <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-[#4de082]/15 text-[#4de082]">
                          Selected
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-[#4de082] tnum mt-0.5">
                      Available: {formatINR(acc.balance)}
                    </div>
                  </div>
                </div>

                <span
                  className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
                    isSelected
                      ? 'bg-[#4de082] text-[#00210c]'
                      : 'bg-[#2d3036] border border-white/15'
                  }`}
                >
                  {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                </span>
              </button>
            );
          })}
        </div>

        {/* Auto-reassign future matches */}
        <div className="rounded-2xl bg-[#121316] border border-white/[0.06] p-4 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-[#2dd4bf]/15 text-[#2dd4bf] flex items-center justify-center shrink-0">
              <Wand2 className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <div className="text-sm font-semibold text-white">
                Auto-reassign future matches
              </div>
              <div className="text-xs text-white/50 truncate">
                Apply rule to all upcoming &apos;{merchantToken}&apos; entries
              </div>
            </div>
          </div>

          <button
            type="button"
            role="switch"
            aria-checked={autoReassignFuture}
            aria-label="Auto-reassign future matches"
            onClick={() => setAutoReassignFuture((v) => !v)}
            className={`w-12 h-7 rounded-full p-1 transition-colors shrink-0 ${
              autoReassignFuture ? 'bg-[#4de082]' : 'bg-[#343538]'
            }`}
          >
            <span
              className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                autoReassignFuture ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {/* Actions */}
        <div className="grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={onClose}
            className="h-12 rounded-2xl bg-[#222428] hover:bg-[#2d3036] text-sm font-semibold text-white transition-colors"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => {
              onConfirm({
                transactionId: transaction.id,
                newAccountId: selectedAccountId,
                autoReassignFuture,
              });
              onClose();
            }}
            className="h-12 rounded-2xl bg-[#6dfe9c] hover:bg-[#4de082] text-[#00210c] text-sm font-bold flex items-center justify-center gap-2 transition-colors"
          >
            <ArrowLeftRight className="w-4 h-4" />
            <span>Confirm</span>
          </button>
        </div>
      </div>
    </div>
  );
}
