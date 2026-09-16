# 04. 画面仕様（全ページ）

Thymeleaf テンプレートで実装する。見た目は簡素でよいが、**表示する情報・フォーム・遷移・アクセス制御はこの通りにすること**。
スタイルは `static/css/app.css` 1枚。凝ったデザインは不要（カード風の枠、テーブル、ボタンが判別できれば十分）。

## 4.0 共通レイアウト

全ページ共通のヘッダ・フッタ（Thymeleaf の layout/fragment を使用）。

- ヘッダ左: ロゴ「Salonify」→ `/`
- ヘッダナビ（常時）: 「サロンを探す」→ `/salons`
- 未ログイン時: 「ログイン」→ `/login`、「新規登録」→ `/signup`
- ログイン時: 「マイページ」→ `/dashboard`、「サロン開設」→ `/owner/salons/new`、（isAdmin のみ）「管理」→ `/admin`、表示名（+アバター）→ `/account`、「ログアウト」（POST `/api/auth/logout` のフォームボタン）
- フッタ: 「© Salonify · プラットフォーム手数料5% · 決済はモック実装」

### フラッシュメッセージ

- 各ページはクエリ `?msg=<code>` を受け取ったらメッセージバーを表示する共通フラグメントを持つ。
- コード→日本語文言のマップを1箇所に定義（例: `joined`→「サロンに入会しました」、`cancelled`→「退会しました」、`posted`→「投稿しました」、`salon-created`→「サロンを開設しました」等。全コード: cancelled, joined, plan-changed, posted, thread-created, commented, reported, salon-created, salon-updated, plan-updated, plan-added, invite-created, invite-disabled, member-removed, member-plan-changed, account-updated, payout-requested, account-deleted, reset-complete, user-suspended, admin-toggled, salon-deleted, report-resolved, post-updated, post-deleted）。未定義コードはそのまま表示。
- コードに `error` または `declined` を含む場合は赤、それ以外は緑で表示。
- ログイン/登録系ページの `?error=` は各ページ内で個別に日本語表示する（下記参照）。

### 危険操作の確認ダイアログ

削除・退会・停止などのボタンには `onclick="return confirm('...')"` 相当の確認を付ける。二重送信防止（送信中 disabled）もあるとよい。

## 4.1 公開ページ

### `/` トップ
- 誰でも閲覧可。ヒーロー（キャッチコピー「手数料5%・審査なし」等 + 「サロンを開設する」→`/owner/salons/new`、「サロンを探す」→`/salons`）。
- 「新着サロン」: `visibility=public` のサロンを新着順に6件。カード=サムネ（無ければプレースホルダ）、カテゴリ、名前、tagline、会員数、プラン数。→ `/salons/{id}`。

### `/salons` サロン一覧
- 誰でも。`visibility=public` を全件表示（ページネーション無し）。
- カテゴリフィルタチップ: すべて / ビジネス / 趣味 / アート / テクノロジー / 教育 / その他（`?category=`）。
- 並び替え: 新着順（デフォルト）/ 人気順=会員数降順（`?sort=popular`）。すべてリンク遷移。
- カードに最低価格「¥{最安プラン}〜/月」を表示。

### `/salons/{id}` サロンLP
- 誰でも閲覧可（`invite` サロンもLP自体は見える）。存在しないIDは404。
- 表示: カバー画像、名前、tagline、カテゴリ、アクティブ会員数、説明文（**Markdown→HTML変換+サニタイズして表示**）、オーナー紹介（アバター・名前・bio）、プラン一覧（価格昇順。名前、月額、トライアル日数、初月割引、説明）。
- プランごとのCTAボタン（状態で出し分け）:
  - オーナー本人: 「オーナーとして運営中」（無効）
  - そのプランで active 会員: 「✓ 現在のプラン」（無効)
  - 別プランの active 会員: 「このプランに変更」→ `/salons/{id}/join?plan={planId}`
  - 未入会: 「このプランで入会」→ 同上
  - `invite` サロンで有効な `?invite=` コードが無い場合: 「招待が必要です」（無効）+ 注意書き。`?invite=` は検証（存在・サロン一致・disabled でない・期限内・上限未満）し、有効なら join リンクに引き継ぐ。
