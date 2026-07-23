export interface OAuth2AuthenticateOptions {
  authorizationEndpoint: string;
  clientId: string;
  redirectUri: string;
  scope: string;
  tokenEndpoint: string;
  /**
   * URL scheme to watch for the OAuth callback.
   * The flow finishes when the browser is redirected to this scheme.
   * Read more about this in the ["Callback handling"](#callback-handling) section of the docs.
   *
   * @example yourapp
   */
  callbackScheme?: string;
  /**
   * Combination of host+path to watch for the OAuth callback.
   * The flow finishes when the browser is redirected to this host+path combination.
   * Read more about this in the ["Callback handling"](#callback-handling) section of the docs.
   *
   * Three important notes:
   *
   * 1. `callbackHttps` is preferred over `callbackScheme` for improved security, DX and UX.
   *
   * 2. `callbackHttps` requires the `host` to be associated correctly with your app using [Digital Asset Links](https://developer.android.com/studio/write/app-link-indexing) on Android and [Associated Domains](https://developer.apple.com/documentation/xcode/supporting-associated-domains) on iOS.
   *
   * 3. If used without a `callbackScheme` fallback, authentication is rejected on iOS/macOS versions that don't support it.
   *
   * @example { host: 'example.com', path: '/callback' }
   */
  callbackHttps?: {
    host: string;
    path: string;
  };
  responseType: string;
  /**
   * Optional. When omitted, the parameter is left off the authorization URL entirely and thus the provider's default applies.
   */
  responseMode?: string;
  codeChallengeMethod: 'S256';
  /**
   * Ask the browser to use an ephemeral session that doesn't share cookies/website data between the authentication session and the user's normal browser session.
   * This is best-effort: browsers that don't support ephemeral browsing ignore the hint.
   * Consequently, this forces a fresh login.
   * On iOS this also suppresses the "AppName wants to use example.com to sign in" prompt.
   *
   * @default false
   */
  preferEphemeralBrowsing?: boolean;
}

export interface OAuth2AuthenticateResult {
  idToken: string;
  rawNonce: string;
}

export interface GenericOAuth2Plugin {
  authenticate(options: OAuth2AuthenticateOptions): Promise<OAuth2AuthenticateResult>;
}
