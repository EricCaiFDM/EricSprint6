package com.example.banking.lib.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

class PasswordHasher {
    String hash(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Hash algorithm unavailable", ex);
        }
    }

    boolean verify(String plain, String expectedHash) {
        return hash(plain).equals(expectedHash);
    }
}
