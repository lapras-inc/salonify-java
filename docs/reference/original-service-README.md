
# イベント用 - セキュリティチェック対象サービス

このリポジトリは、セキュリティ勉強会イベントのチェック対象サービスとして、ほぼ全てをバイブコーディングで実装したものです。
このREADME.mdでは、実装方法においての情報や、サービスの機能仕様、設計、ローカルでの動作確認の手順などをまとめています。

## エンジニアリング知識を用いた箇所

一部、本件の進行をスムーズに、また、リスクを低減するために以下についてはエンジニア視点で手を施してあります。

- 環境を汚さずにお渡しするために、dockerですべて動くようにしています。サービスの実装後にAIに明確に指示を出して構築しました。
- 構築中のサプライチェーンアタックを最低限ケアするため、`.npmrc` にて 2026-01-06以前のパッケージを利用するように指定しています。これらはこちらから明確に指示をしたもので、何も指示をしない場合はnpm installをオプションなしに実行していくような挙動をしていました。
    - その中でも脆弱性が報告されているバージョンは避けるように(next@14.2.15 にセキュリティ脆弱性警告。2025-12-11 のパッチを当てたいので、2026-01-06以前の修正済み 14.x を探します)
- Vercel, Neon, ResendなどのManagedサービスは今回用にアカウントを作成し、クレジットカード情報等は入力せずに利用しており、token等も全てテスト用のものです
    -　※望ましくはありませんが、AI開発の過程で一度Repositoryに含まれることがありましたが、履歴を消す等のケアはしておりません

## 構築手順について

Claude Opus4.6にオンラインサロンプラットフォームを作るための要件定義を指示し、その出力を元にClaude Codeに実装を指示して構築しました。
アーキテクチャ、依存サービス、各種設定値などは全てAIの提案をそのまま採用しています。Managedサービスによっては、Claude in Chromeで設定値を入力させたものもあります。
実装後に画面の動きをベースに、不具合や画面の動線、Loading表示などの体験面のフィードバックを何度か送り、完成させました。
構築におけるコマンド、いわゆる黒い画面の操作は一歳自分では行わず、ローカルサービスの起動や、本番へのデプロイ(mainブランチへのpush --> Vercelに自動デプロイですが)など含め、全てAIへの指示で実行しています。

より詳しくは `./LOG.md` に記載しております。

# Salonify — オンラインサロンプラットフォーム

手数料わずか **5%**（決済手数料込み）。審査なし、即日開設。
誰でもサブスク型コミュニティを作って運営できるプラットフォームです。

## サービスURL

サービスは `https://salon-platform-sand.vercel.app/` で稼働しています。

---

## サービス概要

| 項目 | 内容 |
|------|------|
| 対象 | サロンオーナー（発信者）＋ メンバー（参加者） |
| 収益モデル | 月額サブスク × プラットフォーム手数料 5% |
| 決済 | Stripe（カード情報はStripeが管理） |
| 公開方式 | 公開 / 招待URL限定 の2択 |

---

## 主な機能

### メンバー向け

- **サロン検索・閲覧** — カテゴリ別に公開サロンを探せる
- **プラン選択＆入会** — Stripe Checkoutで決済、無料トライアル・初月割引対応
- **投稿閲覧** — オーナーの投稿をMarkdown＋画像＋YouTube埋め込みで読める
- **プラン限定コンテンツ** — 上位プラン専用の投稿
- **コミュニティ掲示板** — スレッド作成、コメント（1段ネスト）、いいね、通報
- **請求管理** — 加入中プラン一覧、Stripe請求書/領収書の閲覧、退会

### オーナー向け

- **サロン開設** — 名前・説明・カバー画像・カテゴリを入力して即公開
- **ダッシュボード** — 月間売上見込み、アクティブ会員数、投稿数、スレッド数
- **プラン管理** — 複数プラン作成、料金・トライアル日数・初月割引の設定
- **投稿管理** — Markdownエディタ（プレビュー付き）、画像アップロード、下書き、ピン留め
- **メンバー管理** — 一覧表示、プラン変更、強制退会
- **招待URL管理** — 発行・無効化・利用上限・有効期限・ラベル付きで複数管理
- **売上・振込** — 累計売上、手数料控除後の振込額、振込申請（最低¥3,000〜）

### 管理者向け

- **ユーザー管理** — 検索、停止、管理者権限付与
- **サロン管理** — 一覧・削除
- **通報管理** — コミュニティ通報の確認・対応済みマーク

