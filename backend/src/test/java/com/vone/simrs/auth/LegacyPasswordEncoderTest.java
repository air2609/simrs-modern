package com.vone.simrs.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LegacyPasswordEncoderTest {

    private final LegacyPasswordEncoder legacyPasswordEncoder = new LegacyPasswordEncoder();

    @Test
    void shouldMatchStandardMd5Output() {
        Assertions.assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", legacyPasswordEncoder.encode("password"));
    }
}
