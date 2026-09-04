package io.sj.saju.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RandomProfileGeneratorTest {

    @Test
    void randomNicknameMatchesTheAdjectiveAnimalNumberPattern() {
        String nickname = RandomProfileGenerator.randomNickname();

        assertThat(nickname).matches("^\\S+ \\S+\\d{4}$");
    }

    @Test
    void randomAvatarIsAlwaysOneOfThePresets() {
        AvatarPreset avatar = RandomProfileGenerator.randomAvatar();

        assertThat(avatar).isIn((Object[]) AvatarPreset.values());
    }
}
