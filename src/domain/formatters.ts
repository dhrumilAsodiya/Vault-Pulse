export function formatINR(amount: number, options?: { showDecimals?: boolean; sign?: 'auto' | 'plus' | 'minus' | 'none' }): string {
  const showDecimals = options?.showDecimals ?? true;
  const abs = Math.abs(amount);
  const formatted = abs.toLocaleString('en-IN', {
    minimumFractionDigits: showDecimals ? 2 : 0,
    maximumFractionDigits: showDecimals ? 2 : 0,
  });

  const signMode = options?.sign ?? 'none';
  if (signMode === 'plus') return `+₹${formatted}`;
  if (signMode === 'minus') return `-₹${formatted}`;
  if (signMode === 'auto') {
    if (amount > 0) return `+₹${formatted}`;
    if (amount < 0) return `-₹${formatted}`;
  }
  return `₹${formatted}`;
}

export function splitINRParts(amount: number): { whole: string; decimals: string } {
  const abs = Math.abs(amount);
  const [wholeRaw, decRaw = '00'] = abs.toFixed(2).split('.');
  const wholeFormatted = Number(wholeRaw).toLocaleString('en-IN');
  return {
    whole: `₹${wholeFormatted}`,
    decimals: `.${decRaw}`,
  };
}

export function formatRelativeTime(timestamp: number, now: number = Date.now()): string {
  if (!timestamp) return 'Never synced';
  const diffMs = Math.max(0, now - timestamp);
  const mins = Math.floor(diffMs / 60_000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return 'yesterday';
  return `${days}d ago`;
}

export function formatTimeShort(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString('en-IN', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
}

export function isSameLocalDay(tsA: number, tsB: number): boolean {
  const a = new Date(tsA);
  const b = new Date(tsB);
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export function getDateGroupLabel(timestamp: number, now: number = Date.now()): { primary: string; secondary: string } {
  const d = new Date(timestamp);
  const today = new Date(now);
  const yesterday = new Date(now - 86_400_000);

  const shortDate = d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
  const weekday = d.toLocaleDateString('en-IN', { weekday: 'long' });

  if (isSameLocalDay(timestamp, today.getTime())) {
    return { primary: 'Today', secondary: shortDate };
  }
  if (isSameLocalDay(timestamp, yesterday.getTime())) {
    return { primary: 'Yesterday', secondary: shortDate };
  }
  const fullDate = d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  return { primary: fullDate, secondary: weekday };
}
