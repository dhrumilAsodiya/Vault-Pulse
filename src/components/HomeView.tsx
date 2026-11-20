import React from 'react';
import {
  ChevronRight,
  Landmark,
  MoreVertical,
  Phone,
  Plus,
  RefreshCw,
  Scale,
  ShieldCheck,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import { formatINR, formatRelativeTime, splitINRParts } from '../domain/formatters';
import { AccountProfile } from '../domain/types';
import { AccountIcon } from './CategoryIcon';

interface HomeViewProps {
  accounts: AccountProfile[];
  netLiquidity: number;
  monthlyIncome: number;
  monthlySpent: number;
  onCallBank: (account: AccountProfile) => void;
  onSyncSms: (account: AccountProfile) => void;
  onReconcileCash: (account: AccountProfile) => void;
  onLogCashExpense: (account: AccountProfile) => void;
  onOpenAddTransaction: () => void;
  onOpenManageAccounts: () => void;
}

export function HomeView({
  accounts,
  netLiquidity,
  monthlyIncome,
  monthlySpent,
  onCallBank,
  onSyncSms,
  onReconcileCash,
  onLogCashExpense,
  onOpenAddTransaction,
  onOpenManageAccounts,
}: HomeViewProps) {
  const liquidityParts = splitINRParts(netLiquidity);
  const bankAccounts = accounts.filter((a) => a.accountType === 'BANK');
  const cashAccount = accounts.find((a) => a.accountType === 'CASH');
  const netSaved = Math.max(0, monthlyIncome - monthlySpent);
  const latestSyncTs = Math.max(
    0,
    ...bankAccounts.map((a) => a.lastSyncedSmsTimestamp || 0)
  );

  return (
    <div className="space-y-6">
      {/* Hero Net Liquidity Card */}
      <section
        aria-label="Total Net Liquidity Summary"
        className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-7 relative overflow-hidden"
        style={{ boxShadow: '0 8px 32px rgba(45, 212, 191, 0.09)' }}
      >
        <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2 text-xs font-semibold tracking-wide text-white/65">
            <ShieldCheck className="w-4 h-4 text-[#2dd4bf]" />
            <span>Total Net Liquidity</span>
          </div>
          <span className="text-xs font-medium text-[#4de082] whitespace-nowrap">
            SMS Auto-Sync · {formatRelativeTime(latestSyncTs)}
          </span>
        </div>

        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-5">
          <div>
            <div className="tnum flex items-baseline">
              <span className="text-3xl sm:text-4xl md:text-5xl font-bold tracking-tight text-white">
                {liquidityParts.whole}
              </span>
              <span className="text-xl sm:text-2xl font-semibold text-white/45">
                {liquidityParts.decimals}
              </span>
            </div>

            <div className="mt-2.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
              <span className="inline-flex items-center gap-1 text-[#4de082] font-semibold tnum">
                <TrendingUp className="w-3.5 h-3.5" />
                +4.8% this month
              </span>
              <span className="text-white/40" aria-hidden="true">·</span>
              <span className="text-white/65 tnum">
                {formatINR(netSaved, { showDecimals: false })} net saved this cycle
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2.5 sm:gap-3 md:min-w-[360px]">
            <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-2.5 sm:p-3.5 flex items-center gap-2.5 sm:gap-3">
              <div className="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0">
                <Landmark className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <div className="text-xs font-semibold text-white tnum">
                  {bankAccounts.length} Banks
                </div>
                <div className="text-[11px] text-white/55 truncate">
                  {bankAccounts.map((b) => b.shortName.split(' ')[0]).join(' · ')}
                </div>
              </div>
            </div>

            <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-2.5 sm:p-3.5 flex items-center gap-2.5 sm:gap-3">
              <div className="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-[#4ade80]/15 text-[#4ade80] flex items-center justify-center shrink-0">
                <Wallet className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <div className="text-xs font-semibold text-white">
                  Cash Stash
                </div>
                <div className="text-[11px] text-white/55 tnum">
                  {cashAccount ? formatINR(cashAccount.balance, { showDecimals: false }) : '₹0'}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Mobile Primary Add Transaction CTA (non-overlapping) */}
        <div className="mt-4 md:hidden">
          <button
            type="button"
            onClick={onOpenAddTransaction}
            className="w-full h-11 rounded-2xl bg-[#4de082] hover:bg-[#6dfe9c] text-[#0b0c0e] font-bold text-sm flex items-center justify-center gap-2 transition-colors whitespace-nowrap"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            <span>Add Transaction</span>
          </button>
        </div>
      </section>

      {/* Accounts Section Header */}
      <section aria-label="Your Accounts and Profiles" className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <h2 className="text-lg font-bold text-white tracking-tight">
              Your Accounts &amp; Profiles
            </h2>
            <span className="text-xs font-semibold text-white/60 tnum">
              · {accounts.length}
            </span>
          </div>
          <button
            type="button"
            onClick={onOpenManageAccounts}
            className="min-h-[40px] px-3 rounded-xl text-sm font-semibold text-[#818cf8] hover:bg-[#818cf8]/10 inline-flex items-center gap-1 transition-colors whitespace-nowrap"
          >
            <span>Manage</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>

        {/* Account Cards Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {accounts.map((account) => {
            const isBank = account.accountType === 'BANK';
            const syncLabel = isBank
              ? account.pendingNewSmsCount > 0
                ? `SMS ${formatRelativeTime(account.lastSyncedSmsTimestamp)}`
                : `Synced ${formatRelativeTime(account.lastSyncedSmsTimestamp)}`
              : `Updated ${formatRelativeTime(account.lastUpdatedTimestamp)}`;

            return (
              <article
                key={account.id}
                data-testid={`account-card-${account.id}`}
                className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 flex flex-col justify-between gap-4"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3.5 min-w-0">
                    <AccountIcon
                      accentColor={account.accentColor}
                      accountType={account.accountType}
                    />
                    <div className="min-w-0">
                      <h3 className="text-base font-semibold text-white truncate">
                        {account.name}
                      </h3>
                      <div className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-white/55">
                        {account.maskedDigits ? (
                          <span className="tnum font-mono">•••• {account.maskedDigits}</span>
                        ) : (
                          <span>Petty Cash / Desk</span>
                        )}
                        <span aria-hidden="true">·</span>
                        <span
                          className={
                            account.isPrimaryUpi
                              ? 'text-[#2dd4bf] font-semibold'
                              : 'text-white/60'
                          }
                        >
                          {account.tagLabel}
                        </span>
                      </div>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={onOpenManageAccounts}
                    aria-label={`Manage ${account.name}`}
                    className="min-h-[40px] min-w-[40px] rounded-xl text-white/45 hover:text-white hover:bg-white/[0.05] flex items-center justify-center transition-colors shrink-0"
                  >
                    <MoreVertical className="w-4 h-4" />
                  </button>
                </div>

                <div className="flex items-end justify-between gap-3 pt-1">
                  <div>
                    <div className="text-[11px] font-medium tracking-wider text-white/45 uppercase">
                      {isBank ? 'Available Balance' : 'Cash In Hand'}
                    </div>
                    <div className="mt-1 text-2xl font-bold text-white tnum">
                      {formatINR(account.balance)}
                    </div>
                  </div>

                  <div
                    className={`text-xs whitespace-nowrap font-medium ${
                      account.pendingNewSmsCount > 0
                        ? 'text-[#fbbf24]'
                        : isBank
                        ? 'text-[#4de082]'
                        : 'text-white/55'
                    }`}
                  >
                    {syncLabel}
                  </div>
                </div>

                {/* 2-Step Bank Sync or Cash Wallet Actions */}
                {isBank ? (
                  <div className="grid grid-cols-2 gap-3 pt-1">
                    <button
                      type="button"
                      onClick={() => onCallBank(account)}
                      className="min-h-[44px] px-3 rounded-2xl bg-[#222428] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-white flex items-center justify-center gap-2 transition-colors whitespace-nowrap"
                    >
                      <Phone className="w-3.5 h-3.5 text-[#2dd4bf]" />
                      <span>Call Bank</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => onSyncSms(account)}
                      className="min-h-[44px] px-3 rounded-2xl bg-[#222428] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-white flex items-center justify-center gap-2 transition-colors whitespace-nowrap"
                    >
                      <RefreshCw className="w-3.5 h-3.5 text-[#818cf8]" />
                      <span>Sync SMS</span>
                      {account.pendingNewSmsCount > 0 && (
                        <span className="px-1.5 py-0.5 rounded-full bg-[#818cf8] text-[#0b0c0e] text-[10px] font-bold tnum leading-none">
                          {account.pendingNewSmsCount} new
                        </span>
                      )}
                    </button>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 gap-3 pt-1">
                    <button
                      type="button"
                      onClick={() => onReconcileCash(account)}
                      className="min-h-[44px] px-3 rounded-2xl bg-[#222428] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-white flex items-center justify-center gap-2 transition-colors whitespace-nowrap"
                    >
                      <Scale className="w-3.5 h-3.5 text-[#2dd4bf]" />
                      <span>Reconcile</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => onLogCashExpense(account)}
                      className="min-h-[44px] px-3 rounded-2xl bg-[#222428] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-white flex items-center justify-center gap-2 transition-colors whitespace-nowrap"
                    >
                      <Plus className="w-3.5 h-3.5 text-[#4de082]" />
                      <span>Log Cash Exp</span>
                    </button>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      </section>
    </div>
  );
}
