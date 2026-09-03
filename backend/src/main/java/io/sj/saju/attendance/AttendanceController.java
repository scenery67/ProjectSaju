package io.sj.saju.attendance;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/attendance/**는 permitAll 목록에 없어 SecurityConfig의
// anyRequest().authenticated()가 그대로 적용된다 — 로그인 필요.
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/status")
    public ResponseEntity<AttendanceService.Status> status(@AuthenticationPrincipal UUID userAccountId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(attendanceService.status(userAccountId));
    }

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceService.CheckInResult> checkIn(@AuthenticationPrincipal UUID userAccountId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(attendanceService.checkIn(userAccountId));
    }
}
