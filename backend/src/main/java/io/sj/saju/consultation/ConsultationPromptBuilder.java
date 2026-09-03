package io.sj.saju.consultation;

import io.sj.saju.reading.dto.PersonalityProfile;
import io.sj.saju.reading.dto.SajuReadingResult;

/**
 * Builds the LLM system prompt from an already-computed reading. Reuses the
 * reading's own summary/detail text (already written in the app's warm,
 * plain-language tone — see SajuReadingService) instead of re-deriving raw
 * chart fields, so the LLM's tone stays consistent with the rest of the app.
 *
 * <p>Two structural templates only — single-person (BREAKUP류) vs 두 사람
 * (COUPLE_COMPATIBILITY류) — since 상담 컨텍스트 구조는 "혼자 보는 사주"와
 * "두 사람을 같이 보는 사주" 두 가지뿐이다. 캐릭터 이름/성격/말투는
 * {@link PersonaCharacters}에서 가져온다 — 새 페르소나 타입을 추가할 때
 * 이 구조(1인용/커플용 중 어느 템플릿을 쓸지)만 한 줄 정하면 되고, 문구
 * 자체를 다시 쓸 필요는 없다.
 */
final class ConsultationPromptBuilder {

    private static final String GUARDRAILS = """
            의료·법률·재무 조언(질병 예측, 투자 권유 등)은 하지 마세요. 사주는 재미로
            참고하는 내용임을 필요할 때 자연스럽게 알려주세요. 답변은 3~5문장 정도로 짧게 하세요.""";

    private ConsultationPromptBuilder() {
    }

    static String systemPrompt(SajuReadingResult result) {
        return switch (result.personaType()) {
            case BREAKUP -> singlePersonPrompt(result);
            case COUPLE_COMPATIBILITY -> twoPersonPrompt(result);
        };
    }

    private static String singlePersonPrompt(SajuReadingResult result) {
        PersonaCharacters.Character character = PersonaCharacters.of(result.personaType());
        PersonalityProfile profile = result.selfChart().personalityProfile();
        return """
                당신은 "%s"라는 이름의 사주 상담사입니다. %s

                상담 대상자의 사주 풀이 결과:
                %s

                %s

                - 성격: %s
                - 연애: %s
                - 직업: %s
                - 재물: %s
                - 대인관계: %s

                위 내용을 참고해서 사용자의 질문에 이 캐릭터의 성격과 말투에 맞게 답변하세요.
                %s
                """.formatted(
                character.name(), character.roleAndTone(),
                result.summary(), result.detail(),
                profile.personality(), profile.love(), profile.career(),
                profile.wealth(), profile.relationships(),
                GUARDRAILS);
    }

    private static String twoPersonPrompt(SajuReadingResult result) {
        PersonaCharacters.Character character = PersonaCharacters.of(result.personaType());
        PersonalityProfile selfProfile = result.selfChart().personalityProfile();
        PersonalityProfile partnerProfile = result.partnerChart().personalityProfile();
        return """
                당신은 "%s"라는 이름의 사주 상담사입니다. %s

                상담 대상자 커플의 궁합 풀이 결과:
                %s

                %s

                두 사람 각각의 성향도 참고하세요.
                - 본인 — 성격: %s / 연애: %s
                - 상대방 — 성격: %s / 연애: %s

                위 내용을 참고해서 사용자의 질문에 이 캐릭터의 성격과 말투에 맞게 답변하세요.
                %s
                """.formatted(
                character.name(), character.roleAndTone(),
                result.summary(), result.detail(),
                selfProfile.personality(), selfProfile.love(),
                partnerProfile.personality(), partnerProfile.love(),
                GUARDRAILS);
    }
}
