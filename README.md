<p align="center"><br><img src="https://user-images.githubusercontent.com/236501/85893648-1c92e880-b7a8-11ea-926d-95355b8175c7.png" width="128" height="128" /></p>
<h3 align="center">Generic OAuth 2</h3>
<p align="center"><strong><code>@capacitor-community/generic-oauth2</code></strong></p>
<p align="center">
  Opinionated Capacitor Plugin that makes it easy to add login functionality to your app using the OpenID Connect (OIDC) protocol.
</p>

<p align="center">
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" />
  <a href="https://www.npmjs.com/package/@capacitor-community/generic-oauth2"><img src="https://img.shields.io/npm/l/@capacitor-community/generic-oauth2?style=flat-square" /></a>
<br>
  <a href="https://www.npmjs.com/package/@capacitor-community/generic-oauth2"><img src="https://img.shields.io/npm/dw/@capacitor-community/generic-oauth2?style=flat-square" /></a>
  <a href="https://www.npmjs.com/package/@capacitor-community/generic-oauth2"><img src="https://img.shields.io/npm/v/@capacitor-community/generic-oauth2?style=flat-square" /></a>
</p>

## Introduction

This is a **generic OAuth 2 client** plugin.

## Installation

```bash
npm install @capacitor-community/generic-oauth2
npx cap sync
```

## Recipes

### Facebook

Facebook support OIDC perfectly. They call it "Limited Login". You should remember to whitelist the `redirectUri`. Pass a value for `callbackHttps` to match the `redirectUri`.

```ts
GenericOAuth2.authenticate({
  authorizationEndpoint: 'https://www.facebook.com/v25.0/dialog/oauth',
  clientId: 'example',
  redirectUri: 'https://example.com/facebook-redirect',
  scope: 'openid email',
  tokenEndpoint: 'https://graph.facebook.com/v25.0/oauth/access_token',
  callbackScheme: 'yourapp', // alternatively you could opt for utilizing the `fbexample` scheme that you might already have setup
  callbackHttps: {
    host: 'example.com',
    path: '/facebook-redirect',
  },
  responseType: 'code',
  responseMode: 'query',
  codeChallengeMethod: 'S256',
});
```

### Google

Google currently doesn't support PKCE using the Web Client (see https://issuetracker.google.com/issues/238577008 and https://issuetracker.google.com/issues/531782504).

