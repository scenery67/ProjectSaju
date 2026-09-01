package io.sj.saju.reading.saju;

/**
 * 오행 상생(generating)/상극(overcoming) relation between two dominant elements.
 * This is a simplified single-factor compatibility signal, not a full 궁합
 * analysis (real 궁합 also weighs 십성/합충형파해 between the two full charts —
 * TODO once that's needed).
 */
public enum FiveElementRelation {
    SAME,
    GENERATING,
    OVERCOMING,
    UNRELATED;

    private static final String ORDER = "목화토금수";

    public static FiveElementRelation between(String a, String b) {
        int i = ORDER.indexOf(a);
        int j = ORDER.indexOf(b);
        if (i < 0 || j < 0) {
            return UNRELATED;
        }
        if (i == j) {
            return SAME;
        }
        if ((i + 1) % 5 == j || (j + 1) % 5 == i) {
            return GENERATING;
        }
        if ((i + 2) % 5 == j || (j + 2) % 5 == i) {
            return OVERCOMING;
        }
        return UNRELATED;
    }
}
