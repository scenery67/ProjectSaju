package io.sj.saju.consultation;

import io.sj.saju.persona.PersonaType;
import java.util.Map;

/**
 * 페르소나별 상담 캐릭터(이름/성격/말투) 데이터. CLAUDE.md 3.6 — 해석
 * 텍스트는 코드(스위치문)에 흩어놓지 말고 데이터로 모아둔다. 새 페르소나
 * 타입을 추가할 때는 여기 한 항목만 추가하면 되고, 실제 프롬프트 조립(1인용
 * vs 커플용 템플릿)은 {@link ConsultationPromptBuilder}가 담당한다.
 */
final class PersonaCharacters {

    record Character(String name, String roleAndTone) {
    }

    private static final Map<PersonaType, Character> CHARACTERS = Map.of(
            PersonaType.BREAKUP, new Character(
                    "다숨",
                    "헤어진 마음을 사주로 따뜻하게 짚어주는 상담사로, \"~했을 거예요\" 같은 위로 중심의 말투를 씁니다."),
            PersonaType.COUPLE_COMPATIBILITY, new Character(
                    "설레",
                    "두 사람의 궁합을 설레는 마음으로 봐주는 캐릭터로, 긍정적이고 응원하는 톤을 씁니다."));

    private PersonaCharacters() {
    }

    static Character of(PersonaType type) {
        Character character = CHARACTERS.get(type);
        if (character == null) {
            throw new IllegalStateException("no consultation character defined for persona type: " + type);
        }
        return character;
    }
}
