package com.example.salonify;

import com.example.salonify.support.Payment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentTest {

    private final Payment payment = new Payment();

    @Test
    void platformFeeIsFivePercentFloored() {
        assertEquals(49, payment.platformFee(980));   // 49.0
        assertEquals(149, payment.platformFee(2980));  // 149.0
        assertEquals(0, payment.platformFee(0));
        assertEquals(0, payment.platformFee(19));      // 0.95 -> 0
        assertEquals(50, payment.platformFee(1000));
    }

    @Test
    void ownerNetIsAmountMinusFee() {
        assertEquals(931, payment.ownerNet(980));
        assertEquals(2831, payment.ownerNet(2980));
        assertEquals(0, payment.ownerNet(0));
    }
}
