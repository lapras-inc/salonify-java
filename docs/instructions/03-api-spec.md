# 03. 機能仕様（共通基盤・全エンドポイント）

参照実装の挙動を仕様として書き起こしたもの。**URLパス・メソッド・リダイレクト先・クエリパラメータ（`?msg=` / `?error=`）まで同じにすること**。

## 3.0 全体の作り

- 画面はサーバーサイドレンダリング。更新系はすべて **HTMLフォームの POST → 処理 → 303 See Other リダイレクト**（PRGパターン）。エラーもJSONではなく `?error=xxx` を付けて元の画面へ戻す。
- 一部のエンドポイント（rate limit超過、存在しないリソース等）のみ 401/404/429 を返す。
- 完了メッセージはリダイレクト先の `?msg=xxx` をフラッシュメッセージとして画面に表示する（04参照）。

## 3.1 共通基盤（support パッケージ）

### 認証・セッション

- パスワードハッシュ: **BCrypt strength 10**。
- セッション: JWT **HS256**、ペイロードは `{ "uid": "<userId>" }` のみ、発行時刻付き、**有効期限30日**。
- 署名鍵: 環境変数 `SESSION_SECRET`（未設定時 `dev-secret-change-me-please-32chars-min`）。
- Cookie: 名前 `session`、httpOnly、SameSite=Lax、path=/、maxAge=30日。secure属性はローカルでは付けない。
- サーバー側にセッションを保存しない完全ステートレス方式。ログアウト=Cookie削除のみ。
- ヘルパー: `createSession(uid)` / `destroySession()` / `getCurrentUser()`（Cookie検証→User取得。無効ならnull）。

### 認可ガード（参照実装と同じ挙動にする）

- `requireSalonOwner(salonId)`: 未ログイン→ `/login` へredirect。サロンが存在しない→404。`salon.ownerId != user.id` → **404を返す（403ではない。存在を隠す）**。
- `requireSalonMember(salonId)`: 未ログイン→ `/login`。サロン無し→404。オーナー本人は常にOK。それ以外は該当サロンの Membership が `status == "active"` でなければ `/salons/{salonId}` へredirect。
- `requireAdmin()`: 未ログイン or `!user.isAdmin` → `/` へredirect。

### レート制限

- **プロセス内メモリ（ConcurrentHashMap）による固定ウィンドウ方式**。分散対応は不要。
- `rateLimit(key, limit, windowMs)`: ウィンドウ内で `limit` 回まで許可。超過時の挙動は各エンドポイント欄に記載。

### サニタイズ・Markdown

- `renderMarkdown(text)`: commonmark で Markdown→HTML 変換後、`sanitizeHtml` を通す。
- `sanitizeHtml(html)`: **正規表現ベースのホワイトリスト方式**（参照実装と同じ）。
  - 許可タグ: `p, br, strong, em, u, a, ul, ol, li, h1, h2, h3, blockquote, code, pre, img, hr`
  - 許可属性: `a` → `href, title`。`img` → `src, alt`。その他の属性は除去。
  - `<script>`ブロック・`<style>`ブロック・`on*=` イベントハンドラ属性・`javascript:` 文字列を除去。許可外タグは丸ごと除去。
- `extractYouTubeId(url)`: 正規表現 `(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/embed/)([\w-]{11})` で11文字IDを抽出。
- スレッドやコメントの本文はプレーンテキスト保存・表示時HTMLエスケープ（テンプレート側の標準エスケープでよい）。

### メール送信（モック）

- 実際のメール送信は行わない。`EmailSender` は**コードをログ出力するだけ**の実装にする: `[DEV] to=<email> code=<code>`。
- あわせて、認証コード入力画面にコードを直接表示する（04参照。参照実装のローカルモードと同じ挙動）。

### 決済（モック）

- Stripe 連携は**実装しない**。参照実装の「STRIPE_SECRET_KEY 未設定時」の挙動（決済スキップで即入会）を正式仕様とする。
- `/api/stripe/webhook` と Stripe Checkout への遷移は**実装対象外**。
- 手数料計算のみ実装する: `platformFee(amount) = floor(amount * 0.05)`、`ownerNet(amount) = amount - platformFee(amount)`。売上画面で使用。

