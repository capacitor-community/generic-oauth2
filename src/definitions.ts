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
     * Url for retrieving the access_token by the authorization code.
     */
    accessTokenEndpoint?: string;
    /**
     * Protected resource url. For authentification you only need the basic user details.
     */
    resourceUrl?: string;
    /**
     * Defaults to 'token' if null or undefined. Be aware that this plugin does not support
     * code flow with client secret because of security reason. However code + PKCE will be supported
     * in future. Please see github issue #4
     */
    responseType?: ResponseTypeType
    /**
     * A space-delimited list of permissions that identify the resources that your application could access on the user's behalf.
     */
    scope?: string;
    /**
     * If this is null the plugin uses a own random string to make sure the red
     */
    state?: string;
    /**
     * In case you do no want that the plugins adds a state.
     */
    stateDisabled?: boolean;
    /**
     * Force the lib to only return the authorization code in the result. This becomes handy if you want to use it as part
     * of a server side authorization code flow.
     */
    authorizationCodeOnly?: boolean;
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
         * Parameter for overwriting the root or default responseType.
         */
        responseType?: ResponseTypeType
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
