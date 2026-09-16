package com.example.salonify.service;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import com.example.salonify.entity.Plan;
import com.example.salonify.entity.User;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Stripe Checkoutによる実決済の参加フロー。STRIPE_SECRET_KEYが設定されている場合のみ有効(enabled()を参照)。 */
@Service
public class StripeCheckoutService {

    private final UserRepository users;
    private final PlanRepository plans;
    private final String secretKey;
    private final StripeClient client;

    public StripeCheckoutService(@Value("${salonify.stripe-secret-key}") String secretKey,
                                 UserRepository users, PlanRepository plans) {
        this.secretKey = secretKey;
        this.users = users;
        this.plans = plans;
        this.client = (secretKey != null && !secretKey.isBlank()) ? new StripeClient(secretKey) : null;
    }

    /** Stripeのシークレットキーが設定されているかどうか。falseの場合、呼び出し元はモックの参加フローにフォールバックする。 */
    public boolean enabled() {
        return secretKey != null && !secretKey.isBlank();
    }

    /** 指定されたプランのStripe Checkoutセッションを作成し、そのホスト型URLを返す。 */
    public String createCheckoutUrl(User user, String salonId, Plan plan, String inviteCode, String baseUrl) {
        try {
            String customerId = ensureCustomer(user);
            String priceId = ensurePrice(plan);

            String cancelUrl = baseUrl + "/salons/" + salonId + "/join?plan=" + plan.getId()
                    + (inviteCode != null && !inviteCode.isEmpty() ? "&invite=" + inviteCode : "");

            SessionCreateParams.SubscriptionData.Builder subscriptionData = SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("userId", user.getId())
                    .putMetadata("salonId", salonId)
                    .putMetadata("planId", plan.getId());
            if (plan.getTrialDays() > 0) {
                subscriptionData.setTrialPeriodDays((long) plan.getTrialDays());
            }

            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .setSuccessUrl(baseUrl + "/api/join/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .putMetadata("userId", user.getId())
                    .putMetadata("salonId", salonId)
                    .putMetadata("planId", plan.getId())
                    .setSubscriptionData(subscriptionData.build());

            if (plan.getIntroDiscount() > 0) {
                Coupon coupon = client.coupons().create(CouponCreateParams.builder()
                        // JPYはStripeのゼロ小数通貨。円の額面をそのまま渡す(USD等のように100倍しない)。
                        .setAmountOff((long) plan.getIntroDiscount())
                        .setCurrency("jpy")
                        .setDuration(CouponCreateParams.Duration.ONCE)
                        .setName(plan.getName() + " 初月割引")
                        .build());
                params.addDiscount(SessionCreateParams.Discount.builder().setCoupon(coupon.getId()).build());
            }

            Session session = client.checkout().sessions().create(params.build());
            return session.getUrl();
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe checkout failed", e);
        }
    }

    /** 完了したCheckoutセッションの結果: 誰がどのサロン/プランに参加したかと、結果として生成されたサブスクリプションを特定する。 */
    public record JoinSession(String userId, String salonId, String planId, String subscriptionId) {}

    /** 完了したCheckoutセッションを取得し、そのメタデータから参加の詳細を抽出する。未完了の場合はnullを返す。 */
    public JoinSession retrieveJoinSession(String sessionId) {
        try {
            Session session = client.checkout().sessions().retrieve(sessionId,
                    SessionRetrieveParams.builder().addExpand("subscription").build(), null);

            Map<String, String> metadata = session.getMetadata();
            String userId = metadata != null ? metadata.get("userId") : null;
            String salonId = metadata != null ? metadata.get("salonId") : null;
            String planId = metadata != null ? metadata.get("planId") : null;
            if (userId == null || salonId == null || planId == null) return null;

            String subscriptionId = session.getSubscriptionObject() != null
                    ? session.getSubscriptionObject().getId()
                    : session.getSubscription();

            return new JoinSession(userId, salonId, planId, subscriptionId);
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe checkout failed", e);
        }
    }

    private String ensureCustomer(User user) {
        if (user.getStripeCustomerId() != null) return user.getStripeCustomerId();
        try {
            Customer customer = client.customers().create(CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .putMetadata("userId", user.getId())
                    .build());
            user.setStripeCustomerId(customer.getId());
            users.save(user);
            return customer.getId();
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe checkout failed", e);
        }
    }

    private String ensurePrice(Plan plan) {
        if (plan.getStripePriceId() != null) return plan.getStripePriceId();
        try {
            Price price = client.prices().create(PriceCreateParams.builder()
                    // JPYはStripeのゼロ小数通貨。円の額面をそのまま渡す(USD等のように100倍しない)。
                    .setUnitAmount((long) plan.getPriceJpy())
                    .setCurrency("jpy")
                    .setRecurring(PriceCreateParams.Recurring.builder()
                            .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                            .build())
                    .setProductData(PriceCreateParams.ProductData.builder()
                            .setName(plan.getName())
                            .build())
                    .build());
            plan.setStripePriceId(price.getId());
            plans.save(plan);
            return price.getId();
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe checkout failed", e);
        }
    }
}
