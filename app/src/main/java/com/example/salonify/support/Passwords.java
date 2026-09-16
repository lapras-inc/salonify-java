package com.example.salonify.support;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * コストファクター10のBCryptを使用したパスワードハッシュ化(リファレンス実装と同一)。
 * spring-security-crypto のみを使用しており、Spring Security のフィルターチェーンは有効化していない。
 */
@Component
public class Passwords {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public boolean verify(String raw, String hash) {
        if (hash == null || hash.isEmpty()) return false;
        try {
            return encoder.matches(raw, hash);
        } catch (IllegalArgumentException e) {
            // hash が有効な bcrypt 文字列でない場合(例: "suspended" / "deleted") -> 決して一致しない
            return false;
        }
    }
}
