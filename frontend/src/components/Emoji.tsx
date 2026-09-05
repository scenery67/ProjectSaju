import { EMOJI, type EmojiName } from '../assets/emoji';

interface EmojiProps {
  name: EmojiName;
  className?: string;
}

/** 트위모지 플랫 SVG 아이콘 — 기본 크기는 텍스트 한 줄 높이 정도(20px). */
export default function Emoji({ name, className = 'h-5 w-5' }: EmojiProps) {
  return <img src={EMOJI[name]} alt="" className={`inline-block shrink-0 ${className}`} />;
}