---

## 技術スタック

| レイヤー | 技術 |
|----------|------|
| フロントエンド | Next.js 14 (App Router) + React 18 + TypeScript + Tailwind CSS |
| バックエンド | Next.js API Routes (Route Handlers) |
| データベース | PostgreSQL + Prisma ORM |
| 認証 | bcryptjs（パスワードハッシュ）+ jose（JWT Cookie セッション） |
| メール | Resend（認証コード・パスワードリセット） |
| 決済 | Stripe（サブスク・Checkout・Webhook） |
| 画像 | Vercel Blob |
| コンテンツ | marked（Markdown→HTML） |
| バリデーション | Zod |

---

## アーキテクチャ

```
┌─────────────────────────────────────────────────────┐
│  Browser                                            │
│  Next.js (React Server Components + Client)         │
└──────────────┬──────────────────────────────────────┘
               │ HTTP
┌──────────────▼──────────────────────────────────────┐
│  Next.js API Routes (/api/*)                        │
│  ┌────────────┐ ┌──────────┐ ┌───────────────────┐  │
│  │ Auth       │ │ Owner    │ │ Membership/Join   │  │
│  │ (JWT+bcrypt│ │ Guard    │ │ (Stripe Checkout) │  │
│  └────────────┘ └──────────┘ └───────────────────┘  │
│  ┌────────────┐ ┌──────────┐ ┌───────────────────┐  │
│  │ Rate Limit │ │ Sanitize │ │ Admin Guard       │  │
│  └────────────┘ └──────────┘ └───────────────────┘  │
└──────┬───────────────────┬──────────────────────────┘
       │                   │
┌──────▼──────┐    ┌───────▼───────┐    ┌─────────────┐
│  PostgreSQL │    │  Stripe API   │    │ Vercel Blob │
│  (Prisma)   │    │  (決済/請求)  │    │ (画像CDN)   │
└─────────────┘    └───────┬───────┘    └─────────────┘
                           │
                   ┌───────▼───────┐
                   │ Stripe Webhook│──→ /api/stripe/webhook
                   │ (支払成功/失敗│    (Invoice作成, 停止処理)
                   │  サブスク削除)│
                   └───────────────┘
```

### 認証フロー

```
会員登録:
  メール+パスワード入力 → 認証コード送信(Resend) → コード入力
  → Userレコード作成 → JWT Cookieセット → ダッシュボードへ

ログイン:
  メール+パスワード → bcrypt照合 → JWT Cookie(30日) → ダッシュボードへ

パスワードリセット:
  メール入力 → 6桁コード送信 → コード入力 → 新パスワード設定
```

### 入会〜決済フロー

```
プラン選択 → Stripe Checkout (カード入力)
  → 成功コールバック → Membershipレコード作成
  → トライアルあり: 即課金なし、N日後にStripeが自動引落
  → 初月割引あり: Stripeクーポン適用

退会:
  Stripeサブスクキャンセル → Membership.status = cancelled

Webhook:
  invoice.payment_succeeded → Invoiceレコード作成
  invoice.payment_failed   → failedCount++, 3回で suspended
  subscription.deleted     → Membership cancelled
```

### ディレクトリ構成

```
src/
├── app/
│   ├── page.tsx                    # トップページ（サロン一覧）
│   ├── login/ signup/              # 認証
│   ├── dashboard/                  # メンバーダッシュボード
│   ├── account/                    # プロフィール・請求管理
│   ├── salons/[id]/                # サロンLP・入会・投稿・掲示板
│   ├── owner/salons/[id]/          # オーナー管理画面
│   ├── admin/                      # 管理者画面
│   └── api/                        # APIルート
├── components/                     # 共通コンポーネント
│   ├── FlashMessage.tsx            # トースト通知
│   ├── SubmitButton.tsx            # 確認ダイアログ付きボタン
│   ├── MarkdownEditor.tsx          # エディタ（プレビュー・画像アップロード）
│   └── ImageUpload.tsx             # 画像アップロード
├── lib/                            # ユーティリティ
│   ├── session.ts                  # JWT管理
│   ├── db.ts                       # Prismaクライアント
│   ├── stripe.ts                   # Stripeクライアント
│   ├── email.ts                    # Resendメール送信
│   ├── owner-guard.ts              # オーナー認可
│   ├── admin-guard.ts              # 管理者認可
│   ├── rate-limit.ts               # レート制限
│   └── sanitize.ts                 # HTMLサニタイズ
└── middleware.ts                    # ルート保護
prisma/
├── schema.prisma                   # データモデル定義
└── seed.ts                         # テストデータ
```

