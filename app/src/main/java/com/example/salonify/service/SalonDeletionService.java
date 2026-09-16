package com.example.salonify.service;

import com.example.salonify.entity.Membership;
import com.example.salonify.entity.Post;
import com.example.salonify.entity.Thread;
import com.example.salonify.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * サロンとその依存レコードをすべて削除する(単純なString型FKを使用しているため手動でカスケード処理する)。
 * PrismaのonDelete: Cascadeリレーションを踏襲している。
 */
@Service
public class SalonDeletionService {

    private final SalonRepository salons;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final InvoiceRepository invoices;
    private final PostRepository posts;
    private final PostReactionRepository postReactions;
    private final ThreadRepository threads;
    private final CommentRepository comments;
    private final ReactionRepository reactions;
    private final ReportRepository reports;
    private final InviteRepository invites;

    public SalonDeletionService(SalonRepository salons, PlanRepository plans, MembershipRepository memberships,
                                InvoiceRepository invoices, PostRepository posts, PostReactionRepository postReactions,
                                ThreadRepository threads, CommentRepository comments, ReactionRepository reactions,
                                ReportRepository reports, InviteRepository invites) {
        this.salons = salons;
        this.plans = plans;
        this.memberships = memberships;
        this.invoices = invoices;
        this.posts = posts;
        this.postReactions = postReactions;
        this.threads = threads;
        this.comments = comments;
        this.reactions = reactions;
        this.reports = reports;
        this.invites = invites;
    }

    @Transactional
    public void deleteSalon(String salonId) {
        List<String> threadIds = threads.findBySalonIdOrderByCreatedAtDesc(salonId)
                .stream().map(Thread::getId).toList();
        if (!threadIds.isEmpty()) {
            comments.deleteByThreadIdIn(threadIds);
            reactions.deleteByThreadIdIn(threadIds);
            reports.deleteByThreadIdIn(threadIds);
        }
        threads.deleteBySalonId(salonId);

        List<String> postIds = posts.findBySalonIdOrderByCreatedAtDesc(salonId)
                .stream().map(Post::getId).toList();
        if (!postIds.isEmpty()) {
            postReactions.deleteByPostIdIn(postIds);
        }
        posts.deleteBySalonId(salonId);

        List<String> membershipIds = memberships.findBySalonIdOrderByJoinedAtDesc(salonId)
                .stream().map(Membership::getId).toList();
        if (!membershipIds.isEmpty()) {
            invoices.deleteByMembershipIdIn(membershipIds);
        }
        memberships.deleteBySalonId(salonId);

        plans.deleteBySalonId(salonId);
        invites.deleteBySalonId(salonId);
        salons.deleteById(salonId);
    }
}
