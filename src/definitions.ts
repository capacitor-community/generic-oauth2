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
    /**
     * Logout from the authenticated OAuth 2 provider
     * @param {OAuth2AuthenticateOptions} options Although not all options are needed. We simply reuse the options from authenticate
     * @returns {Promise<boolean>} true if the logout was successful else false.
     */
    logout(options: OAuth2AuthenticateOptions): Promise<void>;
}

type ResponseTypeType = "token" | "code";

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
     * Url for retrieving the access_token by the authorization code flow.
     */
    accessTokenEndpoint?: string;
    /**
     * Protected resource url. For authentification you only need the basic user details.
     */
    resourceUrl?: string;
    /**
     * Defaults to 'token' aka implicit flow if emtpy.
     *
     * Be aware that this plugin does not support authorization code flow with client secrets because of security reason.
     *
     */
    responseType?: ResponseTypeType
    /**
     * PKCE is enabled by default when using @responseType 'code'. This options disables it if needed.
     */
    pkceDisabled?: boolean;

    /**
     * A space-delimited list of permissions that identify the resources that your application could access on the user's behalf.
     */
    scope?: string;
    /**
     * A unique alpha numeric string used to prevent CSRF. If not set the plugin automatically generate a string
     * and sends it as using state is recommended.
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
         * Parameter for overwriting the root or default responseType.
         */
        responseType?: ResponseTypeType
        /**
         * Parameter for overwriting the root or default option.
         */
        pkceDisabled?: boolean;
        /**
         * Url to  which the oauth provider redirects after authentication
         */
        redirectUrl: string;
        /**
         * Options for the window the plugin open for authentication. e.g. width=500,height=600,left=0,top=0
         */
        windowOptions?: string;
        /**
         * Options for the window target. defaults to _blank
         */
        windowTarget?: string;
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
         * Parameter for overwriting the root or default responseType.
         */
        responseType?: ResponseTypeType
        /**
         * Parameter for overwriting the root or default option.
         */
        pkceDisabled?: boolean;
        /**
         * Use your app's custom scheme here. e.g. com.companyname.appname:/
         */
        customScheme?: string;
        /**
         * Some oauth provider especially Facebook forces us to use their SDK for apps.
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
         * Parameter for overwriting the root or default responseType.
         */
        responseType?: ResponseTypeType
        /**
         * Parameter for overwriting the root or default option.
         */
        pkceDisabled?: boolean;
        /**
         * Use your app's custom scheme here. e.g. com.companyname.appname:/
         */
        customScheme?: string;
        /**
         * Some oauth provider especially Facebook forces us to use their SDK for apps.
         *
         * Provide a class name implementing the 'ByteowlsCapacitorOauth2.OAuth2CustomHandler' protocol.
         */
        customHandlerClass?: string;
    }
}