---

## セキュリティ

- **パスワード**: bcryptjs（ソルトラウンド10）
- **セッション**: HTTP-only Secure Cookie + JWT (HS256)
- **レート制限**: IP/メール単位（会員登録 10回/分、ログイン 20回/分）
- **HTMLサニタイズ**: ホワイトリスト方式、script/イベントハンドラ除去
- **認可ガード**: `requireSalonOwner` / `requireSalonMember` / `requireAdmin`
- **カード情報**: Stripe側で管理（PCI DSS準拠）

---

## ローカル開発（Docker で一発起動）

**必要なもの**: Docker (Docker Desktop / OrbStack / Colima など)

```bash
cp .env.example .env          # 環境変数ファイルを作成
docker compose up              # DB + アプリを起動（初回はビルドあり）
```

これだけで http://localhost:3001 にアクセスできます。
DB作成・スキーマ反映・テストデータ投入もすべて自動で行われます。

### テストアカウント

| メール | パスワード | 役割 |
|--------|-----------|------|
| owner@example.com | password123 | サロンオーナー |
| member@example.com | password123 | メンバー |

### ローカルでの挙動

| 機能 | ローカルでの動作 |
|------|-----------------|
| **メール認証** | メールは送信されず、認証コードが画面に直接表示されます |
| **パスワードリセット** | 同上（リセットコードが画面に表示されます） |
| **入会・決済** | Stripe をスキップして即入会できます（キー設定不要） |
| **画像アップロード** | `BLOB_READ_WRITE_TOKEN` が未設定の場合は動作しません |

### 環境変数 (.env)

`.env.example` をコピーすればそのまま動きます。各変数の役割:

```env
# DB — docker compose が起動する PostgreSQL に自動接続（変更不要）
DATABASE_URL="postgresql://salon:salon@db:5432/salon_dev"

# セッション署名キー（開発用固定値、変更不要）
SESSION_SECRET="dev-secret-change-me-please-32chars-min"

# メール — 空のままでOK（認証コードは画面に表示されます）
RESEND_API_KEY=""

# Stripe — 空のままでOK（決済スキップで直接入会できます）
STRIPE_SECRET_KEY=""
```

### Stripe テストキーの設定（任意）

`STRIPE_SECRET_KEY` が空の場合、入会ボタンを押すと Stripe Checkout をスキップして即座にサロンに参加できます。
全機能を一通り試すのに Stripe キーは不要です。

実際の Stripe Checkout 画面を確認したい場合のみ:

1. [Stripe ダッシュボード](https://dashboard.stripe.com/test/apikeys) でテストモードの Secret Key (`sk_test_...`) を取得
2. `.env` の `STRIPE_SECRET_KEY` にセット
3. `docker compose restart app` で反映
4. テストカード: `4242 4242 4242 4242`（有効期限・CVC は適当でOK）

### Docker を使わない場合

Node.js 20+ と PostgreSQL が手元にある場合:

```bash
cp .env.example .env
# .env の DATABASE_URL を localhost に変更:
#   postgresql://salon:salon@localhost:5432/salon_dev
npm install
npx prisma db push
npx tsx prisma/seed.ts
npx next dev -p 3001
```

---

## 外部サービス

| サービス | 用途 | プラン | リージョン/備考 |
|----------|------|--------|----------------|
| [Vercel](https://vercel.com) | ホスティング (Next.js) | Hobby (無料) | Function Region: Singapore (sin1) |
| [Neon](https://neon.tech) | PostgreSQL データベース | Free Tier | Singapore (ap-southeast-1) |
| [Stripe](https://stripe.com) | 決済 (サブスク・Checkout・Webhook) | テストモード | — |
| [Resend](https://resend.com) | メール送信 (認証コード・パスワードリセット) | Free (100通/日) | 送信元: noreply@rocky-manobi.com |
| [Vercel Blob](https://vercel.com/docs/storage/vercel-blob) | 画像ストレージ (CDN配信) | Vercel Hobby に含む | — |

> **Note**: Vercel FunctionsとNeon DBは同一リージョン (Singapore) に配置してレイテンシを最小化しています。

---

## デプロイ

```bash
git push origin main
# → Vercel が自動デプロイ
```

Prismaスキーマ変更時は本番DBにも反映:

```bash
DATABASE_URL="<Neon接続文字列>" npx prisma db push
```
