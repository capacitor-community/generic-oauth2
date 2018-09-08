declare global {
    interface PluginRegistry {
        OAuth2ClientPlugin?: OAuth2ClientPlugin;
    }
}

export interface OAuth2ClientPlugin {

    /**
     * Authenicate against a OAuth 2 provider.
     * @param {OAuth2AuthenticateOptions} options
     * @returns {Promise<any>} the resource url response
     */
    authenticate(options: OAuth2AuthenticateOptions): Promise<any>;
}

export interface OAuth2AuthenticateOptions {
    /**
     * The app id (client id) you get from the oauth provider like Google
     */
    appId: string;
    /**
     * The base url for retrieving the access_token from a OAuth 2 provider. e.g. https://accounts.google.com/o/oauth2/auth
     */
    authorizationBaseUrl: string;

    /**
     * Url for retrieving the access_token by the code. TODO maybe not needed
     */
    accessTokenEndpoint?: string;
    /**
     * Protected resource url. For authentification you only need the basic user details.
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
    web?: {
        /**
         * Url to  which the oauth provider redirects after authentication
         */
        redirectUrl: string;
        /**
         * Options for the window the plugin open for authentication. e.g. width=500,height=600,left=0,top=0
         */
        windowOptions?: string;
    },
    android?: {
        /**
         * Use your app's custom scheme here. e.g. com.byteowls.teamconductor:/
         */
        customScheme: string;
    }
}
