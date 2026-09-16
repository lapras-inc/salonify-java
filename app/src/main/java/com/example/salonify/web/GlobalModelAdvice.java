package com.example.salonify.web;

import com.example.salonify.entity.User;
import com.example.salonify.support.Auth;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 現在のユーザーをすべてのビューに公開する（ヘッダーで使用される）。 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUser")
    public User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute(Auth.CURRENT_USER_ATTR);
    }
}
