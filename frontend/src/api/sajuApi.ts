import { apiPost } from './client';
import { getAuthToken } from '../lib/auth';
import type {
  BreakupReadingRequest,
  CoupleCompatibilityRequest,
  SajuReadingResult,
} from '../types/saju';

// Attaching the token (when logged in) lets the backend tag this reading with
// the account so it shows up in server-side history — it's still a public
// endpoint either way, this only changes whether the record gets linked.
function authHeader(): Record<string, string> | undefined {
  const token = getAuthToken();
  return token ? { Authorization: `Bearer ${token}` } : undefined;
}

export function requestBreakupReading(payload: BreakupReadingRequest) {
  return apiPost<SajuReadingResult>('/saju/breakup', payload, authHeader());
}

export function requestCoupleCompatibilityReading(
  payload: CoupleCompatibilityRequest,
) {
  return apiPost<SajuReadingResult>('/saju/couple-compatibility', payload, authHeader());
}