- ログイン済み会員には「サロンのメインページを表示」→`/salons/{id}/home`、オーナーには「運営ダッシュボード」→`/owner/salons/{id}/dashboard` のリンク。

### `/salons/{id}/join` 入会確認
- 未ログイン → `/login?next=/salons/{id}/join`。invite サロンは招待コード必須（無効なら `/salons/{id}` へ）。
- `?plan=` のプラン（無指定なら先頭プラン）の料金内訳を表示: 月額、初月割引、トライアル、**初回請求額**（トライアル有→¥0、無→`max(0, price-introDiscount)`）。
- フォーム: POST `/api/join`（hidden: salonId, planId, invite）。ボタン「入会する」。
- 「← サロンページ」→ `/salons/{id}`。

## 4.2 認証ページ

### `/login`
- email / password / hidden `next`。POST `/api/auth/login`。
- `?error=invalid` →「メールアドレスまたはパスワードが正しくありません」。
- リンク: 「パスワードを忘れた方」→`/forgot-password`、「新規登録」→`/signup`。

### `/signup`
- displayName（40字まで）/ email / password（8字以上）/ hidden `next`。POST `/api/auth/signup`。
- `?error=exists` →「このメールアドレスは登録済みです」、`?error=invalid` →「入力内容を確認してください」、`?error=rate` → レート制限の旨。

### `/signup/verify`
- `?email=` のコード入力画面。6桁コード + hidden email/next。POST `/api/auth/verify`。
- **開発モード表示: この画面に、該当メールの最新 EmailVerification のコードをDBから読んで直接表示する**（「[開発用] 認証コード: 123456」のような枠。メールを送らない代替。必ず実装すること）。
- 再送フォーム: POST `/api/auth/resend-code`。
- `?error=invalid|rate|exists` を日本語表示。

### `/forgot-password`
- email 入力 → POST `/api/auth/forgot-password`。`?msg=sent|not-found|rate` を表示。

### `/forgot-password/verify`
- `?email=` + コード + 新パスワード + 確認。POST `/api/auth/reset-password`。
- 開発モード表示: 最新 PasswordReset コードをDBから読んで表示。
- `?error=invalid|rate|mismatch|short` を日本語表示。

## 4.3 会員ページ

### `/dashboard` マイページ
- 未ログイン → `/login`。
- 「参加中のサロン」: 自分の active な Membership（入会日降順）。サロン名・プラン名 → `/salons/{id}/home`。
- 「運営中のサロン」: 自分がオーナーのサロン（会員数付き）→ `/owner/salons/{id}/dashboard`。
- 「+ 新規開設」→ `/owner/salons/new`、「サロンを探す」→ `/salons`。

### `/account` アカウント設定
- プロフィール編集: displayName / bio / avatarUrl（画像アップロード部品）→ POST `/api/account/update`。
- 「支払い・課金管理へ」→ `/account/billing`。
- 退会（confirm付き）→ POST `/api/account/delete`。

### `/account/billing` 支払い管理
- 自分の全 Membership（サロン名・プラン・ステータス）と、各 Membership の Invoice 一覧（日付・金額・ステータス、降順）。
- active な Membership に「解約する」（confirm付き）→ POST `/api/membership/cancel`（hidden membershipId）。

## 4.4 サロン内ページ（requireSalonMember）

### `/salons/{id}/home` サロンホーム
- カバー画像・名前・tagline。サロン内ナビ: 「投稿一覧」→`.../posts`、「掲示板」→`.../board`、（オーナーのみ）「運営」→`/owner/salons/{id}/dashboard`。
- 会員（非オーナー）には現在プランカード: プラン名・月額、「プラン変更」→`/salons/{id}`、「退会する」（confirm付き）→ POST `/api/membership/cancel`（redirect=`/salons/{id}`）。
- 最新投稿5件（draft 除外、pinned 優先→新着順）。**プラン限定投稿の閲覧可否バッジ**付き（下記 4.7）。閲覧不可のものは薄く表示。
- 最新スレッド5件（新着順、返信数付き）→ `/salons/{id}/board/all/{threadId}`。

