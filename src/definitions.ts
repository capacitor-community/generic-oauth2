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
     * The app id (client id) you get from the oauth provider like facebook
     */
    appId: string;
    /**
     *
     */
    authorizationBaseUrl: string;
    /**
     * not needed because response type is always token
     */
    accessTokenEndpoint: string,
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
}

export interface OAuth2AuthenticateResult {

    id: string;
    data: any;

}
