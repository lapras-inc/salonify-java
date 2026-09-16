package com.example.salonify.controller;

import com.example.salonify.entity.Post;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.repository.PostReactionRepository;
import com.example.salonify.repository.PostRepository;
import com.example.salonify.service.PostViewService;
import com.example.salonify.support.Auth;
import com.example.salonify.support.Forms;
import com.example.salonify.support.PostAccess;
import com.example.salonify.support.RateLimiter;
import com.example.salonify.support.Sanitizer;
import com.example.salonify.web.NotFoundException;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** オーナー向けの投稿管理（一覧、新規/編集フォーム、作成・更新・削除）。 */
@Controller
public class OwnerPostController {

    private final PostRepository posts;
    private final PlanRepository plans;
    private final PostReactionRepository postReactions;
    private final Auth auth;
    private final Sanitizer sanitizer;
    private final RateLimiter rateLimiter;
    private final PostViewService postViewService;

    public OwnerPostController(PostRepository posts, PlanRepository plans, PostReactionRepository postReactions,
                               Auth auth, Sanitizer sanitizer, RateLimiter rateLimiter,
                               PostViewService postViewService) {
        this.posts = posts;
        this.plans = plans;
        this.postReactions = postReactions;
        this.auth = auth;
        this.sanitizer = sanitizer;
        this.rateLimiter = rateLimiter;
        this.postViewService = postViewService;
    }

    @GetMapping("/owner/salons/{id}/posts")
    public String manage(@PathVariable String id, HttpServletRequest request, Model model) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);
        model.addAttribute("salon", ctx.salon());
        model.addAttribute("posts", posts.findBySalonIdOrderByCreatedAtDesc(id));
        model.addAttribute("plans", plans.findBySalonIdOrderByPriceJpyAsc(id));
        return "owner/posts";
    }

    @GetMapping("/owner/salons/{id}/posts/new")
    public String newPost(@PathVariable String id, HttpServletRequest request, Model model) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);
        model.addAttribute("salon", ctx.salon());
        model.addAttribute("plans", plans.findBySalonIdOrderByPriceJpyAsc(id));
        model.addAttribute("post", null);
        return "owner/post-form";
    }

    @GetMapping("/owner/salons/{id}/posts/{postId}/edit")
    public String editPost(@PathVariable String id, @PathVariable String postId,
                           HttpServletRequest request, Model model) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);
        Post post = posts.findById(postId).orElseThrow(NotFoundException::new);
        if (!post.getSalonId().equals(id)) throw new NotFoundException();
        model.addAttribute("salon", ctx.salon());
        model.addAttribute("plans", plans.findBySalonIdOrderByPriceJpyAsc(id));
        model.addAttribute("post", post);
        String defaultBody = post.getBodyMarkdown() != null ? post.getBodyMarkdown() : post.getBodyHtml();
        model.addAttribute("defaultBody", defaultBody);
        return "owner/post-form";
    }

    @PostMapping("/api/owner/salons/{id}/posts/create")
    @Transactional
    public ResponseEntity<Void> create(@PathVariable String id, HttpServletRequest request,
                                       @RequestParam(defaultValue = "") String title,
                                       @RequestParam(name = "bodyHtml", defaultValue = "") String bodyMarkdown,
                                       @RequestParam(required = false) String videoUrl,
                                       @RequestParam(defaultValue = PostAccess.ALL) String visibility,
                                       @RequestParam(required = false) String pinned,
                                       @RequestParam(required = false) String draft) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);
        if (!rateLimiter.allow("post:" + ctx.user().getId(), 30, 60_000))
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();

        Post post = new Post();
        post.setSalonId(id);
        post.setAuthorId(ctx.user().getId());
        applyForm(post, id, title, bodyMarkdown, videoUrl, visibility, pinned, draft);
        posts.save(post);
        return Redirects.see("/salons/" + id + "/posts?msg=posted");
    }

    @PostMapping("/api/owner/salons/{id}/posts/{postId}/update")
    @Transactional
    public ResponseEntity<Void> update(@PathVariable String id, @PathVariable String postId,
                                       HttpServletRequest request,
                                       @RequestParam(defaultValue = "") String title,
                                       @RequestParam(name = "bodyHtml", defaultValue = "") String bodyMarkdown,
                                       @RequestParam(required = false) String videoUrl,
                                       @RequestParam(defaultValue = PostAccess.ALL) String visibility,
                                       @RequestParam(required = false) String pinned,
                                       @RequestParam(required = false) String draft) {
        auth.requireSalonOwner(request, id);
        Post post = posts.findById(postId).orElse(null);
        if (post == null || !post.getSalonId().equals(id)) throw new NotFoundException();
        applyForm(post, id, title, bodyMarkdown, videoUrl, visibility, pinned, draft);
        posts.save(post);
        return Redirects.see("/owner/salons/" + id + "/posts?msg=post-updated");
    }

    @PostMapping("/api/owner/salons/{id}/posts/{postId}/delete")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable String id, @PathVariable String postId,
                                       HttpServletRequest request) {
        auth.requireSalonOwner(request, id);
        Post post = posts.findById(postId).orElse(null);
        if (post == null || !post.getSalonId().equals(id)) throw new NotFoundException();
        postReactions.deleteByPostIdIn(List.of(postId));
        posts.delete(post);
        return Redirects.see("/owner/salons/" + id + "/posts?msg=post-deleted");
    }

    private void applyForm(Post post, String salonId, String title, String bodyMarkdown, String videoUrl,
                           String visibility, String pinned, String draft) {
        post.setTitle(Forms.slice(title, 200));
        post.setBodyMarkdown(bodyMarkdown);
        post.setBodyHtml(sanitizer.renderMarkdown(bodyMarkdown));
        post.setVideoUrl(Forms.nullIfBlank(videoUrl));
        post.setVisibility(postViewService.normalizeVisibility(salonId, Forms.orDefault(visibility, PostAccess.ALL)));
        post.setPinned("1".equals(pinned));
        post.setDraft("1".equals(draft));
    }
}
