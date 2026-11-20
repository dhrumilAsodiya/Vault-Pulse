import React, { useRef, useState } from 'react';
import {
  ArrowLeftRight,
  AtSign,
  Check,
  Copy,
  Download,
  Lock,
  MessageSquare,
  Pencil,
  Phone,
  Plus,
  RefreshCw,
  ShieldCheck,
  SlidersHorizontal,
  Trash2,
  Upload,
} from 'lucide-react';
import { AccountProfile, CategoryId, KeywordRule } from '../domain/types';

interface SettingsViewProps {
  accounts: AccountProfile[];
  keywordRules: KeywordRule[];
  autoParseUpiSms: boolean;
  autoBackupEnabled: boolean;
  biometricLockEnabled: boolean;
  onSetPrimaryUpiAccount: (accountId: string) => void;
  onToggleAutoParseUpi: (enabled: boolean) => void;
  onToggleAutoBackup: (enabled: boolean) => void;
  onToggleBiometricLock: (enabled: boolean) => void;
  onUpdateDialCode: (accountId: string, newNumber: string) => void;
  onAddCustomBank: (input: {
    name: string;
    maskedDigits: string;
    senderId: string;
    missedCallNumber: string;
  }) => void;
  onCallBank: (account: AccountProfile) => void;
  onAddKeywordRule: (keywords: string[], category: CategoryId) => void;
  onUpdateKeywordRule: (ruleId: string, keywords: string[], category: CategoryId) => void;
  onDeleteKeywordRule: (ruleId: string) => void;
  onExportEncryptedBackup: (passphrase: string) => Promise<void>;
  onImportEncryptedBackup: (file: File, passphrase: string) => Promise<void>;
  onOpenCaptureSimulator: () => void;
}

const RULE_CATEGORIES: CategoryId[] = [
  'Food & Dining',
  'Fuel & Transport',
  'Shopping & Retail',
  'Groceries',
  'Subscriptions',
  'Utilities',
  'Entertainment',
  'Income / Salary',
  'Other',
];

