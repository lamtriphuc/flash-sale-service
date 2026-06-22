package com.baas.flashsale.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return "fs_" + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
