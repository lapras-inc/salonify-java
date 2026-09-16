package com.example.salonify.controller;

import com.example.salonify.repository.EmailVerificationRepository;
import com.example.salonify.repository.PasswordResetRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthPageController {

    private final EmailVerificationRepository verifications;
    private final PasswordResetRepository resets;

    public AuthPageController(EmailVerificationRepository verifications, PasswordResetRepository resets) {
        this.verifications = verifications;
        this.resets = resets;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String next,
                        @RequestParam(required = false) String error, Model model) {
        model.addAttribute("next", next);
        model.addAttribute("error", error);
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup(@RequestParam(required = false) String next,
                         @RequestParam(required = false) String error, Model model) {
        model.addAttribute("next", next);
        model.addAttribute("error", error);
        return "auth/signup";
    }

    @GetMapping("/signup/verify")
    public String signupVerify(@RequestParam(required = false) String email,
                               @RequestParam(required = false) String next,
                               @RequestParam(required = false) String error, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("next", next);
        model.addAttribute("error", error);
        String devCode = null;
        if (email != null && !email.isEmpty()) {
            devCode = verifications.findFirstByEmailOrderByCreatedAtDesc(email)
                    .map(v -> v.getCode()).orElse(null);
        }
        model.addAttribute("devCode", devCode);
        return "auth/verify";
    }

    @GetMapping("/forgot-password")
    public String forgot(@RequestParam(required = false) String msg, Model model) {
        model.addAttribute("msg", msg);
        return "auth/forgot";
    }

    @GetMapping("/forgot-password/verify")
    public String forgotVerify(@RequestParam(required = false) String email,
                               @RequestParam(required = false) String error, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("error", error);
        String devCode = null;
        if (email != null && !email.isEmpty()) {
            devCode = resets.findFirstByEmailOrderByCreatedAtDesc(email)
                    .map(r -> r.getCode()).orElse(null);
        }
        model.addAttribute("devCode", devCode);
        return "auth/forgot-verify";
    }
}
