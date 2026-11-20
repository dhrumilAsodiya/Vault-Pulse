import React, { useEffect, useState } from 'react';
import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  Calendar,
  CheckCircle2,
  Clock,
  Landmark,
  PiggyBank,
  Receipt,
  Store,
  X,
} from 'lucide-react';
import { formatINR } from '../domain/formatters';
import { AccountProfile, CategoryId, TransactionType } from '../domain/types';
import { CategoryIcon } from './CategoryIcon';

interface AddTransactionModalProps {
  isOpen: boolean;
  accounts: AccountProfile[];
  initialAccountId?: string;
  initialType?: TransactionType;
  onClose: () => void;
  onSave: (input: {
    type: TransactionType;
    amount: number;
    merchant: string;
    category: CategoryId;
    accountId: string;
    toAccountId?: string;
    timestamp: number;
    taxDeductible: boolean;
    autoDeductCashStash: boolean;
  }) => void;
}

const CATEGORIES: { id: CategoryId; short: string }[] = [
  { id: 'Food & Dining', short: 'Dining' },
  { id: 'Shopping & Retail', short: 'Shopping' },
  { id: 'Fuel & Transport', short: 'Travel' },
  { id: 'Groceries', short: 'Groceries' },
  { id: 'Utilities', short: 'Utilities' },
  { id: 'Subscriptions', short: 'Subs' },
  { id: 'Entertainment', short: 'Media' },
  { id: 'Income / Salary', short: 'Salary' },
  { id: 'Other', short: 'More' },
];

const QUICK_CHIPS = [100, 500, 1000, 5000];

