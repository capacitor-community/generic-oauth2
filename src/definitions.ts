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
     * protected resource url. normally the user detail url
     */
    resourceUrl: string;
    /**
     * Url or customScheme to which the provider redirects after login
     */
    redirectUrl: string;
    /**
     * default to false == GET
     */
    resourcePostRequest?: boolean;
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
    name: string;

}
