import type { SajuPersona } from '../types/saju';

// Placeholder personas for the initial 2 offerings.
// Character name/art/copy are intentionally generic — final branding TBD.
// 초기 2종 페르소나 placeholder. 캐릭터명/일러스트/카피는 추후 확정 예정.
export const PERSONAS: SajuPersona[] = [
  {
    id: 'breakup',
    type: 'BREAKUP',
    title: '이별사주',
    subtitle: '헤어진 마음, 사주로 짚어보기',
    characterName: '캐릭터 A (미정)',
    isPremium: false,
    accentColor: '#c98ea6',
  },
  {
    id: 'couple-compatibility',
    type: 'COUPLE_COMPATIBILITY',
    title: '연인 궁합 사주',
    subtitle: '두 사람의 사주로 보는 궁합',
    characterName: '캐릭터 B (미정)',
    isPremium: false,
    accentColor: '#e0a458',
  },
];

export function findPersonaById(id: string): SajuPersona | undefined {
  return PERSONAS.find((p) => p.id === id);
}
