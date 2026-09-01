package io.sj.saju.reading;

import io.sj.saju.reading.dto.BreakupReadingRequest;
import io.sj.saju.reading.dto.CoupleCompatibilityRequest;
import io.sj.saju.reading.dto.SajuReadingResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saju")
public class SajuReadingController {

    private final SajuReadingService sajuReadingService;

    public SajuReadingController(SajuReadingService sajuReadingService) {
        this.sajuReadingService = sajuReadingService;
    }

    @PostMapping("/breakup")
    public SajuReadingResult readBreakup(@Valid @RequestBody BreakupReadingRequest request) {
        return sajuReadingService.readBreakup(request);
    }

    @PostMapping("/couple-compatibility")
    public SajuReadingResult readCoupleCompatibility(
            @Valid @RequestBody CoupleCompatibilityRequest request) {
        return sajuReadingService.readCoupleCompatibility(request);
    }
}
