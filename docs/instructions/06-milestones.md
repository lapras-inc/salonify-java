# 06. 実装マイルストーンと完了条件

**必ずこの順番で進めること。** 各ステップの完了条件をすべて満たしてから次に進む。
完了条件の検証は、原則 `docker compose up` で起動した実物に対して `curl` かブラウザで行う。

前ステップの成果を壊していないか、各ステップの最後に `docker compose up --build` での起動確認を必ず行うこと。

---

## Step 0: プロジェクト骨格と docker compose 起動

- `app/` に Spring Boot プロジェクトを作成（01 の構成・依存関係）。トップページに仮の「Salonify」表示。
- ルートに `docker-compose.yml` と `app/Dockerfile`（マルチステージ）。

**完了条件**
- [ ] ホストに Java が無くても `docker compose up --build` だけで起動する
- [ ] `curl -s http://localhost:8080/` が 200 を返す
- [ ] DB コンテナに接続できている（起動ログにエラーが無い）

## Step 1: エンティティ・リポジトリ・シード

- 02 の13テーブルを JPA エンティティ化、Spring Data リポジトリ作成。
- 05 のシードを CommandLineRunner で投入（冪等）。

**完了条件**
- [ ] 起動ログに `Seeded:` が出る。再起動しても二重投入されない
- [ ] `docker compose exec db psql -U salon -d salon_dev -c '\dt'` で13テーブルが見える
- [ ] users / salons / plans / memberships / invoices / posts / threads にシード行が入っている

## Step 2: セッション基盤と認証（signup / verify / login / logout / パスワードリセット）

- 03 の共通基盤: BCrypt、JWT Cookie セッション、getCurrentUser、レート制限、EmailSender（ログ出力モック）。
- 認証エンドポイント7本と認証系ページ5枚（04 の 4.2）。共通レイアウト・フラッシュメッセージもここで作る。

**完了条件**
- [ ] 新規登録 → verify 画面に開発用コードが表示される → 入力すると User が作成され `/dashboard` に着地する
- [ ] 誤ったコードで `?error=invalid` に戻る。10分期限が効いている（コード上の確認でよい）
- [ ] `salonify-owner@example.com / password123` でログインできる。誤パスワードは `/login?error=invalid`
- [ ] ログアウトで Cookie が消え、`/dashboard` が `/login` にリダイレクトされる
- [ ] パスワードリセット一式が通る（コード画面表示 → 新パスワードでログイン）
- [ ] ログインを短時間に21回試行すると 429 が返る

## Step 3: サロン閲覧・開設（公開側 + オーナーCRUD）

- ページ: `/`、`/salons`、`/salons/{id}`、`/owner/salons/new`、`/owner/salons/{id}/edit`、`/owner/salons/{id}/dashboard`。
- API: salons/create、update、plan/create、plan/update。ガード `requireSalonOwner` 実装。

**完了条件**
- [ ] トップと一覧にシードのサロンが出る。カテゴリフィルタ・人気順が機能する
- [ ] LP に説明（Markdown変換済み）、プラン、オーナー紹介、会員数が出る
- [ ] ログイン状態でサロンを新規開設でき、初期プランが同時に作られ、ダッシュボードに `?msg=salon-created` が出る
- [ ] 他人のサロンの `/owner/salons/{id}/edit` を開くと **404** になる（403やリダイレクトではない）
- [ ] 未ログインで `/owner/salons/{id}/edit` を開くと `/login` に飛ぶ
- [ ] プランの追加・更新ができ、価格が 0〜100000 にクランプされる

## Step 4: 入会・退会（モック決済）・マイページ

- ページ: `/salons/{id}/join`、`/dashboard`、`/account`、`/account/billing`、`/salons/{id}/home`。
- API: join、membership/cancel、account/update、account/delete。ガード `requireSalonMember`。

**完了条件**
- [ ] 新規ユーザーで「ライト」に入会 → `/salons/{id}/home?msg=joined`、Invoice(980, paid) が作られる
- [ ] トライアル付きプランなら Invoice が作られない（firstAmount=0）
- [ ] 同一プランに再入会しようとすると何も起きず home へ。別プランを選ぶと `?msg=plan-changed` でプランだけ変わる
- [ ] `/account/billing` で解約 → status=cancelled になり、`/salons/{id}/home` が LP へリダイレクトするようになる
- [ ] 退会（account/delete）で email が `deleted_...@example.invalid` に書き換わり、ログイン不能になる
- [ ] 非会員が `/salons/{id}/home` に直アクセスすると `/salons/{id}` に飛ぶ

## Step 5: 投稿（Markdown・可視性・下書き・ピン留め・いいね）+ 画像アップロード

