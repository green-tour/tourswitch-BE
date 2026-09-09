package com.tourswitch.global.security.jwt;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RefreshTokenHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Refresh Token을 SHA-256 해시 문자열로 변환
     */
    public String hash(String refreshToken) {
        try {
            MessageDigest messageDigest =
                MessageDigest.getInstance(HASH_ALGORITHM);

            byte[] hashBytes = messageDigest.digest(
                refreshToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 해시 알고리즘을 사용할 수 없습니다.",
                exception
            );
        }
    }
}