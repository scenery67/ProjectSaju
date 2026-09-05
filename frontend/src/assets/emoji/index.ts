import bear from './bear.svg';
import bulb from './bulb.svg';
import candle from './candle.svg';
import card from './card.svg';
import cart from './cart.svg';
import cat from './cat.svg';
import check from './check.svg';
import clipboard from './clipboard.svg';
import crystalball from './crystalball.svg';
import dog from './dog.svg';
import door from './door.svg';
import fox from './fox.svg';
import gear from './gear.svg';
import gift from './gift.svg';
import house from './house.svg';
import locked from './locked.svg';
import mail from './mail.svg';
import mailbox from './mailbox.svg';
import megaphone from './megaphone.svg';
import moon from './moon.svg';
import owl from './owl.svg';
import panda from './panda.svg';
import person from './person.svg';
import rabbit from './rabbit.svg';
import sadface from './sadface.svg';
import search from './search.svg';
import shield from './shield.svg';
import sparkles from './sparkles.svg';
import speech from './speech.svg';
import tiger from './tiger.svg';
import unlocked from './unlocked.svg';
import warning from './warning.svg';

// Twemoji(CC-BY 4.0, Twitter/jdecked) 플랫 SVG를 로컬에 내려받아 쓴다 —
// 네이티브 OS 이모지 폰트는 기기마다 모양이 달라 "만화 같다"는 인상을 주니,
// 일관되고 또렷한 벡터 아이콘으로 통일한다. 출처: twemoji@14.0.2
export const EMOJI = {
  bear,
  bulb,
  candle,
  card,
  cart,
  cat,
  check,
  clipboard,
  crystalball,
  dog,
  door,
  fox,
  gear,
  gift,
  house,
  locked,
  mail,
  mailbox,
  megaphone,
  moon,
  owl,
  panda,
  person,
  rabbit,
  sadface,
  search,
  shield,
  sparkles,
  speech,
  tiger,
  unlocked,
  warning,
} as const;

export type EmojiName = keyof typeof EMOJI;
