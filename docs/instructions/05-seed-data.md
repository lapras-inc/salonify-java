# 05. シードデータ

アプリ起動時に `CommandLineRunner` で投入する。**冪等**にすること（`salonify-owner@example.com` の User が既に存在し、かつそのユーザーがサロンを持っていれば何もしない）。

パスワードはすべて `password123`（BCrypt strength 10 でハッシュして保存）。

## ユーザー

| email | 表示名 | 役割 | 備考 |
|---|---|---|---|
| salonify-owner@example.com | 山田オーナー | サロンオーナー | |
| salonify-member@example.com | 鈴木メンバー | メンバー | 下記サロンの「ライト」プランに加入 |
| salonify-admin@example.com | 管理者 | 管理者 | isAdmin=true。**参照実装のseedには無いが、管理画面の動作確認用に追加する** |

## サロン

salonify-owner@example.com がオーナーの1サロン:

- name: `テック起業ラボ`
- tagline: `エンジニア出身の起業家のための実践コミュニティ`
- description: `スタートアップ立ち上げのノウハウを共有します。\n\n毎週ライブ勉強会を開催。`
- category: `ビジネス`、visibility: `public`

### プラン

| name | priceJpy | description |
|---|---|---|
| ライト | 980 | （なし） |
| スタンダード | 2980 | 全コンテンツ + 月1面談 |

## メンバーシップ・請求

- salonify-member@example.com を「ライト」プランで加入させる: status=active、nextBillAt=now+30日。
- その Membership に Invoice を1件: amountJpy=980、status=paid。

## コンテンツ

- 投稿1（オーナー作成）: title `ようこそ！`、bodyHtml `<p>はじめまして。このサロンでは起業の実践ノウハウを共有します。</p>`、pinned=true、visibility=all。
- 投稿2（オーナー作成）: title `【スタンダード限定】月次戦略レポート`、本文は適当なMarkdown、visibility=`plan:<スタンダードプランのid>`。**参照実装のseedには無いが、プラン限定表示（🔒バッジ・閲覧不可画面）の動作確認用に追加する**。
- スレッド1（メンバー作成）: title `自己紹介スレ`、body `よろしくお願いします！`。

## 起動ログ

投入後に以下をログ出力する:

```
Seeded:
  salonify-owner@example.com / password123
  salonify-member@example.com / password123
  salonify-admin@example.com / password123
```
