package com.example.salonify.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

/** 303 See Other レスポンスを構築するためのヘルパー（PRG パターン）。 */
public final class Redirects {
    private Redirects() {}

    public static ResponseEntity<Void> see(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(location)).build();
    }
}
