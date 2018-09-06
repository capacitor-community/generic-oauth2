declare global {
    interface PluginRegistry {
        OAuth2ClientPlugin?: OAuth2ClientPlugin;
    }
}

export interface OAuth2ClientPlugin {

    /**
     * Authenicate against a OAuth 2 server.
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
     * Your api secret
     */
    apiSecret?: string;
    /**
     * Callback url to which the provider redirects
     */
    callbackUrl: string;
    /**
     *
     */
    scope?: string;
    /**
     *
     */
    state?: string;
    /**
     *
     */
    responseType: string;
    /**
     *
     */
    userAgent: string;
    /**
     *
     */
    authorizationBaseUrl: string;
    accessTokenEndpoint: string,
}

export interface OAuth2AuthenticateResult {

}
