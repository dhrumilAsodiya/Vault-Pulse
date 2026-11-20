import React, { useState } from 'react';
import { BellRing, MessageSquareWarning, Send, ShieldAlert, Smartphone, X } from 'lucide-react';
import { WHITELISTED_UPI_PACKAGES } from '../domain/smsParser';
import { CaptureProcessResult, SmsInboxMessage } from '../domain/types';

interface CaptureSimulatorModalProps {
  isOpen: boolean;
  smsInbox: SmsInboxMessage[];
  onClose: () => void;
  onSimulateSms: (senderId: string, body: string) => CaptureProcessResult;
  onSimulateUpi: (packageName: string, title: string, body: string) => CaptureProcessResult;
}

export function CaptureSimulatorModal({
  isOpen,
  smsInbox,
  onClose,
  onSimulateSms,
  onSimulateUpi,
}: CaptureSimulatorModalProps) {
  const [mode, setMode] = useState<'sms' | 'upi'>('sms');
  const [senderId, setSenderId] = useState('AD-SBIINB');
  const [smsBody, setSmsBody] = useState(
    'A/c XX4892 debited by Rs.320.00 at Zepto Grocery on today. Avl Bal: INR 2,45,500.50'
  );

  const [upiPkg, setUpiPkg] = useState('com.google.android.apps.nbu.paisa.user');
  const [upiTitle, setUpiTitle] = useState('Paid ₹489.00');
  const [upiBody, setUpiBody] = useState('Paid to Swiggy Food Delivery via UPI');

  const [lastOutcome, setLastOutcome] = useState<CaptureProcessResult | null>(null);

  if (!isOpen) return null;

  const presets = [
    {
      label: 'OTP Blocklist Test',
      action: () => {
        setMode('sms');
        setSenderId('VM-HDFCBK');
        setSmsBody('Your OTP for Rs 4,500 transaction at Amazon is 883920. Do not share.');
      },
    },
    {
      label: 'Failed / Declined Test',
      action: () => {
        setMode('sms');
        setSenderId('JX-ICICIB');
        setSmsBody('Card XX1278 payment of INR 1,299.00 at Uber was declined.');
      },
    },
    {
      label: 'Zepto Debit + Avl Bal',
      action: () => {
        setMode('sms');
        setSenderId('AD-SBIINB');
        setSmsBody(
          'A/c XX4892 debited by Rs.320.00 at Zepto Grocery on today. Avl Bal: INR 2,45,500.50'
        );
      },
    },
    {
      label: 'GPay UPI Notification',
      action: () => {
        setMode('upi');
        setUpiPkg('com.google.android.apps.nbu.paisa.user');
        setUpiTitle('Paid ₹275.00');
        setUpiBody('Paid to Blue Tokai Coffee via UPI');
      },
    },
  ];

  const handleRun = (e: React.FormEvent) => {
    e.preventDefault();
    if (mode === 'sms') {
      setLastOutcome(onSimulateSms(senderId, smsBody));
    } else {
      setLastOutcome(onSimulateUpi(upiPkg, upiTitle, upiBody));
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="capture-sim-title"
    >
      <div className="w-full sm:max-w-xl max-h-[90vh] overflow-y-auto rounded-t-3xl sm:rounded-3xl bg-[#1a1c1e] border border-white/[0.08] p-5 sm:p-6 space-y-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center">
              <BellRing className="w-5 h-5" />
            </div>
            <div>
              <h2 id="capture-sim-title" className="text-lg font-bold text-white">
                Event-Driven SMS &amp; UPI Capture
              </h2>
              <p className="text-xs text-white/55">
                BroadcastReceiver &amp; NotificationListenerService Pipeline
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Capture Simulator"
            className="w-9 h-9 rounded-full bg-[#2d3036] text-white/80 flex items-center justify-center"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Presets */}
        <div className="flex flex-wrap gap-2">
          {presets.map((p) => (
            <button
              key={p.label}
              type="button"
              onClick={p.action}
              className="px-3 py-1.5 rounded-full bg-[#222428] hover:bg-[#2d3036] border border-white/[0.06] text-xs font-semibold text-white/85"
            >
              {p.label}
            </button>
          ))}
        </div>

        {/* Mode Switch */}
        <div className="grid grid-cols-2 p-1 rounded-full bg-[#121316] border border-white/[0.06]">
          <button
            type="button"
            onClick={() => setMode('sms')}
            className={`h-9 rounded-full text-xs font-bold flex items-center justify-center gap-1.5 ${
              mode === 'sms' ? 'bg-[#818cf8] text-[#0b0c0e]' : 'text-white/65'
            }`}
          >
            <MessageSquareWarning className="w-3.5 h-3.5" />
            <span>SMS_RECEIVED</span>
          </button>
          <button
            type="button"
            onClick={() => setMode('upi')}
            className={`h-9 rounded-full text-xs font-bold flex items-center justify-center gap-1.5 ${
              mode === 'upi' ? 'bg-[#2dd4bf] text-[#003731]' : 'text-white/65'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>UPI Notification</span>
          </button>
        </div>

        <form onSubmit={handleRun} className="space-y-3">
          {mode === 'sms' ? (
            <>
              <div>
                <label className="block text-xs text-white/60 mb-1" htmlFor="sim-sender">
                  Sender ID
                </label>
                <input
                  id="sim-sender"
                  type="text"
                  required
                  value={senderId}
                  onChange={(e) => setSenderId(e.target.value)}
                  className="w-full h-10 px-3 rounded-xl bg-[#222428] border border-white/[0.06] text-xs font-mono text-white"
                />
              </div>
              <div>
                <label className="block text-xs text-white/60 mb-1" htmlFor="sim-sms-body">
                  SMS Body
                </label>
                <textarea
                  id="sim-sms-body"
                  rows={3}
                  required
                  value={smsBody}
                  onChange={(e) => setSmsBody(e.target.value)}
                  className="w-full p-3 rounded-xl bg-[#222428] border border-white/[0.06] text-xs text-white"
                />
              </div>
            </>
          ) : (
            <>
              <div>
                <label className="block text-xs text-white/60 mb-1" htmlFor="sim-upi-pkg">
                  Whitelisted UPI Package
                </label>
                <select
                  id="sim-upi-pkg"
                  value={upiPkg}
                  onChange={(e) => setUpiPkg(e.target.value)}
                  className="w-full h-10 px-3 rounded-xl bg-[#222428] border border-white/[0.06] text-xs text-white"
                >
                  {Object.entries(WHITELISTED_UPI_PACKAGES).map(([pkg, name]) => (
                    <option key={pkg} value={pkg}>
                      {name} ({pkg})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-white/60 mb-1" htmlFor="sim-upi-title">
                  Notification Title
                </label>
                <input
                  id="sim-upi-title"
                  type="text"
                  required
                  value={upiTitle}
                  onChange={(e) => setUpiTitle(e.target.value)}
                  className="w-full h-10 px-3 rounded-xl bg-[#222428] border border-white/[0.06] text-xs text-white"
                />
              </div>
              <div>
                <label className="block text-xs text-white/60 mb-1" htmlFor="sim-upi-body">
                  Notification Body
                </label>
                <input
                  id="sim-upi-body"
                  type="text"
                  required
                  value={upiBody}
                  onChange={(e) => setUpiBody(e.target.value)}
                  className="w-full h-10 px-3 rounded-xl bg-[#222428] border border-white/[0.06] text-xs text-white"
                />
              </div>
            </>
          )}

          <button
            type="submit"
            className="w-full h-11 rounded-2xl bg-[#4de082] hover:bg-[#6dfe9c] text-[#00210c] font-bold text-xs flex items-center justify-center gap-2"
          >
            <Send className="w-4 h-4" />
            <span>Dispatch Event Into Vault Pipeline</span>
          </button>
        </form>

        {lastOutcome && (
          <div
            role="status"
            className={`rounded-2xl p-3.5 border text-xs flex items-start gap-2.5 ${
              lastOutcome.status === 'BLOCKED'
                ? 'bg-[#f87171]/10 border-[#f87171]/30 text-[#ffb4ab]'
                : lastOutcome.status === 'MERGED'
                ? 'bg-[#2dd4bf]/10 border-[#2dd4bf]/30 text-[#2dd4bf]'
                : lastOutcome.status === 'INSERTED'
                ? 'bg-[#4de082]/10 border-[#4de082]/30 text-[#4de082]'
                : 'bg-[#fbbf24]/10 border-[#fbbf24]/30 text-[#fbbf24]'
            }`}
          >
            <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5" />
            <div>
              <div className="font-bold">{lastOutcome.status}</div>
              <div className="text-white/80 mt-0.5">{lastOutcome.reason}</div>
            </div>
          </div>
        )}

        {/* Recent Local SMS Inbox */}
        <div className="space-y-2 pt-2 border-t border-white/[0.06]">
          <div className="text-xs font-semibold text-white/60">
            Local SMS Inbox ({smsInbox.length})
          </div>
          <div className="max-h-36 overflow-y-auto space-y-2">
            {smsInbox.map((sms) => (
              <div
                key={sms.id}
                className="rounded-xl bg-[#121316] border border-white/[0.05] p-2.5 text-[11px]"
              >
                <div className="flex items-center justify-between text-white/50">
                  <span className="font-mono font-semibold text-[#818cf8]">{sms.senderId}</span>
                  <span>{new Date(sms.timestamp).toLocaleTimeString('en-IN')}</span>
                </div>
                <div className="mt-1 text-white/80">{sms.body}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
