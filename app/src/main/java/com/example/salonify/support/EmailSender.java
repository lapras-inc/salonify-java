package com.example.salonify.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * モックのメール送信クラス。実際のメール送信は行わず、コードをログ出力する(開発環境では画面にも表示される)。
 * これは RESEND_API_KEY が未設定の場合のリファレンス実装の挙動を再現したもの。
 */
@Component
public class EmailSender {
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    public void sendVerificationCode(String to, String code) {
        log.info("[DEV] verification to={} code={}", to, code);
    }

    public void sendPasswordResetCode(String to, String code) {
        log.info("[DEV] password-reset to={} code={}", to, code);
    }
}
