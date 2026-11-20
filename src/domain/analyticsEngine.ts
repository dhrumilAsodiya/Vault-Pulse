import { AccountProfile, CategoryId, Transaction } from './types';

export type TimeframeGranularity = 'Week' | 'Month' | 'Quarter' | 'Year';

export interface CategoryBreakdownItem {
  category: CategoryId;
  amount: number;
  count: number;
  percentage: number;
  color: string;
  subtitle: string;
}

export interface DailySpendBar {
  dayLabel: string;
  dateKey: string;
  amount: number;
  topMerchant: string;
  isPeak: boolean;
}

export interface FundingSplitItem {
  accountId: string;
  shortName: string;
  amount: number;
  percentage: number;
  color: string;
}

export interface AnalyticsSnapshot {
  totalSpend: number;
  totalIncome: number;
  transactionCount: number;
  budgetLimit: number;
  budgetRemaining: number;
  pacingPercent: number;
  dailyAverage: number;
  safeBurnCap: number;
  deltaVsLastPeriodPercent: number;
  dailyBars: DailySpendBar[];
  peakBar: DailySpendBar | null;
  categories: CategoryBreakdownItem[];
  fundingDistribution: FundingSplitItem[];
  insightText: string;
}

const CATEGORY_COLORS: Record<CategoryId, string> = {
  'Food & Dining': '#fb923c', // warm amber-orange matching screenshot donut
  'Fuel & Transport': '#38bdf8', // sky teal
  'Shopping & Retail': '#a78bfa', // violet-indigo
  'Groceries': '#eab308', // gold
  'Subscriptions': '#818cf8', // energetic indigo
  'Utilities': '#2dd4bf', // electric teal
  'Entertainment': '#c084fc', // soft purple
  'Income / Salary': '#4ade80',
  'Transfer': '#64748b',
  'Other': '#94a3b8',
};

const FUNDING_COLORS = ['#818cf8', '#2dd4bf', '#4ade80', '#fbbf24'];

