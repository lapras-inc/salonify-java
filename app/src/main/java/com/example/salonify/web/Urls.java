package com.example.salonify.web;

import jakarta.servlet.http.HttpServletRequest;

public final class Urls {
    private Urls() {}

    /** リクエストから取得したベースオリジン（scheme://host[:port]）。取得できない場合は localhost:8080 にフォールバックする。 */
    public static String origin(HttpServletRequest request) {
        String host = request.getHeader("host");
        if (host == null || host.isEmpty()) return "http://localhost:8080";
        String scheme = request.getScheme() == null ? "http" : request.getScheme();
        return scheme + "://" + host;
    }
}
