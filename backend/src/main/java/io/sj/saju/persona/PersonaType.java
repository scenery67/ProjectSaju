package io.sj.saju.persona;

/**
 * Saju persona offerings. Mirrors the frontend PersonaType union in
 * src/types/saju.ts — keep both in sync when adding a new persona.
 * 프론트엔드 PersonaType과 값이 일치해야 한다. 새 페르소나 추가 시 함께 수정.
 */
public enum PersonaType {
    BREAKUP,
    COUPLE_COMPATIBILITY
}
