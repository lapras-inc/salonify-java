package com.example.salonify.controller;

import com.example.salonify.entity.EmailVerification;
import com.example.salonify.entity.PasswordReset;
import com.example.salonify.entity.User;
import com.example.salonify.repository.EmailVerificationRepository;
import com.example.salonify.repository.PasswordResetRepository;
import com.example.salonify.repository.UserRepository;
import com.example.salonify.support.*;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@Controller
public class AuthController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository users;
    private final EmailVerificationRepository verifications;
    private final PasswordResetRepository resets;
    private final Passwords passwords;
    private final Tokens tokens;
    private final EmailSender emailSender;
    private final RateLimiter rateLimiter;
    private final Auth auth;

    public AuthController(UserRepository users, EmailVerificationRepository verifications,
                          PasswordResetRepository resets, Passwords passwords, Tokens tokens,
                          EmailSender emailSender, RateLimiter rateLimiter, Auth auth) {
        this.users = users;
        this.verifications = verifications;
        this.resets = resets;
        this.passwords = passwords;
        this.tokens = tokens;
        this.emailSender = emailSender;
        this.rateLimiter = rateLimiter;
        this.auth = auth;
    }

    // ---------- サインアップ ----------
    @PostMapping("/api/auth/signup")
    @Transactional
    public ResponseEntity<Void> signup(@RequestParam(defaultValue = "") String email,
                                       @RequestParam(defaultValue = "") String password,
                                       @RequestParam(defaultValue = "") String displayName,
                                       @RequestParam(required = false) String next,
                                       HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!rateLimiter.allow("signup:" + ip, 10, 60_000)) return tooMany();
        if (!rateLimiter.allow("email:" + email, 3, 600_000)) return see("/signup?error=rate" + nextParam(next));

        boolean valid = EMAIL.matcher(email).matches()
                && password.length() >= 8
                && !displayName.isEmpty() && displayName.length() <= 40;
        if (!valid) return see("/signup?error=invalid" + nextParam(next));
        if (users.existsByEmail(email)) return see("/signup?error=exists" + nextParam(next));

        String code = tokens.sixDigitCode();
        verifications.deleteByEmail(email);
        EmailVerification v = new EmailVerification();
        v.setEmail(email);
        v.setCode(code);
        v.setDisplayName(displayName);
        v.setPasswordHash(passwords.hash(password));
        v.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        verifications.save(v);
        emailSender.sendVerificationCode(email, code);

        return see("/signup/verify?email=" + enc(email) + nextParam(next));
    }

    // ---------- 確認コード検証 ----------
    @PostMapping("/api/auth/verify")
    @Transactional
    public ResponseEntity<Void> verify(@RequestParam(defaultValue = "") String email,
                                       @RequestParam(defaultValue = "") String code,
                                       @RequestParam(required = false) String next,
                                       HttpServletResponse response) {
        if (!rateLimiter.allow("verify:" + email, 5, 600_000))
            return see("/signup/verify?email=" + enc(email) + "&error=rate" + nextParam(next));

        String dest = safeDest(next);
        EmailVerification v = verifications.findFirstByEmailAndCodeOrderByCreatedAtDesc(email, code).orElse(null);
        if (v == null || v.getExpiresAt().isBefore(Instant.now()))
            return see("/signup/verify?email=" + enc(email) + "&error=invalid" + nextParam(next));

        if (users.existsByEmail(email)) {
            verifications.deleteByEmail(email);
            return see("/signup/verify?email=" + enc(email) + "&error=exists" + nextParam(next));
        }

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(v.getPasswordHash());
        u.setDisplayName(v.getDisplayName());
        u.setEmailVerified(true);
        users.save(u);
        verifications.deleteByEmail(email);

        auth.login(response, u.getId());
        return see(dest);
    }

    // ---------- コード再送信 ----------
    @PostMapping("/api/auth/resend-code")
    @Transactional
    public ResponseEntity<Void> resend(@RequestParam(defaultValue = "") String email) {
        if (!rateLimiter.allow("email:" + email, 3, 600_000))
            return see("/signup/verify?email=" + enc(email));
        if (email.isEmpty()) return see("/signup");
        EmailVerification v = verifications.findFirstByEmailOrderByCreatedAtDesc(email).orElse(null);
        if (v == null) return see("/signup");
        String code = tokens.sixDigitCode();
        v.setCode(code);
        v.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        verifications.save(v);
        emailSender.sendVerificationCode(email, code);
        return see("/signup/verify?email=" + enc(email));
    }

    // ---------- ログイン ----------
    @PostMapping("/api/auth/login")
    public ResponseEntity<Void> login(@RequestParam(defaultValue = "") String email,
                                      @RequestParam(defaultValue = "") String password,
                                      @RequestParam(required = false) String next,
                                      HttpServletRequest request, HttpServletResponse response) {
        String ip = request.getRemoteAddr();
        if (!rateLimiter.allow("login:" + ip, 20, 60_000)) return tooMany();

        String dest = safeDest(next);
        User u = users.findByEmail(email).orElse(null);
        if (u == null || !passwords.verify(password, u.getPasswordHash()))
            return see("/login?error=invalid" + nextParam(next));

        auth.login(response, u.getId());
        return see(dest);
    }

    // ---------- ログアウト ----------
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        auth.logout(response);
        return see("/");
    }

    // ---------- パスワードを忘れた場合 ----------
    @PostMapping("/api/auth/forgot-password")
    @Transactional
    public ResponseEntity<Void> forgot(@RequestParam(defaultValue = "") String email) {
        if (!rateLimiter.allow("reset:" + email, 3, 600_000)) return see("/forgot-password?msg=rate");
        User u = users.findByEmail(email).orElse(null);
        if (u == null) return see("/forgot-password?msg=not-found");

        String code = tokens.sixDigitCode();
        resets.deleteByEmail(email);
        PasswordReset r = new PasswordReset();
        r.setEmail(email);
        r.setCode(code);
        r.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        resets.save(r);
        emailSender.sendPasswordResetCode(email, code);
        return see("/forgot-password/verify?email=" + enc(email));
    }

    // ---------- パスワードリセット ----------
    @PostMapping("/api/auth/reset-password")
    @Transactional
    public ResponseEntity<Void> reset(@RequestParam(defaultValue = "") String email,
                                      @RequestParam(defaultValue = "") String code,
                                      @RequestParam(defaultValue = "") String password,
                                      @RequestParam(defaultValue = "") String confirmPassword) {
        if (!rateLimiter.allow("reset-verify:" + email, 5, 600_000))
            return see("/forgot-password/verify?email=" + enc(email) + "&error=rate");
        if (!password.equals(confirmPassword))
            return see("/forgot-password/verify?email=" + enc(email) + "&error=mismatch");
        if (password.length() < 8)
            return see("/forgot-password/verify?email=" + enc(email) + "&error=short");

        PasswordReset r = resets.findFirstByEmailAndCodeOrderByCreatedAtDesc(email, code).orElse(null);
        if (r == null || r.getExpiresAt().isBefore(Instant.now()))
            return see("/forgot-password/verify?email=" + enc(email) + "&error=invalid");

        User u = users.findByEmail(email).orElse(null);
        if (u != null) {
            u.setPasswordHash(passwords.hash(password));
            users.save(u);
        }
        resets.deleteByEmail(email);
        return see("/login?msg=reset-complete");
    }

    // ---------- ヘルパー ----------
    private static String safeDest(String next) {
        return (next != null && next.startsWith("/")) ? next : "/dashboard";
    }

    private static String nextParam(String next) {
        if (next == null || next.isEmpty()) return "";
        return "&next=" + enc(next);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static ResponseEntity<Void> see(String location) {
        return Redirects.see(location);
    }

    private static ResponseEntity<Void> tooMany() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
