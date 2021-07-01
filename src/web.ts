import {WebPlugin} from '@capacitor/core';
import type {OAuth2AuthenticateOptions, OAuth2ClientPlugin, OAuth2RefreshTokenOptions} from "./definitions";
import {WebOptions, WebUtils} from "./web-utils";

export class OAuth2ClientPluginWeb extends WebPlugin implements OAuth2ClientPlugin {

    private webOptions: WebOptions;
    private windowHandle: Window | null;
    private intervalId: number;
    private loopCount = 2000;
    private intervalLength = 100;
    private windowClosedByPlugin: boolean;

    /**
     * Get a new access token using an existing refresh token.
     */
    async refreshToken(_options: OAuth2RefreshTokenOptions): Promise<any> {
        return new Promise<any>((_resolve, reject) => {
            reject(new Error("Functionality not implemented for PWAs yet"));
        });
    }

    async authenticate(options: OAuth2AuthenticateOptions): Promise<any> {
        this.webOptions = await WebUtils.buildWebOptions(options);
        return new Promise<any>((resolve, reject) => {
            // validate
            if (!this.webOptions.appId || this.webOptions.appId.length == 0) {
                reject(new Error("ERR_PARAM_NO_APP_ID"));
            } else if (!this.webOptions.authorizationBaseUrl || this.webOptions.authorizationBaseUrl.length == 0) {
                reject(new Error("ERR_PARAM_NO_AUTHORIZATION_BASE_URL"));
            } else if (!this.webOptions.redirectUrl || this.webOptions.redirectUrl.length == 0) {
                reject(new Error("ERR_PARAM_NO_REDIRECT_URL"));
            } else if (!this.webOptions.responseType || this.webOptions.responseType.length == 0) {
                reject(new Error("ERR_PARAM_NO_RESPONSE_TYPE"));
            } else {
                // init internal control params
                let loopCount = this.loopCount;
                this.windowClosedByPlugin = false;
                // open window
                const authorizationUrl = WebUtils.getAuthorizationUrl(this.webOptions);
                if (this.webOptions.logsEnabled) {
                    console.log("AuthorizationUrl: " + authorizationUrl);
                }
                this.windowHandle = window.open(
                    authorizationUrl,
                    this.webOptions.windowTarget,
                    this.webOptions.windowOptions,
                    true);
                // wait for redirect and resolve the
                this.intervalId = window.setInterval(() => {
                    if (loopCount-- < 0) {
                        this.closeWindow();
                    } else if (this.windowHandle?.closed && !this.windowClosedByPlugin) {
                        window.clearInterval(this.intervalId);
                        reject(new Error("USER_CANCELLED"));
                    } else {
                        let href: string = undefined!;
                        try {
                            href = this.windowHandle?.location.href!;
                        } catch (ignore) {
                            // ignore DOMException: Blocked a frame with origin "http://localhost:4200" from accessing a cross-origin frame.
                        }

                        if (href != null && href.indexOf(this.webOptions.redirectUrl) >= 0) {
                            if (this.webOptions.logsEnabled) {
                                console.log("Url from Provider: " + href);
                            }
                            let authorizationRedirectUrlParamObj = WebUtils.getUrlParams(href);
                            if (authorizationRedirectUrlParamObj) {
                                if (this.webOptions.logsEnabled) {
                                    console.log("Url Params: ", authorizationRedirectUrlParamObj);
                                }
                                window.clearInterval(this.intervalId);
                                // check state
                                if (authorizationRedirectUrlParamObj.state === this.webOptions.state) {
                                    if (this.webOptions.accessTokenEndpoint) {
                                        const self = this;
                                        let authorizationCode = authorizationRedirectUrlParamObj.code;
                                        if (authorizationCode) {
                                            const tokenRequest = new XMLHttpRequest();
                                            tokenRequest.onload = function () {
                                                if (this.status === 200) {
                                                    let accessTokenResponse = JSON.parse(this.response);
                                                    self.requestResource(accessTokenResponse.access_token, resolve, reject, authorizationRedirectUrlParamObj, accessTokenResponse);
                                                }
                                            };
                                            tokenRequest.onerror = function () {
                                                console.log("ERR_GENERAL: See client logs. It might be CORS. Status text: " + this.statusText);
                                                reject(new Error("ERR_GENERAL"));
                                            };
                                            tokenRequest.open("POST", this.webOptions.accessTokenEndpoint, true);
                                            tokenRequest.setRequestHeader('accept', 'application/json');
                                            tokenRequest.setRequestHeader('cache-control', 'no-cache');
                                            tokenRequest.setRequestHeader('content-type', 'application/x-www-form-urlencoded');
                                            tokenRequest.send(WebUtils.getTokenEndpointData(this.webOptions, authorizationCode));
                                        } else {
                                            reject(new Error("ERR_NO_AUTHORIZATION_CODE"));
                                        }
                                        this.closeWindow();
                                    } else {
                                        // if no accessTokenEndpoint exists request the resource
                                        this.requestResource(authorizationRedirectUrlParamObj.access_token, resolve, reject, authorizationRedirectUrlParamObj);
                                    }
                                } else {
                                    if (this.webOptions.logsEnabled) {
                                        console.log("State from web options: " + this.webOptions.state);
                                        console.log("State returned from provider: " + authorizationRedirectUrlParamObj.state);
                                    }
                                    reject(new Error("ERR_STATES_NOT_MATCH"));
                                    this.closeWindow();
                                }
                            }
                            // this is no error no else clause required
                        }
                    }
                }, this.intervalLength);
            }
        });
    }

