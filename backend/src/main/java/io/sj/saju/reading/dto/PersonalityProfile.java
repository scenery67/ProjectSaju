package io.sj.saju.reading.dto;

/**
 * 성격/연애/직업/재물/대인관계 5개 영역 평문 해석. 십성(十神) 5대 분류
 * (비겁/식상/재성/관성/인성)를 각 영역에 대응시켜 만든다 — 임의 텍스트가 아니라
 * 명리학의 표준 대응 관계를 그대로 쓴다 (TenGodGroup 참고).
 */
public record PersonalityProfile(
        String personality,
        String love,
        String career,
        String wealth,
        String relationships) {
}
