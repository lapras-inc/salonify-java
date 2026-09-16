package com.example.salonify.controller;

import com.example.salonify.entity.*;
import com.example.salonify.entity.Thread;
import com.example.salonify.repository.*;
import com.example.salonify.support.Auth;
import com.example.salonify.support.Forms;
import com.example.salonify.support.RateLimiter;
import com.example.salonify.support.Sanitizer;
import com.example.salonify.view.CommentView;
import com.example.salonify.view.ThreadListRow;
import com.example.salonify.web.NotFoundException;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class BoardController {

    private static final int PAGE_SIZE = 20;

    private final SalonRepository salons;
    private final ThreadRepository threads;
    private final CommentRepository comments;
    private final ReactionRepository reactions;
    private final ReportRepository reports;
    private final UserRepository users;
    private final Auth auth;
    private final Sanitizer sanitizer;
    private final RateLimiter rateLimiter;

    public BoardController(SalonRepository salons, ThreadRepository threads, CommentRepository comments,
                           ReactionRepository reactions, ReportRepository reports, UserRepository users,
                           Auth auth, Sanitizer sanitizer, RateLimiter rateLimiter) {
        this.salons = salons;
        this.threads = threads;
        this.comments = comments;
        this.reactions = reactions;
        this.reports = reports;
        this.users = users;
        this.auth = auth;
        this.sanitizer = sanitizer;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/salons/{id}/board")
    public String board(@PathVariable String id) {
        return "redirect:/salons/" + id + "/board/all";
    }

    /**
     * 掲示板スレッド一覧(カテゴリタブ)。categoryIdはURL/画面上のタブ表示にのみ使い、DBの絞り込みには使わない
     * (Threadにカテゴリ列が無いため、どのカテゴリでも同一の全件一覧になる)。
     */
    @GetMapping("/salons/{id}/board/{categoryId}")
    public String category(@PathVariable String id, @PathVariable String categoryId,
                           @RequestParam(defaultValue = "1") int page,
                           HttpServletRequest request, Model model) {
        Auth.MemberCtx ctx = auth.requireSalonMember(request, id);
        int p = Math.max(1, page);
        Page<Thread> pg = threads.findBySalonIdOrderByCreatedAtDesc(id, PageRequest.of(p - 1, PAGE_SIZE));

        List<ThreadListRow> rows = new ArrayList<>();
        for (Thread t : pg.getContent()) {
            rows.add(new ThreadListRow(
                    t,
                    users.findById(t.getAuthorId()).map(User::getDisplayName).orElse("?"),
                    comments.countByThreadId(t.getId()),
                    reactions.countByThreadId(t.getId())));
        }
        model.addAttribute("salon", ctx.salon());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("rows", rows);
        model.addAttribute("page", p);
        model.addAttribute("totalPages", pg.getTotalPages());
        return "board/list";
    }

    @GetMapping("/salons/{id}/board/{categoryId}/new")
    public String newThread(@PathVariable String id, @PathVariable String categoryId,
                            HttpServletRequest request, Model model) {
        Auth.MemberCtx ctx = auth.requireSalonMember(request, id);
        model.addAttribute("salon", ctx.salon());
        model.addAttribute("categoryId", categoryId);
        return "board/new";
    }

    @GetMapping("/salons/{id}/board/{categoryId}/{threadId}")
    public String thread(@PathVariable String id, @PathVariable String categoryId, @PathVariable String threadId,
                         HttpServletRequest request, Model model) {
        Auth.MemberCtx ctx = auth.requireSalonMember(request, id);
        Thread t = threads.findById(threadId).orElseThrow(NotFoundException::new);
        if (!t.getSalonId().equals(id)) throw new NotFoundException();

        List<Comment> all = comments.findByThreadIdOrderByCreatedAtAsc(threadId);
        List<CommentView> topLevel = new ArrayList<>();
        for (Comment c : all) {
            if (c.getParentId() != null) continue;
            List<CommentView> replies = new ArrayList<>();
            for (Comment r : all) {
                if (c.getId().equals(r.getParentId())) {
                    replies.add(new CommentView(
                            r,
                            users.findById(r.getAuthorId()).map(User::getDisplayName).orElse("?"),
                            sanitizer.renderMarkdown(r.getBody()),
                            Collections.emptyList()));
                }
            }
            topLevel.add(new CommentView(
                    c,
                    users.findById(c.getAuthorId()).map(User::getDisplayName).orElse("?"),
                    sanitizer.renderMarkdown(c.getBody()),
                    replies));
        }

        model.addAttribute("salon", ctx.salon());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("thread", t);
        model.addAttribute("threadAuthor", users.findById(t.getAuthorId()).map(User::getDisplayName).orElse("?"));
        model.addAttribute("threadBodyHtml", sanitizer.renderMarkdown(t.getBody()));
        model.addAttribute("comments", topLevel);
        model.addAttribute("reactionCount", reactions.countByThreadId(threadId));
        model.addAttribute("liked", reactions.findByThreadIdAndUserIdAndKind(threadId, ctx.user().getId(), "like").isPresent());
        return "board/thread";
    }

    // ---------- アクション ----------
    @PostMapping("/api/salons/{id}/threads/create")
    @Transactional
    public ResponseEntity<Void> createThread(@PathVariable String id, HttpServletRequest request,
                                             @RequestParam(defaultValue = "") String title,
                                             @RequestParam(defaultValue = "") String body) {
        Auth.MemberCtx ctx = auth.requireSalonMember(request, id);
        if (!rateLimiter.allow("thread:" + ctx.user().getId(), 10, 60_000))
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        Thread t = new Thread();
        t.setSalonId(id);
        t.setAuthorId(ctx.user().getId());
        t.setTitle(Forms.slice(title, 200));
        t.setBody(body);
        threads.save(t);
        return Redirects.see("/salons/" + id + "/board/all/" + t.getId() + "?msg=thread-created");
    }

    @PostMapping("/api/threads/{id}/comment")
    @Transactional
    public ResponseEntity<Void> comment(@PathVariable String id, HttpServletRequest request,
                                        @RequestParam(required = false) String parentId,
                                        @RequestParam(defaultValue = "") String body) {
        Thread t = threads.findById(id).orElseThrow(NotFoundException::new);
        Auth.MemberCtx ctx = auth.requireSalonMember(request, t.getSalonId());
        if (!rateLimiter.allow("comment:" + ctx.user().getId(), 30, 60_000))
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();

        // ネストを1階層に平坦化する: 親に更に親がある場合は、そのルートに紐付ける
        String effParent = (parentId != null && !parentId.isEmpty()) ? parentId : null;
        if (effParent != null) {
            Comment parent = comments.findById(effParent).orElse(null);
            if (parent != null && parent.getParentId() != null) {
                effParent = parent.getParentId();
            }
        }

        Comment c = new Comment();
        c.setThreadId(id);
        c.setAuthorId(ctx.user().getId());
        c.setParentId(effParent);
        c.setBody(Forms.slice(body, 2000));
        comments.save(c);
        return Redirects.see("/salons/" + t.getSalonId() + "/board/all/" + id + "?msg=commented");
    }

    @PostMapping("/api/threads/{id}/react")
    @Transactional
    public ResponseEntity<Void> react(@PathVariable String id, HttpServletRequest request) {
        Thread t = threads.findById(id).orElseThrow(NotFoundException::new);
        Auth.MemberCtx ctx = auth.requireSalonMember(request, t.getSalonId());
        Reaction existing = reactions.findByThreadIdAndUserIdAndKind(id, ctx.user().getId(), "like").orElse(null);
        if (existing != null) {
            reactions.delete(existing);
        } else {
            Reaction r = new Reaction();
            r.setThreadId(id);
            r.setUserId(ctx.user().getId());
            r.setKind("like");
            reactions.save(r);
        }
        return Redirects.see("/salons/" + t.getSalonId() + "/board/all/" + id);
    }

    @PostMapping("/api/threads/{id}/report")
    @Transactional
    public ResponseEntity<Void> report(@PathVariable String id, HttpServletRequest request,
                                       @RequestParam(defaultValue = "unspecified") String reason) {
        Thread t = threads.findById(id).orElseThrow(NotFoundException::new);
        Auth.MemberCtx ctx = auth.requireSalonMember(request, t.getSalonId());
        Report r = new Report();
        r.setThreadId(id);
        r.setReporterId(ctx.user().getId());
        r.setReason(Forms.slice(reason, 500));
        reports.save(r);
        return Redirects.see("/salons/" + t.getSalonId() + "/board/all/" + id + "?msg=reported");
    }
}