> **方針変更（2026-07 追記）**: 参照実装と同じ「`STRIPE_SECRET_KEY` の有無で分岐する二段構え」を後から実装した。
> - キー未設定（デフォルト）: 上記のとおりモック即入会（従来どおり）。
> - テストキー（`sk_test_...`）設定時: 新規/再入会時に Stripe Checkout（テストモード）へ遷移し、`GET /api/join/success?session_id=...` で復帰して入会確定（`stripeCustomerId` / `stripePriceId` / `stripeSubscriptionId` をこのとき初めて保存する）。
> - Webhook・継続課金/支払失敗/解約の同期・プラン変更時のサブスク更新は引き続き実装対象外。

### 画像アップロード（ローカル保存）

- Vercel Blob の代わりに**コンテナ内 `/app/uploads` ディレクトリへ保存**し、`/uploads/**` で静的配信する（Spring の ResourceHandler で紐付け）。docker volume で永続化。

## 3.2 エンドポイント仕様

記法: 【認可】は guest（誰でも）/ member（要ログイン）/ owner（requireSalonOwner）/ admin（requireAdmin）。フォーム項目の `slice N` は「N文字に切り詰める」（エラーにしない）。

### 認証

#### POST `/api/auth/signup` 【guest】
- レート制限: IP単位 10回/60秒 → 超過は 429。email単位 3回/600秒 → 超過は `/signup?error=rate` へ。
- 入力: `email`（メール形式）, `password`（8文字以上）, `displayName`（1〜40文字）, `next`（任意）。
- 検証失敗 → `/signup?error=invalid`。email の User が既存 → `/signup?error=exists`。
- 処理: 6桁ゼロ埋めコードを乱数生成（`SecureRandom`）。パスワードをBCryptハッシュ。同メールの EmailVerification を全削除して新規作成（expiresAt = now+10分）。メール送信（モック）。**この時点で User は作らない**。
- 成功 → 303 `/signup/verify?email=...&next=...`。

#### POST `/api/auth/verify` 【guest】
- レート制限: email単位 5回/600秒 → `?error=rate`。
- 入力: `email`, `code`, `next`。リダイレクト先は `next` が `/` 始まりの場合のみ採用、それ以外は `/dashboard`（オープンリダイレクト対策）。
- email+code で EmailVerification 検索。無し or 期限切れ → `?error=invalid`。User が既に存在 → レコード削除して `?error=exists`。
- 成功: User 作成（emailVerified=true、一時保存の passwordHash/displayName を使用）、EmailVerification 削除、セッション発行 → 303 リダイレクト先へ。

#### POST `/api/auth/resend-code` 【guest】
- レート制限: email単位 3回/600秒。email 空 or レコード無し → `/signup`。
- 新コード生成してレコード更新（期限 now+10分）、再送（モック）→ `/signup/verify?email=...`。

#### POST `/api/auth/login` 【guest】
- レート制限: IP単位 20回/60秒 → 429。
- 入力: `email`, `password`, `next`（`/`始まりのみ採用、デフォルト `/dashboard`）。
- User 検索 + BCrypt 照合。失敗 → `/login?error=invalid`。成功 → セッション発行 → 303。
- ※ emailVerified や停止状態の明示チェックはしない（suspend されたユーザーは passwordHash が `suspended` に上書きされているため照合が自然に失敗する）。

#### POST `/api/auth/logout` 【誰でも】
- Cookie 削除 → 303 `/`。

#### POST `/api/auth/forgot-password` 【guest】
- レート制限: email単位 3回/600秒 → `/forgot-password?msg=rate`。
- User 無し → `/forgot-password?msg=not-found`（参照実装どおり。存在が分かる挙動だがそのまま再現する）。
- 6桁コード生成、同メールの PasswordReset 全削除→新規作成（期限10分）、送信（モック）→ `/forgot-password/verify?email=...`。

#### POST `/api/auth/reset-password` 【guest】
- レート制限: email単位 5回/600秒。
- 入力: `email`, `code`, `password`, `confirmPassword`。
- 不一致 → `?error=mismatch`。8文字未満 → `?error=short`。email+code 無効/期限切れ → `?error=invalid`。
- 成功: User のパスワード更新、同メールの PasswordReset 全削除 → `/login?msg=reset-complete`。

### アカウント

#### POST `/api/account/update` 【member】
- 入力: `displayName`（slice 40）, `bio`（制限なし）, `avatarUrl`（空文字→null）→ User 更新 → `/account?msg=account-updated`。

