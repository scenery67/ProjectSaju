// Persona type shared with backend PersonaType enum (io.sj.saju.persona.PersonaType).
// 백엔드의 PersonaType enum과 동일한 값을 사용한다.
export type PersonaType = 'BREAKUP' | 'COUPLE_COMPATIBILITY';

export interface SajuPersona {
  id: string;
  type: PersonaType;
  title: string;
  subtitle: string;
  characterName: string;
  isPremium: boolean;
  accentColor: string;
}

export type CalendarType = 'SOLAR' | 'LUNAR';

export interface PersonReadingInput {
  name: string;
  birthDate: string; // YYYY-MM-DD
  birthTime: string | null; // HH:mm, optional if unknown
  calendarType: CalendarType;
  isLunarLeapMonth: boolean;
  gender: 'MALE' | 'FEMALE';
}

export interface BreakupReadingRequest {
  self: PersonReadingInput;
}

export interface CoupleCompatibilityRequest {
  self: PersonReadingInput;
  partner: PersonReadingInput;
}

export interface SajuReadingResult {
  personaType: PersonaType;
  summary: string;
  detail: string;
}
