package com.flowguard.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class KeyGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int KEY_LENGTH_BYTES = 32;

    public String generateKey() {
        byte[] bytes = new byte[KEY_LENGTH_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