#### POST `/api/account/delete` 【member】
- 本人の全 Membership を `cancelled` に更新。User はハード削除せず: email=`deleted_{id}@example.invalid`, passwordHash=`deleted`, displayName=`(退会したユーザー)` に書き換え。セッション破棄 → `/?msg=account-deleted`。

### アップロード

#### POST `/api/upload` 【member】
- 未ログイン → 401 JSON。ファイル無し → 400 `{"error":"no file"}`。
- 許可 Content-Type: `image/jpeg, image/png, image/gif, image/webp` → 違反は 400 `{"error":"invalid file type"}`。
- サイズ上限 **4MB** → 超過は 400 `{"error":"file too large (max 4MB)"}`。
- `/app/uploads/{userId}/{timestamp}-{filename}` に保存 → 200 `{"url":"/uploads/{userId}/{timestamp}-{filename}"}`。

### 入会・退会

#### POST `/api/join` 【member】
- 未ログイン → `/login`。入力: `salonId`, `planId`, `invite`（招待コード、任意）。
- サロン無し → `/`。
- サロンが `visibility == "invite"` かつ自分が active メンバーでない場合、招待コード必須。コード検証（存在・サロン一致・disabled でない・期限内・上限未満）。失敗 → `/salons/{salonId}`。成功したら `uses` を +1（参照実装どおり、この後の検証で失敗しても戻さない）。
- Plan 検証（存在・サロン一致）失敗 → `/salons/{salonId}`。
- 既に同一プランで active → `/salons/{salonId}/home`（何もしない）。
- **既に active で別プラン（プラン変更）**: Membership.planId を更新 → `/salons/{salonId}/home?msg=plan-changed`。
- **新規または再入会**: `nextBillAt = now + (trialDays > 0 ? trialDays日 : 30日)`。`firstAmount = trialDays > 0 ? 0 : max(0, priceJpy - introDiscount)`。既存 Membership があれば更新（status=active, planId更新, failedCount=0, joinedAt=now）、なければ作成。`firstAmount > 0` なら Invoice(amountJpy=firstAmount, status="paid") を作成 → `/salons/{salonId}/home?msg=joined`。
- ※決済処理は行わない（モック仕様）。

#### POST `/api/membership/cancel` 【member・本人のみ】
- 入力: `membershipId`, `redirect`（デフォルト `/account/billing`、`/`始まりのみ許可）。
- Membership の userId が本人なら status=`cancelled` → `{redirect}?msg=cancelled`。

### 掲示板

#### POST `/api/salons/{id}/threads/create` 【member】
- requireSalonMember。レート制限: user単位 10回/60秒 → 429。
- 入力: `title`（slice 200）, `body`。Thread 作成 → `/salons/{id}/board/all/{threadId}?msg=thread-created`。

#### POST `/api/threads/{id}/comment` 【member】
- Thread 無し → 404。requireSalonMember(thread.salonId)。レート制限: user単位 30回/60秒 → 429。
- 入力: `parentId`（任意）, `body`（slice 2000）。**ネストは1段まで: parentId の親コメントがさらに親を持つ場合、その親（ルート）に付け替える**。
- 作成 → `/salons/{salonId}/board/all/{threadId}?msg=commented`。

#### POST `/api/threads/{id}/react` 【member】
- Thread 無し → 404。requireSalonMember。kind="like" のトグル（あれば削除、なければ作成）→ スレッド詳細へ。

#### POST `/api/threads/{id}/report` 【member】
- Thread 無し → 404。requireSalonMember。入力: `reason`（デフォルト "unspecified"、slice 500）。Report 作成 → 元スレッドへ `?msg=reported`。

#### POST `/api/posts/{id}/react` 【member】
- Post 無し → 404。requireSalonMember(post.salonId)。PostReaction トグル → `/salons/{salonId}/posts/{postId}`。

### オーナー

#### POST `/api/owner/salons/create` 【member（作成者がオーナーになる）】
- 未ログイン → `/login`。
- 入力: `name`（slice 80）, `tagline`（slice 140）, `description`, `category`, `visibility`（デフォルト public）, `coverUrl`/`thumbUrl`（空→null）。
- 同時に初期プランを1つ作成: `planName`（デフォルト「スタンダード」）, `planPrice`→priceJpy（0〜100000にクランプ）, `planTrial`→trialDays（0〜30）, `planDiscount`→introDiscount（0以上）。
- → `/owner/salons/{id}/dashboard?msg=salon-created`。

