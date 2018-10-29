declare global {
    interface PluginRegistry {
        OAuth2Client?: OAuth2ClientPlugin;
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
     * The app id (client id) you get from the oauth provider like Google, Facebook,...
     */
    appId: string;
    /**
     * The base url for retrieving the access_token from a OAuth 2 provider. e.g. https://accounts.google.com/o/oauth2/auth
     */
    authorizationBaseUrl: string;
    /**
     * Url for retrieving the access_token by the authorization code.
     */
    accessTokenEndpoint: string;
    /**
     * Protected resource url. For authentification you only need the basic user details.
     */
    resourceUrl: string;
    /**
     * Code authorisation flow might require to send the app secret to aquire the access token. Facebook needs this step.
     * Setting your secret on client side is insecure please try to avoid.
     */
    appSecret?: string;
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
         * Parameter for overwriting the root app id.
         * This is useful e.g. Google OAuth because you have to use different client ids for web, android, ios
         */
        appId?: string;
        /**
         * Url to  which the oauth provider redirects after authentication
         */
        redirectUrl: string;
        /**
         * Options for the window the plugin open for authentication. e.g. width=500,height=600,left=0,top=0
         */
        windowOptions?: string;
    },
    /**
     * Custom options for the platform "android"
     */
    android?: {
        /**
         * Parameter for overwriting the root app id.
         * This is useful for Google OAuth because you have different client ids for web, android, ios
         */
        appId?: string;
        /**
         * Use your app's custom scheme here. e.g. com.byteowls.teamconductor:/
         */
        customScheme?: string;
        /**
         * Some oauth provider especially Facebook does not support the standard oauth flow. Therefore a custom implementation
         * using the provider's SDK is needed.
         *
         * Provide a class name implementing the 'com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler' interface.
         */
        customHandlerClass?: string;
    },
    /**
     * Custom options for the platform "ios"
     */
    ios?: {
        /**
         * Parameter for overwriting the root app id.
         * This is useful e.g. Google OAuth because you have to use different client ids for web, android, ios
         */
        appId?: string;
        /**
         * Use your app's custom scheme here. e.g. com.byteowls.teamconductor:/
         */
        customScheme?: string;
    }
}
