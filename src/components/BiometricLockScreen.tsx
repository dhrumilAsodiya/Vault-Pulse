import React from 'react';
import { Fingerprint, ShieldCheck } from 'lucide-react';

interface BiometricLockScreenProps {
  onUnlock: () => void;
}

export function BiometricLockScreen({ onUnlock }: BiometricLockScreenProps) {
  return (
    <div className="min-h-screen bg-[#0b0c0e] text-white flex items-center justify-center p-6">
      <div className="w-full max-w-sm rounded-3xl bg-[#1a1c1e] border border-white/[0.08] p-8 text-center space-y-6">
        <div className="w-16 h-16 rounded-2xl overflow-hidden mx-auto border border-white/[0.1] bg-[#121316]">
          <img
            src="/src/assets/images/vaultpulse_shield_logo_1795168121140.jpg"
            alt="VaultPulse Shield"
            referrerPolicy="no-referrer"
            className="w-full h-full object-cover"
          />
        </div>

        <div className="space-y-2">
          <h1 className="text-2xl font-bold tracking-tight">VaultPulse Locked</h1>
          <p className="text-sm text-white/60">
            Protected by Keystore AES-256 &amp; FLAG_SECURE screen shield.
          </p>
        </div>

        <div className="rounded-2xl bg-[#222428] border border-white/[0.05] p-4 flex items-center justify-center gap-2 text-xs text-[#4de082]">
          <ShieldCheck className="w-4 h-4" />
          <span>100% Offline Vault · Zero Internet Permission</span>
        </div>

        <button
          type="button"
          onClick={onUnlock}
          className="w-full h-12 rounded-2xl bg-[#818cf8] hover:bg-[#bdc2ff] text-[#0b0c0e] font-bold text-sm flex items-center justify-center gap-2 transition-colors"
        >
          <Fingerprint className="w-5 h-5" />
          <span>Unlock with Biometrics</span>
        </button>
      </div>
    </div>
  );
}