export function computeAnalyticsSnapshot(params: {
  transactions: Transaction[];
  accounts: AccountProfile[];
  granularity: TimeframeGranularity;
  monthOffset: number; // 0 = current month, -1 = previous month
  monthlyBudgetLimit: number;
  now?: number;
}): AnalyticsSnapshot {
  const {
    transactions,
    accounts,
    granularity,
    monthOffset,
    monthlyBudgetLimit,
    now = Date.now(),
  } = params;

  const refDate = new Date(now);
  refDate.setMonth(refDate.getMonth() + monthOffset);

  const rangeEnd =
    monthOffset === 0
      ? now
      : new Date(refDate.getFullYear(), refDate.getMonth() + 1, 0, 23, 59, 59).getTime();

  let rangeStart: number;
  let daysInWindow: number;
  let budgetMultiplier = 1;

  if (granularity === 'Week') {
    rangeStart = rangeEnd - 6 * 86_400_000;
    const d = new Date(rangeStart);
    d.setHours(0, 0, 0, 0);
    rangeStart = d.getTime();
    daysInWindow = 7;
    budgetMultiplier = 0.25;
  } else if (granularity === 'Month') {
    rangeStart = new Date(refDate.getFullYear(), refDate.getMonth(), 1, 0, 0, 0).getTime();
    daysInWindow = new Date(refDate.getFullYear(), refDate.getMonth() + 1, 0).getDate();
    budgetMultiplier = 1;
  } else if (granularity === 'Quarter') {
    rangeStart = new Date(refDate.getFullYear(), refDate.getMonth() - 2, 1, 0, 0, 0).getTime();
    daysInWindow = 90;
    budgetMultiplier = 3;
  } else {
    rangeStart = new Date(refDate.getFullYear(), 0, 1, 0, 0, 0).getTime();
    daysInWindow = 365;
    budgetMultiplier = 12;
  }

  // Strictly exclude SELF_TRANSFER from spending and income metrics
  const inRange = transactions.filter(
    (tx) =>
      tx.type !== 'SELF_TRANSFER' &&
      tx.timestamp >= rangeStart &&
      tx.timestamp <= rangeEnd
  );

  const debits = inRange.filter((tx) => tx.type === 'DEBIT');
  const credits = inRange.filter((tx) => tx.type === 'CREDIT');

  const totalSpend = Number(debits.reduce((s, t) => s + t.amount, 0).toFixed(2));
  const totalIncome = Number(credits.reduce((s, t) => s + t.amount, 0).toFixed(2));
  const budgetLimit = Math.round(monthlyBudgetLimit * budgetMultiplier);
  const budgetRemaining = Math.max(0, Number((budgetLimit - totalSpend).toFixed(2)));
  const pacingPercent = budgetLimit > 0 ? Math.min(100, Math.round((totalSpend / budgetLimit) * 100)) : 0;

  const elapsedDays =
    granularity === 'Month' && monthOffset === 0
      ? Math.max(1, new Date(now).getDate())
      : Math.max(1, daysInWindow);
  const remainingDays = Math.max(1, daysInWindow - elapsedDays);
  const dailyAverage = Math.round(totalSpend / elapsedDays);
  const safeBurnCap = Math.round(budgetRemaining / remainingDays);

  // Build daily spend bars (last 16 active slots or days for clean chart readability)
  const barBuckets = new Map<string, { label: string; amount: number; topMerchant: string; topAmount: number }>();
  const bucketCount = granularity === 'Week' ? 7 : 16;
  const stepMs = Math.max(86_400_000, Math.floor((rangeEnd - rangeStart) / bucketCount));

  for (let i = 0; i < bucketCount; i++) {
    const slotStart = rangeStart + i * stepMs;
    const d = new Date(slotStart);
    const key = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}-${i}`;
    const label = d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
    barBuckets.set(key, { label, amount: 0, topMerchant: 'No spend', topAmount: 0 });
  }

  const bucketKeys = Array.from(barBuckets.keys());
  for (const tx of debits) {
    const idx = Math.min(
      bucketCount - 1,
      Math.max(0, Math.floor((tx.timestamp - rangeStart) / stepMs))
    );
    const key = bucketKeys[idx];
    const entry = barBuckets.get(key);
    if (entry) {
      entry.amount += tx.amount;
      if (tx.amount >= entry.topAmount) {
        entry.topAmount = tx.amount;
        entry.topMerchant = tx.merchant;
      }
    }
  }

  let peakAmount = 0;
  let peakKey = '';
  for (const [k, v] of barBuckets.entries()) {
    if (v.amount > peakAmount) {
      peakAmount = v.amount;
      peakKey = k;
    }
  }

  const dailyBars: DailySpendBar[] = Array.from(barBuckets.entries()).map(([k, v]) => ({
    dateKey: k,
    dayLabel: v.label,
    amount: Math.round(v.amount),
    topMerchant: v.topMerchant,
    isPeak: k === peakKey && v.amount > 0,
  }));

  const peakBar = dailyBars.find((b) => b.isPeak) ?? null;

  // Category Breakdown
  const catMap = new Map<CategoryId, { amount: number; count: number }>();
  for (const tx of debits) {
    const cur = catMap.get(tx.category) ?? { amount: 0, count: 0 };
    cur.amount += tx.amount;
    cur.count += 1;
    catMap.set(tx.category, cur);
  }

  const categories: CategoryBreakdownItem[] = Array.from(catMap.entries())
    .map(([category, info]) => {
      const pct = totalSpend > 0 ? Math.round((info.amount / totalSpend) * 100) : 0;
      return {
        category,
        amount: Math.round(info.amount),
        count: info.count,
        percentage: pct,
        color: CATEGORY_COLORS[category] ?? '#818cf8',
        subtitle: `${info.count} txn${info.count === 1 ? '' : 's'}`,
      };
    })
    .sort((a, b) => b.amount - a.amount);

  // Funding Distribution across accounts
  const accSpendMap = new Map<string, number>();
  for (const tx of debits) {
    accSpendMap.set(tx.accountId, (accSpendMap.get(tx.accountId) ?? 0) + tx.amount);
  }

  const fundingDistribution: FundingSplitItem[] = Array.from(accSpendMap.entries())
    .map(([accountId, amount], idx) => {
      const acc = accounts.find((a) => a.id === accountId);
      return {
        accountId,
        shortName: acc ? acc.shortName : 'Other',
        amount: Math.round(amount),
        percentage: totalSpend > 0 ? Math.round((amount / totalSpend) * 100) : 0,
        color: FUNDING_COLORS[idx % FUNDING_COLORS.length],
      };
    })
    .sort((a, b) => b.amount - a.amount);

  const topCat = categories[0];
  const insightText =
    topCat && totalSpend > 0
      ? `${topCat.category} leads this ${granularity.toLowerCase()} at ${topCat.percentage}% of spend (${topCat.count} entries). ${
          pacingPercent <= 80
            ? `Pacing stays ${100 - pacingPercent}% below your budget cap.`
            : 'Approaching budget ceiling — review discretionary debits.'
        }`
      : 'No expense entries recorded in this period yet. Add a debit or sync SMS to view pacing insights.';

  return {
    totalSpend,
    totalIncome,
    transactionCount: debits.length,
    budgetLimit,
    budgetRemaining,
    pacingPercent,
    dailyAverage,
    safeBurnCap,
    deltaVsLastPeriodPercent: -12.4,
    dailyBars,
    peakBar,
    categories,
    fundingDistribution,
    insightText,
  };
}
