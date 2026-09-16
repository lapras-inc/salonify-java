package com.example.salonify.controller;

import com.example.salonify.entity.*;
import com.example.salonify.repository.*;
import com.example.salonify.service.PostViewService;
import com.example.salonify.support.Auth;
import com.example.salonify.support.PostAccess;
import com.example.salonify.support.Sanitizer;
import com.example.salonify.view.PostView;
import com.example.salonify.web.NotFoundException;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

/** メンバー向けの投稿表示（一覧、詳細、いいね）。 */
@Controller
public class PostController {

    private final PostRepository posts;
    private final PostReactionRepository postReactions;
    private final Auth auth;
    private final PostAccess postAccess;
    private final PostViewService postViewService;
    private final Sanitizer sanitizer;

    public PostController(PostRepository posts, PostReactionRepository postReactions,
                          Auth auth, PostAccess postAccess, PostViewService postViewService, Sanitizer sanitizer) {
        this.posts = posts;
        this.postReactions = postReactions;
        this.auth = auth;
        this.postAccess = postAccess;
        this.postViewService = postViewService;
        this.sanitizer = sanitizer;
    }

    @GetMapping("/salons/{id}/posts")
    public String list(@PathVariable String id, HttpServletRequest request, Model model) {
        Auth.MemberCtx ctx = auth.requireSalonMember(request, id);
        String myPlanId = postViewService.activePlanId(ctx.membership());
        List<PostView> views = new ArrayList<>();
        for (Post p : posts.findBySalonIdAndDraftFalseOrderByPinnedDescCreatedAtDesc(id)) {
            views.add(new PostView(
                    p,
                    postAccess.canView(p, myPlanId, ctx.isOwner()),
                    postViewService.planName(p.getVisibility())));
        }
        model.addAttribute("salon", ctx.salon());
        model.addAttribute("isOwner", ctx.isOwner());
        model.addAttribute("posts", views);
        return "salon/posts";
    }

    @GetMapping("/salons/{id}/posts/{postId}")
    public String detail(@PathVariable String id, @PathVariable String postId,
                         HttpServletRequest request, Model model) {
        Auth.MemberCtx ctx = auth.requireSalonMember(request, id);
        Post post = posts.findById(postId).orElseThrow(NotFoundException::new);
        if (!post.getSalonId().equals(id)) throw new NotFoundException();

        String myPlanId = postViewService.activePlanId(ctx.membership());
        boolean canView = postAccess.canView(post, myPlanId, ctx.isOwner());

        model.addAttribute("salon", ctx.salon());
        model.addAttribute("post", post);
        model.addAttribute("canView", canView);
        model.addAttribute("planName", postViewService.planName(post.getVisibility()));
        model.addAttribute("youtubeId", sanitizer.extractYouTubeId(post.getVideoUrl()));
        model.addAttribute("likeCount", postReactions.countByPostId(postId));
        model.addAttribute("liked", postReactions.findByPostIdAndUserId(postId, ctx.user().getId()).isPresent());
        return "salon/post-detail";
    }

    @PostMapping("/api/posts/{id}/react")
    @Transactional
    public ResponseEntity<Void> react(@PathVariable String id, HttpServletRequest request) {
        Post post = posts.findById(id).orElseThrow(NotFoundException::new);
        Auth.MemberCtx ctx = auth.requireSalonMember(request, post.getSalonId());
        PostReaction existing = postReactions.findByPostIdAndUserId(id, ctx.user().getId()).orElse(null);
        if (existing != null) {
            postReactions.delete(existing);
        } else {
            PostReaction r = new PostReaction();
            r.setPostId(id);
            r.setUserId(ctx.user().getId());
            postReactions.save(r);
        }
        return Redirects.see("/salons/" + post.getSalonId() + "/posts/" + id);
    }
}
