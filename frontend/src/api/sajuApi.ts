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

// useCredit: 로그인 계정이 오늘 무료 한도(하루 2회)를 다 쓴 뒤, 크레딧
// 1개를 내고 계속 볼지 백엔드가 물을 때(429 daily_limit_reached) 재시도용.
export function requestBreakupReading(
  payload: BreakupReadingRequest,
  useCredit = false,
) {
  return apiPost<SajuReadingResult>(
    `/saju/breakup${useCredit ? '?useCredit=true' : ''}`,
    payload,
    authHeader(),
  );
}

export function requestCoupleCompatibilityReading(
  payload: CoupleCompatibilityRequest,
  useCredit = false,
) {
  return apiPost<SajuReadingResult>(
    `/saju/couple-compatibility${useCredit ? '?useCredit=true' : ''}`,
    payload,
    authHeader(),
  );
}
