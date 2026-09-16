package com.example.salonify.support;

import com.example.salonify.entity.Post;
import org.springframework.stereotype.Component;

/** 投稿の公開範囲チェック。リファレンス実装の canViewPost に対応する。 */
@Component("postAccess")
public class PostAccess {

    /** 制限なし: 全ての会員に公開される。 */
    public static final String ALL = "all";
    /** プラン限定の公開範囲値のプレフィックス。例: "plan:<planId>"。 */
    public static final String PLAN_PREFIX = "plan:";

    /** プラン限定の投稿に対して "plan:<planId>" という公開範囲の値を構築する。 */
    public static String planVisibility(String planId) {
        return PLAN_PREFIX + planId;
    }

    /**
     * オーナーは全てを閲覧できる。null/"all" は会員に公開される。"plan:<id>" は現在のプランが一致する必要がある。
     * それ以外の(未知の)値はデフォルトで閲覧不可とする。
     */
    public boolean canView(String visibility, String currentPlanId, boolean isOwner) {
        if (isOwner) return true;
        if (visibility == null || ALL.equals(visibility)) return true;
        if (visibility.startsWith(PLAN_PREFIX)) {
            String planId = visibility.substring(PLAN_PREFIX.length());
            return planId.equals(currentPlanId);
        }
        return false;
    }

    public boolean canView(Post post, String currentPlanId, boolean isOwner) {
        return canView(post.getVisibility(), currentPlanId, isOwner);
    }

    /** "plan:<id>" の公開範囲が参照するプランID。該当しない場合は null。 */
    public String requiredPlanId(String visibility) {
        if (visibility != null && visibility.startsWith(PLAN_PREFIX)) {
            return visibility.substring(PLAN_PREFIX.length());
        }
        return null;
    }
}
