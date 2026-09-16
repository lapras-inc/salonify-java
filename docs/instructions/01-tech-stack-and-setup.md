# 01. 技術スタックとプロジェクトセットアップ（固定事項）

この文書の内容は**決定事項**である。別の技術・別のバージョン系への変更は行わないこと。
迷ったらこの文書に従う。ここに書いていない細部（パッケージ名の深い階層など）は一般的な慣習に従ってよい。

## 技術スタック（固定）

| レイヤー | 技術 | 備考 |
|---|---|---|
| 言語 | Java 21 | |
| フレームワーク | Spring Boot 3.5系 | Spring MVC（サーバーサイドレンダリング） |
| ビルド | Maven | `mvnw` ラッパー同梱 |
| テンプレート | Thymeleaf | |
| DB | PostgreSQL 16 | docker compose で起動 |
| ORM | Spring Data JPA (Hibernate) | ActiveRecord 的な使い方でよい。集約・リポジトリ分割等の凝った設計は不要 |
| スキーマ管理 | `spring.jpa.hibernate.ddl-auto=update` | マイグレーションツール（Flyway等）は使わない |
| パスワード | `spring-security-crypto` の `BCryptPasswordEncoder(10)` | **Spring Security 本体（フィルタチェーン設定）は使わない** |
| セッション | JWT (HS256) を httpOnly Cookie に格納。ライブラリは `jjwt` (io.jsonwebtoken) | 自前の `HandlerInterceptor` で認証チェック |
| Markdown | `org.commonmark:commonmark` | |
| テスト | JUnit 5 + spring-boot-starter-test | ユニットテスト中心でよい（対象は 06 参照） |
| CSS | 手書きの1枚のCSSファイル | Node/Tailwind 等のフロントビルドは導入しない。見た目は簡素でよい |

## やらないこと（重要）

- **Spring Security のフィルタチェーン設定はしない**。認証・認可は参照実装と同様に自前実装する（JWT Cookie + Interceptor + ガード関数）。
- **実際の外部サービス連携はしない**。メール送信・Stripe決済・クラウドストレージはすべてローカル完結のモック実装にする（詳細は 03 参照）。
- デプロイ関連の作業はしない。
- SPA化・REST API化はしない。参照実装と同じ「フォームPOST → 303リダイレクト（PRGパターン）」で作る。

> **方針変更（2026-07 追記）**: 決済のみ例外を後から追加した。`STRIPE_SECRET_KEY` にテストキー（`sk_test_...`）を設定した場合に限り Stripe Checkout（テストモード）へ実接続する。未設定なら従来どおりモック即入会。詳細は 03 の「決済」の追記を参照。

## プロジェクト構成

リポジトリルート直下に `app/` ディレクトリを作り、その中に Spring Boot プロジェクトを置く。

```
app/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/
├── src/main/java/com/example/salonify/
│   ├── SalonifyApplication.java
│   ├── controller/        # 画面ごと・API相当のコントローラ
│   ├── entity/            # JPAエンティティ（02の定義に従う）
│   ├── repository/        # Spring Data JPA リポジトリ
│   ├── service/           # 必要最小限でよい。コントローラに書いても減点しない
│   ├── support/           # session, guards, sanitize, rate-limit, mail, payment 等
│   └── config/            # Interceptor登録など
├── src/main/resources/
│   ├── application.yml
│   ├── templates/         # Thymeleaf
│   └── static/css/app.css
└── src/test/java/...
```

## docker compose（必須要件）

リポジトリルートに `docker-compose.yml` を置き、**`docker compose up` の一発でDB起動 → スキーマ反映 → シードデータ投入 → アプリ起動まで完了**し、`http://localhost:8080` でアクセスできること。ホストマシンに Java や Maven が入っていなくても動くこと（ビルドはDockerfile内のマルチステージビルドで行う）。

```yaml
# 例（このまま使ってよい）
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: salon
      POSTGRES_PASSWORD: salon
      POSTGRES_DB: salon_dev
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U salon -d salon_dev"]
      interval: 3s
      retries: 10
  app:
    build: ./app
    ports: ["8080:8080"]
    environment:
      DATABASE_URL: jdbc:postgresql://db:5432/salon_dev
      DATABASE_USER: salon
      DATABASE_PASSWORD: salon
      SESSION_SECRET: dev-secret-change-me-please-32chars-min
    volumes:
      - uploads:/app/uploads      # 画像アップロード保存先
    depends_on:
      db:
        condition: service_healthy
volumes:
  pgdata:
  uploads:
```

Dockerfile はマルチステージにする（例: stage1 `maven:3-eclipse-temurin-21` でビルド → stage2 `eclipse-temurin:21-jre` で実行）。

## 環境変数

| 変数 | 用途 | デフォルト（未設定時） |
|---|---|---|
| `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD` | DB接続 | compose が渡す値 |
| `SESSION_SECRET` | JWT署名鍵 | `dev-secret-change-me-please-32chars-min` にフォールバック |

メール・決済・画像はモック実装のため環境変数不要（03参照）。

## シードデータ投入

アプリ起動時に `CommandLineRunner` で投入する。**冪等にすること**（`salonify-owner@example.com` の User が存在し、かつそのユーザーがサロンを保有していればコンテンツ投入をスキップする。詳細は `05-seed-data.md` 参照）。投入内容は `05-seed-data.md` に従う。
