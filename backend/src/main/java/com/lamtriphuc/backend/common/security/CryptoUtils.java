package com.lamtriphuc.backend.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class CryptoUtils {
    @Value("${crypto.aes.secret:MySuperSecretKeyForAes2561234567}") // Key dài 32 bytes
    private String aesSecret;

    public String hashIdentifier(String rawIdentifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawIdentifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi băm dữ liệu", e);
        }
    }

    public String encryptProfile(String rawProfile) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(aesSecret.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(rawProfile.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa AES", e);
        }
    }
}
