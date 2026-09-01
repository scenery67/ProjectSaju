package io.sj.saju.reading.saju;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenGodGroupTest {

    @Test
    void allTenTenGodsMapToExactlyOneGroup() {
        assertThat(TenGodGroup.of("비견")).isEqualTo(TenGodGroup.BIGYEOP);
        assertThat(TenGodGroup.of("겁재")).isEqualTo(TenGodGroup.BIGYEOP);
        assertThat(TenGodGroup.of("식신")).isEqualTo(TenGodGroup.SIKSANG);
        assertThat(TenGodGroup.of("상관")).isEqualTo(TenGodGroup.SIKSANG);
        assertThat(TenGodGroup.of("편재")).isEqualTo(TenGodGroup.JAESEONG);
        assertThat(TenGodGroup.of("정재")).isEqualTo(TenGodGroup.JAESEONG);
        assertThat(TenGodGroup.of("편관")).isEqualTo(TenGodGroup.GWANSEONG);
        assertThat(TenGodGroup.of("정관")).isEqualTo(TenGodGroup.GWANSEONG);
        assertThat(TenGodGroup.of("편인")).isEqualTo(TenGodGroup.INSEONG);
        assertThat(TenGodGroup.of("정인")).isEqualTo(TenGodGroup.INSEONG);
    }

    @Test
    void unknownTenGodThrows() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> TenGodGroup.of("일주"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