### `/salons/{id}/posts` 投稿一覧
- draft 除外、pinned 優先→新着順、全件。各行にタイトル・日付・可視性バッジ。→ `/salons/{id}/posts/{postId}`。
- オーナーには「+ 新規投稿」→ `/owner/salons/{id}/posts/new`。

### `/salons/{id}/posts/{postId}` 投稿詳細
- post の salonId が一致しなければ404。
- `plan:<planId>` 限定投稿で自分のプランが不一致（かつ非オーナー）→ 本文の代わりに「上位プラン限定です」案内 + `/salons/{id}` へのリンク。
- videoUrl があれば YouTube ID を抽出して iframe 埋め込み。
- 本文は保存済み `bodyHtml` を**そのまま**（再変換せず）表示。
- いいねボタン（トグル・件数表示）→ POST `/api/posts/{id}/react`。
- 「← 投稿一覧」。

### `/salons/{id}/board` → `/salons/{id}/board/all` へリダイレクト（カテゴリは "all" 1つだけのMVP仕様）

### `/salons/{id}/board/all` スレッド一覧
- スレッドを新着順に **20件/ページ**（`?page=`、ページ番号リンク）。各行: タイトル、投稿者名、コメント数、いいね数。
- 「+ 新規スレッド」→ `.../board/all/new`。「← サロンホーム」。
- ※URL上の `all` はパス変数 `{categoryId}` として受けるが、フィルタには使わない（参照実装どおり）。

### `/salons/{id}/board/all/new` スレッド作成
- title（200字）+ body。POST `/api/salons/{id}/threads/create`。

### `/salons/{id}/board/all/{threadId}` スレッド詳細
- 本文・コメント・返信は**表示時に Markdown→HTML 変換+サニタイズ**（Post とは方式が異なる。参照実装どおり）。
- いいねトグル → POST `/api/threads/{id}/react`。通報ボタン → POST `/api/threads/{id}/report`（hidden reason="user report"。理由入力UIは無し）。
- コメント一覧: トップレベルコメント＋その返信（1段ネスト）。各コメントに返信フォーム（hidden parentId）、末尾に新規コメントフォーム。どちらも POST `/api/threads/{id}/comment`。
- 「← スレッド一覧」。

## 4.5 オーナーページ（requireSalonOwner）

### `/owner/salons/new` サロン開設
- 未ログイン → `/login`。フォーム → POST `/api/owner/salons/create`。
- 項目: name（80字）、tagline（140字）、description（textarea）、category（select: ビジネス/趣味/アート/テクノロジー/教育/その他）、visibility（**「公開」「招待URL限定」の2択**）、coverUrl/thumbUrl（画像アップロード部品）、初期プラン（planName デフォルト「スタンダード」、planPrice デフォルト980、planTrial、planDiscount）。

### `/owner/salons/{id}/dashboard` 運営ダッシュボード
- 統計カード: 月次売上見込み（Σ プラン価格 × そのプランの active 会員数）、アクティブ会員数、投稿数、スレッド数。
- プラン別会員数テーブル。
- visibility=invite のとき: 有効な招待URL一覧（完全URL `http://localhost:8080/salons/{id}?invite={code}` と利用数）+ 簡易発行フォーム（label のみ）→ POST `.../invite/create`。
- ナビカード: 会員管理 / 投稿管理 / 売上・振込 / サロンホーム。「LP」→`/salons/{id}`、「設定」→`.../edit`。

### `/owner/salons/{id}/edit` サロン設定
- サロン更新フォーム（開設時と同項目）→ POST `.../update`。
- visibility=invite のとき招待URL管理: 各招待のステータス（有効/無効化済み/期限切れ/利用上限到達）、完全URL、利用数（`uses/maxUses`、0は「無制限」）、「無効化」→ POST `.../invite/disable`。追加発行フォーム（label / maxUses / expDays）→ POST `.../invite/create`。
- プラン管理: 既存プランごとに更新フォーム（name/price/trial/discount/description）→ POST `.../plan/update`。新規プラン追加フォーム → POST `.../plan/create`。
- 「← ダッシュボード」。

