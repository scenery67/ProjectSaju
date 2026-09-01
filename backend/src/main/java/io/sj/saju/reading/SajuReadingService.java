package io.sj.saju.reading;

import io.sj.saju.persona.PersonaType;
import io.sj.saju.reading.dto.BreakupReadingRequest;
import io.sj.saju.reading.dto.CoupleCompatibilityRequest;
import io.sj.saju.reading.dto.PersonInput;
import io.sj.saju.reading.dto.SajuReadingResult;
import org.springframework.stereotype.Service;

/**
 * TODO: replace the stub text below with the real saju calculation engine
 * (사주팔자 계산 로직 연동 필요). For now this only proves the request/response
 * and persistence wiring end to end.
 */
@Service
public class SajuReadingService {

    private final ReadingRecordRepository readingRecordRepository;

    public SajuReadingService(ReadingRecordRepository readingRecordRepository) {
        this.readingRecordRepository = readingRecordRepository;
    }

    public SajuReadingResult readBreakup(BreakupReadingRequest request) {
        PersonInput self = request.self();
        String summary = self.name() + "님의 이별사주 결과 (샘플)";
        String detail = "실제 사주 풀이 로직은 아직 연결되지 않았습니다. 입력값: " + self;

        readingRecordRepository.save(new ReadingRecord(
                PersonaType.BREAKUP, self.name(), null, summary, detail));

        return new SajuReadingResult(PersonaType.BREAKUP, summary, detail);
    }

    public SajuReadingResult readCoupleCompatibility(CoupleCompatibilityRequest request) {
        PersonInput self = request.self();
        PersonInput partner = request.partner();
        String summary = self.name() + " & " + partner.name() + "님의 궁합 결과 (샘플)";
        String detail = "실제 사주 풀이 로직은 아직 연결되지 않았습니다. 본인: " + self + ", 상대방: " + partner;

        readingRecordRepository.save(new ReadingRecord(
                PersonaType.COUPLE_COMPATIBILITY, self.name(), partner.name(), summary, detail));

        return new SajuReadingResult(PersonaType.COUPLE_COMPATIBILITY, summary, detail);
    }
}
