package com.example.salonify.entity;

/** Salon.visibility の値。永続化された値に影響を与えないよう、エンティティ上ではString型のまま保持する(enumカラムにしない)。 */
public final class SalonVisibility {
    public static final String PUBLIC = "public";
    public static final String INVITE = "invite";
    public static final String PRIVATE = "private"; // レガシー

    private SalonVisibility() {}

    /** 書き込み時にvisibilityの生値を正規化する: PUBLIC/INVITEはそのまま通し、それ以外(nullを含む)はPUBLICにする。 */
    public static String normalize(String raw) {
        if (PUBLIC.equals(raw) || INVITE.equals(raw)) return raw;
        return PUBLIC;
    }
}