Both the Android Client and iOS client do have for support PKCE though. So you can choose to setup both of them and pass the values linked to the platforms respectively. Alternatively, you can opt to just setup one (either Android or iOS doesn't matter) and use that client for both platforms. Keep in mind that both native platforms do not support specifying redirect URIs. So you are bound to using the `callbackScheme` that Google itself provides (see https://issuetracker.google.com/issues/531782504). Pass a value for `callbackScheme` to match the `redirectUri`.

```ts
GenericOAuth2.authenticate({
  authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
  clientId: 'example.apps.googleusercontent.com',
  redirectUri: 'com.googleusercontent.apps.example:/',
  scope: 'openid email profile',
  tokenEndpoint: 'https://oauth2.googleapis.com/token',
  callbackScheme: 'com.googleusercontent.apps.example',
  // `callbackHttps` cannot be used here as Google does not support it for the native clients:
  responseType: 'code',
  responseMode: 'query',
  codeChallengeMethod: 'S256',
});
```

## Callback handling

Upon authenticating a user, a browser window is opened. This plugin utilizes Auth Tab on Android and ASWebAuthenticationSession on iOS to achieve this. These APIs both rely on specifying a host+path (`callbackHttps`) or a scheme (`callbackScheme`) to be able to detect on which navigation hit it should finish the flow and close the browser window. Typically this should match the `redirectUri` passed to the `authenticate` method. But there are valid, more advanced, use cases where this can differ.

At least one of `callbackHttps` and `callbackScheme` must be provided. When both are provided, `callbackHttps` wins wherever it's supported:

| Platform                  | Callback used                                                               |
| ------------------------- | --------------------------------------------------------------------------- |
| Android                   | `callbackHttps` when provided, else `callbackScheme`                        |
| iOS 17.4+ / macOS 14.4+   | `callbackHttps` when provided, else `callbackScheme`                        |
| iOS < 17.4 / macOS < 14.4 | `callbackScheme` (see below); rejected if only `callbackHttps` was provided |

**Important — older iOS/macOS fallback:** those older iOS versions can't watch https callbacks, so the plugin watches `callbackScheme` instead. Your `redirectUri` page must then redirect the user to this `callbackScheme` directly. Some OAuth providers do not support passing a scheme as the `redirectUri`. To workaround this, you should redirect from the `redirectUri` once more to `callbackScheme` (e.g. `https://yourapp.com/oauth-redirect` redirects to `yourapp://callback?code=...&state=...`, forwarding all query parameters) to complete the flow. Without that extra hop, the login sheet will hang on those versions.

`callbackHttps` requires the `host` to be associated correctly with your app using [Digital Asset Links](https://developer.android.com/studio/write/app-link-indexing) on Android and [Associated Domains](https://developer.apple.com/documentation/xcode/supporting-associated-domains) on iOS.

`callbackScheme` does **not** require an intent filter (Android) or `CFBundleURLSchemes` (iOS). However, on Android, if a browser does not support Auth Tab, it falls back to a plain Custom Tab. And for that to work, it **does** require an intent filter to be [setup](#android-fallback-browsers).

## Sessions and logout

This plugin intentionally has no `logout` method. It performs authentication only and does not manage sessions. The philosophy of this plugin is that you should logout users by calling your backend. Your backend should then revoke the tokens with the provider. Note that logging out on your backend (or revoking tokens with the provider) does **not** clear the provider's session cookie in the device's system browser, so a subsequent `authenticate` call may silently sign the same user back in. If that's not what you want:

- Pass `preferEphemeralBrowsing: true` so no persistent provider cookie is created in the first place, or
- Force re-authentication per request by adding `prompt=login` (or `prompt=select_account`) to your `authorizationEndpoint`, e.g. `https://example-provider.com/authorize?prompt=login` — the plugin preserves query parameters already present on the endpoint URL.

## Known limitations

### Android process death

If the OS kills the app while the browser tab is open, the pending flow and its state (i.e. PKCE verifier, `state`) is lost and the pending JavaScript promise dies with the process. The user simply needs to tap sign-in again.

### Android fallback browsers

On browsers without Auth Tab support, the flow falls back to a plain Custom Tab and completes via a deep link. So that means your app **must** declare an intent filter matching your callback (being it either the `callbackHttps` or the `callbackScheme`). Note that a provider that (non-compliantly) omits the `state` on error redirects will hang the `authenticate` promise. In reality this hardly ever happens though.

## API

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### authenticate(...)

```typescript
authenticate(options: OAuth2AuthenticateOptions) => Promise<OAuth2AuthenticateResult>
```

| Param         | Type                                                                            |
| ------------- | ------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#oauth2authenticateoptions">OAuth2AuthenticateOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#oauth2authenticateresult">OAuth2AuthenticateResult</a>&gt;</code>

---

### Interfaces

#### OAuth2AuthenticateResult

| Prop           | Type                |
| -------------- | ------------------- |
| **`idToken`**  | <code>string</code> |
| **`rawNonce`** | <code>string</code> |

#### OAuth2AuthenticateOptions

| Prop                          | Type                                         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Default            |
| ----------------------------- | -------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ |
| **`authorizationEndpoint`**   | <code>string</code>                          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |                    |
| **`clientId`**                | <code>string</code>                          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |                    |
| **`redirectUri`**             | <code>string</code>                          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |                    |
| **`scope`**                   | <code>string</code>                          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |                    |
| **`tokenEndpoint`**           | <code>string</code>                          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |                    |
| **`callbackScheme`**          | <code>string</code>                          | URL scheme to watch for the OAuth callback. The flow finishes when the browser is redirected to this scheme. Read more about this in the ["Callback handling"](#callback-handling) section of the docs.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |                    |
| **`callbackHttps`**           | <code>{ host: string; path: string; }</code> | Combination of host+path to watch for the OAuth callback. The flow finishes when the browser is redirected to this host+path combination. Read more about this in the ["Callback handling"](#callback-handling) section of the docs. Three important notes: 1. `callbackHttps` is preferred over `callbackScheme` for improved security, DX and UX. 2. `callbackHttps` requires the `host` to be associated correctly with your app using [Digital Asset Links](https://developer.android.com/studio/write/app-link-indexing) on Android and [Associated Domains](https://developer.apple.com/documentation/xcode/supporting-associated-domains) on iOS. 3. If used without a `callbackScheme` fallback, authentication is rejected on iOS/macOS versions that don't support it. |                    |
| **`responseType`**            | <code>string</code>                          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |                    |
| **`responseMode`**            | <code>string</code>                          | Optional. When omitted, the parameter is left off the authorization URL entirely and thus the provider's default applies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |                    |
| **`codeChallengeMethod`**     | <code>'S256'</code>                          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |                    |
| **`preferEphemeralBrowsing`** | <code>boolean</code>                         | Ask the browser to use an ephemeral session that doesn't share cookies/website data between the authentication session and the user's normal browser session. This is best-effort: browsers that don't support ephemeral browsing ignore the hint. Consequently, this forces a fresh login. On iOS this also suppresses the "AppName wants to use example.com to sign in" prompt.                                                                                                                                                                                                                                                                                                                                                                                                | <code>false</code> |

</docgen-api>
