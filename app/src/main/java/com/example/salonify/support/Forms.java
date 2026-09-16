package com.example.salonify.support;

/** フォーム値操作(truncate/clamp)のための小さなヘルパー群。リファレンス実装の slice/clamp の挙動に合わせている。 */
public final class Forms {
    private Forms() {}

    /** 最大 n 文字まで切り詰める(エラーにはならない)。 */
    public static String slice(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }

    /** 空文字列を null に変換する(任意項目の url/description フィールドで使用)。 */
    public static String nullIfBlank(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    public static String orDefault(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    public static int parseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
