# planetlink.js

本レポジトリは、[planetlink] の npm レポジトリです。[planetlink] は Kotlin Multiplatform を用いて作成されたマルチ SNS クライアント抽象化ライブラリです。
複数のソーシャルメディアプラットフォームに対して統一的なインターフェースを提供します。
そのため、Web アプリケーションや Node.js 環境でも使用していただくことができます。

## 対応プラットフォーム

| プラットフォーム | 認証方法 |
|---------------|---------|
| Bluesky | パスワード |
| Mastodon | OAuth / アクセストークン |
| Misskey | OAuth / アクセストークン |
| Tumblr | OAuth / アクセストークン |
| Slack | OAuth / アクセストークン |
| Nostr | 秘密鍵 (nsec) |
| Matrix | パスワード / アクセストークン |
| X / Twitter | Cookie / Guest (読み取り専用) |

## 使用方法

### 追加方法

npm で管理している場合、以下のコマンドでアプリケーションに追加することができます。
本レポジトリにはバージョンは存在せず、[planetlink] のバージョンと一致するブランチが存在します。
どのバージョンの [planetlink] を使用するかは、本レポジトリのブランチを指定することで決定します。
[ブランチ一覧](https://github.com/uakihir0/planetlink.js/branches) からバージョンに対応するブランチを確認してください。

```shell
npm add uakihir0/planetlink.js
or
npm add uakihir0/planetlink.js#{{BRANCH_NAME}}
```

### 基本的な使い方

TypeScript の型情報も含まれており、TypeScript での記述をオススメします。
詳しい使い方については、[planetlink] の README も合わせて確認してください。

#### インポートパターン

```typescript
import {
  BlueskyAuth,
  BlueskyAction,
} from "planetlink-js/kotlin/planetlink-bluesky.mjs";

import {
  MastodonAuth,
} from "planetlink-js/kotlin/planetlink-mastodon.mjs";

import {
  BorderPaging,
  Account,
} from "planetlink-js/kotlin/planetlink-core.mjs";
```

#### Bluesky での認証

```typescript
import { BlueskyAuth } from "planetlink-js/kotlin/planetlink-bluesky.mjs";
import { BorderPaging } from "planetlink-js/kotlin/planetlink-core.mjs";

// 認証情報を作成してログイン
const auth = new BlueskyAuth("https://bsky.social", "wss://bsky.network");
const account = auth.accountWithIdentifyAndPassword("YOUR_HANDLE", "YOUR_PASSWORD");

// 認証ユーザー情報を取得
const user = await account.action.userMe();
console.log(user.name);       // 表示名
console.log(user.screenName); // ハンドル名

// ホームタイムラインを取得
const paging = new BorderPaging();
paging.count = 20;
const timeline = await account.action.homeTimeLine(paging);
const posts = timeline.entities.asJsReadonlyArrayView();

for (const post of posts) {
  console.log(post.text?.displayText);
}
```

#### Mastodon での認証

```typescript
import { MastodonAuth } from "planetlink-js/kotlin/planetlink-mastodon.mjs";

// アクセストークンでの認証
const auth = new MastodonAuth("https://mastodon.social", "mastodon");
const account = auth.accountWithAccessToken("YOUR_ACCESS_TOKEN", null, null);

const user = await account.action.userMe();
console.log(user.name);

// OAuth フローでの認証
auth.setClientInfo("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET");
const url = await auth.authorizationURL("YOUR_REDIRECT_URI", "read write");
// ... ユーザーが認証して認証コードを取得 ...
const account2 = await auth.accountWithCode("YOUR_REDIRECT_URI", "AUTH_CODE");
```

#### Misskey での認証

```typescript
import { MisskeyAuth } from "planetlink-js/kotlin/planetlink-misskey.mjs";

const auth = new MisskeyAuth("https://misskey.io/api/");
const account = auth.accountWithAccessToken("YOUR_ACCESS_TOKEN");

const user = await account.action.userMe();
console.log(user.name);
```

#### Slack での認証

```typescript
import { SlackAuth } from "planetlink-js/kotlin/planetlink-slack.mjs";

const auth = new SlackAuth("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET");
const account = auth.getAccountWithToken("YOUR_USER_TOKEN");

const user = await account.action.userMe();
console.log(user.name);
```

#### Nostr での認証

```typescript
import { NostrAuth } from "planetlink-js/kotlin/planetlink-nostr.mjs";
import { KtList } from "planetlink-js/kotlin/kotlin-kotlin-stdlib.mjs";

const relays = KtList.fromJsArray(["wss://relay.damus.io"]);
const auth = new NostrAuth(relays, "YOUR_NSEC_KEY");
const account = auth.accountWithPrivateKey();

const user = await account.action.userMe();
console.log(user.name);
```

#### Matrix での認証

```typescript
import { MatrixAuth } from "planetlink-js/kotlin/planetlink-matrix.mjs";

// アクセストークンでの認証
const auth = new MatrixAuth("https://matrix.org");
const account = auth.accountWithAccessToken("YOUR_ACCESS_TOKEN");

// またはパスワードでの認証
const account2 = await auth.accountWithPassword("@user:matrix.org", "YOUR_PASSWORD");

const user = await account.action.userMe();
console.log(user.name);
```

#### Tumblr での認証

```typescript
import { TumblrAuth } from "planetlink-js/kotlin/planetlink-tumblr.mjs";

const auth = new TumblrAuth();
auth.setConsumerInfo("YOUR_CONSUMER_KEY", "YOUR_CONSUMER_SECRET");
const account = auth.accountWithAccessToken("YOUR_ACCESS_TOKEN", "YOUR_REFRESH_TOKEN");

const user = await account.action.userMe();
console.log(user.name);
```

#### X / Twitter での認証

```typescript
import { XAuth, XPaging } from "planetlink-js/kotlin/planetlink-x.mjs";

const auth = new XAuth();
const account = auth.accountWithCookies("X_AUTH_TOKEN_COOKIE", "X_CT0_COOKIE");

// 共通ホームタイムラインは X の Following タイムラインに対応します。
const paging = new XPaging(20);
const following = await account.action.homeTimeLine(paging);

// Guest アカウントでは一部の公開情報だけを取得できます。
const guest = new XAuth().guestAccount();
```

X アダプターは意図的に読み取り専用です。投稿、返信、削除、いいね、
リポスト、ブックマーク変更、フォローは実装していません。

### 統一 API

どのプラットフォームで認証しても、同じ `AccountAction` インターフェースで操作できます:

```typescript
// プラットフォームに関係なく同じ API
const user = await account.action.userMe();
const timeline = await account.action.homeTimeLine(paging);
// プラットフォーム固有の変更操作は capabilities() で確認してください。
await account.action.postComment(commentForm);
await account.action.likeComment(identify);
await account.action.followUser(identify);
```

### コレクションの扱い方

Kotlin のコレクションはラップされています。`asJsReadonlyArrayView()` で JS 配列に変換できます:

```typescript
const timeline = await account.action.homeTimeLine(paging);
const posts = timeline.entities.asJsReadonlyArrayView();

// 通常の JS 配列メソッドが使えます
posts.forEach(post => console.log(post.text?.displayText));
```

Kotlin リストを API に渡す場合は `KtList.fromJsArray()` を使用します:

```typescript
import { KtList } from "planetlink-js/kotlin/kotlin-kotlin-stdlib.mjs";
const relays = KtList.fromJsArray(["wss://relay1.example.com", "wss://relay2.example.com"]);
```

## ライセンス

MIT License

## 作者

[Akihiro Urushihara](https://github.com/uakihir0)

[planetlink]: https://github.com/uakihir0/planetlink
