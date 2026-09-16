package com.example.salonify.web;

/**
 * ガードやアクションからリクエストを 303 リダイレクトで中断させるためにスローされる
 * （同様に例外をスローする Next.js の redirect() を模したもの）。
 */
public class RedirectException extends RuntimeException {
    private final String location;

    public RedirectException(String location) {
        super("redirect:" + location);
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