#### POST `/api/owner/salons/{id}/update` 【owner】
- Salon 更新（項目・切り詰めは create と同じ）。
- visibility を `invite` に変更したとき、そのサロンに Invite が1件も無ければ無制限の招待コードを自動生成（12バイト乱数の base64url、maxUses=0）。
- → `/owner/salons/{id}/edit?msg=salon-updated`。

#### POST `/api/owner/salons/{id}/payout` 【owner】
- **モック**: 何もせず → `/owner/salons/{id}/revenue?msg=payout-requested`。

#### POST `/api/owner/salons/{id}/plan/create` 【owner】
- 入力: `name`, `priceJpy`（0〜100000）, `trialDays`（0〜30）, `introDiscount`（0以上）→ 作成 → `?msg=plan-added`（edit画面へ）。

#### POST `/api/owner/salons/{id}/plan/update` 【owner】
- `planId` がこのサロンの Plan か検証（不一致→edit画面へ戻る）。name/priceJpy/trialDays/introDiscount/description（空→null）更新 → `?msg=plan-updated`。

#### POST `/api/owner/salons/{id}/posts/create` 【owner】
- レート制限: user単位 30回/60秒 → 429。
- 入力: フォーム名 `bodyHtml`（**実体はMarkdown原文**。参照実装のフィールド名をそのまま踏襲）, `title`（slice 200）, `videoUrl`（空→null）, `visibility`（デフォルト "all"）, `pinned`（"1"のとき true）, `draft`（"1"のとき true）。
- `bodyHtml カラム = sanitizeHtml(markdown→HTML変換結果)`、`bodyMarkdown カラム = 原文`。
- → `/salons/{id}/posts?msg=posted`。

#### POST `/api/owner/salons/{id}/posts/{postId}/update` 【owner】
- Post がこのサロン所属か検証（不一致→404）。create と同じ変換で更新 → `/owner/salons/{id}/posts?msg=post-updated`。

#### POST `/api/owner/salons/{id}/posts/{postId}/delete` 【owner】
- 所属検証（不一致→404）。削除 → `?msg=post-deleted`。

#### POST `/api/owner/salons/{id}/invite/create` 【owner】
- 入力: `label`（slice 40）, `maxUses`（0以上）, `expDays`（0以上。>0なら expiresAt=now+expDays日、0ならnull）。code は12バイト乱数の base64url。
- → `redirect` が `/` 始まりならそこへ、なければ edit 画面へ `?msg=invite-created`。

#### POST `/api/owner/salons/{id}/invite/disable` 【owner】
- `inviteId` がこのサロン所属なら disabled=true → `?msg=invite-disabled`。

#### POST `/api/owner/salons/{id}/members/change-plan` 【owner】
- `membershipId` と `planId` の両方がこのサロン所属か検証 → Membership.planId 更新 → `/owner/salons/{id}/members?msg=member-plan-changed`。

#### POST `/api/owner/salons/{id}/members/remove` 【owner】
- `membershipId` がこのサロン所属なら status=`cancelled` → `?msg=member-removed`。

### 管理者

#### POST `/api/admin/salons/{id}/delete` 【admin】
- Salon 無し → 404。削除（Cascade で Plan/Membership/Post/Thread/Invite 等も削除）→ `/admin/salons?msg=salon-deleted`。

#### POST `/api/admin/users/{id}/suspend` 【admin】
- User 無し → 404。**passwordHash を文字列 `suspended` に上書き**（ログイン不能化。参照実装どおり）→ `/admin/users?msg=user-suspended`。

#### POST `/api/admin/users/{id}/toggle-admin` 【admin】
- User 無し → 404。isAdmin を反転 → `?msg=admin-toggled`。

#### POST `/api/admin/reports/{id}/resolve` 【admin】
- Report 無し → 404。resolved=true → `/admin/reports?msg=report-resolved`。

## 3.3 実装対象外（参照実装にあるが作らないもの）

- Stripe 実連携（Checkout / Webhook / Customer / Price / サブスクリプション操作）→ モック挙動で代替。`GET /api/join/success` も不要。
- Resend による実メール送信 → ログ出力+画面表示で代替。
- Vercel Blob → ローカルディスク保存で代替。

> **方針変更（2026-07 追記）**: 上記のうち Stripe 実連携は、Checkout 遷移と `GET /api/join/success` での入会確定のみ後から実装した（`STRIPE_SECRET_KEY` 設定時のみ有効。詳細は 3.1「決済」の追記を参照）。Webhook ほかは引き続き対象外。
