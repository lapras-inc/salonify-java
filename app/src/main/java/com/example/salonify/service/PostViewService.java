package com.example.salonify.service;

import com.example.salonify.entity.Membership;
import com.example.salonify.entity.Plan;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.support.PostAccess;
import org.springframework.stereotype.Service;

@Service
public class PostViewService {

    private final PlanRepository plans;
    private final PostAccess postAccess;

    public PostViewService(PlanRepository plans, PostAccess postAccess) {
        this.plans = plans;
        this.postAccess = postAccess;
    }

    /** "plan:<id>"で制限された投稿が必要とするプランの表示名。制限がない場合はnull。 */
    public String planName(String visibility) {
        String pid = postAccess.requiredPlanId(visibility);
        if (pid == null) return null;
        return plans.findById(pid).map(Plan::getName).orElse("限定");
    }

    /** 閲覧者のアクティブなメンバーシップのプランID。存在しない場合はnull。 */
    public String activePlanId(Membership membership) {
        return (membership != null && membership.isActive()) ? membership.getPlanId() : null;
    }

    /**
     * 永続化前に投稿のvisibility値を検証する: null/空文字/"all" は ALL になる。"plan:<id>" 値は、
     * そのプランが存在し、かつこのサロンに属している場合のみ保持される。それ以外はALLにフォールバックする。
     */
    public String normalizeVisibility(String salonId, String raw) {
        if (raw == null || raw.isEmpty() || PostAccess.ALL.equals(raw)) return PostAccess.ALL;
        if (raw.startsWith(PostAccess.PLAN_PREFIX)) {
            String planId = raw.substring(PostAccess.PLAN_PREFIX.length());
            Plan plan = plans.findById(planId).orElse(null);
            if (plan != null && plan.getSalonId().equals(salonId)) return raw;
        }
        return PostAccess.ALL;
    }
}
