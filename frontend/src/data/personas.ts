import type { PersonaType, SajuPersona } from '../types/saju';

// 캐릭터명/정체성(personality)은 확정됨 — 톤은 "다정한 위로형".
// 일러스트/아트는 아직 placeholder(🔮, accentColor 배경)이고 추후 자체 제작 예정
// (참고 사이트 foxbunny.io/saju와 겹치지 않는 오리지널 캐릭터로).
// Character name/identity are final ("warm comforter" tone); art is still
// placeholder pending original illustration work.
export const PERSONAS: SajuPersona[] = [
  {
    id: 'breakup',
    type: 'BREAKUP',
    title: '이별사주',
    subtitle: '이 마음, 언제쯤 괜찮아질까요?',
    characterName: '다숨',
    personality: '헤어진 마음을 사주로 따뜻하게 짚어주는 상담사. "~했을 거예요" 같은 위로 중심의 말투.',
    isPremium: false,
    accentColor: '#c98ea6',
  },
  {
    id: 'couple-compatibility',
    type: 'COUPLE_COMPATIBILITY',
    title: '연인 궁합 사주',
    subtitle: '우리 둘, 얼마나 잘 맞는 사이일까요?',
    characterName: '설레',
    personality: '두 사람의 케미를 설레는 마음으로 봐주는 캐릭터. 긍정적이고 응원하는 톤.',
    isPremium: false,
    accentColor: '#e0a458',
  },
];

export function findPersonaById(id: string): SajuPersona | undefined {
  return PERSONAS.find((p) => p.id === id);
}

export function findPersonaByType(type: PersonaType): SajuPersona | undefined {
  return PERSONAS.find((p) => p.type === type);
}
