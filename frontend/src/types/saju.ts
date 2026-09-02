// Persona type shared with backend PersonaType enum (io.sj.saju.persona.PersonaType).
// 백엔드의 PersonaType enum과 동일한 값을 사용한다.
export type PersonaType = 'BREAKUP' | 'COUPLE_COMPATIBILITY';

export interface SajuPersona {
  id: string;
  type: PersonaType;
  title: string;
  subtitle: string;
  characterName: string;
  personality: string; // one-line tone/identity blurb, not the character's visual design
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

export interface DaYunPeriod {
  startAge: number;
  endAge: number;
  pillar: string;
}

export interface LiuNianPeriod {
  year: number;
  age: number;
  pillar: string;
  tenGod: string;
}

export interface PersonalityProfile {
  personality: string;
  love: string;
  career: string;
  wealth: string;
  relationships: string;
}

export interface SajuChart {
  yearPillar: string;
  monthPillar: string;
  dayPillar: string;
  hourPillar: string | null; // null if birth time is unknown
  dayMaster: string;
  fiveElementCounts: Record<string, number>;
  dominantFiveElement: string;
  yearTenGod: string;
  monthTenGod: string;
  timeTenGod: string | null; // null if birth time is unknown
  yearHideGan: string[];
  monthHideGan: string[];
  dayHideGan: string[];
  timeHideGan: string[] | null; // null if birth time is unknown
  yearTwelveStage: string;
  monthTwelveStage: string;
  dayTwelveStage: string;
  timeTwelveStage: string | null; // null if birth time is unknown
  daYunPeriods: DaYunPeriod[];
  currentLiuNian: LiuNianPeriod[];
  personalityProfile: PersonalityProfile;
}

export interface SajuReadingResult {
  // reading_record id — only meaningful for logged-in requests; undefined for
  // results saved before this field existed (old localStorage history entries).
  id?: string;
  personaType: PersonaType;
  summary: string;
  detail: string;
  selfChart: SajuChart;
  partnerChart: SajuChart | null; // null for the breakup (single-person) reading
}
