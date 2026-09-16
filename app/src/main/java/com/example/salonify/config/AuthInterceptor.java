package com.example.salonify.config;

import com.example.salonify.entity.User;
import com.example.salonify.support.Auth;
import com.example.salonify.support.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * リクエストごとに一度、セッションCookieから現在のユーザーを解決し、リクエスト属性として保存する。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final Auth auth;

    public AuthInterceptor(Auth auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (SessionService.COOKIE_NAME.equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        User user = auth.resolveFromCookie(token);
        if (user != null) {
            request.setAttribute(Auth.CURRENT_USER_ATTR, user);
        }
        return true;
    }
}
