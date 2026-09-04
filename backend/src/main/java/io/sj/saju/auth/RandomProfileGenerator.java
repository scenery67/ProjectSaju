package io.sj.saju.auth;

import java.security.SecureRandom;

/**
 * 카카오 등 일부 공급자의 기본 닉네임은 실명인 경우가 많아 그대로 쓰면
 * 개인정보가 노출된다(CLAUDE.md 3.2). 가입 시점에 공급자 닉네임을 아예
 * 쓰지 않고 이 랜덤 별명/아바타로 대체한다.
 */
final class RandomProfileGenerator {

    private static final String[] ADJECTIVES = {
        "용감한", "다정한", "느긋한", "반짝이는", "씩씩한", "포근한", "엉뚱한", "차분한", "명랑한", "든든한",
    };
    private static final String[] ANIMALS = {
        "토끼", "여우", "곰", "고양이", "호랑이", "판다", "강아지", "부엉이",
    };
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomProfileGenerator() {
    }

    static String randomNickname() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String animal = ANIMALS[RANDOM.nextInt(ANIMALS.length)];
        int number = 1000 + RANDOM.nextInt(9000);
        return "%s %s%d".formatted(adjective, animal, number);
    }

    static AvatarPreset randomAvatar() {
        AvatarPreset[] values = AvatarPreset.values();
        return values[RANDOM.nextInt(values.length)];
    }
}
