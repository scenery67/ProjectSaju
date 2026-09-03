// 비로그인 사용자의 일일 무료 사주 횟수는 서버가 추적하지 않는다(CLAUDE.md
// 3.2 — 보관 정책 없이 reading_record에 식별자를 추가하지 않는다는 원칙).
// 그래서 이 카운터는 오직 localStorage에만 존재하고, 쉽게 우회될 수 있다는
// 것을 감수한다 — 실제 비용이 드는 LLM 상담은 로그인+크레딧으로 이미 막혀
// 있어서, 이건 악용 방지가 아니라 로그인 유도용 넛지에 가깝다.
const STORAGE_KEY = 'saju.dailyQuota.v1';
export const FREE_DAILY_READING_LIMIT = 2;

interface QuotaState {
  date: string; // YYYY-MM-DD, KST 기준
  count: number;
}

// 브라우저 로컬 타임존과 무관하게 서버와 같은 기준(KST)으로 하루를 나눈다.
function todayKstDateString(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date());
}

function readState(): QuotaState {
  const today = todayKstDateString();
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { date: today, count: 0 };
    const parsed = JSON.parse(raw) as QuotaState;
    return parsed.date === today ? parsed : { date: today, count: 0 };
  } catch {
    return { date: today, count: 0 };
  }
}

export function remainingFreeReadings(): number {
  return Math.max(0, FREE_DAILY_READING_LIMIT - readState().count);
}

export function canStartFreeReading(): boolean {
  return remainingFreeReadings() > 0;
}

export function recordFreeReading(): void {
  try {
    const state = readState();
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...state, count: state.count + 1 }));
  } catch {
    // 프라이빗 모드 등으로 localStorage를 못 쓰면 조용히 무시 — 매번 무료로 취급된다.
  }
}