- ページ: 投稿一覧/詳細（会員側）、投稿管理/作成/編集（オーナー側）。
- API: posts/create・update・delete、posts/{id}/react、upload。sanitizeHtml・renderMarkdown・extractYouTubeId 実装。

**完了条件**
- [ ] オーナーが Markdown で投稿 → 会員側で HTML 表示される。`<script>alert(1)</script>` を本文に入れても実行されない（サニタイズ確認）
- [ ] 下書きは会員の一覧・ホームに出ず、オーナーの投稿管理には出る
- [ ] pinned 投稿が一覧の先頭に来る
- [ ] スタンダード限定投稿がライト会員には🔒表示になり、詳細を開くと「上位プラン限定」案内になる。オーナーは見える
- [ ] YouTube URL 付き投稿で iframe 埋め込みが出る
- [ ] いいねがトグルする（2回押すと消える）
- [ ] `curl -F "file=@test.png" -b "session=..." http://localhost:8080/api/upload` が `{"url":"/uploads/..."}` を返し、その URL で画像が見える。5MB 超は 400

## Step 6: 掲示板（スレッド・コメント・いいね・通報）

- ページ: board 一覧（20件ページング）/ 作成 / 詳細。
- API: threads/create、comment、react、report。

**完了条件**
- [ ] スレッド作成 → `?msg=thread-created` で詳細へ。本文が Markdown 表示される
- [ ] コメントと返信ができ、返信への返信がルート直下にフラット化される（1段ネスト）
- [ ] 21件スレッドを作るとページ2が生まれる
- [ ] 通報すると Report 行が作られる
- [ ] 非会員はスレッド系 API を叩いても操作できない（LP へリダイレクト）

## Step 7: 招待URL限定サロン

- API: invite/create、invite/disable。visibility=invite 切替時の自動発行。LP・join の招待コード検証。

**完了条件**
- [ ] サロンを「招待URL限定」に変更すると招待コードが自動発行され、edit/dashboard に完全URLが表示される
- [ ] 招待URL無しの非会員は LP で「招待が必要です」になり join できない
- [ ] `?invite=<code>` 付きなら入会でき、uses が増える
- [ ] 無効化した招待・期限切れ・上限到達の招待では入会できない

## Step 8: 売上・振込 + 管理者画面

- ページ: revenue、admin 4画面。API: payout、admin 4本。`requireAdmin`。

**完了条件**
- [ ] revenue に累計売上・手数料5%（floor）・振込額が正しく出る（980円なら手数料49円・振込931円）
- [ ] 振込可能額 ¥3,000 未満でボタン無効。申請すると `?msg=payout-requested`（処理は無しでよい）
- [ ] salonify-admin@example.com で `/admin` に入れる。salonify-member@example.com では `/` に戻される
- [ ] ユーザー検索・20件ページング・停止（→そのユーザーがログイン不能に）・管理者トグルが機能する
- [ ] サロン削除で配下の投稿・スレッド・メンバーシップも消える
- [ ] 通報一覧で「解決済にする」が機能する

## Step 9: 仕上げ・テスト・最終検証

- ユニットテスト（最低限。JUnit 5）: `platformFee`/`ownerNet` の境界、`sanitizeHtml`（script除去・許可タグ通過・onclick除去・javascript:除去）、`extractYouTubeId`、レート制限、コメントのフラット化ロジック。
- `./mvnw test` が docker 内ビルドで実行されるようにする（Dockerfile のビルドステージで `mvn -q test` を回すか、少なくともローカルで green にする）。
- リポジトリルートに `README.md` を書く: 起動手順（`docker compose up`）、テストアカウント表、機能一覧、モック仕様（メール=コード画面表示、決済=スキップ、振込=何もしない）の説明。

**完了条件（最終受け入れ）**
- [ ] クリーンな状態（`docker compose down -v` 後）から `docker compose up --build` の一発で全機能が動く
- [ ] 03 に列挙した全エンドポイントが存在し、指定どおりのリダイレクト先・`?msg=` を返す
- [ ] 3アカウント（owner/member/admin）で 04 の全ページを一巡してエラーが出ない
- [ ] `./mvnw test` が成功する

---

## 詰まったときのルール

- 仕様に迷ったら 03/04 を最優先で信頼する。それでも不明なら `refs/web-app/salon-platform` の該当ソース（Next.js実装）を直接読んで挙動を合わせる。
- 参照実装の「変な仕様」（フィールド名 `bodyHtml` にMarkdownが入る、404で存在を隠す、suspend が passwordHash 上書き、招待 uses の先行インクリメント等）は**バグではなく仕様**なのでそのまま作る。
- 勝手に機能を追加・改善しない（Spring Security 導入、REST API 化、リッチUI化などをしない）。
