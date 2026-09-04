package io.sj.saju.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sj.saju.auth.OAuthProvider;
import io.sj.saju.auth.UserAccount;
import io.sj.saju.auth.UserAccountRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 실제 로컬 Postgres를 쓰는 통합 테스트 — CreditServiceTest와 같은 이유(크레딧 지급이 실제로 원자적 SQL을 거친다). */
@SpringBootTest
@Transactional
class AttendanceServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceCheckRepository attendanceCheckRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "test-" + UUID.randomUUID(), "테스터"));
    }

    @Test
    void statusBeforeAnyCheckInShowsStreakOneAndBaseReward() {
        AttendanceService.Status status = attendanceService.status(user.getId());

        assertThat(status.checkedInToday()).isFalse();
        assertThat(status.streak()).isEqualTo(1);
        assertThat(status.baseReward()).isEqualTo(AttendanceService.BASE_REWARD);
        assertThat(status.bonusReward()).isZero();
    }

    @Test
    void firstCheckInGrantsBaseRewardAndStartsStreakAtOne() {
        AttendanceService.CheckInResult result = attendanceService.checkIn(user.getId());

        assertThat(result.streak()).isEqualTo(1);
        assertThat(result.creditsGranted()).isEqualTo(AttendanceService.BASE_REWARD);
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance())
                .isEqualTo(AttendanceService.BASE_REWARD);
    }

    @Test
    void checkingInTwiceOnTheSameDayIsRejected() {
        attendanceService.checkIn(user.getId());

        assertThatThrownBy(() -> attendanceService.checkIn(user.getId()))
                .isInstanceOf(AlreadyCheckedInException.class);
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance())
                .isEqualTo(AttendanceService.BASE_REWARD);
    }

    @Test
    void statusAfterCheckingInReflectsTodaysActualStreakAndZeroFurtherReward() {
        attendanceService.checkIn(user.getId());

        AttendanceService.Status status = attendanceService.status(user.getId());

        assertThat(status.checkedInToday()).isTrue();
        assertThat(status.streak()).isEqualTo(1);
        assertThat(status.baseReward()).isZero();
        assertThat(status.bonusReward()).isZero();
    }

    @Test
    void checkingInOnConsecutiveDaysExtendsTheStreak() {
        LocalDate today = LocalDate.now(KST);
        seedCheckIn(today.minusDays(1), 3);

        AttendanceService.CheckInResult result = attendanceService.checkIn(user.getId());

        assertThat(result.streak()).isEqualTo(4);
        assertThat(result.creditsGranted()).isEqualTo(AttendanceService.BASE_REWARD);
    }

    @Test
    void missingADayResetsTheStreakToOne() {
        LocalDate today = LocalDate.now(KST);
        seedCheckIn(today.minusDays(2), 5); // 어제가 아니라 그저께 — 하루 빠뜨림

        AttendanceService.CheckInResult result = attendanceService.checkIn(user.getId());

        assertThat(result.streak()).isEqualTo(1);
    }

    @Test
    void theSeventhConsecutiveDayGrantsTheStreakBonus() {
        LocalDate today = LocalDate.now(KST);
        seedCheckIn(today.minusDays(1), 6);

        AttendanceService.CheckInResult result = attendanceService.checkIn(user.getId());

        assertThat(result.streak()).isEqualTo(7);
        assertThat(result.baseReward()).isEqualTo(AttendanceService.BASE_REWARD);
        assertThat(result.bonusReward()).isEqualTo(AttendanceService.STREAK_BONUS);
        assertThat(result.creditsGranted())
                .isEqualTo(AttendanceService.BASE_REWARD + AttendanceService.STREAK_BONUS);
    }

    private void seedCheckIn(LocalDate date, int streak) {
        attendanceCheckRepository.saveAndFlush(new AttendanceCheck(user.getId(), date, streak));
    }
}
