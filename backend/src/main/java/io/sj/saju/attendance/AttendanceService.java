package io.sj.saju.attendance;

import io.sj.saju.billing.CreditService;
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

    public AttendanceService(AttendanceCheckRepository attendanceCheckRepository, CreditService creditService) {
        this.attendanceCheckRepository = attendanceCheckRepository;
        this.creditService = creditService;
    }

    /**
     * @param checkedInToday 오늘 이미 체크했는지
     * @param streak         오늘 이미 체크했으면 실제 달성한 연속 일수, 아니면
     *                       "지금 체크하면" 달성할 연속 일수
     * @param reward         오늘 이미 체크했으면 0(추가 지급 없음), 아니면
     *                       "지금 체크하면" 받을 크레딧 수
     */
    public record Status(boolean checkedInToday, int streak, int reward) {
    }

    public record CheckInResult(int streak, int creditsGranted) {
    }

    public Status status(UUID userAccountId) {
        LocalDate today = LocalDate.now(KST);
        Optional<AttendanceCheck> todayCheck =
                attendanceCheckRepository.findByUserAccountIdAndCheckedDate(userAccountId, today);
        if (todayCheck.isPresent()) {
            return new Status(true, todayCheck.get().getStreakCount(), 0);
        }
        int streak = nextStreak(userAccountId, today);
        return new Status(false, streak, rewardFor(streak));
    }

    @Transactional
    public CheckInResult checkIn(UUID userAccountId) {
        LocalDate today = LocalDate.now(KST);
        if (attendanceCheckRepository.existsByUserAccountIdAndCheckedDate(userAccountId, today)) {
            throw new AlreadyCheckedInException(userAccountId);
        }
        int streak = nextStreak(userAccountId, today);
        int reward = rewardFor(streak);
        AttendanceCheck check = attendanceCheckRepository.save(new AttendanceCheck(userAccountId, today, streak));
        creditService.grantFree(userAccountId, reward, check.getId(),
                "출석 체크 보상 (%d일 연속)".formatted(streak));
        return new CheckInResult(streak, reward);
    }

    /** 어제도 체크했으면 그 스트릭 +1, 아니면(끊겼거나 첫 출석) 1부터 다시. */
    private int nextStreak(UUID userAccountId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        return attendanceCheckRepository.findByUserAccountIdAndCheckedDate(userAccountId, yesterday)
                .map(c -> c.getStreakCount() + 1)
                .orElse(1);
    }

    private int rewardFor(int streak) {
        int reward = BASE_REWARD;
        if (streak % STREAK_BONUS_INTERVAL == 0) {
            reward += STREAK_BONUS;
        }
        return reward;
    }
}
