package com.example.salonify.config;

import com.example.salonify.entity.*;
import com.example.salonify.entity.Thread;
import com.example.salonify.repository.*;
import com.example.salonify.support.Passwords;
import com.example.salonify.support.PostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 起動時にデモデータを投入する。冪等性あり: salonify-owner@example.com が既に存在し、
 * サロンを既に所有している場合は何も行わない。
 */
@Component
public class SeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final UserRepository users;
    private final SalonRepository salons;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final InvoiceRepository invoices;
    private final PostRepository posts;
    private final ThreadRepository threads;
    private final Passwords passwords;

    public SeedRunner(UserRepository users, SalonRepository salons, PlanRepository plans,
                      MembershipRepository memberships, InvoiceRepository invoices,
                      PostRepository posts, ThreadRepository threads, Passwords passwords) {
        this.users = users;
        this.salons = salons;
        this.plans = plans;
        this.memberships = memberships;
        this.invoices = invoices;
        this.posts = posts;
        this.threads = threads;
        this.passwords = passwords;
    }

    @Override
    public void run(String... args) {
        String pw = passwords.hash("password123");

        User owner = users.findByEmail("salonify-owner@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("salonify-owner@example.com");
            u.setPasswordHash(pw);
            u.setDisplayName("山田オーナー");
            u.setEmailVerified(true);
            return users.save(u);
        });

        User member = users.findByEmail("salonify-member@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("salonify-member@example.com");
            u.setPasswordHash(pw);
            u.setDisplayName("鈴木メンバー");
            u.setEmailVerified(true);
            return users.save(u);
        });

        users.findByEmail("salonify-admin@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("salonify-admin@example.com");
            u.setPasswordHash(pw);
            u.setDisplayName("管理者");
            u.setEmailVerified(true);
            u.setAdmin(true);
            return users.save(u);
        });

        // 冪等性: オーナーが既にサロンを持っている場合はコンテンツ投入をスキップする。
        if (!salons.findByOwnerIdOrderByCreatedAtDesc(owner.getId()).isEmpty()) {
            log.info("already seeded");
            return;
        }

        Salon salon = new Salon();
        salon.setOwnerId(owner.getId());
        salon.setName("テック起業ラボ");
        salon.setTagline("エンジニア出身の起業家のための実践コミュニティ");
        salon.setDescription("スタートアップ立ち上げのノウハウを共有します。\n\n毎週ライブ勉強会を開催。");
        salon.setCategory("ビジネス");
        salon.setVisibility(SalonVisibility.PUBLIC);
        salons.save(salon);

        Plan light = new Plan();
        light.setSalonId(salon.getId());
        light.setName("ライト");
        light.setPriceJpy(980);
        plans.save(light);

        Plan standard = new Plan();
        standard.setSalonId(salon.getId());
        standard.setName("スタンダード");
        standard.setPriceJpy(2980);
        standard.setDescription("全コンテンツ + 月1面談");
        plans.save(standard);

        Membership ms = new Membership();
        ms.setUserId(member.getId());
        ms.setSalonId(salon.getId());
        ms.setPlanId(light.getId());
        ms.setNextBillAt(Instant.now().plus(30, ChronoUnit.DAYS));
        memberships.save(ms);

        Invoice inv = new Invoice();
        inv.setMembershipId(ms.getId());
        inv.setAmountJpy(980);
        inv.setStatus(InvoiceStatus.PAID);
        invoices.save(inv);

        Post welcome = new Post();
        welcome.setSalonId(salon.getId());
        welcome.setAuthorId(owner.getId());
        welcome.setTitle("ようこそ！");
        welcome.setBodyHtml("<p>はじめまして。このサロンでは起業の実践ノウハウを共有します。</p>");
        welcome.setBodyMarkdown("はじめまして。このサロンでは起業の実践ノウハウを共有します。");
        welcome.setPinned(true);
        posts.save(welcome);

        Post premium = new Post();
        premium.setSalonId(salon.getId());
        premium.setAuthorId(owner.getId());
        premium.setTitle("【スタンダード限定】月次戦略レポート");
        premium.setBodyHtml("<p>今月の戦略レポートです。スタンダードプラン限定でお届けします。</p>");
        premium.setBodyMarkdown("今月の戦略レポートです。スタンダードプラン限定でお届けします。");
        premium.setVisibility(PostAccess.planVisibility(standard.getId()));
        posts.save(premium);

        Thread thread = new Thread();
        thread.setSalonId(salon.getId());
        thread.setAuthorId(member.getId());
        thread.setTitle("自己紹介スレ");
        thread.setBody("よろしくお願いします！");
        threads.save(thread);

        log.info("Seeded:");
        log.info("  salonify-owner@example.com / password123");
        log.info("  salonify-member@example.com / password123");
        log.info("  salonify-admin@example.com / password123");
    }
}
