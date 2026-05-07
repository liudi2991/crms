package com.company.crms.common;

import com.company.crms.common.annotation.SensitiveField.Mask;
import com.company.crms.common.util.MaskUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskUtilTest {

    @Test
    void phone() {
        assertEquals("138****1234", MaskUtil.mask("13812341234", Mask.PHONE));
    }

    @Test
    void email() {
        assertEquals("z****@example.com", MaskUtil.mask("zhangsan@example.com", Mask.EMAIL));
    }

    @Test
    void name_chinese() {
        assertEquals("张*", MaskUtil.mask("张三", Mask.NAME));
        assertEquals("张*三", MaskUtil.mask("张二三", Mask.NAME));
    }

    @Test
    void id_card() {
        assertEquals("3101**********1234", MaskUtil.mask("310101199001011234", Mask.ID_CARD));
    }

    @Test
    void none_returns_origin() {
        assertEquals("abc", MaskUtil.mask("abc", Mask.NONE));
    }
}
