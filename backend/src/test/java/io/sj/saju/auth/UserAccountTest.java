package io.sj.saju.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    @Test
    void recordLoginUpdatesLastLoginAtButNeverTouchesNickname() throws InterruptedException {
        UserAccount account = new UserAccount(OAuthProvider.KAKAO, "kakao-1", "랜덤 별명", AvatarPreset.FOX);
        Instant createdLastLogin = account.getLastLoginAt();

        Thread.sleep(5);
        account.recordLogin();

        assertThat(account.getNickname()).isEqualTo("랜덤 별명");
        assertThat(account.getLastLoginAt()).isAfter(createdLastLogin);
    }

    @Test
    void updateProfileChangesNicknameAndAvatar() {
        UserAccount account = new UserAccount(OAuthProvider.KAKAO, "kakao-1", "랜덤 별명", AvatarPreset.FOX);

        account.updateProfile("새 별명", AvatarPreset.PANDA);

        assertThat(account.getNickname()).isEqualTo("새 별명");
        assertThat(account.getAvatarKey()).isEqualTo(AvatarPreset.PANDA);
    }
}
