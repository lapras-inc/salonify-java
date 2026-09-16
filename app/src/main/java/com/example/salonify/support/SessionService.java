package com.example.salonify.support;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * ステートレスなJWTセッション(HS256)。ペイロードは {uid}、有効期限は30日で、httpOnlyクッキー "session" に保存される。
 * リファレンス実装(jose HS256)に対応する。サーバー側のセッションストアは持たず、ログアウトは単にクッキーをクリアするだけ。
 */
@Component
public class SessionService {

    public static final String COOKIE_NAME = "session";
    private static final long MAX_AGE_SECONDS = 30L * 24 * 60 * 60; // 30日

    private final SecretKey key;

    public SessionService(@Value("${salonify.session-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(String uid) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("uid", uid)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(MAX_AGE_SECONDS)))
                .signWith(key)
                .compact();
    }

    /** uid を返す。トークンが存在しない/不正/期限切れの場合は null を返す。 */
    public String verify(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().get("uid", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String buildSessionCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true).sameSite("Lax").path("/").maxAge(MAX_AGE_SECONDS)
                .build().toString();
    }

    public String buildClearCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true).sameSite("Lax").path("/").maxAge(0)
                .build().toString();
    }
}