export function SettingsView({
  accounts,
  keywordRules,
  autoParseUpiSms,
  autoBackupEnabled,
  biometricLockEnabled,
  onSetPrimaryUpiAccount,
  onToggleAutoParseUpi,
  onToggleAutoBackup,
  onToggleBiometricLock,
  onUpdateDialCode,
  onAddCustomBank,
  onCallBank,
  onAddKeywordRule,
  onUpdateKeywordRule,
  onDeleteKeywordRule,
  onExportEncryptedBackup,
  onImportEncryptedBackup,
  onOpenCaptureSimulator,
}: SettingsViewProps) {
  const bankAccounts = accounts.filter((a) => a.accountType === 'BANK');
  const primaryAccount =
    bankAccounts.find((a) => a.isPrimaryUpi) ?? bankAccounts[0];

  const [copiedAlias, setCopiedAlias] = useState(false);
  const [showPrimaryPicker, setShowPrimaryPicker] = useState(false);

  // Dial code editing
  const [editingDialId, setEditingDialId] = useState<string | null>(null);
  const [dialDraft, setDialDraft] = useState('');

  // Add custom bank inline form
  const [showAddBank, setShowAddBank] = useState(false);
  const [bankName, setBankName] = useState('');
  const [bankDigits, setBankDigits] = useState('');
  const [bankSender, setBankSender] = useState('');
  const [bankDial, setBankDial] = useState('');

  // Keyword rules form
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [showAddRule, setShowAddRule] = useState(false);
  const [ruleKeywordsDraft, setRuleKeywordsDraft] = useState('');
  const [ruleCategoryDraft, setRuleCategoryDraft] = useState<CategoryId>('Food & Dining');

  // Encrypted backup modals/state
  const [backupMode, setBackupMode] = useState<'export' | 'import' | null>(null);
  const [passphrase, setPassphrase] = useState('');
  const [backupStatus, setBackupStatus] = useState<string | null>(null);
  const [pendingImportFile, setPendingImportFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const handleCopyAlias = async () => {
    const alias = primaryAccount?.upiAlias ?? 'aarav.mehta@okhdfcbank';
    try {
      await navigator.clipboard.writeText(alias);
    } catch {
      // Clipboard may be restricted in headless browser
    }
    setCopiedAlias(true);
    setTimeout(() => setCopiedAlias(false), 1800);
  };

  const handleSaveRule = (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = ruleKeywordsDraft
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    if (parsed.length === 0) return;

    if (editingRuleId) {
      onUpdateKeywordRule(editingRuleId, parsed, ruleCategoryDraft);
      setEditingRuleId(null);
    } else {
      onAddKeywordRule(parsed, ruleCategoryDraft);
      setShowAddRule(false);
    }
    setRuleKeywordsDraft('');
  };

  const handleCreateCustomBank = (e: React.FormEvent) => {
    e.preventDefault();
    if (!bankName.trim() || !bankDigits.trim() || !bankDial.trim()) return;
    onAddCustomBank({
      name: bankName.trim(),
      maskedDigits: bankDigits.replace(/\D/g, '').slice(-4).padStart(4, '0'),
      senderId: (bankSender.trim() || bankName.trim().slice(0, 6)).toUpperCase(),
      missedCallNumber: bankDial.trim(),
    });
    setBankName('');
    setBankDigits('');
    setBankSender('');
    setBankDial('');
    setShowAddBank(false);
  };

  const handleRunBackupAction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!passphrase.trim()) return;
    try {
      if (backupMode === 'export') {
        await onExportEncryptedBackup(passphrase);
        setBackupStatus('Encrypted AES-256-GCM backup exported locally.');
      } else if (backupMode === 'import' && pendingImportFile) {
        await onImportEncryptedBackup(pendingImportFile, passphrase);
        setBackupStatus('Encrypted backup verified and restored.');
      }
      setBackupMode(null);
      setPassphrase('');
      setPendingImportFile(null);
    } catch (err) {
      setBackupStatus(
        err instanceof Error ? err.message : 'Failed to process encrypted backup.'
      );
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <div className="text-xs font-semibold tracking-wider text-[#bdc2ff] uppercase">
          Configuration Suite
        </div>
        <h1 className="mt-1 text-2xl sm:text-3xl font-bold text-white tracking-tight">
          Settings &amp; Automation
        </h1>
        <p className="mt-1 text-sm text-white/55">
          Local keyword rules, UPI routing, missed-call codes &amp; AES-256 encrypted backups
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
        {/* 1. Default UPI & Routing */}
        <section className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0">
                <ArrowLeftRight className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-white">Default UPI &amp; Routing</h2>
                <p className="text-xs text-white/55">
                  Prioritize account for GPay, PhonePe, Paytm &amp; BHIM
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold text-[#2dd4bf] bg-[#2dd4bf]/15 px-2.5 py-1 rounded-full">
              Active
            </span>
          </div>

          {primaryAccount && (
            <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-4 space-y-3">
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="text-sm font-bold text-white truncate">
                    {primaryAccount.name}
                  </div>
                  <div className="text-xs text-white/55 tnum">
                    Primary Account •••• {primaryAccount.maskedDigits}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setShowPrimaryPicker((v) => !v)}
                  className="min-h-[38px] px-3.5 rounded-full bg-[#2d3036] hover:bg-[#38393c] text-xs font-semibold text-white whitespace-nowrap"
                >
                  {showPrimaryPicker ? 'Close' : 'Switch'}
                </button>
              </div>

              {showPrimaryPicker && (
                <div className="pt-2 border-t border-white/[0.06] space-y-1.5">
                  {bankAccounts.map((acc) => (
                    <button
                      key={acc.id}
                      type="button"
                      onClick={() => {
                        onSetPrimaryUpiAccount(acc.id);
                        setShowPrimaryPicker(false);
                      }}
                      className={`w-full px-3 py-2.5 rounded-xl flex items-center justify-between text-xs font-semibold transition-colors ${
                        acc.id === primaryAccount.id
                          ? 'bg-[#818cf8]/20 text-[#bdc2ff]'
                          : 'bg-[#1a1c1e] hover:bg-[#2d3036] text-white/80'
                      }`}
                    >
                      <span>
                        {acc.name} (•••• {acc.maskedDigits})
                      </span>
                      {acc.id === primaryAccount.id && <Check className="w-4 h-4" />}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-[#2d3036] text-white/75 flex items-center justify-center shrink-0">
                <AtSign className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <div className="text-xs text-white/50">UPI ID Alias</div>
                <div className="text-sm font-semibold text-white truncate">
                  {primaryAccount?.upiAlias ?? 'aarav.mehta@okhdfcbank'}
                </div>
              </div>
            </div>
            <button
              type="button"
              onClick={handleCopyAlias}
              aria-label="Copy UPI ID Alias"
              className="min-h-[40px] min-w-[40px] rounded-xl bg-[#2d3036] hover:bg-[#38393c] text-white/80 flex items-center justify-center shrink-0"
            >
              {copiedAlias ? (
                <Check className="w-4 h-4 text-[#4de082]" />
              ) : (
                <Copy className="w-4 h-4" />
              )}
            </button>
          </div>

          <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-[#4de082]/15 text-[#4de082] flex items-center justify-center shrink-0">
                <MessageSquare className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <div className="text-sm font-semibold text-white">
                  Auto-parse UPI &amp; Bank SMS
                </div>
                <div className="text-xs text-white/55">
                  Filters OTP/failed alerts and deduplicates within ±3 min
                </div>
              </div>
            </div>

            <button
              type="button"
              role="switch"
              aria-checked={autoParseUpiSms}
              aria-label="Toggle auto-parse UPI payment SMS"
              onClick={() => onToggleAutoParseUpi(!autoParseUpiSms)}
              className={`w-12 h-7 rounded-full p-1 transition-colors shrink-0 ${
                autoParseUpiSms ? 'bg-[#4de082]' : 'bg-[#343538]'
              }`}
            >
              <span
                className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                  autoParseUpiSms ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          <button
            type="button"
            onClick={onOpenCaptureSimulator}
            className="w-full min-h-[44px] rounded-2xl bg-[#222428] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-[#818cf8] flex items-center justify-center gap-2 transition-colors"
          >
            <SlidersHorizontal className="w-4 h-4" />
            <span>Open SMS &amp; UPI Capture Simulator</span>
          </button>
        </section>

        {/* 2. Missed-Call Balance Inquiry */}
        <section className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 space-y-4">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#2dd4bf]/15 text-[#2dd4bf] flex items-center justify-center shrink-0">
              <Phone className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Missed-Call Balance Inquiry</h2>
              <p className="text-xs text-white/55">
                Launches ACTION_DIAL cleanly with Dual SIM without internet access.
              </p>
            </div>
          </div>

          <div className="space-y-2.5">
            {bankAccounts.map((acc) => {
              const isEditing = editingDialId === acc.id;
              return (
                <div
                  key={acc.id}
                  className="rounded-2xl bg-[#222428] border border-white/[0.05] p-3.5 space-y-2.5"
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-white truncate">
                          {acc.shortName}
                        </span>
                        <span className="text-xs text-white/50 tnum">
                          •••• {acc.maskedDigits}
                        </span>
                      </div>
                      <div className="text-xs font-semibold text-[#818cf8] tnum mt-0.5">
                        {acc.missedCallNumber}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        type="button"
                        onClick={() => {
                          setEditingDialId(isEditing ? null : acc.id);
                          setDialDraft(acc.missedCallNumber ?? '');
                        }}
                        aria-label={`Edit dial code for ${acc.shortName}`}
                        className="w-9 h-9 rounded-full bg-[#2d3036] hover:bg-[#38393c] text-white/75 flex items-center justify-center"
                      >
                        <Pencil className="w-3.5 h-3.5" />
                      </button>

                      <button
                        type="button"
                        onClick={() => onCallBank(acc)}
                        aria-label={`Call ${acc.shortName} missed-call number`}
                        className="w-9 h-9 rounded-full bg-[#4de082]/15 hover:bg-[#4de082]/25 text-[#4de082] flex items-center justify-center"
                      >
                        <Phone className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {isEditing && (
                    <div className="flex items-center gap-2 pt-2 border-t border-white/[0.06]">
                      <input
                        type="tel"
                        value={dialDraft}
                        onChange={(e) => setDialDraft(e.target.value)}
                        aria-label={`Dial number for ${acc.shortName}`}
                        className="flex-1 h-9 px-3 rounded-xl bg-[#1a1c1e] border border-white/[0.08] text-xs text-white focus:outline-none focus:border-[#818cf8]"
                      />
                      <button
                        type="button"
                        onClick={() => {
                          if (dialDraft.trim()) {
                            onUpdateDialCode(acc.id, dialDraft.trim());
                            setEditingDialId(null);
                          }
                        }}
                        className="h-9 px-3.5 rounded-xl bg-[#818cf8] text-[#0b0c0e] text-xs font-bold"
                      >
                        Save
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {showAddBank ? (
            <form
              onSubmit={handleCreateCustomBank}
              className="rounded-2xl bg-[#222428] border border-white/[0.06] p-4 space-y-3"
            >
              <div className="grid grid-cols-2 gap-2.5">
                <input
                  type="text"
                  required
                  value={bankName}
                  onChange={(e) => setBankName(e.target.value)}
                  placeholder="Bank Name (e.g. Axis)"
                  className="h-10 px-3 rounded-xl bg-[#1a1c1e] border border-white/[0.08] text-xs text-white"
                />
                <input
                  type="text"
                  required
                  maxLength={4}
                  value={bankDigits}
                  onChange={(e) => setBankDigits(e.target.value)}
                  placeholder="Last 4 digits"
                  className="h-10 px-3 rounded-xl bg-[#1a1c1e] border border-white/[0.08] text-xs text-white tnum"
                />
                <input
                  type="text"
                  value={bankSender}
                  onChange={(e) => setBankSender(e.target.value)}
                  placeholder="SMS Sender ID (AXISBK)"
                  className="h-10 px-3 rounded-xl bg-[#1a1c1e] border border-white/[0.08] text-xs text-white"
                />
                <input
                  type="tel"
                  required
                  value={bankDial}
                  onChange={(e) => setBankDial(e.target.value)}
                  placeholder="Missed-Call Number"
                  className="h-10 px-3 rounded-xl bg-[#1a1c1e] border border-white/[0.08] text-xs text-white"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddBank(false)}
                  className="h-9 px-4 rounded-xl bg-[#2d3036] text-xs font-semibold text-white/80"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="h-9 px-4 rounded-xl bg-[#4de082] text-[#0b0c0e] text-xs font-bold"
                >
                  Add Bank
                </button>
              </div>
            </form>
          ) : (
            <button
              type="button"
              onClick={() => setShowAddBank(true)}
              className="w-full min-h-[44px] rounded-2xl bg-[#222428] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-white flex items-center justify-center gap-2 transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>Add Custom Bank Dial Code</span>
            </button>
          )}
        </section>

        {/* 3. Auto-Categorization Rules */}
        <section className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0">
                <SlidersHorizontal className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-white">Auto-Categorization Rules</h2>
                <p className="text-xs text-white/55">
                  Transactions matching keywords are categorized locally on insert.
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold text-white/65 bg-[#2d3036] px-2.5 py-1 rounded-full tnum whitespace-nowrap">
              {keywordRules.length} Rules
            </span>
          </div>

          <div className="space-y-2.5">
            {keywordRules.map((rule) => (
              <div
                key={rule.id}
                className="rounded-2xl bg-[#222428] border border-white/[0.05] p-3.5 flex items-center justify-between gap-3"
              >
                <div className="min-w-0">
                  <div className="text-sm font-semibold text-white truncate">
                    {rule.keywords.join(', ')}
                  </div>
                  <div className="mt-1 text-xs text-white/55">
                    Maps to <span className="text-[#2dd4bf] font-semibold">{rule.category}</span>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <button
                    type="button"
                    onClick={() => {
                      setEditingRuleId(rule.id);
                      setShowAddRule(true);
                      setRuleKeywordsDraft(rule.keywords.join(', '));
                      setRuleCategoryDraft(rule.category);
                    }}
                    aria-label={`Edit rule ${rule.keywords.join(', ')}`}
                    className="w-9 h-9 rounded-xl hover:bg-[#2d3036] text-white/70 flex items-center justify-center"
                  >
                    <Pencil className="w-3.5 h-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDeleteKeywordRule(rule.id)}
                    aria-label={`Delete rule ${rule.keywords.join(', ')}`}
                    className="w-9 h-9 rounded-xl hover:bg-[#f87171]/15 text-[#f87171] flex items-center justify-center"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          {showAddRule ? (
            <form
              onSubmit={handleSaveRule}
              className="rounded-2xl bg-[#222428] border border-white/[0.06] p-4 space-y-3"
            >
              <div>
                <label className="block text-xs text-white/60 mb-1" htmlFor="rule-keywords">
                  Comma-separated keywords
                </label>
                <input
                  id="rule-keywords"
                  type="text"
                  required
                  value={ruleKeywordsDraft}
                  onChange={(e) => setRuleKeywordsDraft(e.target.value)}
                  placeholder="e.g. Zepto, Blinkit, Instamart"
                  className="w-full h-10 px-3 rounded-xl bg-[#1a1c1e] border border-white/[0.08] text-xs text-white focus:outline-none focus:border-[#818cf8]"
                />
              </div>

              <div>
                <label className="block text-xs text-white/60 mb-1" htmlFor="rule-category">
                  Target category
                </label>
                <select
                  id="rule-category"
                  value={ruleCategoryDraft}
                  onChange={(e) => setRuleCategoryDraft(e.target.value as CategoryId)}
                  className="w-full h-10 px-3 rounded-xl bg-[#1a1c1e] border border-white/[0.08] text-xs text-white focus:outline-none focus:border-[#818cf8]"
                >
                  {RULE_CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddRule(false);
                    setEditingRuleId(null);
                    setRuleKeywordsDraft('');
                  }}
                  className="h-9 px-4 rounded-xl bg-[#2d3036] text-xs font-semibold text-white/80"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="h-9 px-4 rounded-xl bg-[#4de082] text-[#0b0c0e] text-xs font-bold"
                >
                  {editingRuleId ? 'Update Rule' : 'Save Rule'}
                </button>
              </div>
            </form>
          ) : (
            <button
              type="button"
              onClick={() => {
                setEditingRuleId(null);
                setRuleKeywordsDraft('');
                setShowAddRule(true);
              }}
              className="w-full min-h-[44px] rounded-2xl bg-[#222428] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-white flex items-center justify-center gap-2 transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>Add New Keyword Rule</span>
            </button>
          )}
        </section>

        {/* 4. 100% On-Device & Encrypted Security & Backup */}
        <section className="rounded-3xl bg-[#1a1c1e] border border-white/[0.06] p-5 md:p-6 space-y-4">
          <div className="rounded-2xl bg-[#4de082]/10 border border-[#4de082]/25 p-4 flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#4de082]/15 text-[#4de082] flex items-center justify-center shrink-0">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-[#4de082]">
                100% On-Device &amp; Encrypted
              </h2>
              <p className="text-xs text-white/70 mt-0.5">
                AES-256-GCM / SQLCipher local persistence. Zero internet permissions or cloud telemetry.
              </p>
            </div>
          </div>

          <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-[#2d3036] text-[#818cf8] flex items-center justify-center shrink-0">
                <Lock className="w-4 h-4" />
              </div>
              <div>
                <div className="text-sm font-semibold text-white">Biometric Lock on Launch</div>
                <div className="text-xs text-white/55">
                  Requires BiometricPrompt + FLAG_SECURE shield
                </div>
              </div>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={biometricLockEnabled}
              aria-label="Toggle biometric lock on launch"
              onClick={() => onToggleBiometricLock(!biometricLockEnabled)}
              className={`w-12 h-7 rounded-full p-1 transition-colors shrink-0 ${
                biometricLockEnabled ? 'bg-[#4de082]' : 'bg-[#343538]'
              }`}
            >
              <span
                className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                  biometricLockEnabled ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-[#2d3036] text-white/75 flex items-center justify-center shrink-0">
                <RefreshCw className="w-4 h-4" />
              </div>
              <div>
                <div className="text-sm font-semibold text-white">
                  Auto-Backup to Local Storage
                </div>
                <div className="text-xs text-white/55">
                  Retains encrypted snapshots offline
                </div>
              </div>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={autoBackupEnabled}
              aria-label="Toggle auto-backup to local storage"
              onClick={() => onToggleAutoBackup(!autoBackupEnabled)}
              className={`w-12 h-7 rounded-full p-1 transition-colors shrink-0 ${
                autoBackupEnabled ? 'bg-[#4de082]' : 'bg-[#343538]'
              }`}
            >
              <span
                className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                  autoBackupEnabled ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0">
                <Download className="w-4 h-4" />
              </div>
              <div>
                <div className="text-sm font-semibold text-white">Export Encrypted Backup</div>
                <div className="text-xs text-white/55">
                  Password-protected AES-256-GCM JSON vault
                </div>
              </div>
            </div>
            <button
              type="button"
              onClick={() => {
                setBackupMode('export');
                setBackupStatus(null);
              }}
              className="min-h-[38px] px-4 rounded-full bg-[#bdc2ff] hover:bg-[#e0e0ff] text-[#0b0c0e] text-xs font-bold whitespace-nowrap"
            >
              Export Now
            </button>
          </div>

          <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-[#2d3036] text-white/75 flex items-center justify-center shrink-0">
                <Upload className="w-4 h-4" />
              </div>
              <div>
                <div className="text-sm font-semibold text-white">Import Backup File</div>
                <div className="text-xs text-white/55">
                  Restore accounts, ledger &amp; rules offline
                </div>
              </div>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept=".json"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) {
                  setPendingImportFile(file);
                  setBackupMode('import');
                  setBackupStatus(null);
                }
              }}
            />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="min-h-[38px] px-4 rounded-full bg-[#2d3036] hover:bg-[#38393c] text-white text-xs font-semibold whitespace-nowrap"
            >
              Choose File
            </button>
          </div>

          {backupMode && (
            <form
              onSubmit={handleRunBackupAction}
              className="rounded-2xl bg-[#121316] border border-[#818cf8]/40 p-4 space-y-3"
            >
              <div className="text-xs font-semibold text-white">
                {backupMode === 'export'
                  ? 'Enter encryption passphrase for export:'
                  : `Enter passphrase to decrypt ${pendingImportFile?.name}:`}
              </div>
              <input
                type="password"
                required
                value={passphrase}
                onChange={(e) => setPassphrase(e.target.value)}
                placeholder="Vault passphrase"
                className="w-full h-10 px-3 rounded-xl bg-[#222428] border border-white/[0.08] text-xs text-white focus:outline-none focus:border-[#818cf8]"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setBackupMode(null);
                    setPassphrase('');
                  }}
                  className="h-9 px-4 rounded-xl bg-[#2d3036] text-xs font-semibold text-white/80"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="h-9 px-4 rounded-xl bg-[#4de082] text-[#0b0c0e] text-xs font-bold"
                >
                  {backupMode === 'export' ? 'Encrypt & Download' : 'Decrypt & Restore'}
                </button>
              </div>
            </form>
          )}

          {backupStatus && (
            <div className="rounded-2xl bg-[#2dd4bf]/10 border border-[#2dd4bf]/30 px-4 py-2.5 text-xs text-[#2dd4bf] font-medium">
              {backupStatus}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
