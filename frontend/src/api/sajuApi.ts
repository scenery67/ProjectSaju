import { apiPost } from './client';
import type {
  BreakupReadingRequest,
  CoupleCompatibilityRequest,
  SajuReadingResult,
} from '../types/saju';

export function requestBreakupReading(payload: BreakupReadingRequest) {
  return apiPost<SajuReadingResult>('/saju/breakup', payload);
}

export function requestCoupleCompatibilityReading(
  payload: CoupleCompatibilityRequest,
) {
  return apiPost<SajuReadingResult>('/saju/couple-compatibility', payload);
}
