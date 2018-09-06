declare global {
    interface PluginRegistry {
        OAuth2ClientPlugin?: OAuth2ClientPlugin;
    }
}

export interface OAuth2ClientPlugin {

    /**
     * Authenicate against a OAuth 2 provider.
     * @param {OAuth2AuthenticateOptions} options
     * @returns {Promise<OAuth2AuthenticateResult>}
     */
    authenticate(options: OAuth2AuthenticateOptions): Promise<OAuth2AuthenticateResult>;
}

export interface OAuth2AuthenticateOptions {
    /**
     * Your api key
     */
    apiKey: string;
    /**
     *
     */
    authorizationBaseUrl: string;
    /**
     *
     */
    accessTokenEndpoint: string,
    /**
     * Your api secret
     */
    apiSecret?: string;
    /**
     * Url to which the provider redirects after login
     */
    redirectUrl: string;
    /**
     *
     */
    scope?: string;
    /**
     *
     */
    state?: string;
    /**
     * defaults to token
     */
    responseType?: string;
    /**
     *
     */
    userAgent?: string;
}

export interface OAuth2AuthenticateResult {

    id: string;
    data: any;

}
