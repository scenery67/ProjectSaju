package io.sj.saju.attendance;

import io.sj.saju.billing.CreditService;
import io.sj.saju.notification.NotificationService;
import io.sj.saju.notification.NotificationType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 하루 1회 출석 체크로 크레딧을 지급한다. 매일 기본 {@value #BASE_REWARD}개,
 * {@value #STREAK_BONUS_INTERVAL}일 연속 출석마다(7, 14, 21...) 보너스
 * {@value #STREAK_BONUS}개를 추가로 준다 — 재방문을 유도하는 순수 게임화
 * 요소라 실제 결제/환불과는 무관하다({@link CreditService#grantFree} 재사용).
 */
@Service
public class AttendanceService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final int BASE_REWARD = 2;
    static final int STREAK_BONUS = 3;
    static final int STREAK_BONUS_INTERVAL = 7;

    private final AttendanceCheckRepository attendanceCheckRepository;
    private final CreditService creditService;
    private final NotificationService notificationService;

    public AttendanceService(
            AttendanceCheckRepository attendanceCheckRepository,
            CreditService creditService,
            NotificationService notificationService) {
        this.attendanceCheckRepository = attendanceCheckRepository;
        this.creditService = creditService;
        this.notificationService = notificationService;
    }

    /**
     * @param checkedInToday 오늘 이미 체크했는지
     * @param streak         오늘 이미 체크했으면 실제 달성한 연속 일수, 아니면
     *                       "지금 체크하면" 달성할 연속 일수
     * @param baseReward     오늘 이미 체크했으면 0(추가 지급 없음), 아니면
     *                       "지금 체크하면" 받을 기본 크레딧 수
     * @param bonusReward    오늘 이미 체크했으면 0, 아니면 "지금 체크하면" 추가로
     *                       받을 스트릭 보너스(보너스 없는 날이면 0) — 프론트가
     *                       기본/보너스 이펙트를 따로 보여줄 수 있게 나눠서 준다
     */
    public record Status(boolean checkedInToday, int streak, int baseReward, int bonusReward) {
    }

    public record CheckInResult(int streak, int baseReward, int bonusReward) {
        public int creditsGranted() {
            return baseReward + bonusReward;
        }
    }

    public Status status(UUID userAccountId) {
        LocalDate today = LocalDate.now(KST);
        Optional<AttendanceCheck> todayCheck =
                attendanceCheckRepository.findByUserAccountIdAndCheckedDate(userAccountId, today);
        if (todayCheck.isPresent()) {
            return new Status(true, todayCheck.get().getStreakCount(), 0, 0);
        }
        int streak = nextStreak(userAccountId, today);
        return new Status(false, streak, BASE_REWARD, bonusFor(streak));
    }

    @Transactional
    public CheckInResult checkIn(UUID userAccountId) {
        LocalDate today = LocalDate.now(KST);
        if (attendanceCheckRepository.existsByUserAccountIdAndCheckedDate(userAccountId, today)) {
            throw new AlreadyCheckedInException(userAccountId);
        }
        int streak = nextStreak(userAccountId, today);
        int bonus = bonusFor(streak);
        int totalReward = BASE_REWARD + bonus;
        AttendanceCheck check = attendanceCheckRepository.save(new AttendanceCheck(userAccountId, today, streak));
        creditService.grantFree(userAccountId, totalReward, check.getId(),
                "출석 체크 보상 (%d일 연속)".formatted(streak));
        String body = bonus > 0
                ? "%d일 연속 출석 보너스로 총 %d크레딧을 받았어요! (기본 %d + 보너스 %d)"
                        .formatted(streak, totalReward, BASE_REWARD, bonus)
                : "%d일 연속 출석으로 %d크레딧을 받았어요!".formatted(streak, totalReward);
        notificationService.notify(userAccountId, NotificationType.ATTENDANCE_BONUS, "출석 체크 완료", body, totalReward);
        return new CheckInResult(streak, BASE_REWARD, bonus);
    }

    /** 어제도 체크했으면 그 스트릭 +1, 아니면(끊겼거나 첫 출석) 1부터 다시. */
    private int nextStreak(UUID userAccountId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        return attendanceCheckRepository.findByUserAccountIdAndCheckedDate(userAccountId, yesterday)
                .map(c -> c.getStreakCount() + 1)
                .orElse(1);
    }

    private int bonusFor(int streak) {
        return streak % STREAK_BONUS_INTERVAL == 0 ? STREAK_BONUS : 0;
    }
}
