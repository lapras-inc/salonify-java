package com.example.salonify.entity;

/** Invoice.status の値。永続化された値に影響を与えないよう、エンティティ上ではString型のまま保持する(enumカラムにしない)。 */
public final class InvoiceStatus {
    public static final String PAID = "paid";

    private InvoiceStatus() {}
}
