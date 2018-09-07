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
     *
     */
    scope?: string;
    /**
     *
     */
    state?: string;

    /**
     * Custom options for the platform "web"
     */
    web: {
        /**
         * Options for the window the plugin open for authentication. e.g. width=500,height=600,left=0,top=0
         */
        windowOptions: string;
        /**
         * Url to  which the oauth provider redirects after authentication
         */
        redirectUrl: string;
    }
}

export interface OAuth2AuthenticateResult {

    id: string;
    name: string;

}
