export interface OAuth2AuthenticateOptions {
  authorizationEndpoint: string;
  clientId: string;
  redirectUri: string;
  scope: string;
  tokenEndpoint: string;
  callbackScheme: string;
  callbackHttps?: {
    host: string;
    path: string;
  };
  responseType: string;
  responseMode: string;
  codeChallengeMethod: 'S256';
}

export interface OAuth2AuthenticateResult {
  idToken: string;
  rawNonce: string;
}

export interface GenericOAuth2Plugin {
  authenticate(options: OAuth2AuthenticateOptions): Promise<OAuth2AuthenticateResult>;
}
