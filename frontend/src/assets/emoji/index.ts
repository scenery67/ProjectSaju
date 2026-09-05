import bear from './bear.svg';
import cat from './cat.svg';
import dog from './dog.svg';
import fox from './fox.svg';
import owl from './owl.svg';
import panda from './panda.svg';
import rabbit from './rabbit.svg';
import tiger from './tiger.svg';

// Twemoji(CC-BY 4.0, Twitter/jdecked) 플랫 SVG — 프로필 아바타 8종 전용.
// 그 외 UI 아이콘은 전부 lucide-react(모노크롬 라인 아이콘)로 통일했다 —
// 참고 사이트(foxbunny.io)가 아바타만 컬러고 나머지는 다 모노크롬이었다.
export const EMOJI = {
  bear,
  cat,
  dog,
  fox,
  owl,
  panda,
  rabbit,
  tiger,
} as const;

export type EmojiName = keyof typeof EMOJI;
