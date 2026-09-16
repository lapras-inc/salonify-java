package com.example.salonify.web;

/** ガードや lookup 処理から 404 を発生させるためにスローされる（権限のないユーザーからリソースを隠す目的でも使用される）。 */
public class NotFoundException extends RuntimeException {
    public NotFoundException() {
        super("not found");
    }
}
