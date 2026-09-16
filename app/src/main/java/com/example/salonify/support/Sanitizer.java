package com.example.salonify.support;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正規表現ベースの最小限のHTMLサニタイザー兼Markdownレンダラー。リファレンス実装の
 * lib/sanitize.ts を忠実に移植したもの。ホワイトリスト方式であり、意図的にリファレンスに
 * 合わせて本番グレードの実装にはなっていない。
 */
@Component("sanitizer")
public class Sanitizer {

    private static final List<String> ALLOWED_TAGS = List.of(
            "p", "br", "strong", "em", "u", "a", "ul", "ol", "li",
            "h1", "h2", "h3", "blockquote", "code", "pre", "img", "hr");

    private static final Map<String, List<String>> ALLOWED_ATTRS = Map.of(
            "a", List.of("href", "title"),
            "img", List.of("src", "alt"));

    private static final Pattern SCRIPT = Pattern.compile("<\\s*script[\\s\\S]*?<\\s*/\\s*script\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE = Pattern.compile("<\\s*style[\\s\\S]*?<\\s*/\\s*style\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ON_DQ = Pattern.compile(" on\\w+\\s*=\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern ON_SQ = Pattern.compile(" on\\w+\\s*=\\s*'[^']*'", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile("</?([a-zA-Z][a-zA-Z0-9]*)([^>]*)>");
    private static final Pattern ATTR = Pattern.compile("\\s([a-zA-Z-]+)\\s*=\\s*(\"[^\"]*\"|'[^']*')");

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public String sanitizeHtml(String input) {
        if (input == null) return "";
        String out = input;
        out = SCRIPT.matcher(out).replaceAll("");
        out = STYLE.matcher(out).replaceAll("");
        out = ON_DQ.matcher(out).replaceAll("");
        out = ON_SQ.matcher(out).replaceAll("");
        out = JS.matcher(out).replaceAll("");

        Matcher m = TAG.matcher(out);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String whole = m.group();
            String tag = m.group(1).toLowerCase();
            String attrs = m.group(2);
            String replacement;
            if (!ALLOWED_TAGS.contains(tag)) {
                replacement = "";
            } else if (attrs == null || attrs.isEmpty()) {
                replacement = whole;
            } else {
                List<String> allowed = ALLOWED_ATTRS.getOrDefault(tag, List.of());
                Matcher am = ATTR.matcher(attrs);
                StringBuilder clean = new StringBuilder();
                while (am.find()) {
                    String name = am.group(1);
                    String val = am.group(2);
                    if (allowed.contains(name.toLowerCase())) {
                        clean.append(' ').append(name).append('=').append(val);
                    }
                }
                boolean closing = whole.startsWith("</");
                replacement = "<" + (closing ? "/" : "") + tag + clean + ">";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Markdown をサニタイズ済みHTMLに変換する(ユーザーコンテンツのサーバーサイドレンダリング)。 */
    public String renderMarkdown(String text) {
        if (text == null) return "";
        String html = renderer.render(parser.parse(text));
        return sanitizeHtml(html);
    }

    /** 11文字のYouTube動画IDを抽出する。該当しない場合は null。 */
    public String extractYouTubeId(String url) {
        if (url == null || url.isEmpty()) return null;
        Matcher m = Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([\\w-]{11})").matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
