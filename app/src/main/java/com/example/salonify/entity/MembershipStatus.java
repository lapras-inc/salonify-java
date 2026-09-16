package com.example.salonify.entity;

/** Membership.status の値。永続化された値に影響を与えないよう、エンティティ上ではString型のまま保持する(enumカラムにしない)。 */
public final class MembershipStatus {
    public static final String ACTIVE = "active";
    public static final String PAST_DUE = "past_due"; // 現状どこからも設定されない(決済失敗/dunning処理は未実装)
    public static final String CANCELLED = "cancelled";
    public static final String SUSPENDED = "suspended"; // 現状どこからも設定されない(凍結/強制退会のステータス連携は未実装)

    private MembershipStatus() {}
}
