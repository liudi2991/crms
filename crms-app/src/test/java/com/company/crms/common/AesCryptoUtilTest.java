package com.company.crms.common;

import com.company.crms.common.util.AesCryptoUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesCryptoUtilTest {

    private final AesCryptoUtil util = new AesCryptoUtil("0123456789abcdef0123456789abcdef");

    @Test
    void roundtrip() {
        String origin = "13800001234";
        String enc = util.encrypt(origin);
        assertNotEquals(origin, enc);
        assertEquals(origin, util.decrypt(enc));
    }

    @Test
    void each_encrypt_should_use_random_iv() {
        assertNotEquals(util.encrypt("hello"), util.encrypt("hello"));
    }

    @Test
    void null_input_returns_null() {
        assertNull(util.encrypt(null));
        assertNull(util.decrypt(null));
    }

    @Test
    void invalid_key_length_throws() {
        assertThrows(IllegalArgumentException.class, () -> new AesCryptoUtil("short"));
    }
}
