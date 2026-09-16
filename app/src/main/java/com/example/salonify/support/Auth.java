package com.example.salonify.support;

import com.example.salonify.entity.Membership;
import com.example.salonify.entity.Salon;
import com.example.salonify.entity.User;
import com.example.salonify.repository.MembershipRepository;
import com.example.salonify.repository.SalonRepository;
import com.example.salonify.repository.UserRepository;
import com.example.salonify.web.NotFoundException;
import com.example.salonify.web.RedirectException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * 認証・認可のガード処理。リファレンス実装の lib/session.ts, owner-guard.ts, admin-guard.ts に対応する。
 * 現在のユーザーはリクエストごとに AuthInterceptor によって一度だけ解決され、リクエスト属性として保存される。
 */
@Component
public class Auth {

    public static final String CURRENT_USER_ATTR = "currentUser";

    private final SessionService sessions;
    private final UserRepository users;
    private final SalonRepository salons;
    private final MembershipRepository memberships;

    public Auth(SessionService sessions, UserRepository users, SalonRepository salons,
                MembershipRepository memberships) {
        this.sessions = sessions;
        this.users = users;
        this.salons = salons;
        this.memberships = memberships;
    }

    /** セッションクッキーからユーザーを解決する(インターセプターから使用される)。 */
    public User resolveFromCookie(String token) {
        String uid = sessions.verify(token);
        if (uid == null) return null;
        return users.findById(uid).orElse(null);
    }

    public User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute(CURRENT_USER_ATTR);
    }

    public void login(HttpServletResponse response, String uid) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessions.buildSessionCookie(sessions.issue(uid)));
    }

    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessions.buildClearCookie());
    }

    /** ログイン済みユーザーであることを要求する。未ログインの場合は /login にリダイレクトする。 */
    public User requireUser(HttpServletRequest request) {
        User u = currentUser(request);
        if (u == null) throw new RedirectException("/login");
        return u;
    }

    /** サロンオーナーであることを要求する。未認証の場合は /login へ、オーナーでない場合やサロンが存在しない場合は404(存在を隠す)。 */
    public OwnerCtx requireSalonOwner(HttpServletRequest request, String salonId) {
        User u = currentUser(request);
        if (u == null) throw new RedirectException("/login");
        Salon salon = salons.findById(salonId).orElseThrow(NotFoundException::new);
        if (!salon.getOwnerId().equals(u.getId())) throw new NotFoundException();
        return new OwnerCtx(u, salon);
    }

    /** 有効な会員登録を要求する(オーナーは常に許可)。未認証は /login へ、サロンが存在しない場合は404へ、非会員は /salons/{id} へ。 */
    public MemberCtx requireSalonMember(HttpServletRequest request, String salonId) {
        User u = currentUser(request);
        if (u == null) throw new RedirectException("/login");
        Salon salon = salons.findById(salonId).orElseThrow(NotFoundException::new);
        boolean isOwner = salon.getOwnerId().equals(u.getId());
        Membership membership = memberships.findByUserIdAndSalonId(u.getId(), salonId).orElse(null);
        if (!isOwner && (membership == null || !membership.isActive())) {
            throw new RedirectException("/salons/" + salonId);
        }
        return new MemberCtx(u, salon, membership, isOwner);
    }

    /** 管理者であることを要求する。管理者でない場合は / にリダイレクトする。 */
    public User requireAdmin(HttpServletRequest request) {
        User u = currentUser(request);
        if (u == null || !u.isAdmin()) throw new RedirectException("/");
        return u;
    }

    public record OwnerCtx(User user, Salon salon) {}
    public record MemberCtx(User user, Salon salon, Membership membership, boolean isOwner) {}
}