### `/owner/salons/{id}/members` 会員管理
- 全 Membership（入会日降順）のテーブル: 会員（表示名+email）/ 入会日 / プラン（select+「変更」→ POST `.../members/change-plan`）/ ステータス（active=緑, past_due=橙, その他=灰）/ 直近の Invoice（金額+status）/「退会させる」（confirm付き）→ POST `.../members/remove`。

### `/owner/salons/{id}/posts` 投稿管理
- 全投稿（draft 含む、新着順）。下書き行は背景色を変える。
- 列: タイトル（pinned は📌）/ 作成日 / 状態（下書き|公開中）/ 公開範囲（全会員|プラン限定）/ 操作（「表示」→`/salons/{id}/posts/{postId}`、「編集」→`.../posts/{postId}/edit`、「削除」confirm付き→ POST `.../posts/{postId}/delete`）。
- 「新規投稿」→ `.../posts/new`。

### `/owner/salons/{id}/posts/new` ・ `/owner/salons/{id}/posts/{postId}/edit` 投稿作成/編集
- 項目: title、本文（textarea。フォーム名は `bodyHtml`、**中身はMarkdown**）、videoUrl、公開範囲（select: 全会員 / 各プラン限定 `plan:{planId}`）、pinned チェックボックス。
- ボタン2つ: 「公開する」と「下書き保存」（name=draft value=1 の submit ボタンで区別）。
- 編集時の本文初期値は `bodyMarkdown`（null なら bodyHtml）。
- 本文中への画像挿入: 「画像を挿入」ボタン → `/api/upload` に fetch でアップロードし、カーソル位置に `![name](url)` を挿入する簡単なJSを付ける（プレビュー機能は任意。無くても可）。

### `/owner/salons/{id}/revenue` 売上・振込
- paid の Invoice（このサロン配下、新着順）から: 累計売上、手数料（5%）、振込可能額（net）。
- 振込申請フォーム → POST `.../payout`。**振込可能額が ¥3,000 未満のときはボタン無効**。
- Invoice テーブル: 日付 / 金額 / 手数料 / 振込額。

## 4.6 管理者ページ（requireAdmin）

### `/admin`
- 統計: ユーザー数 / サロン数 / 有効メンバーシップ数 / 総売上（paid Invoice 合計）。
- リンク: ユーザー管理 / サロン管理 / 通報管理。

### `/admin/users`
- `?q=`（email 部分一致・大文字小文字無視）検索フォーム（GET）。**20件/ページ**、登録日降順。
- 列: email / 表示名 / 登録日 / 認証済 / 管理者 / 操作。
- 操作: 「管理者トグル」→ POST `.../toggle-admin`、「停止」（confirm付き）→ POST `.../suspend`。passwordHash が `suspended` の行は「停止済」表示。

### `/admin/salons`
- **20件/ページ**、作成日降順。列: サロン名（→`/salons/{id}`）/ オーナーemail / カテゴリ / 公開設定 / 会員数 / 作成日 / 「削除」（confirm付き）→ POST `.../delete`。

### `/admin/reports`
- 全通報（新着順、ページング無し）。列: スレッド（→`/salons/{salonId}/board/all/{threadId}`、削除済みなら「削除済」）/ 通報者 / 理由 / 状態（解決済|未解決）/ 日時 / 「解決済にする」（未解決のみ）→ POST `.../resolve`。

## 4.7 共通部品

- **可視性バッジ + canViewPost(visibility, currentPlanId, isOwner)**: オーナーは常に閲覧可。`all` は閲覧可。`plan:<id>` は自分の planId が一致するときのみ可。バッジ表示: 「全会員」/「{プラン名}限定」/ 閲覧不可時は「🔒{プラン名}限定」。
- **画像アップロード部品**: hidden input + ファイル選択。選択時に JS で `/api/upload` へ POST し、返った url を hidden にセットしてプレビュー表示。4MB 超・非対応形式はエラーメッセージ表示。
- **サロンカバープレースホルダ**: 画像未設定時に「SALON」+カテゴリ+名前を出す枠。
