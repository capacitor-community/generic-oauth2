export interface GenericOAuth2Plugin {
    /**
     * Authenticate against a OAuth 2 provider.
     * @param {OAuth2AuthenticateOptions} options
     * @returns {Promise<any>} the resource url response
     */
    authenticate(options: OAuth2AuthenticateOptions): Promise<any>;
    /**
     * Listens for OAuth implicit redirect flow queryString CODE to generate an access_token
     * @param {OAuth2RedirectAuthenticationOptions} options
     * @returns {Promise<any>} the token endpoint response
     */
    redirectFlowCodeListener(options: ImplicitFlowRedirectOptions): Promise<any>;
    /**
     * Get a new access token based on the given refresh token.
     * @param {OAuth2RefreshTokenOptions} options
     * @returns {Promise<any>} the token endpoint response
     */
    refreshToken(options: OAuth2RefreshTokenOptions): Promise<any>;
    /**
     * Logout from the authenticated OAuth 2 provider
     * @param {OAuth2AuthenticateOptions} options Although not all options are needed. We simply reuse the options from authenticate
     * @param {String} id_token Optional idToken, only for Android
     * @returns {Promise<boolean>} true if the logout was successful else false.
     */
    logout(options: OAuth2AuthenticateOptions, id_token?: string): Promise<boolean>;
}
export interface ImplicitFlowRedirectOptions extends OAuth2AuthenticateOptions {
    /**
     * The URL where we get the code
     */
    response_url: string;
}
export interface OAuth2RefreshTokenOptions {
    /**
     * The app id (client id) you get from the oauth provider like Google, Facebook,...
     */
    appId: string;
    /**
     * Url for retrieving the access_token.
     */
    accessTokenEndpoint: string;
    /**
     * The refresh token that will be used to obtain the new access token.
     */
    refreshToken: string;
    /**
     * A space-delimited list of permissions that identify the resources that your application could access on the user's behalf.
     */
    scope?: string;
}
export interface OAuth2AuthenticateBaseOptions {
    /**
     * The app id (client id) you get from the oauth provider like Google, Facebook,...
     *
     * required!
     */
    appId?: string;
    /**
     * The base url for retrieving tokens depending on the response type from a OAuth 2 provider. e.g. https://accounts.google.com/o/oauth2/auth
     *
     * required!
     */
    authorizationBaseUrl?: string;
    /**
     * Tells the authorization server which grant to execute. Be aware that a full code flow is not supported as clientCredentials are not included in requests.
     *
     * But you can retrieve the authorizationCode if you don't set a accessTokenEndpoint.
     *
     * required!
     */
    responseType?: string;
    /**
     * Url to  which the oauth provider redirects after authentication.
     *
     * required!
     */
    redirectUrl?: string;
    /**
     * Url for retrieving the access_token by the authorization code flow.
     */
    accessTokenEndpoint?: string;
    /**
     * Protected resource url. For authentication you only need the basic user details.
     */
    resourceUrl?: string;
    /**
     * Enable PKCE if you need it.
     */
    pkceEnabled?: boolean;
    /**
     * A space-delimited list of permissions that identify the resources that your application could access on the user's behalf.
     * If you want to get a refresh token, you most likely will need the offline_access scope (only supported in Code Flow!)
     */
    scope?: string;
    /**
     * A unique alpha numeric string used to prevent CSRF. If not set the plugin automatically generate a string
     * and sends it as using state is recommended.
     */
    state?: string;
    /**
     * Additional parameters for the created authorization url
     */
    additionalParameters?: {
        [key: string]: string;
    };
    /**
     * @since 3.0.0
     */
    logsEnabled?: boolean;
    /**
     * @since 3.1.0 ... not implemented yet!
     */
    logoutUrl?: string;
    /**
     * Additional headers for resource url request
     * @since 3.0.0
     */
    additionalResourceHeaders?: {
        [key: string]: string;
    };
}
export interface OAuth2AuthenticateOptions extends OAuth2AuthenticateBaseOptions {
    /**
     * Custom options for the platform "web"
     */
    web?: WebOption;
    /**
     * Custom options for the platform "android"
     */
    android?: AndroidOptions;
    /**
     * Custom options for the platform "ios"
     */
    ios?: IosOptions;
}
export interface WebOption extends OAuth2AuthenticateBaseOptions {
    /**
     * Options for the window the plugin open for authentication. e.g. width=500,height=600,left=0,top=0
     */
    windowOptions?: string;
    /**
     * Options for the window target. Defaults to _blank
     */
    windowTarget?: string;
    /**
     * Whether to send the cache control header with the token request, unsupported by some providers. Defaults to true.
     */
    sendCacheControlHeader?: boolean;
}
export interface AndroidOptions extends OAuth2AuthenticateBaseOptions {
    /**
     * Some oauth provider especially Facebook forces us to use their SDK for apps.
     *
     * Provide a class name implementing the 'CapacitorCommunityGenericOAuth2.OAuth2CustomHandler' protocol.
     */
    customHandlerClass?: string;
    /**
     * Alternative to handle the activity result. The `onNewIntent` method is only call if the App was killed while logging in.
     */
    handleResultOnNewIntent?: boolean;
    /**
     * Default handling the activity result.
     */
    handleResultOnActivityResult?: boolean;
}
export interface IosOptions extends OAuth2AuthenticateBaseOptions {
    /**
     * If true the iOS 13+ feature Sign in with Apple (SiWA) try to build the scope from the standard "scope" parameter.
     *
     * If false scope is set to email and fullName.
     */
    siwaUseScope?: boolean;
    /**
     * Some oauth provider especially Facebook forces us to use their SDK for apps.
     *
     * Provide a class name implementing the 'CapacitorCommunityGenericOAuth2.OAuth2CustomHandler' protocol.
     */
    customHandlerClass?: string;
}
