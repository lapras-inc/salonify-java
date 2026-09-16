# 参照実装（refs/web-app/salon-platform）調査メモ

指示書作成のために参照実装の全ソース（約3,700行）を調査した際の特記事項。指示書本体は `docs/instructions/` を参照。

## 実装状況の要点

- 機能はほぼ README どおり実装済み。ただし**決済・振込・メールは実サービスキーが無い場合モック動作**であり、ローカル（docker compose）では常にモック:
  - メール: `RESEND_API_KEY` 空 → コンソール出力 + 認証コードを画面に直接表示
  - 決済: `STRIPE_SECRET_KEY` 空 → Stripe Checkout をスキップして即入会（Invoice だけ paid で記録）
  - 振込申請（payout）: キーの有無に関係なく完全モック（成功メッセージを返すだけ）
- 定期課金バッチは存在しない。`Membership.nextBillAt` は設定されるが消費するジョブが無い（本番は Stripe Webhook 依存）。
- 掲示板のカテゴリは "all" 1つに固定（URL 構造だけ複数カテゴリ対応の名残）。

## 参照実装の癖・既知の問題（Java版でも「仕様」として再現する）

1. 投稿フォームのフィールド名が `bodyHtml` だが中身は Markdown 原文
2. オーナー権限エラーは 403 でなく 404（存在を隠す）。admin はトップへリダイレクト
3. ユーザー停止 = `passwordHash` を文字列 `suspended` で上書き（ログイン照合が自然に失敗する仕掛け）
4. 退会 = 物理削除せず email/passwordHash/displayName をスクランブル
5. 招待コードの `uses` は join 処理の途中で先にインクリメントされ、その後の検証失敗でも戻らない
6. forgot-password はユーザー不在時 `msg=not-found` を返す（メールアドレスの存在が分かる）
7. アップロード上限は実際 4MB だが、コメント等に 5MB の記述が残る不整合
8. sanitizeHtml は正規表現ベースの自作ホワイトリスト（コメント自ら DOMPurify 推奨と記載）
9. Post は保存時サニタイズ+表示時 raw、Thread/Comment/サロン説明は生保存+表示時変換、という非対称
10. JWT はステートレス30日。ログアウトは Cookie 削除のみで失効機構なし。suspend/パスワード変更でも既存トークンは生きる
11. レート制限はプロセス内メモリの固定ウィンドウ（分散非対応）
12. `Salon.visibility` の `private` はレガシー値（UI は public / invite の2択）
13. 通報理由は固定文字列 "user report"（入力 UI なし）
14. seed に管理者ユーザーが存在しない（Java 版では動作確認用に admin@example.com を追加した — 05-seed-data.md 参照）

## Java 版で参照実装から意図的に変えた点

- 決済: Stripe 実連携コードパス（Checkout/Webhook/`/api/join/success`）は実装対象外にし、モック挙動を正式仕様化
- 画像: Vercel Blob → ローカルディスク保存（`/uploads/**` 静的配信、docker volume）
- メール: Resend → ログ出力のみ（コード画面表示は同じ）
- ポート: 3001 → 8080
- seed: admin ユーザーとプラン限定投稿を動作確認用に追加
