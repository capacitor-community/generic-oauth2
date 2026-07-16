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

```ts
GenericOAuth2.authenticate({
  authorizationEndpoint: 'https://www.facebook.com/v25.0/dialog/oauth',
  clientId: 'example',
  redirectUri: 'https://example.com/facebook-redirect',
  scope: 'openid email',
  tokenEndpoint: 'https://graph.facebook.com/v25.0/oauth/access_token',
  callbackScheme: 'fbexample',
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

```ts
GenericOAuth2.authenticate({
  authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
  clientId: 'example.apps.googleusercontent.com',
  redirectUri: 'com.googleusercontent.apps.example:/',
  scope: 'openid email profile',
  tokenEndpoint: 'https://oauth2.googleapis.com/token',
  callbackScheme: 'com.googleusercontent.apps.example',
  // `callbackHttps` cannot be used here as Google does not support it for the iOS client: https://issuetracker.google.com/issues/238577008, https://issuetracker.google.com/issues/531782504
  responseType: 'code',
  responseMode: 'query',
  codeChallengeMethod: 'S256',
});
```

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

| Prop                        | Type                                         |
| --------------------------- | -------------------------------------------- |
| **`authorizationEndpoint`** | <code>string</code>                          |
| **`clientId`**              | <code>string</code>                          |
| **`redirectUri`**           | <code>string</code>                          |
| **`scope`**                 | <code>string</code>                          |
| **`tokenEndpoint`**         | <code>string</code>                          |
| **`callbackScheme`**        | <code>string</code>                          |
| **`callbackHttps`**         | <code>{ host: string; path: string; }</code> |
| **`responseType`**          | <code>string</code>                          |
| **`responseMode`**          | <code>string</code>                          |
| **`codeChallengeMethod`**   | <code>'S256'</code>                          |

</docgen-api>
