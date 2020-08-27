import { registerWebPlugin, WebPlugin } from '@capacitor/core';
import { OAuth2AuthenticateOptions, OAuth2ClientPlugin, OAuth2RefreshTokenOptions } from "./definitions";
import { WebOptions, WebUtils } from "./web-utils";

export class OAuth2ClientPluginWeb extends WebPlugin implements OAuth2ClientPlugin {

    private webOptions: WebOptions;
    private windowHandle: Window = null;
    private intervalId: number = null;
    private loopCount = 2000;
    private intervalLength = 100;
    private windowClosedByPlugin: boolean;

    constructor() {
        super({
            name: 'OAuth2Client',
            platforms: ['web']
        });
    }

    /**
     * Get a new access token using an existing refresh token.
     */
    async refreshToken(options: OAuth2RefreshTokenOptions): Promise<any> {
        return new Promise<any>((resolve, reject) => {
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
                this.windowHandle = window.open(
                    WebUtils.getAuthorizationUrl(this.webOptions),
                    this.webOptions.windowTarget,
                    this.webOptions.windowOptions,
                    true);
                // wait for redirect and resolve the
                this.intervalId = window.setInterval(() => {
                    if (loopCount-- < 0) {
                        this.closeWindow();
                    } else if (this.windowHandle.closed && !this.windowClosedByPlugin) {
                        window.clearInterval(this.intervalId);
                        reject(new Error("USER_CANCELLED"));
                    } else {
                        let href: string;
                        try {
                            href = this.windowHandle.location.href;
                        } catch (ignore) {
                            // ignore DOMException: Blocked a frame with origin "http://localhost:4200" from accessing a cross-origin frame.
                        }

                        if (href != null && href.indexOf(this.webOptions.redirectUrl) >= 0) {
                            let urlParamObj = WebUtils.getUrlParams(href);
                            if (urlParamObj) {
                                window.clearInterval(this.intervalId);
                                // check state
                                if (urlParamObj.state === this.webOptions.state) {
                                    if (this.webOptions.accessTokenEndpoint) {
                                        const self = this;
                                        let authorizationCode = urlParamObj.code;
                                        if (authorizationCode) {
                                            const tokenRequest = new XMLHttpRequest();
                                            tokenRequest.onload = function () {
                                                if (this.status === 200) {
                                                    let accessTokenResponse = JSON.parse(this.response);
                                                    self.requestResource(accessTokenResponse, resolve, reject);
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
                                        this.requestResource(urlParamObj, resolve, reject);
                                    }
                                } else {
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

    private requestResource(tokenObj: any, resolve: any, reject: (reason?: any) => void) {
        if (this.webOptions.resourceUrl) {
            if (tokenObj.access_token) {
                const self = this;
                const request = new XMLHttpRequest();
                request.onload = function () {
                    if (this.status === 200) {
                        let resp = JSON.parse(this.response);
                        if (resp) {
                            resp["access_token"] = tokenObj.access_token;
                        }
                        resolve(resp);
                    } else {
                        reject(new Error(this.statusText));
                    }
                    self.closeWindow();
                };
                request.onerror = function () {
                    console.log("ERR_GENERAL: " + this.statusText);
                    reject(new Error("ERR_GENERAL"));
                    self.closeWindow();
                };
                request.open("GET", this.webOptions.resourceUrl, true);
                request.setRequestHeader('Authorization', `Bearer ${tokenObj.access_token}`);
                request.send();
            } else {
                reject(new Error("ERR_NO_ACCESS_TOKEN"));
                this.closeWindow();
            }
        } else {
            // if no resource url exists just return the accessToken response
            resolve(tokenObj);
            this.closeWindow();
        }
    }

    async logout(options: OAuth2AuthenticateOptions): Promise<void> {
        return new Promise<any>((resolve, reject) => {
            localStorage.removeItem(WebUtils.getAppId(options));
            resolve();
        });
    }

    private closeWindow() {
        window.clearInterval(this.intervalId);
        this.windowHandle.close();
        this.windowClosedByPlugin = true;
    }
}

const OAuth2Client = new OAuth2ClientPluginWeb();

export { OAuth2Client };

registerWebPlugin(OAuth2Client);