export function AddTransactionModal({
  isOpen,
  accounts,
  initialAccountId,
  initialType = 'DEBIT',
  onClose,
  onSave,
}: AddTransactionModalProps) {
  const [txType, setTxType] = useState<TransactionType>(initialType);
  const [amountStr, setAmountStr] = useState('1450.00');
  const [accountId, setAccountId] = useState(initialAccountId ?? accounts[0]?.id ?? '');
  const [toAccountId, setToAccountId] = useState(accounts[1]?.id ?? accounts[0]?.id ?? '');
  const [category, setCategory] = useState<CategoryId>('Food & Dining');
  const [merchant, setMerchant] = useState('');
  const [dateValue, setDateValue] = useState(() => new Date().toISOString().slice(0, 10));
  const [timeValue, setTimeValue] = useState(() => {
    const d = new Date();
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  });
  const [autoDeductCashStash, setAutoDeductCashStash] = useState(true);
  const [taxDeductible, setTaxDeductible] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setTxType(initialType);
      setAccountId(initialAccountId ?? accounts[0]?.id ?? '');
      const alt = accounts.find((a) => a.id !== (initialAccountId ?? accounts[0]?.id));
      setToAccountId(alt?.id ?? accounts[0]?.id ?? '');
      setCategory(initialType === 'CREDIT' ? 'Income / Salary' : 'Food & Dining');
      setMerchant('');
      setAmountStr('1450.00');
      setErrorMsg(null);
    }
  }, [isOpen, initialAccountId, initialType, accounts]);

  if (!isOpen) return null;

  const selectedAccount = accounts.find((a) => a.id === accountId) ?? accounts[0];

  const handleQuickAdd = (delta: number) => {
    const current = Number(amountStr.replace(/,/g, '')) || 0;
    const next = current + delta;
    setAmountStr(next.toFixed(2));
    setErrorMsg(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const numericAmount = Number(amountStr.replace(/,/g, ''));
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setErrorMsg('Please enter an amount greater than ₹0.');
      return;
    }

    if (txType === 'SELF_TRANSFER' && accountId === toAccountId) {
      setErrorMsg('Source and destination accounts must be different for a Self-Transfer.');
      return;
    }

    const trimmedMerchant = merchant.trim();
    if (!trimmedMerchant && txType !== 'SELF_TRANSFER') {
      setErrorMsg('Please enter a merchant or note.');
      return;
    }

    const parsedDate = new Date(`${dateValue}T${timeValue}:00`);
    const ts = Number.isNaN(parsedDate.getTime()) ? Date.now() : parsedDate.getTime();

    const fromAcc = accounts.find((a) => a.id === accountId);
    const toAcc = accounts.find((a) => a.id === toAccountId);
    const finalMerchant =
      txType === 'SELF_TRANSFER' && !trimmedMerchant
        ? `Self-Transfer (${fromAcc?.shortName.split(' ')[0]} → ${toAcc?.shortName.split(' ')[0]})`
        : trimmedMerchant;

    onSave({
      type: txType,
      amount: numericAmount,
      merchant: finalMerchant,
      category: txType === 'SELF_TRANSFER' ? 'Transfer' : category,
      accountId,
      toAccountId: txType === 'SELF_TRANSFER' ? toAccountId : undefined,
      timestamp: ts,
      taxDeductible,
      autoDeductCashStash,
    });
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="add-tx-title"
    >
      <div className="w-full sm:max-w-lg max-h-[92vh] overflow-y-auto rounded-t-3xl sm:rounded-3xl bg-[#1a1c1e] border border-white/[0.08] p-5 sm:p-6 space-y-5">
        <div className="w-10 h-1 rounded-full bg-white/20 mx-auto sm:hidden" />

        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span className="w-2.5 h-2.5 rounded-full bg-[#4de082]" />
            <h2 id="add-tx-title" className="text-lg font-bold text-white">
              Add New Transaction
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Add New Transaction dialog"
            className="w-9 h-9 rounded-full bg-[#2d3036] hover:bg-[#38393c] text-white/80 flex items-center justify-center"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Segmented Type Selector */}
          <div className="grid grid-cols-3 p-1 rounded-full bg-[#121316] border border-white/[0.06]">
            <button
              type="button"
              onClick={() => {
                setTxType('DEBIT');
                if (category === 'Income / Salary') setCategory('Food & Dining');
              }}
              className={`min-h-[40px] rounded-full text-xs font-bold flex items-center justify-center gap-1.5 transition-colors ${
                txType === 'DEBIT'
                  ? 'bg-[#ffb4ab] text-[#690005]'
                  : 'text-white/70 hover:text-white'
              }`}
            >
              <ArrowUpRight className="w-3.5 h-3.5" />
              <span>Expense</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setTxType('CREDIT');
                setCategory('Income / Salary');
              }}
              className={`min-h-[40px] rounded-full text-xs font-bold flex items-center justify-center gap-1.5 transition-colors ${
                txType === 'CREDIT'
                  ? 'bg-[#4de082] text-[#003919]'
                  : 'text-white/70 hover:text-white'
              }`}
            >
              <ArrowDownLeft className="w-3.5 h-3.5" />
              <span>Income</span>
            </button>

            <button
              type="button"
              onClick={() => setTxType('SELF_TRANSFER')}
              className={`min-h-[40px] rounded-full text-xs font-bold flex items-center justify-center gap-1.5 transition-colors ${
                txType === 'SELF_TRANSFER'
                  ? 'bg-[#2dd4bf] text-[#003731]'
                  : 'text-white/70 hover:text-white'
              }`}
            >
              <ArrowLeftRight className="w-3.5 h-3.5" />
              <span>Transfer</span>
            </button>
          </div>

          {/* Hero Amount Card */}
          <div className="rounded-2xl bg-[#222428] border border-white/[0.06] p-4 space-y-3 text-center">
            <label
              htmlFor="tx-amount-input"
              className="block text-[11px] font-semibold tracking-wider text-white/55 uppercase"
            >
              {txType === 'DEBIT'
                ? 'Debit Amount'
                : txType === 'CREDIT'
                ? 'Credit Amount'
                : 'Self-Transfer Amount'}
            </label>

            <div className="flex items-center justify-center gap-1.5">
              <span className="text-2xl font-bold text-white/60">₹</span>
              <input
                id="tx-amount-input"
                type="number"
                step="0.01"
                min="0"
                value={amountStr}
                onChange={(e) => {
                  setAmountStr(e.target.value);
                  setErrorMsg(null);
                }}
                className="w-48 bg-transparent text-4xl font-bold text-white tnum text-center focus:outline-none border-b border-transparent focus:border-[#4de082]"
              />
            </div>

            <div className="flex flex-wrap items-center justify-center gap-2">
              {QUICK_CHIPS.map((amt) => (
                <button
                  key={amt}
                  type="button"
                  onClick={() => handleQuickAdd(amt)}
                  className="px-3 py-1.5 rounded-full bg-[#2d3036] hover:bg-[#38393c] text-xs font-semibold text-white/85 tnum"
                >
                  + ₹{amt.toLocaleString('en-IN')}
                </button>
              ))}
            </div>
          </div>

          {/* Account Selector(s) */}
          <div className="space-y-3">
            <div>
              <label
                htmlFor="tx-from-account"
                className="block text-xs font-semibold text-white/65 mb-1.5"
              >
                {txType === 'CREDIT' ? 'Credited To Account' : 'Paid From Account'}
              </label>
              <div className="rounded-2xl bg-[#222428] border border-white/[0.06] p-3 flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0">
                  <Landmark className="w-4 h-4" />
                </div>
                <div className="flex-1 min-w-0">
                  <select
                    id="tx-from-account"
                    value={accountId}
                    onChange={(e) => setAccountId(e.target.value)}
                    className="w-full bg-transparent text-sm font-semibold text-white focus:outline-none"
                  >
                    {accounts.map((acc) => (
                      <option key={acc.id} value={acc.id} className="bg-[#222428] text-white">
                        {acc.shortName} {acc.maskedDigits ? `•••• ${acc.maskedDigits}` : ''}
                      </option>
                    ))}
                  </select>
                  {selectedAccount && (
                    <div className="text-xs text-white/50 tnum mt-0.5">
                      Available: {formatINR(selectedAccount.balance)}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {txType === 'SELF_TRANSFER' && (
              <div>
                <label
                  htmlFor="tx-to-account"
                  className="block text-xs font-semibold text-white/65 mb-1.5"
                >
                  Transfer To Account
                </label>
                <div className="rounded-2xl bg-[#222428] border border-white/[0.06] p-3 flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-[#2dd4bf]/15 text-[#2dd4bf] flex items-center justify-center shrink-0">
                    <ArrowLeftRight className="w-4 h-4" />
                  </div>
                  <select
                    id="tx-to-account"
                    value={toAccountId}
                    onChange={(e) => setToAccountId(e.target.value)}
                    className="flex-1 bg-transparent text-sm font-semibold text-white focus:outline-none"
                  >
                    {accounts.map((acc) => (
                      <option key={acc.id} value={acc.id} className="bg-[#222428] text-white">
                        {acc.shortName} {acc.maskedDigits ? `•••• ${acc.maskedDigits}` : ''}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            )}
          </div>

          {/* Category Horizontal Picker (hidden for Self-Transfer) */}
          {txType !== 'SELF_TRANSFER' && (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-white/65">Category</span>
                <span className="font-semibold text-[#2dd4bf]">Selected: {category}</span>
              </div>
              <div className="flex items-center gap-2 overflow-x-auto pb-1">
                {CATEGORIES.map((cat) => {
                  const active = category === cat.id;
                  return (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() => setCategory(cat.id)}
                      className={`min-w-[68px] p-2 rounded-2xl border flex flex-col items-center gap-1.5 transition-colors shrink-0 ${
                        active
                          ? 'bg-[#2dd4bf]/20 border-[#2dd4bf] text-[#2dd4bf]'
                          : 'bg-[#222428] border-white/[0.05] text-white/70 hover:bg-[#2d3036]'
                      }`}
                    >
                      <CategoryIcon category={cat.id} size="sm" />
                      <span className="text-[11px] font-semibold whitespace-nowrap">
                        {cat.short}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Merchant / Note */}
          <div>
            <label
              htmlFor="tx-merchant-input"
              className="block text-xs font-semibold text-white/65 mb-1.5"
            >
              Merchant / Note
            </label>
            <div className="relative">
              <Store className="w-4 h-4 text-white/45 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                id="tx-merchant-input"
                type="text"
                value={merchant}
                onChange={(e) => {
                  setMerchant(e.target.value);
                  setErrorMsg(null);
                }}
                placeholder={
                  txType === 'SELF_TRANSFER'
                    ? 'Optional transfer note (e.g. Monthly savings)'
                    : 'e.g. Blue Tokai Coffee'
                }
                className="w-full h-12 pl-10 pr-4 rounded-2xl bg-[#222428] border border-white/[0.06] text-sm text-white placeholder:text-white/35 focus:outline-none focus:border-[#818cf8]"
              />
            </div>
          </div>

          {/* Date & Time */}
          <div className="grid grid-cols-2 gap-3">
            <div className="relative">
              <Calendar className="w-4 h-4 text-[#2dd4bf] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="date"
                aria-label="Transaction Date"
                value={dateValue}
                onChange={(e) => setDateValue(e.target.value)}
                className="w-full h-11 pl-9 pr-3 rounded-2xl bg-[#222428] border border-white/[0.06] text-xs font-semibold text-white focus:outline-none focus:border-[#818cf8]"
              />
            </div>
            <div className="relative">
              <Clock className="w-4 h-4 text-[#2dd4bf] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="time"
                aria-label="Transaction Time"
                value={timeValue}
                onChange={(e) => setTimeValue(e.target.value)}
                className="w-full h-11 pl-9 pr-3 rounded-2xl bg-[#222428] border border-white/[0.06] text-xs font-semibold text-white focus:outline-none focus:border-[#818cf8]"
              />
            </div>
          </div>

          {/* Toggles */}
          <div className="rounded-2xl bg-[#222428] border border-white/[0.05] divide-y divide-white/[0.05]">
            <div className="p-3.5 flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-xl bg-[#4de082]/15 text-[#4de082] flex items-center justify-center shrink-0">
                  <PiggyBank className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs font-semibold text-white">
                    Auto-deduct from cash stash
                  </div>
                  <div className="text-[11px] text-white/50">
                    Protects monthly essential runway
                  </div>
                </div>
              </div>
              <button
                type="button"
                role="switch"
                aria-checked={autoDeductCashStash}
                aria-label="Auto-deduct from cash stash"
                onClick={() => setAutoDeductCashStash((v) => !v)}
                className={`w-11 h-6 rounded-full p-0.5 transition-colors shrink-0 ${
                  autoDeductCashStash ? 'bg-[#4de082]' : 'bg-[#343538]'
                }`}
              >
                <span
                  className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                    autoDeductCashStash ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>

            <div className="p-3.5 flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-xl bg-[#2d3036] text-white/75 flex items-center justify-center shrink-0">
                  <Receipt className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs font-semibold text-white">
                    Mark as tax deductible
                  </div>
                  <div className="text-[11px] text-white/50">
                    Section 80C tagged tracker
                  </div>
                </div>
              </div>
              <button
                type="button"
                role="switch"
                aria-checked={taxDeductible}
                aria-label="Mark as tax deductible"
                onClick={() => setTaxDeductible((v) => !v)}
                className={`w-11 h-6 rounded-full p-0.5 transition-colors shrink-0 ${
                  taxDeductible ? 'bg-[#4de082]' : 'bg-[#343538]'
                }`}
              >
                <span
                  className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                    taxDeductible ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>
          </div>

          {errorMsg && (
            <div
              role="alert"
              className="rounded-xl bg-[#f87171]/15 border border-[#f87171]/40 px-3.5 py-2.5 text-xs font-semibold text-[#ffb4ab]"
            >
              {errorMsg}
            </div>
          )}

          {/* Save CTA */}
          <button
            type="submit"
            className="w-full h-12 rounded-2xl bg-[#6dfe9c] hover:bg-[#4de082] text-[#00210c] font-bold text-sm flex items-center justify-center gap-2 transition-colors"
          >
            <CheckCircle2 className="w-4 h-4" />
            <span>Save Transaction</span>
          </button>
        </form>
      </div>
    </div>
  );
}
