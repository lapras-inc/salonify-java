# 指示書: Salonify（オンラインサロンプラットフォーム）の Java/Spring Boot 実装

あなたのタスクは、参照実装（Next.js製のオンラインサロンプラットフォーム「Salonify」）と**同じ仕様のサービスを Java / Spring Boot で構築する**ことです。

## このリポジトリについて

- これはコードレビュー実演イベント用のリポジトリです。あなたが作ったサービスに対して Pull Request ベースのレビューが行われます。
- 参照実装は `refs/web-app/salon-platform/`（git submodule）にあります。仕様に迷ったらここのソースを直接読んで挙動を合わせてください。
- 元の要件定義は `docs/reference/requirements.md`、参照実装の README は `docs/reference/original-service-README.md` にあります。

## 読む順番

| ファイル | 内容 |
|---|---|
| [01-tech-stack-and-setup.md](01-tech-stack-and-setup.md) | 技術スタック（固定）、プロジェクト構成、docker compose 要件 |
| [02-data-model.md](02-data-model.md) | 全13テーブルの定義 |
| [03-api-spec.md](03-api-spec.md) | 共通基盤（認証・ガード・サニタイズ・レート制限・モック方針）と全エンドポイント仕様 |
| [04-pages-spec.md](04-pages-spec.md) | 全画面の仕様（表示・フォーム・遷移・アクセス制御） |
| [05-seed-data.md](05-seed-data.md) | シードデータ |
| [06-milestones.md](06-milestones.md) | **実装順序と各ステップの完了条件（この順で進める）** |

## 絶対に守ること

1. **技術選定を変えない**（01 に固定事項として明記。Spring Security 本体は使わない、フロントビルドは入れない、等）。
2. **`docker compose up` の一発で起動する**こと。ホストマシンには Docker 以外何も要求しない。
3. **外部サービスに接続しない**。メール・決済・画像ストレージはすべて 03 に定義したモック/ローカル実装。デプロイもしない。
4. **参照実装の挙動を仕様として忠実に再現する**。参照実装には奇妙な仕様（例: Markdown を受け取るフォーム名が `bodyHtml`、権限エラー時に404で存在を隠す、ユーザー停止は passwordHash の上書き）が含まれるが、これらは意図的にそのまま作る。勝手に「改善」しない。
5. **06 のマイルストーン順に実装し、各ステップの完了条件を検証してから次へ進む**。
6. 作業は `app/` ディレクトリと ルートの `docker-compose.yml`、ルート `README.md` に閉じる。`refs/` と `docs/` は変更しない。

## スコープ外（作らないもの）

- Stripe 実連携（Checkout / Webhook）、Resend による実メール送信、Vercel Blob、デプロイ設定
- 定期課金バッチ（参照実装にも存在しない。入会後の再請求は発生しない）
- 掲示板の複数カテゴリ（"all" 固定）、通報理由の入力UI、モバイルアプリ、多言語化

> **方針変更（2026-07 追記）**: Stripe 実連携のうち「Checkout 遷移 + `GET /api/join/success` での入会確定」のみ、後から実装対象に加えた。参照実装と同じく `STRIPE_SECRET_KEY` の有無で分岐する二段構えで、キー未設定（デフォルト）なら従来どおりモック即入会のため、`docker compose up` 一発起動は変わらない。Webhook・継続課金・解約同期・プラン変更時のサブスク更新は引き続きスコープ外。詳細は 03 の「決済」の追記を参照。
