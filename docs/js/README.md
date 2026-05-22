> [日本語](./README_ja.md)

# planetlink.js

This repository is the npm repository for [planetlink]. [planetlink] is a multi-social media client abstraction library created using Kotlin Multiplatform.
It provides a unified interface to interact with multiple social media platforms transparently.
Therefore, it can be used in web applications and Node.js environments.

## Supported Platforms

| Platform | Auth Method |
|----------|------------|
| Bluesky | Password |
| Mastodon | OAuth / Access Token |
| Misskey | OAuth / Access Token |
| Tumblr | OAuth / Access Token |
| Slack | OAuth / Access Token |
| Nostr | Private Key (nsec) |
| Matrix | Password / Access Token |

## Usage

### Installation

If you're managing with npm, you can add it to your application using the following command.
There are no versions in this repository, but there are branches corresponding to [planetlink] versions.
Please check the branches on the [branch list](https://github.com/uakihir0/planetlink.js/branches) to find the branch corresponding to the version you want to use.

```shell
npm add uakihir0/planetlink.js
or
npm add uakihir0/planetlink.js#{{BRANCH_NAME}}
```

### Basic Usage

TypeScript type information is included, so it's recommended to write in TypeScript.
Please also refer to the README of [planetlink] for detailed usage.

#### Import Pattern

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

#### Bluesky Authentication

```typescript
import { BlueskyAuth } from "planetlink-js/kotlin/planetlink-bluesky.mjs";
import { BorderPaging } from "planetlink-js/kotlin/planetlink-core.mjs";

// Create auth and authenticate
const auth = new BlueskyAuth("https://bsky.social", "wss://bsky.network");
const account = auth.accountWithIdentifyAndPassword("YOUR_HANDLE", "YOUR_PASSWORD");

// Get authenticated user info
const user = await account.action.userMe();
console.log(user.name);       // Display name
console.log(user.screenName); // Handle

// Get home timeline
const paging = new BorderPaging();
paging.count = 20;
const timeline = await account.action.homeTimeLine(paging);
const posts = timeline.entities.asJsReadonlyArrayView();

for (const post of posts) {
  console.log(post.text?.displayText);
}
```

#### Mastodon Authentication

```typescript
import { MastodonAuth } from "planetlink-js/kotlin/planetlink-mastodon.mjs";

// With access token
const auth = new MastodonAuth("https://mastodon.social", "mastodon");
const account = auth.accountWithAccessToken("YOUR_ACCESS_TOKEN", null, null);

const user = await account.action.userMe();
console.log(user.name);

// With OAuth flow
auth.setClientInfo("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET");
const url = await auth.authorizationURL("YOUR_REDIRECT_URI", "read write");
// ... user authorizes and returns with code ...
const account2 = await auth.accountWithCode("YOUR_REDIRECT_URI", "AUTH_CODE");
```

#### Misskey Authentication

```typescript
import { MisskeyAuth } from "planetlink-js/kotlin/planetlink-misskey.mjs";

const auth = new MisskeyAuth("https://misskey.io/api/");
const account = auth.accountWithAccessToken("YOUR_ACCESS_TOKEN");

const user = await account.action.userMe();
console.log(user.name);
```

#### Slack Authentication

```typescript
import { SlackAuth } from "planetlink-js/kotlin/planetlink-slack.mjs";

const auth = new SlackAuth("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET");
const account = auth.getAccountWithToken("YOUR_USER_TOKEN");

const user = await account.action.userMe();
console.log(user.name);
```

#### Nostr Authentication

```typescript
import { NostrAuth } from "planetlink-js/kotlin/planetlink-nostr.mjs";
import { KtList } from "planetlink-js/kotlin/kotlin-kotlin-stdlib.mjs";

const relays = KtList.fromJsArray(["wss://relay.damus.io"]);
const auth = new NostrAuth(relays, "YOUR_NSEC_KEY");
const account = auth.accountWithPrivateKey();

const user = await account.action.userMe();
console.log(user.name);
```

#### Matrix Authentication

```typescript
import { MatrixAuth } from "planetlink-js/kotlin/planetlink-matrix.mjs";

// With access token
const auth = new MatrixAuth("https://matrix.org");
const account = auth.accountWithAccessToken("YOUR_ACCESS_TOKEN");

// Or with password
const account2 = await auth.accountWithPassword("@user:matrix.org", "YOUR_PASSWORD");

const user = await account.action.userMe();
console.log(user.name);
```

#### Tumblr Authentication

```typescript
import { TumblrAuth } from "planetlink-js/kotlin/planetlink-tumblr.mjs";

const auth = new TumblrAuth();
auth.setConsumerInfo("YOUR_CONSUMER_KEY", "YOUR_CONSUMER_SECRET");
const account = auth.accountWithAccessToken("YOUR_ACCESS_TOKEN", "YOUR_REFRESH_TOKEN");

const user = await account.action.userMe();
console.log(user.name);
```

### Unified API

Once authenticated with any platform, you can use the same `AccountAction` interface:

```typescript
// These work the same regardless of platform
const user = await account.action.userMe();
const timeline = await account.action.homeTimeLine(paging);
await account.action.postComment(commentForm);
await account.action.likeComment(identify);
await account.action.followUser(identify);
```

### Working with Collections

Kotlin collections are wrapped. Use `asJsReadonlyArrayView()` to convert to JS arrays:

```typescript
const timeline = await account.action.homeTimeLine(paging);
const posts = timeline.entities.asJsReadonlyArrayView();

// Now you can use standard JS array methods
posts.forEach(post => console.log(post.text?.displayText));
```

To pass a Kotlin list to the API, use `KtList.fromJsArray()`:

```typescript
import { KtList } from "planetlink-js/kotlin/kotlin-kotlin-stdlib.mjs";
const relays = KtList.fromJsArray(["wss://relay1.example.com", "wss://relay2.example.com"]);
```

## License

MIT License

## Author

[Akihiro Urushihara](https://github.com/uakihir0)

[planetlink]: https://github.com/uakihir0/planetlink
