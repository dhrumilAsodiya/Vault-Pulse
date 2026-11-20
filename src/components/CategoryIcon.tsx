import React from 'react';
import {
  ArrowLeftRight,
  Banknote,
  Building2,
  Car,
  Film,
  Landmark,
  MoreHorizontal,
  PiggyBank,
  ShoppingBag,
  ShoppingCart,
  Tv,
  Utensils,
  Wallet,
  Zap,
} from 'lucide-react';
import { CategoryId } from '../domain/types';

export function CategoryIcon({
  category,
  size = 'md',
}: {
  category: CategoryId;
  size?: 'sm' | 'md';
}) {
  const boxClass = size === 'sm' ? 'w-9 h-9 rounded-xl' : 'w-11 h-11 rounded-xl';
  const iconClass = size === 'sm' ? 'w-4 h-4' : 'w-5 h-5';

  switch (category) {
    case 'Food & Dining':
      return (
        <div className={`${boxClass} bg-[#fb923c]/15 text-[#fb923c] flex items-center justify-center shrink-0`}>
          <Utensils className={iconClass} />
        </div>
      );
    case 'Fuel & Transport':
      return (
        <div className={`${boxClass} bg-[#38bdf8]/15 text-[#38bdf8] flex items-center justify-center shrink-0`}>
          <Car className={iconClass} />
        </div>
      );
    case 'Shopping & Retail':
      return (
        <div className={`${boxClass} bg-[#a78bfa]/15 text-[#a78bfa] flex items-center justify-center shrink-0`}>
          <ShoppingBag className={iconClass} />
        </div>
      );
    case 'Groceries':
      return (
        <div className={`${boxClass} bg-[#eab308]/15 text-[#eab308] flex items-center justify-center shrink-0`}>
          <ShoppingCart className={iconClass} />
        </div>
      );
    case 'Subscriptions':
      return (
        <div className={`${boxClass} bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0`}>
          <Tv className={iconClass} />
        </div>
      );
    case 'Utilities':
      return (
        <div className={`${boxClass} bg-[#2dd4bf]/15 text-[#2dd4bf] flex items-center justify-center shrink-0`}>
          <Zap className={iconClass} />
        </div>
      );
    case 'Entertainment':
      return (
        <div className={`${boxClass} bg-[#c084fc]/15 text-[#c084fc] flex items-center justify-center shrink-0`}>
          <Film className={iconClass} />
        </div>
      );
    case 'Income / Salary':
      return (
        <div className={`${boxClass} bg-[#4ade80]/15 text-[#4ade80] flex items-center justify-center shrink-0`}>
          <Banknote className={iconClass} />
        </div>
      );
    case 'Transfer':
      return (
        <div className={`${boxClass} bg-[#818cf8]/15 text-[#bdc2ff] flex items-center justify-center shrink-0`}>
          <ArrowLeftRight className={iconClass} />
        </div>
      );
    default:
      return (
        <div className={`${boxClass} bg-white/[0.08] text-[#c6c5d5] flex items-center justify-center shrink-0`}>
          <MoreHorizontal className={iconClass} />
        </div>
      );
  }
}

export function AccountIcon({
  accentColor,
  accountType,
}: {
  accentColor: 'indigo' | 'teal' | 'emerald' | 'amber';
  accountType: 'BANK' | 'CASH';
}) {
  if (accountType === 'CASH') {
    return (
      <div className="w-11 h-11 rounded-xl bg-[#4ade80]/15 text-[#4ade80] flex items-center justify-center shrink-0">
        <Wallet className="w-5 h-5" />
      </div>
    );
  }

  switch (accentColor) {
    case 'indigo':
      return (
        <div className="w-11 h-11 rounded-xl bg-[#818cf8]/15 text-[#818cf8] flex items-center justify-center shrink-0">
          <Landmark className="w-5 h-5" />
        </div>
      );
    case 'teal':
      return (
        <div className="w-11 h-11 rounded-xl bg-[#2dd4bf]/15 text-[#2dd4bf] flex items-center justify-center shrink-0">
          <Building2 className="w-5 h-5" />
        </div>
      );
    case 'emerald':
      return (
        <div className="w-11 h-11 rounded-xl bg-[#4ade80]/15 text-[#4ade80] flex items-center justify-center shrink-0">
          <PiggyBank className="w-5 h-5" />
        </div>
      );
    default:
      return (
        <div className="w-11 h-11 rounded-xl bg-[#fbbf24]/15 text-[#fbbf24] flex items-center justify-center shrink-0">
          <Wallet className="w-5 h-5" />
        </div>
      );
  }
}
