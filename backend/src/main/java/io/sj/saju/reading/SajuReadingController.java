package io.sj.saju.reading;

import io.sj.saju.reading.dto.BreakupReadingRequest;
import io.sj.saju.reading.dto.CoupleCompatibilityRequest;
import io.sj.saju.reading.dto.ReadingHistoryEntry;
import io.sj.saju.reading.dto.SajuReadingResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saju")
public class SajuReadingController {

    private final SajuReadingService sajuReadingService;

    public SajuReadingController(SajuReadingService sajuReadingService) {
        this.sajuReadingService = sajuReadingService;
    }

    // userAccountId는 로그인했을 때만 채워진다(Authorization 헤더 없으면 null) —
    // 이 엔드포인트 자체는 계속 비로그인으로도 쓸 수 있는 permitAll이다.
    @PostMapping("/breakup")
    public SajuReadingResult readBreakup(
            @Valid @RequestBody BreakupReadingRequest request,
            @AuthenticationPrincipal UUID userAccountId,
            @RequestParam(defaultValue = "false") boolean useCredit) {
        return sajuReadingService.readBreakup(request, userAccountId, useCredit);
    }

    @PostMapping("/couple-compatibility")
    public SajuReadingResult readCoupleCompatibility(
            @Valid @RequestBody CoupleCompatibilityRequest request,
            @AuthenticationPrincipal UUID userAccountId,
            @RequestParam(defaultValue = "false") boolean useCredit) {
        return sajuReadingService.readCoupleCompatibility(request, userAccountId, useCredit);
    }

    /** 로그인한 사용자의 서버 저장 사주 기록 — SecurityConfig에서 인증을 요구한다. */
    @GetMapping("/history")
    public ResponseEntity<List<ReadingHistoryEntry>> history(@AuthenticationPrincipal UUID userAccountId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(sajuReadingService.history(userAccountId));
    }
}
