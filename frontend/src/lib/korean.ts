// 받침 유무에 따라 조사를 고른다(와/과, 이/가, 을/를 등) — 완성형 한글
// 음절(가~힣) 기준. 한글이 아닌 문자로 끝나면 받침 없는 쪽을 기본값으로 쓴다.
export function withBatchimPostposition(
  word: string,
  withoutBatchim: string,
  withBatchim: string,
): string {
  const code = word.charCodeAt(word.length - 1);
  if (code < 0xac00 || code > 0xd7a3) {
    return word + withoutBatchim;
  }
  const hasBatchim = (code - 0xac00) % 28 !== 0;
  return word + (hasBatchim ? withBatchim : withoutBatchim);
}
