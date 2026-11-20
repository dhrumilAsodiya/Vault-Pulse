import React from 'react';
import {
  BarChart3,
  Bell,
  Lock,
  Plus,
  ReceiptText,
  Settings,
  ShieldCheck,
  Wallet,
} from 'lucide-react';

export type NavTab = 'home' | 'ledger' | 'analytics' | 'settings';

interface AppShellProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  unreadSmsCount: number;
  onOpenCaptureSimulator: () => void;
  onOpenAddTransaction: () => void;
  onLockVault: () => void;
  children: React.ReactNode;
}

const NAV_ITEMS: { id: NavTab; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'home', label: 'Home', icon: Wallet },
  { id: 'ledger', label: 'Ledger', icon: ReceiptText },
  { id: 'analytics', label: 'Analytics', icon: BarChart3 },
  { id: 'settings', label: 'Settings', icon: Settings },
];

const TAB_TITLES: Record<NavTab, string> = {
  home: 'Home',
  ledger: 'Ledger',
  analytics: 'Analytics',
  settings: 'Settings',
};

export function AppShell({
  activeTab,
  onTabChange,
  unreadSmsCount,
  onOpenCaptureSimulator,
  onOpenAddTransaction,
  onLockVault,
  children,
}: AppShellProps) {
  return (
    <div className="min-h-screen bg-[#0b0c0e] text-[#e3e2e6] flex flex-col md:flex-row">
      {/* Desktop Navigation Rail (hidden on mobile) */}
      <aside className="hidden md:flex md:w-64 md:flex-col md:fixed md:inset-y-0 bg-[#121316] border-r border-white/[0.06] z-30">
        <div className="px-6 py-5 flex items-center gap-3 border-b border-white/[0.06]">
          <div className="w-10 h-10 rounded-xl overflow-hidden bg-[#1a1c1e] border border-white/[0.08] shrink-0 flex items-center justify-center">
            <img
              src="/src/assets/images/vaultpulse_shield_logo_1795168121140.jpg"
              alt="VaultPulse Shield"
              referrerPolicy="no-referrer"
              className="w-full h-full object-cover"
            />
          </div>
          <div className="min-w-0">
            <span className="block text-base font-bold tracking-tight text-white truncate">
              VaultPulse
            </span>
            <span className="block text-xs text-white/50 truncate">
              Offline Wealth Vault
            </span>
          </div>
        </div>

        <nav className="flex-1 px-3 py-5 space-y-1.5" aria-label="Primary Navigation">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onTabChange(item.id)}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-semibold transition-colors whitespace-nowrap ${
                  isActive
                    ? 'bg-[#2dd4bf]/15 text-[#2dd4bf]'
                    : 'text-white/65 hover:text-white hover:bg-white/[0.04]'
                }`}
              >
                <Icon className="w-5 h-5 shrink-0" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        <div className="p-4 space-y-3 border-t border-white/[0.06]">
          <button
            type="button"
            onClick={onOpenAddTransaction}
            className="w-full h-12 rounded-2xl bg-[#4de082] hover:bg-[#6dfe9c] text-[#0b0c0e] font-bold text-sm flex items-center justify-center gap-2 transition-colors cursor-pointer whitespace-nowrap"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            <span>Add Transaction</span>
          </button>

          <div className="rounded-2xl bg-[#1a1c1e] border border-white/[0.06] p-3.5 flex items-start gap-2.5">
            <ShieldCheck className="w-4 h-4 text-[#4de082] shrink-0 mt-0.5" />
            <div className="text-xs leading-relaxed text-white/65">
              Zero internet permission. All ledger entries stay encrypted locally.
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 md:pl-64 flex flex-col min-h-screen">
        {/* Sticky Top App Bar */}
        <header className="sticky top-0 z-20 h-14 md:h-16 bg-[#121316]/95 backdrop-blur-md border-b border-white/[0.06] px-4 md:px-8 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="md:hidden w-9 h-9 rounded-xl overflow-hidden bg-[#1a1c1e] border border-white/[0.08] shrink-0">
              <img
                src="/src/assets/images/vaultpulse_shield_logo_1795168121140.jpg"
                alt="VaultPulse Shield"
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="md:hidden text-base font-bold text-white whitespace-nowrap">
                  VaultPulse
                </span>
                <span className="hidden md:inline text-lg font-bold text-white whitespace-nowrap">
                  {TAB_TITLES[activeTab]}
                </span>
              </div>
              <span className="md:hidden block text-[11px] text-white/50 leading-none mt-0.5">
                {TAB_TITLES[activeTab]}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2.5 shrink-0">
            <button
              type="button"
              onClick={onOpenCaptureSimulator}
              aria-label="Open SMS and UPI Capture Inbox"
              className="relative min-h-[40px] px-3 rounded-full bg-[#1a1c1e] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-white/80 flex items-center gap-2 transition-colors whitespace-nowrap"
            >
              <Bell className="w-4 h-4 text-[#818cf8]" />
              <span className="hidden sm:inline">SMS / UPI Inbox</span>
              {unreadSmsCount > 0 && (
                <span className="px-1.5 py-0.5 rounded-full bg-[#818cf8] text-[#0b0c0e] text-[10px] font-bold tnum leading-none">
                  {unreadSmsCount}
                </span>
              )}
            </button>

            <button
              type="button"
              onClick={onLockVault}
              aria-label="Lock Vault with Biometric Shield"
              className="min-h-[40px] min-w-[40px] rounded-full bg-[#1a1c1e] hover:bg-[#2d3036] border border-white/[0.06] text-white/75 hover:text-white flex items-center justify-center transition-colors"
              title="Lock Vault"
            >
              <Lock className="w-4 h-4" />
            </button>

            <div className="w-9 h-9 rounded-full overflow-hidden border border-white/[0.12] shrink-0 bg-[#222428]">
              <img
                src="/src/assets/images/user_profile_avatar_1795168122262.jpg"
                alt="Account Holder Portrait"
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
            </div>
          </div>
        </header>

        {/* Scrollable Screen Body */}
        <main className="flex-1 w-full max-w-6xl mx-auto px-4 md:px-8 pt-5 pb-28 md:pb-12">
          {children}
        </main>
      </div>

      {/* Mobile Material 3 Expressive Bottom Navigation Bar */}
      <nav
        aria-label="Mobile Navigation"
        className="md:hidden fixed bottom-0 left-0 right-0 z-30 h-16 bg-[#121316]/95 backdrop-blur-md border-t border-white/[0.06] grid grid-cols-4 items-center px-2"
      >
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onTabChange(item.id)}
              className="flex flex-col items-center justify-center min-h-[48px] gap-1"
            >
              <span
                className={`w-16 h-8 rounded-full flex items-center justify-center transition-colors ${
                  isActive ? 'bg-[#2dd4bf]/20 text-[#2dd4bf]' : 'text-white/60'
                }`}
              >
                <Icon className="w-5 h-5" />
              </span>
              <span
                className={`text-[11px] font-semibold tracking-wide leading-none ${
                  isActive ? 'text-[#2dd4bf]' : 'text-white/60'
                }`}
              >
                {item.label}
              </span>
            </button>
          );
        })}
      </nav>
    </div>
  );
}
