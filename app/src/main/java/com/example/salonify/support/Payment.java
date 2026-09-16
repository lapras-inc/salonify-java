package com.example.salonify.support;

import org.springframework.stereotype.Component;

/**
 * モックの決済プロバイダー。プラットフォーム手数料の計算(5%、切り捨て)のみが実際の処理となる。
 * 実際の請求・振込処理は行わない — モックモードのリファレンス実装と同一の挙動。
 */
@Component("payment")
public class Payment {

    public static final double PLATFORM_FEE_RATE = 0.05;

    public int platformFee(int amount) {
        return (int) Math.floor(amount * PLATFORM_FEE_RATE);
    }

    public int ownerNet(int amount) {
        return amount - platformFee(amount);
    }
}
