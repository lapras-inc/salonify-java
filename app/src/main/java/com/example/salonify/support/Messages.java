package com.example.salonify.support;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * フラッシュメッセージのコードを日本語テキストに変換する(遷移先ページで ?msg= から描画される)。
 * Thymeleaf から ${@messages.text(...)} としてアクセスできる。
 */
@Component("messages")
public class Messages {

    private static final Map<String, String> TEXT = Map.ofEntries(
            Map.entry("cancelled", "退会しました"),
            Map.entry("joined", "サロンに入会しました"),
            Map.entry("plan-changed", "プランを変更しました"),
            Map.entry("posted", "投稿しました"),
            Map.entry("thread-created", "スレッドを作成しました"),
            Map.entry("commented", "コメントしました"),
            Map.entry("reported", "通報しました"),
            Map.entry("salon-created", "サロンを開設しました"),
            Map.entry("salon-updated", "サロン設定を更新しました"),
            Map.entry("plan-updated", "プランを更新しました"),
            Map.entry("plan-added", "プランを追加しました"),
            Map.entry("invite-created", "招待URLを発行しました"),
            Map.entry("invite-disabled", "招待URLを無効化しました"),
            Map.entry("member-removed", "会員を退会させました"),
            Map.entry("member-plan-changed", "会員のプランを変更しました"),
            Map.entry("account-updated", "アカウントを更新しました"),
            Map.entry("payout-requested", "振込を申請しました"),
            Map.entry("account-deleted", "退会が完了しました"),
            Map.entry("reset-complete", "パスワードをリセットしました。ログインしてください"),
            Map.entry("user-suspended", "ユーザーを停止しました"),
            Map.entry("admin-toggled", "管理者権限を変更しました"),
            Map.entry("salon-deleted", "サロンを削除しました"),
            Map.entry("report-resolved", "通報を解決済みにしました"),
            Map.entry("post-updated", "投稿を更新しました"),
            Map.entry("post-deleted", "投稿を削除しました")
    );

    public String text(String code) {
        if (code == null || code.isEmpty()) return "";
        return TEXT.getOrDefault(code, code);
    }
}
