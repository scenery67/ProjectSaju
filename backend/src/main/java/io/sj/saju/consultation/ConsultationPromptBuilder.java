package io.sj.saju.consultation;

import io.sj.saju.reading.dto.PersonalityProfile;
import io.sj.saju.reading.dto.SajuReadingResult;

/**
 * Builds the LLM system prompt from an already-computed reading. Reuses the
 * reading's own summary/detail text (already written in the app's warm,
 * plain-language tone — see SajuReadingService) instead of re-deriving raw
 * chart fields, so the LLM's tone stays consistent with the rest of the app.
 */
final class ConsultationPromptBuilder {

    private ConsultationPromptBuilder() {
    }

    static String systemPrompt(SajuReadingResult result) {
        return switch (result.personaType()) {
            case BREAKUP -> breakup(result);
            case COUPLE_COMPATIBILITY -> coupleCompatibility(result);
        };
    }

    private static String breakup(SajuReadingResult result) {
        PersonalityProfile profile = result.selfChart().personalityProfile();
        return """
                당신은 "다숨"이라는 이름의 다정한 위로형 사주 상담사입니다. 헤어진 마음을
                사주로 따뜻하게 짚어주는 상담사로, "~했을 거예요" 같은 위로 중심의 말투를 씁니다.

                상담 대상자의 사주 풀이 결과:
                %s

                %s

                - 성격: %s
                - 연애: %s
                - 직업: %s
                - 재물: %s
                - 대인관계: %s

                위 내용을 참고해서 사용자의 질문에 따뜻하고 공감가는 톤으로 답변하세요.
                의료·법률·재무 조언(질병 예측, 투자 권유 등)은 하지 마세요. 사주는 재미로
                참고하는 내용임을 필요할 때 자연스럽게 알려주세요. 답변은 3~5문장 정도로 짧게 하세요.
                """.formatted(
                result.summary(), result.detail(),
                profile.personality(), profile.love(), profile.career(),
                profile.wealth(), profile.relationships());
    }

    private static String coupleCompatibility(SajuReadingResult result) {
        PersonalityProfile selfProfile = result.selfChart().personalityProfile();
        PersonalityProfile partnerProfile = result.partnerChart().personalityProfile();
        return """
                당신은 "설레"라는 이름의 사주 상담사입니다. 두 사람의 궁합을 설레는 마음으로
                봐주는 캐릭터로, 긍정적이고 응원하는 톤을 씁니다.

                상담 대상자 커플의 궁합 풀이 결과:
                %s

                %s

                두 사람 각각의 성향도 참고하세요.
                - 본인 — 성격: %s / 연애: %s
                - 상대방 — 성격: %s / 연애: %s

                위 내용을 참고해서 사용자의 질문에 긍정적이고 응원하는 톤으로 답변하세요.
                의료·법률·재무 조언은 하지 마세요. 사주는 재미로 참고하는 내용임을 필요할 때
                자연스럽게 알려주세요. 답변은 3~5문장 정도로 짧게 하세요.
                """.formatted(
                result.summary(), result.detail(),
                selfProfile.personality(), selfProfile.love(),
                partnerProfile.personality(), partnerProfile.love());
    }
}
