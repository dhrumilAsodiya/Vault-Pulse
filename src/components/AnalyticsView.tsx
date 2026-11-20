import React, { useMemo, useState } from 'react';
import {
  BarChart2,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Download,
  Gauge,
  Landmark,
  List,
  PieChart,
  TrendingDown,
} from 'lucide-react';
import {
  computeAnalyticsSnapshot,
  TimeframeGranularity,
} from '../domain/analyticsEngine';
import { formatINR, splitINRParts } from '../domain/formatters';
import { AccountProfile, Transaction } from '../domain/types';
import { CategoryIcon } from './CategoryIcon';

interface AnalyticsViewProps {
  transactions: Transaction[];
  accounts: AccountProfile[];
  monthlyBudgetLimit: number;
  onExportCsvReport: () => void;
}

const GRANULARITIES: TimeframeGranularity[] = ['Week', 'Month', 'Quarter', 'Year'];

export function AnalyticsView({
  transactions,
  accounts,
  monthlyBudgetLimit,
  onExportCsvReport,
}: AnalyticsViewProps) {
  const [granularity, setGranularity] = useState<TimeframeGranularity>('Month');
  const [monthOffset, setMonthOffset] = useState(0);
  const [breakdownView, setBreakdownView] = useState<'donut' | 'list'>('donut');
  const [selectedBarKey, setSelectedBarKey] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  const snapshot = useMemo(
    () =>
      computeAnalyticsSnapshot({
        transactions,
        accounts,
        granularity,
        monthOffset,
        monthlyBudgetLimit,
      }),
    [transactions, accounts, granularity, monthOffset, monthlyBudgetLimit]
  );

  const monthLabel = useMemo(() => {
    const d = new Date();
    d.setMonth(d.getMonth() + monthOffset);
    return d.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
  }, [monthOffset]);

  const spendParts = splitINRParts(snapshot.totalSpend);

  const activeBar =
    snapshot.dailyBars.find((b) => b.dateKey === selectedBarKey) ??
    snapshot.peakBar ??
    snapshot.dailyBars[0];

  const maxBarAmount = Math.max(1, ...snapshot.dailyBars.map((b) => b.amount));

  // Build SVG Donut arcs
  const donutSegments = useMemo(() => {
    const radius = 64;
    const circumference = 2 * Math.PI * radius;
    let offset = 0;

    return snapshot.categories.map((cat) => {
      const fraction = snapshot.totalSpend > 0 ? cat.amount / snapshot.totalSpend : 0;
      const dashLength = Math.max(0, fraction * circumference - 3);
      const segment = {
        ...cat,
        radius,
        circumference,
        dashArray: `${dashLength} ${circumference}`,
        dashOffset: -offset,
      };
      offset += fraction * circumference;
      return segment;
    });
  }, [snapshot.categories, snapshot.totalSpend]);

  return (
    <div className="space-y-6">
      {/* Month Navigator & Granularity Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setMonthOffset((m) => m - 1)}
            aria-label="Previous period"
            className="w-10 h-10 rounded-full bg-[#1a1c1e] hover:bg-[#2d3036] border border-white/[0.06] flex items-center justify-center text-white/80"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>

          <div className="px-4 h-10 rounded-full bg-[#1a1c1e] border border-white/[0.06] inline-flex items-center gap-2 text-sm font-bold text-white whitespace-nowrap">
            <Calendar className="w-4 h-4 text-[#818cf8]" />
            <span>{monthLabel}</span>
          </div>

          <button
            type="button"
            onClick={() => setMonthOffset((m) => Math.min(0, m + 1))}
            disabled={monthOffset >= 0}
            aria-label="Next period"
            className="w-10 h-10 rounded-full bg-[#1a1c1e] hover:bg-[#2d3036] disabled:opacity-40 border border-white/[0.06] flex items-center justify-center text-white/80"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>

        <div
          className="grid grid-cols-4 p-1 rounded-full bg-[#1a1c1e] border border-white/[0.06]"
          role="tablist"
          aria-label="Timeframe granularity"
        >
          {GRANULARITIES.map((g) => {
            const active = granularity === g;
            return (
              <button
                key={g}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => setGranularity(g)}
                className={`min-h-[36px] px-3 sm:px-4 rounded-full text-xs font-semibold transition-colors whitespace-nowrap ${
                  active ? 'bg-[#818cf8] text-[#0b0c0e]' : 'text-white/65 hover:text-white'
                }`}
              >
                {g}
              </button>
            );
          })}
        </div>
      </div>

      {/* Top Row: Total Spend + Insight */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Total Spend & Budget Pacing Card */}
        <section className="lg:col-span-2 rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="text-xs font-semibold tracking-wider text-white/55 uppercase">
              Total {granularity} Spend
            </span>
            <span className="inline-flex items-center gap-1 text-[#4de082] text-xs font-semibold tnum">
              <TrendingDown className="w-3.5 h-3.5" />
              {Math.abs(snapshot.deltaVsLastPeriodPercent)}% vs last period
            </span>
          </div>

          <div>
            <div className="tnum flex items-baseline">
              <span className="text-3xl sm:text-4xl font-bold text-white">
                {spendParts.whole}
              </span>
              <span className="text-lg font-semibold text-white/45">
                {spendParts.decimals}
              </span>
            </div>
            <div className="mt-1 text-xs text-white/55 tnum">
              Budget limit: {formatINR(snapshot.budgetLimit)}
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#4de082] font-semibold tnum">
                {snapshot.pacingPercent <= 85 ? 'On track' : 'High burn'} ({snapshot.pacingPercent}% pacing)
              </span>
              <span className="text-white/65 tnum">
                {formatINR(snapshot.budgetRemaining, { showDecimals: false })} left
              </span>
            </div>
            <div className="h-2.5 w-full rounded-full bg-[#2d3036] overflow-hidden">
              <div
                className="h-full rounded-full transition-all duration-300"
                style={{
                  width: `${Math.min(100, snapshot.pacingPercent)}%`,
                  backgroundColor: snapshot.pacingPercent > 90 ? '#f87171' : '#2dd4bf',
                }}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-3.5 flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-[#2dd4bf]/15 text-[#2dd4bf] flex items-center justify-center shrink-0">
                <Gauge className="w-4 h-4" />
              </div>
              <div>
                <div className="text-[11px] text-white/55">Daily Average</div>
                <div className="text-sm font-bold text-white tnum">
                  {formatINR(snapshot.dailyAverage, { showDecimals: false })}/day
                </div>
              </div>
            </div>

            <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-3.5 flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0">
                <Calendar className="w-4 h-4" />
              </div>
              <div>
                <div className="text-[11px] text-white/55">Safe Burn Cap</div>
                <div className="text-sm font-bold text-white tnum">
                  {formatINR(snapshot.safeBurnCap, { showDecimals: false })}/day
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Deterministic Local Spend Summary */}
        <section className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 flex flex-col justify-between gap-4">
          <div className="space-y-3">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0">
                <BarChart2 className="w-4 h-4" />
              </div>
              <span className="text-xs font-bold tracking-wider text-[#bdc2ff] uppercase">
                On-Device Pacing Telemetry
              </span>
            </div>
            <p className="text-sm leading-relaxed text-white/80">
              {snapshot.insightText}
            </p>
          </div>

          <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-3.5 space-y-1">
            <div className="text-[11px] text-white/50">Total Income (Excl. Self-Transfers)</div>
            <div className="text-lg font-bold text-[#4de082] tnum">
              {formatINR(snapshot.totalIncome, { showDecimals: false, sign: 'plus' })}
            </div>
          </div>
        </section>
      </div>

      {/* Charts Grid: Spending Trends + Category Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Spending Trends Bar Chart */}
        <section className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 space-y-4">
          <div className="flex items-start justify-between gap-2">
            <div>
              <h2 className="text-base font-bold text-white">Spending Trends</h2>
              <p className="text-xs text-white/55">Daily burn across {granularity.toLowerCase()}</p>
            </div>
            {snapshot.peakBar && (
              <span className="text-xs font-semibold text-[#2dd4bf] whitespace-nowrap">
                Peak · {snapshot.peakBar.dayLabel}
              </span>
            )}
          </div>

          {activeBar && (
            <div className="rounded-2xl bg-[#222428] border border-white/[0.05] px-4 py-2.5 flex items-center justify-between text-xs">
              <span className="text-white/80 truncate">
                {activeBar.dayLabel}: {activeBar.topMerchant}
              </span>
              <span className="font-bold text-[#2dd4bf] tnum ml-2 shrink-0">
                {formatINR(activeBar.amount, { showDecimals: false })}
              </span>
            </div>
          )}

          <div className="pt-4 pb-1">
            <div className="h-44 flex items-end gap-1.5 relative">
              {/* Average dotted line */}
              <div
                className="absolute left-0 right-0 border-t border-dashed border-white/20 pointer-events-none flex justify-end"
                style={{
                  bottom: `${Math.min(92, Math.max(10, (snapshot.dailyAverage / maxBarAmount) * 100))}%`,
                }}
              >
                <span className="text-[10px] text-white/45 -mt-4 bg-[#1a1c1e] px-1 tnum">
                  Avg {formatINR(snapshot.dailyAverage, { showDecimals: false })}
                </span>
              </div>

              {snapshot.dailyBars.map((bar) => {
                const heightPct =
                  bar.amount > 0 ? Math.max(12, Math.round((bar.amount / maxBarAmount) * 100)) : 8;
                const isSelected = activeBar?.dateKey === bar.dateKey;
                return (
                  <button
                    key={bar.dateKey}
                    type="button"
                    onClick={() => setSelectedBarKey(bar.dateKey)}
                    aria-label={`${bar.dayLabel}: ${formatINR(bar.amount, { showDecimals: false })}`}
                    className="flex-1 h-full flex flex-col justify-end group"
                  >
                    <div
                      className={`w-full rounded-t-md transition-all ${
                        bar.isPeak || isSelected
                          ? 'bg-[#2dd4bf]'
                          : bar.amount > 0
                          ? 'bg-[#2d3036] group-hover:bg-[#818cf8]'
                          : 'bg-white/[0.05]'
                      }`}
                      style={{ height: `${heightPct}%` }}
                    />
                  </button>
                );
              })}
            </div>

            <div className="mt-2 flex items-center justify-between text-[11px] text-white/45 tnum">
              <span>{snapshot.dailyBars[0]?.dayLabel}</span>
              <span>{snapshot.dailyBars[Math.floor(snapshot.dailyBars.length / 2)]?.dayLabel}</span>
              <span>{snapshot.dailyBars[snapshot.dailyBars.length - 1]?.dayLabel}</span>
            </div>
          </div>
        </section>

        {/* Category Breakdown Donut + List */}
        <section className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 space-y-5">
          <div className="flex items-center justify-between gap-2">
            <div>
              <h2 className="text-base font-bold text-white">Category Breakdown</h2>
              <p className="text-xs text-white/55 tnum">
                {snapshot.categories.length} active spending classes
              </p>
            </div>

            <div className="flex items-center p-1 rounded-xl bg-[#222428] border border-white/[0.05]">
              <button
                type="button"
                onClick={() => setBreakdownView('donut')}
                aria-label="Donut chart view"
                aria-pressed={breakdownView === 'donut'}
                className={`p-2 rounded-lg transition-colors ${
                  breakdownView === 'donut'
                    ? 'bg-[#818cf8] text-[#0b0c0e]'
                    : 'text-white/60 hover:text-white'
                }`}
              >
                <PieChart className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={() => setBreakdownView('list')}
                aria-label="List breakdown view"
                aria-pressed={breakdownView === 'list'}
                className={`p-2 rounded-lg transition-colors ${
                  breakdownView === 'list'
                    ? 'bg-[#818cf8] text-[#0b0c0e]'
                    : 'text-white/60 hover:text-white'
                }`}
              >
                <List className="w-4 h-4" />
              </button>
            </div>
          </div>

          {breakdownView === 'donut' && snapshot.categories.length > 0 && (
            <div className="flex items-center justify-center py-2">
              <div className="relative w-44 h-44 flex items-center justify-center">
                <svg viewBox="0 0 160 160" className="w-full h-full -rotate-90">
                  <circle
                    cx="80"
                    cy="80"
                    r="64"
                    fill="none"
                    stroke="#2d3036"
                    strokeWidth="18"
                  />
                  {donutSegments.map((seg) => (
                    <circle
                      key={seg.category}
                      cx="80"
                      cy="80"
                      r={seg.radius}
                      fill="none"
                      stroke={seg.color}
                      strokeWidth={selectedCategory === seg.category ? '22' : '18'}
                      strokeDasharray={seg.dashArray}
                      strokeDashoffset={seg.dashOffset}
                      className="transition-all duration-300 cursor-pointer"
                      onClick={() =>
                        setSelectedCategory((prev) =>
                          prev === seg.category ? null : seg.category
                        )
                      }
                    />
                  ))}
                </svg>
                <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none">
                  <span className="text-[10px] tracking-wider uppercase text-white/50">
                    Total
                  </span>
                  <span className="text-lg font-bold text-white tnum">
                    {formatINR(snapshot.totalSpend, { showDecimals: false })}
                  </span>
                  <span className="text-[11px] font-semibold text-[#2dd4bf] tnum">
                    {snapshot.transactionCount} Items
                  </span>
                </div>
              </div>
            </div>
          )}

          <div className="space-y-2.5">
            {snapshot.categories.length === 0 ? (
              <p className="text-sm text-white/50 py-6 text-center">
                No debit transactions in this period.
              </p>
            ) : (
              snapshot.categories.map((cat) => (
                <button
                  key={cat.category}
                  type="button"
                  onClick={() =>
                    setSelectedCategory((prev) =>
                      prev === cat.category ? null : cat.category
                    )
                  }
                  className={`w-full rounded-2xl p-3 flex items-center justify-between gap-3 transition-colors text-left ${
                    selectedCategory === cat.category
                      ? 'bg-[#2d3036] border border-[#818cf8]/50'
                      : 'bg-[#222428] border border-white/[0.04] hover:bg-[#2d3036]'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <CategoryIcon category={cat.category} size="sm" />
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-white truncate">
                          {cat.category}
                        </span>
                        <span
                          className="text-[11px] font-bold tnum"
                          style={{ color: cat.color }}
                        >
                          {cat.percentage}%
                        </span>
                      </div>
                      <div className="text-xs text-white/50 tnum">{cat.subtitle}</div>
                    </div>
                  </div>

                  <span className="text-sm font-bold text-white tnum whitespace-nowrap">
                    {formatINR(cat.amount, { showDecimals: false })}
                  </span>
                </button>
              ))
            )}
          </div>
        </section>
      </div>

      {/* Funding Distribution & Report Export */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <section className="lg:col-span-2 rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-base font-bold text-white">Funding Distribution</h2>
              <p className="text-xs text-white/55">Where spend was deducted</p>
            </div>
            <Landmark className="w-5 h-5 text-white/45" />
          </div>

          <div className="h-3 w-full rounded-full bg-[#2d3036] overflow-hidden flex gap-0.5">
            {snapshot.fundingDistribution.map((item) => (
              <div
                key={item.accountId}
                style={{
                  width: `${Math.max(4, item.percentage)}%`,
                  backgroundColor: item.color,
                }}
                className="h-full"
              />
            ))}
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            {snapshot.fundingDistribution.map((item) => (
              <div
                key={item.accountId}
                className="rounded-2xl bg-[#222428] border border-white/[0.04] p-3.5"
              >
                <div className="flex items-center gap-2 text-xs text-white/75 truncate">
                  <span
                    className="w-2 h-2 rounded-full shrink-0"
                    style={{ backgroundColor: item.color }}
                  />
                  <span className="truncate">{item.shortName}</span>
                </div>
                <div className="mt-1.5 text-lg font-bold text-white tnum">
                  {item.percentage}%
                </div>
                <div className="text-xs text-white/50 tnum">
                  {formatINR(item.amount, { showDecimals: false })}
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 flex flex-col justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#2d3036] text-[#818cf8] flex items-center justify-center shrink-0">
              <Download className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Export Tax &amp; Ledger CSV</h2>
              <p className="text-xs text-white/55 mt-1">
                Includes categorized breakdown and tax-deductible tags. Generated locally.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onExportCsvReport}
            className="w-full h-11 rounded-full bg-[#818cf8] hover:bg-[#bdc2ff] text-[#0b0c0e] font-bold text-sm transition-colors"
          >
            Export Ledger CSV
          </button>
        </section>
      </div>
    </div>
  );
}
