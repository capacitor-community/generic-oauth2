import { WebPlugin } from '@capacitor/core';
import type { OAuth2AuthenticateOptions, GenericOAuth2Plugin, OAuth2RefreshTokenOptions, ImplicitFlowRedirectOptions } from './definitions';
export declare class GenericOAuth2Web extends WebPlugin implements GenericOAuth2Plugin {
    private webOptions;
    private windowHandle;
    private intervalId;
    private loopCount;
    private intervalLength;
    private windowClosedByPlugin;
    /**
     * Get a new access token using an existing refresh token.
     */
    refreshToken(_options: OAuth2RefreshTokenOptions): Promise<any>;
    redirectFlowCodeListener(options: ImplicitFlowRedirectOptions): Promise<any>;
    authenticate(options: OAuth2AuthenticateOptions): Promise<any>;
    private readonly MSG_RETURNED_TO_JS;
    private getAccessToken;
    private requestResource;
    assignResponses(resp: any, accessToken: string, authorizationResponse: any, accessTokenResponse?: any): void;
    logout(options: OAuth2AuthenticateOptions): Promise<boolean>;
    private closeWindow;
    private doLog;
}
