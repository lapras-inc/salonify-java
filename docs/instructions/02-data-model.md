# 02. データモデル定義

参照実装（Prisma schema）を JPA エンティティに移植する。**テーブル・カラム構成はこの通りにすること**。

共通事項:

- 主キーはすべて文字列ID。参照実装は cuid を使っている。Java では `UUID.randomUUID().toString()` で生成する `String id` でよい（`@Id` に手動セット。DBの型は `varchar`/`text`）。
- 日時カラムは `Instant` または `LocalDateTime`（UTC基準ならどちらでも可。全体で統一すること）。
- `?` 付きは NULL 許可。それ以外は NOT NULL。
- enum 的なカラム（status, visibility など）は **DB上は文字列**で持つ（参照実装と同じ）。Java 側で enum を使うかは任意。
- 外部キー制約・ユニーク制約・インデックスは下記の指定どおり付ける（JPAアノテーションで表現）。
- 「Cascade削除」と書いた関係は、親削除時に子も消えること（DB の `ON DELETE CASCADE` か JPA の cascade + orphanRemoval のどちらで実現してもよい）。

## テーブル一覧（14）

### User（`users` テーブル。`user` は PostgreSQL の予約語のため注意）

| カラム | 型 | 制約・デフォルト |
|---|---|---|
| id | String | PK |
| email | String | UNIQUE |
| passwordHash | String | |
| displayName | String | |
| avatarUrl | String? | |
| bio | String? | |
| emailVerified | Boolean | default false |
| isAdmin | Boolean | default false |
| stripeCustomerId | String? | （モック実装では常にnullだがカラムは作る） |
| createdAt | DateTime | default now |

### EmailVerification

会員登録時、User を作る**前**に認証コードとともに一時保存するテーブル。

| カラム | 型 | 制約 |
|---|---|---|
| id | String | PK |
| email | String | |
| code | String | 6桁数字（ゼロ埋め文字列） |
| displayName | String | |
| passwordHash | String | ハッシュ済みパスワードを一時保持 |
| expiresAt | DateTime | |
| createdAt | DateTime | default now |

INDEX: (email, code)

### PasswordReset

| カラム | 型 |
|---|---|
| id | String PK |
| email | String |
| code | String |
| expiresAt | DateTime |
| createdAt | DateTime default now |

INDEX: (email, code)

### Salon

| カラム | 型 | 制約・デフォルト |
|---|---|---|
| id | String | PK |
| ownerId | String | FK → User |
| name | String | |
| tagline | String | |
| description | String | 長文可（text型） |
| coverUrl | String? | |
| thumbUrl | String? | |
| category | String | |
| visibility | String | default "public"。値: `public` \| `invite`（DB上は `private` も許容） |
| createdAt | DateTime | default now |

INDEX: (ownerId), (visibility, createdAt)

### Invite（招待URL）— Salon削除でCascade削除

| カラム | 型 | 制約・デフォルト |
|---|---|---|
| id | String | PK |
| salonId | String | FK → Salon |
| code | String | UNIQUE。URLセーフなランダム文字列（12バイトをbase64url等） |
| label | String | default ""（用途メモ） |
| maxUses | Int | default 0（0=無制限） |
| uses | Int | default 0 |
| disabled | Boolean | default false |
| expiresAt | DateTime? | |
| createdAt | DateTime | default now |

INDEX: (salonId)

### Plan — Salon削除でCascade削除

| カラム | 型 | 制約・デフォルト |
|---|---|---|
| id | String | PK |
| salonId | String | FK → Salon |
| name | String | |
| priceJpy | Int | |
| trialDays | Int | default 0 |
| introDiscount | Int | default 0（初月の値引き額・円） |
| description | String? | |
| stripePriceId | String? | （モックでは常にnull） |

INDEX: (salonId)

### Membership — Salon削除でCascade削除

| カラム | 型 | 制約・デフォルト |
|---|---|---|
| id | String | PK |
| userId | String | FK → User |
| salonId | String | FK → Salon |
| planId | String | FK → Plan |
| stripeSubscriptionId | String? | |
| status | String | default "active"。値: `active` \| `past_due` \| `cancelled` \| `suspended` |
| joinedAt | DateTime | default now |
| nextBillAt | DateTime | NOT NULL |
| failedCount | Int | default 0 |

UNIQUE: (userId, salonId)　INDEX: (salonId, status), (userId, status)

### Invoice — Membership削除でCascade削除

| カラム | 型 |
|---|---|
| id | String PK |
| membershipId | String FK → Membership |
| amountJpy | Int |
| status | String（`paid` \| `failed` \| `refunded`） |
| createdAt | DateTime default now |

### Post — Salon削除でCascade削除

| カラム | 型 | 制約・デフォルト |
|---|---|---|
| id | String | PK |
| salonId | String | FK → Salon |
| authorId | String | FK → User |
| title | String | |
| bodyHtml | String | サニタイズ済みHTML（text型） |
| bodyMarkdown | String? | Markdown原文（text型） |
| videoUrl | String? | YouTube URL |
| visibility | String | default "all"。値: `all` \| `plan:<planId>` |
| pinned | Boolean | default false |
| draft | Boolean | default false |
| createdAt | DateTime | default now |

INDEX: (salonId, createdAt)

### Thread — Salon削除でCascade削除

| カラム | 型 |
|---|---|
| id | String PK |
| salonId | String FK → Salon |
| authorId | String FK → User |
| title | String |
| body | String（text型・プレーンテキスト） |
| createdAt | DateTime default now |

INDEX: (salonId, createdAt)

### Comment — Thread削除でCascade削除

| カラム | 型 |
|---|---|
| id | String PK |
| threadId | String FK → Thread |
| authorId | String FK → User |
| parentId | String?（自己参照FK。ネストは1段まで） |
| body | String（text型） |
| createdAt | DateTime default now |

INDEX: (threadId, createdAt), (parentId)

### Reaction（スレッドのいいね）— Thread削除でCascade削除

| カラム | 型 |
|---|---|
| id | String PK |
| threadId | String FK → Thread |
| userId | String FK → User |
| kind | String default "like"（現状 like のみ使用） |

UNIQUE: (threadId, userId, kind)

### PostReaction（投稿のいいね）— Post削除でCascade削除

| カラム | 型 |
|---|---|
| id | String PK |
| postId | String FK → Post |
| userId | String FK → User |

UNIQUE: (postId, userId)　INDEX: (postId)

### Report（通報）

| カラム | 型 |
|---|---|
| id | String PK |
| threadId | String?（FK → Thread。Cascadeなし） |
| reporterId | String FK → User |
| reason | String |
| resolved | Boolean default false |
| createdAt | DateTime default now |
