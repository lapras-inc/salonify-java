package com.example.salonify.support;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class Tokens {
    private final SecureRandom random = new SecureRandom();

    /** ゼロ埋めされた6桁の数字コード。 */
    public String sixDigitCode() {
        int n = random.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    /** URLセーフなランダム招待コード(12バイトのランダム値をbase64url、パディングなしでエンコード)。 */
    public String inviteCode() {
        byte[] buf = new byte[12];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
