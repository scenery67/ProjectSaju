package io.sj.saju.reading.saju;

import java.util.Set;

/**
 * 십성(十神) 10종을 전통적인 5대 분류로 묶는다. 각 그룹이 삶의 특정 영역과
 * 대응된다는 게 명리학의 기본 틀이라, 개인 프로필(성격/연애/직업/재물/대인관계)을
 * 구성할 때 이 분류를 그대로 쓴다 — 임의로 지어낸 매핑이 아니다.
 *
 * <p>비겁(比劫)=비견·겁재 → 자기 주체성/대인관계, 식상(食傷)=식신·상관 → 표현력/매력,
 * 재성(財星)=편재·정재 → 재물, 관성(官星)=편관·정관 → 직업/사회적 위치,
 * 인성(印星)=편인·정인 → 학문/안정.
 */
public enum TenGodGroup {
    BIGYEOP(Set.of("비견", "겁재")),
    SIKSANG(Set.of("식신", "상관")),
    JAESEONG(Set.of("편재", "정재")),
    GWANSEONG(Set.of("편관", "정관")),
    INSEONG(Set.of("편인", "정인"));

    private final Set<String> members;

    TenGodGroup(Set<String> members) {
        this.members = members;
    }

    public static TenGodGroup of(String tenGodHangul) {
        for (TenGodGroup group : values()) {
            if (group.members.contains(tenGodHangul)) {
                return group;
            }
        }
        throw new IllegalArgumentException("Unknown ten god: " + tenGodHangul);
    }
}