    private requestResource(accessToken: string, resolve: any, reject: (reason?: any) => void, authorizationResponse: any, accessTokenResponse: any = null) {
        if (this.webOptions.resourceUrl) {
            if (accessToken) {
                const logsEnabled = this.webOptions.logsEnabled;
                if (logsEnabled) {
                    console.log("Access token: " + accessToken);
                }
                const self = this;
                const request = new XMLHttpRequest();
                request.onload = function () {
                    if (this.status === 200) {
                        let resp = JSON.parse(this.response);
                        if (resp) {
                            // #154
                            if (authorizationResponse) {
                                resp["authorization_response"] = authorizationResponse;
                            }
                            if (accessTokenResponse) {
                                resp["access_token_response"] = authorizationResponse;
                            }
                            resp["access_token"] = accessToken;
                        }
                        if (logsEnabled) {
                            console.log("Resource response: ", resp);
                        }
                        resolve(resp);
                    } else {
                        reject(new Error(this.statusText));
                    }
                    self.closeWindow();
                };
                request.onerror = function () {
                    if (logsEnabled) {
                        console.log("ERR_GENERAL: " + this.statusText);
                    }
                    reject(new Error("ERR_GENERAL"));
                    self.closeWindow();
                };
                if (logsEnabled) {
                    console.log("Resource url: GET " + this.webOptions.resourceUrl);
                }
                request.open("GET", this.webOptions.resourceUrl, true);
                request.setRequestHeader('Authorization', `Bearer ${accessToken}`);
                request.send();
            } else {
                reject(new Error("ERR_NO_ACCESS_TOKEN"));
                this.closeWindow();
            }
        } else {
            // if no resource url exists just return the accessToken response
            resolve(accessToken);
            this.closeWindow();
        }
    }

    async logout(options: OAuth2AuthenticateOptions): Promise<boolean> {
        return new Promise<any>((resolve, _reject) => {
            localStorage.removeItem(WebUtils.getAppId(options));
            resolve(true);
        });
    }

    private closeWindow() {
        window.clearInterval(this.intervalId);
        this.windowHandle?.close();
        this.windowClosedByPlugin = true;
    }
}
