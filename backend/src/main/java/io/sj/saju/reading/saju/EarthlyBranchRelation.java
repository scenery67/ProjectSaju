package io.sj.saju.reading.saju;

/**
 * 지지(地支) 육합/충 판정. 두 사람의 일지(日支, 배우자궁)를 비교하는 데 쓴다.
 * 12지지를 순서대로 색인(자=0..해=11)해서 구조적으로 판정한다 — 표를 따로
 * 외워 적지 않고 인덱스 연산으로 유도하므로 오타로 틀릴 여지가 적다.
 *
 * <p>육합(六合): 인덱스 합이 1 또는 13인 쌍 — 자축, 인해, 묘술, 진유, 사신, 오미.
 * 충(沖): 인덱스 차이가 정확히 6인 쌍(12지지 원의 정반대) — 자오, 축미, 인신,
 * 묘유, 진술, 사해.
 *
 * <p>형(刑)·파(破)·해(害)는 이런 단순 구조식으로 유도되지 않는 개별 규칙이라
 * (오류 검증이 더 필요해) 아직 반영하지 않았다 — ROADMAP Task 001 후속 참고.
 */
public enum EarthlyBranchRelation {
    YUKHAP,
    CHUNG,
    NONE;

    private static final String ZHI_HANGUL = "자축인묘진사오미신유술해";

    public static EarthlyBranchRelation of(char zhiA, char zhiB) {
        int i = ZHI_HANGUL.indexOf(zhiA);
        int j = ZHI_HANGUL.indexOf(zhiB);
        if (i < 0 || j < 0) {
            return NONE;
        }
        int sum = i + j;
        if (sum == 1 || sum == 13) {
            return YUKHAP;
        }
        if (Math.abs(i - j) == 6) {
            return CHUNG;
        }
        return NONE;
    }
}
