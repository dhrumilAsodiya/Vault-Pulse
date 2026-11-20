import React, { useMemo, useState } from 'react';
import {
  ArrowDown,
  ArrowLeftRight,
  ArrowUp,
  Check,
  Plus,
  Search,
  X,
} from 'lucide-react';
import {
  formatINR,
  formatTimeShort,
  getDateGroupLabel,
} from '../domain/formatters';
import { computeLedgerSummary } from '../domain/ledgerEngine';
import { AccountProfile, Transaction, TransactionType } from '../domain/types';
import { CategoryIcon } from './CategoryIcon';

type LedgerFilter = 'ALL' | TransactionType;

interface LedgerViewProps {
  transactions: Transaction[];
  accounts: AccountProfile[];
  onOpenReassign: (tx: Transaction) => void;
  onOpenAddTransaction: () => void;
}

export function LedgerView({
  transactions,
  accounts,
  onOpenReassign,
  onOpenAddTransaction,
}: LedgerViewProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filter, setFilter] = useState<LedgerFilter>('ALL');

  const accountMap = useMemo(() => {
    const map = new Map<string, AccountProfile>();
    for (const acc of accounts) map.set(acc.id, acc);
    return map;
  }, [accounts]);

  const summary = useMemo(() => computeLedgerSummary(transactions), [transactions]);

  const filteredTransactions = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return transactions
      .filter((tx) => {
        if (filter !== 'ALL' && tx.type !== filter) return false;
        if (!q) return true;
        const acc = accountMap.get(tx.accountId);
        return (
          tx.merchant.toLowerCase().includes(q) ||
          tx.category.toLowerCase().includes(q) ||
          (acc?.name.toLowerCase().includes(q) ?? false) ||
          (acc?.maskedDigits.includes(q) ?? false)
        );
      })
      .sort((a, b) => b.timestamp - a.timestamp);
  }, [transactions, filter, searchQuery, accountMap]);

  // Group transactions by local day
  const groupedDays = useMemo(() => {
    const groups: {
      dayKey: string;
      primary: string;
      secondary: string;
      items: Transaction[];
    }[] = [];

    const now = Date.now();
    for (const tx of filteredTransactions) {
      const d = new Date(tx.timestamp);
      const dayKey = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
      let group = groups.find((g) => g.dayKey === dayKey);
      if (!group) {
        const labels = getDateGroupLabel(tx.timestamp, now);
        group = { dayKey, primary: labels.primary, secondary: labels.secondary, items: [] };
        groups.push(group);
      }
      group.items.push(tx);
    }
    return groups;
  }, [filteredTransactions]);

  const filterOptions: {
    id: LedgerFilter;
    label: string;
    icon: React.ReactNode;
  }[] = [
    { id: 'ALL', label: 'All', icon: <Check className="w-3.5 h-3.5" /> },
    { id: 'DEBIT', label: 'Debit', icon: <ArrowDown className="w-3.5 h-3.5 text-[#f87171]" /> },
    { id: 'CREDIT', label: 'Credit', icon: <ArrowUp className="w-3.5 h-3.5 text-[#4de082]" /> },
    {
      id: 'SELF_TRANSFER',
      label: 'Self-Transfer',
      icon: <ArrowLeftRight className="w-3.5 h-3.5 text-[#2dd4bf]" />,
    },
  ];

  return (
    <div className="space-y-5">
      {/* Search + Add Action */}
      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-white/45 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label="Search transactions, merchants, or accounts"
            placeholder="Search transactions, merchants, accounts..."
            className="w-full h-12 pl-11 pr-10 rounded-full bg-[#1a1c1e] border border-white/[0.06] text-sm text-white placeholder:text-white/40 focus:outline-none focus:border-[#818cf8]"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              aria-label="Clear search query"
              className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full text-white/50 hover:text-white flex items-center justify-center"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        <button
          type="button"
          onClick={onOpenAddTransaction}
          className="h-12 px-4 rounded-full bg-[#4de082] hover:bg-[#6dfe9c] text-[#0b0c0e] font-bold text-sm inline-flex items-center gap-1.5 transition-colors shrink-0 whitespace-nowrap"
        >
          <Plus className="w-4 h-4 stroke-[2.5]" />
          <span className="hidden sm:inline">New Entry</span>
        </button>
      </div>

      {/* Filter Chips */}
      <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Filter transactions by type">
        {filterOptions.map((opt) => {
          const active = filter === opt.id;
          return (
            <button
              key={opt.id}
              type="button"
              onClick={() => setFilter(opt.id)}
              aria-pressed={active}
              className={`min-h-[40px] px-4 rounded-full text-xs font-semibold inline-flex items-center gap-1.5 transition-colors whitespace-nowrap ${
                active
                  ? 'bg-[#818cf8] text-[#0b0c0e]'
                  : 'bg-[#1a1c1e] hover:bg-[#2d3036] border border-white/[0.06] text-white/80'
              }`}
            >
              {opt.icon}
              <span>{opt.label}</span>
            </button>
          );
        })}
      </div>

      {/* Summary Strip (Self-Transfers Excluded) */}
      <section
        aria-label="Income and Expense Summary excluding Self-Transfers"
        className="rounded-2xl bg-[#1a1c1e] border border-white/[0.06] px-5 py-4 grid grid-cols-3 divide-x divide-white/[0.06]"
      >
        <div className="pr-3">
          <div className="text-xs text-white/55 flex items-center gap-1">
            <ArrowDown className="w-3 h-3 text-[#f87171]" />
            <span>Spent</span>
          </div>
          <div className="mt-1 text-base sm:text-lg font-bold text-white tnum">
            {formatINR(summary.spent, { showDecimals: false })}
          </div>
        </div>

        <div className="px-4">
          <div className="text-xs text-white/55 flex items-center gap-1">
            <ArrowUp className="w-3 h-3 text-[#4de082]" />
            <span>Income</span>
          </div>
          <div className="mt-1 text-base sm:text-lg font-bold text-[#4de082] tnum">
            {formatINR(summary.income, { showDecimals: false, sign: 'plus' })}
          </div>
        </div>

        <div className="pl-4 text-right">
          <div className="text-xs text-white/55">Saved</div>
          <div className="mt-1 text-base sm:text-lg font-bold text-[#2dd4bf] tnum">
            {summary.savedPercent}%
          </div>
        </div>
      </section>

      {/* Date-Grouped Transaction Feed */}
      {groupedDays.length === 0 ? (
        <div className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-10 text-center space-y-3">
          <p className="text-base font-semibold text-white">No matching transactions</p>
          <p className="text-sm text-white/55">
            Try clearing your search filter or add a new transaction entry.
          </p>
          <button
            type="button"
            onClick={() => {
              setFilter('ALL');
              setSearchQuery('');
            }}
            className="min-h-[40px] px-5 rounded-full bg-[#2d3036] hover:bg-[#38393c] text-xs font-semibold text-white transition-colors"
          >
            Reset Filters
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {groupedDays.map((group) => (
            <section key={group.dayKey} className="space-y-2.5">
              <div className="flex items-baseline justify-between px-1">
                <div className="flex items-baseline gap-2">
                  <h3 className="text-base font-bold text-white">{group.primary}</h3>
                  <span className="text-xs text-white/50">{group.secondary}</span>
                </div>
                <span className="text-xs text-white/45 tnum">
                  {group.items.length} {group.items.length === 1 ? 'entry' : 'entries'}
                </span>
              </div>

              <div className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] divide-y divide-white/[0.05] overflow-hidden">
                {group.items.map((tx) => {
                  const fromAcc = accountMap.get(tx.accountId);
                  const toAcc = tx.toAccountId ? accountMap.get(tx.toAccountId) : undefined;

                  const accountBadgeText =
                    tx.type === 'SELF_TRANSFER' && toAcc
                      ? `${fromAcc?.shortName.split(' ')[0] ?? 'Acc'} → ${toAcc.shortName.split(' ')[0]}`
                      : fromAcc?.maskedDigits
                      ? `${fromAcc.shortName.split(' ')[0]} ••••${fromAcc.maskedDigits}`
                      : fromAcc?.shortName ?? 'Account';

                  const amountColor =
                    tx.type === 'CREDIT'
                      ? 'text-[#4de082]'
                      : tx.type === 'DEBIT'
                      ? 'text-[#f87171]'
                      : 'text-[#e3e2e6]';

                  const formattedAmount =
                    tx.type === 'CREDIT'
                      ? formatINR(tx.amount, { sign: 'plus' })
                      : tx.type === 'DEBIT'
                      ? formatINR(tx.amount, { sign: 'minus' })
                      : formatINR(tx.amount);

                  return (
                    <div
                      key={tx.id}
                      data-testid={`ledger-row-${tx.id}`}
                      className="p-4 flex items-center justify-between gap-3 hover:bg-white/[0.02] transition-colors"
                    >
                      <div className="flex items-center gap-3.5 min-w-0">
                        <CategoryIcon category={tx.category} />

                        <div className="min-w-0 space-y-1">
                          <div className="text-sm sm:text-base font-semibold text-white truncate">
                            {tx.merchant}
                          </div>

                          <div className="flex flex-wrap items-center gap-2">
                            {/* 1-tap Account Reassignment Trigger */}
                            <button
                              type="button"
                              onClick={() => onOpenReassign(tx)}
                              aria-label={`Reassign account for ${tx.merchant}`}
                              className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#2d3036] hover:bg-[#38393c] text-[11px] font-semibold text-white/85 transition-colors whitespace-nowrap"
                              title="Tap to reassign account"
                            >
                              <span className="tnum">{accountBadgeText}</span>
                              <ArrowLeftRight className="w-3 h-3 text-[#818cf8]" />
                            </button>

                            {tx.statusLabel && tx.type !== 'SELF_TRANSFER' && (
                              <span className="text-[11px] font-medium text-[#2dd4bf]">
                                {tx.statusLabel}
                              </span>
                            )}
                          </div>

                          <div className="text-xs text-white/50 flex items-center gap-1.5">
                            <span>{formatTimeShort(tx.timestamp)}</span>
                            <span aria-hidden="true">·</span>
                            <span className="text-white/65">{tx.category}</span>
                          </div>
                        </div>
                      </div>

                      <div className={`text-sm sm:text-base font-bold tnum whitespace-nowrap ${amountColor}`}>
                        {formattedAmount}
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
