package io.sj.saju.auth;

/**
 * 업로드된 사진 대신 고르는 고정 이모지 아바타 — 실제 얼굴 사진을 받지
 * 않아 저장/노출 리스크가 없고, 별도 업로드 인프라도 필요 없다.
 */
public enum AvatarPreset {
    FOX("🦊"),
    RABBIT("🐰"),
    BEAR("🐻"),
    CAT("🐱"),
    TIGER("🐯"),
    PANDA("🐼"),
    DOG("🐶"),
    OWL("🦉");

    private final String emoji;

    AvatarPreset(String emoji) {
        this.emoji = emoji;
    }

    public String emoji() {
        return emoji;
    }
}
