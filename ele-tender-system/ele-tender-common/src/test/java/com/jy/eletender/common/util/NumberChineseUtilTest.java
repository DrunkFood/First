package com.jy.eletender.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumberChineseUtilTest {

    @Test
    void shouldConvertArabicToChinese() {
        assertThat(NumberChineseUtil.toChineseNumber(1)).isEqualTo("一");
        assertThat(NumberChineseUtil.toChineseNumber(10)).isEqualTo("十");
        assertThat(NumberChineseUtil.toChineseNumber(11)).isEqualTo("十一");
        assertThat(NumberChineseUtil.toChineseNumber(20)).isEqualTo("二十");
        assertThat(NumberChineseUtil.toChineseNumber(26)).isEqualTo("二十六");
    }

    @Test
    void shouldConvertChineseToArabic() {
        assertThat(NumberChineseUtil.toArabicNumber("一")).isEqualTo(1);
        assertThat(NumberChineseUtil.toArabicNumber("十")).isEqualTo(10);
        assertThat(NumberChineseUtil.toArabicNumber("十一")).isEqualTo(11);
        assertThat(NumberChineseUtil.toArabicNumber("二十")).isEqualTo(20);
        assertThat(NumberChineseUtil.toArabicNumber("二十六")).isEqualTo(26);
    }
}
