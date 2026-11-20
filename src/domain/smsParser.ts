import { ParsedFinancialEvent, SmsInboxMessage, UpiNotificationPayload } from './types';

/**
 * Negative-keyword blocklist mandated to filter out OTPs and failed/declined transactions.
 */
export const NEGATIVE_BLOCKLIST = [
  'otp',
  'failed',
  'declined',
  'unsuccessful',
  'requested',
] as const;

/**
 * Whitelisted Indian UPI package names for NotificationListenerService.
 */
export const WHITELISTED_UPI_PACKAGES: Record<string, string> = {
  'com.google.android.apps.nbu.paisa.user': 'Google Pay',
  'com.phonepe.app': 'PhonePe',
  'net.one97.paytm': 'Paytm',
  'in.org.npci.upiapp': 'BHIM',
};

export function containsBlockedKeyword(text: string): string | null {
  const lower = text.toLowerCase();
  for (const kw of NEGATIVE_BLOCKLIST) {
    const regex = new RegExp(`\\b${kw}\\b`, 'i');
    if (regex.test(lower)) {
      return kw;
    }
  }
  return null;
}

export function extractAmount(text: string): number | null {
  // Matches Rs. 1,450.00 | INR 489 | ₹10,000.50 | Rs 500
  const amountRegex = /(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)/i;
  const match = text.match(amountRegex);
  if (!match) return null;
  const numeric = Number(match[1].replace(/,/g, ''));
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null;
}

export function extractAvailableBalance(text: string): number | null {
  // Matches "Avl Bal: INR 84,210.00", "Avl Bal Rs.2,45,820.50", "Bal: ₹12,000", "Available Balance is INR 36200.00"
  const balRegex =
    /(?:avl\.?\s*bal(?:ance)?|available\s*bal(?:ance)?|clear\s*bal(?:ance)?|bal(?:ance)?)\s*(?:is|:|-)?\s*(?:rs\.?|inr|₹)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)/i;
  const match = text.match(balRegex);
  if (!match) return null;
  const numeric = Number(match[1].replace(/,/g, ''));
  return Number.isFinite(numeric) && numeric >= 0 ? numeric : null;
}

export function extractMaskedDigits(text: string): string | undefined {
  // Matches "XX4892", "a/c *9104", "A/c ending 1278", "•••• 9104", "XXXX4892"
  const patterns = [
    /(?:a\/c|acct|account|card)\s*(?:no\.?)?\s*(?:ending\s*(?:with|in)?\s*)?(?:[xX*•]+\s*)?(\d{4})\b/i,
    /[xX*•]{2,}(\d{4})\b/,
  ];
  for (const pattern of patterns) {
    const match = text.match(pattern);
    if (match?.[1]) return match[1];
  }
  return undefined;
}

export function inferTransactionType(text: string): 'DEBIT' | 'CREDIT' | null {
  const lower = text.toLowerCase();
  const hasDebit =
    /\b(debited|debit|spent|paid|withdrawn|sent|purchase|charged|deducted)\b/i.test(lower);
  const hasCredit =
    /\b(credited|credit|received|deposited|refund|cashback|salary|payout)\b/i.test(lower);

  if (hasDebit && !hasCredit) return 'DEBIT';
  if (hasCredit && !hasDebit) return 'CREDIT';
  if (hasDebit) return 'DEBIT';
  if (hasCredit) return 'CREDIT';
  return null;
}

export function extractMerchant(text: string, fallback: string): string {
  // Try "at <Merchant>", "to <Merchant>", "from <Merchant>", "towards <Merchant>", "VPA <merchant@bank>"
  const patterns = [
    /\b(?:paid to|sent to|to|at|towards)\s+([A-Za-z0-9 &._'-]{2,36}?)(?:\s+(?:on|via|ref|utr|upi|avl|bal|using|\d{2}[/-])|[.,;]|$)/i,
    /\bfrom\s+([A-Za-z0-9 &._'-]{2,36}?)(?:\s+(?:on|via|ref|utr|upi|avl|bal|to a\/c|[.,;])|$)/i,
    /\binfo[:\s]+([A-Za-z0-9 &._'-]{2,32})/i,
  ];
  for (const p of patterns) {
    const match = text.match(p);
    if (match?.[1]) {
      const cleaned = match[1].trim().replace(/\s+/g, ' ');
      if (cleaned.length >= 2 && !/^(a\/c|account|your|bank|rs|inr)$/i.test(cleaned)) {
        return cleaned;
      }
    }
  }
  return fallback;
}

export function parseBankSms(
  sms: SmsInboxMessage
): { event?: ParsedFinancialEvent; blockedBy?: string; ignoredReason?: string } {
  const blocked = containsBlockedKeyword(sms.body);
  if (blocked) {
    return { blockedBy: blocked };
  }

  const type = inferTransactionType(sms.body);
  const amount = extractAmount(sms.body);
  if (!type || amount === null) {
    return { ignoredReason: 'No valid debit/credit amount detected in SMS.' };
  }

  // Strip the balance clause before extracting transaction amount if the message starts with balance info,
  // or prefer the first transaction amount (which extractAmount already returns).
  const avlBal = extractAvailableBalance(sms.body) ?? undefined;
  const maskedDigits = extractMaskedDigits(sms.body);
  const merchant = extractMerchant(sms.body, `${sms.senderId} Transfer`);

  return {
    event: {
      type,
      amount,
      merchant,
      maskedDigits,
      avlBal,
      senderOrApp: sms.senderId,
      source: 'SMS_AUTO',
      timestamp: sms.timestamp,
      rawBody: sms.body,
    },
  };
}

export function parseUpiNotification(
  payload: UpiNotificationPayload
): { event?: ParsedFinancialEvent; blockedBy?: string; ignoredReason?: string } {
  const appLabel = WHITELISTED_UPI_PACKAGES[payload.packageName];
  if (!appLabel) {
    return { ignoredReason: `Package ${payload.packageName} is not in UPI whitelist.` };
  }

  const combined = `${payload.title} ${payload.body}`.trim();
  const blocked = containsBlockedKeyword(combined);
  if (blocked) {
    return { blockedBy: blocked };
  }

  const type = inferTransactionType(combined);
  const amount = extractAmount(combined);
  if (!type || amount === null) {
    return { ignoredReason: 'No valid UPI payment amount found in notification.' };
  }

  const merchant = extractMerchant(combined, `${appLabel} UPI`);
  return {
    event: {
      type,
      amount,
      merchant,
      senderOrApp: appLabel,
      source: 'UPI_LISTENER',
      timestamp: payload.timestamp,
      rawBody: combined,
    },
  };
}
