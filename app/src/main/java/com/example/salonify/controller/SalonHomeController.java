package com.example.salonify.controller;

import com.example.salonify.entity.*;
import com.example.salonify.entity.Thread;
import com.example.salonify.repository.*;
import com.example.salonify.service.PostViewService;
import com.example.salonify.support.Auth;
import com.example.salonify.support.PostAccess;
import com.example.salonify.view.PostView;
import com.example.salonify.view.ThreadSummary;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@Controller
public class SalonHomeController {

    private final PostRepository posts;
    private final ThreadRepository threads;
    private final CommentRepository comments;
    private final PlanRepository plans;
    private final Auth auth;
    private final PostAccess postAccess;
    private final PostViewService postViewService;

    public SalonHomeController(PostRepository posts, ThreadRepository threads, CommentRepository comments,
                               PlanRepository plans, Auth auth, PostAccess postAccess,
                               PostViewService postViewService) {
        this.posts = posts;
        this.threads = threads;
        this.comments = comments;
        this.plans = plans;
        this.auth = auth;
        this.postAccess = postAccess;
        this.postViewService = postViewService;
    }

    @GetMapping("/salons/{id}/home")
    public String home(@PathVariable String id, HttpServletRequest request, Model model) {
        Auth.MemberCtx ctx = auth.requireSalonMember(request, id);
        Salon salon = ctx.salon();
        boolean isOwner = ctx.isOwner();
        String myPlanId = postViewService.activePlanId(ctx.membership());

        // 最新の投稿（下書きを除く）、最大5件
        List<Post> allPosts = posts.findBySalonIdAndDraftFalseOrderByPinnedDescCreatedAtDesc(id);
        List<PostView> postViews = new ArrayList<>();
        for (Post p : allPosts.subList(0, Math.min(5, allPosts.size()))) {
            postViews.add(new PostView(
                    p,
                    postAccess.canView(p, myPlanId, isOwner),
                    postViewService.planName(p.getVisibility())));
        }

        // 最新のスレッド、最大5件、返信数付き
        List<Thread> allThreads = threads.findBySalonIdOrderByCreatedAtDesc(id);
        List<ThreadSummary> threadViews = new ArrayList<>();
        for (Thread t : allThreads.subList(0, Math.min(5, allThreads.size()))) {
            threadViews.add(new ThreadSummary(t, comments.countByThreadId(t.getId())));
        }

        Plan myPlan = myPlanId != null ? plans.findById(myPlanId).orElse(null) : null;

        model.addAttribute("salon", salon);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("membership", ctx.membership());
        model.addAttribute("myPlan", myPlan);
        model.addAttribute("posts", postViews);
        model.addAttribute("threads", threadViews);
        return "salon/home";
    }
}
