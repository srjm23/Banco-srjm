package com.bancoprogramacao.api.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SensitiveDataService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int IV_LENGTH = 12;
    private final SecretKeySpec encryptionKey;

    public SensitiveDataService(@Value("${app.data-encryption-key}") String configuredKey) {
        this.encryptionKey = new SecretKeySpec(sha256(configuredKey.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível cifrar o dado sensível.", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            ByteBuffer payload = ByteBuffer.wrap(Base64.getDecoder().decode(encryptedValue));
            byte[] iv = new byte[IV_LENGTH];
            payload.get(iv);
            byte[] encrypted = new byte[payload.remaining()];
            payload.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Não foi possível decifrar o dado sensível.", exception);
        }
    }

    public String hash(String value) {
        return HexFormat.of().formatHex(sha256(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }
}
