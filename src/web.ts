import {WebPlugin} from '@capacitor/core';
import {OAuth2AuthenticateOptions, OAuth2ClientPlugin} from "./definitions";
import {WebOptions, WebUtils} from "./web-utils";

export class OAuth2ClientPluginWeb extends WebPlugin implements OAuth2ClientPlugin {

    private webOptions: WebOptions;
    private windowHandle: Window = null;
    private intervalId: number = null;
    private loopCount = 2000;
    private intervalLength = 100;

    constructor() {
        super({
            name: 'OAuth2Client',
            platforms: ['web']
        });
    }

    async authenticate(options: OAuth2AuthenticateOptions): Promise<any> {
        this.webOptions = WebUtils.buildWebOptions(options);
        return new Promise<any>((resolve, reject) => {
            // validate
            if (!this.webOptions.appId) {
                reject(new Error("ERR_PARAM_APP_ID_REQUIRED"))
            } else if (!this.webOptions.authorizationBaseUrl) {
                reject(new Error("ERR_PARAM_AUTHORIZATION_BASE_URL_REQUIREDR"))
            } else if (!this.webOptions.redirectUrl) {
                reject(new Error("ERR_PARAM_REDIRECT_URL_REQUIRED"))
            } else {
                let loopCount = this.loopCount;
                // open window
                this.windowHandle = window.open(
                    WebUtils.getAuthorizationUrl(this.webOptions),
                    this.webOptions.windowTarget,
                    this.webOptions.windowOptions,
                    true);
                // wait for redirect and resolve the
                this.intervalId = setInterval(() => {
                    if (loopCount-- < 0) {
                        clearInterval(this.intervalId);
                        this.windowHandle.close();
                    } else {
                        let href: string;
                        try {
                            href = this.windowHandle.location.href;
                        } catch (ignore) {
                            // ignore DOMException: Blocked a frame with origin "http://localhost:4200" from accessing a cross-origin frame.
                        }

                        if (href != null) {
                            let urlParamObj = WebUtils.getUrlParams(href);
                            if (urlParamObj) {
                                clearInterval(this.intervalId);
                                // check state
                                if (urlParamObj.state === this.webOptions.state) {
                                    if (this.webOptions.responseType === "token") {
                                        // implicit flow
                                        let accessToken = urlParamObj.access_token;
                                        if (accessToken) {
                                            this.requestResource(accessToken, urlParamObj, resolve, reject);
                                        } else {
                                            reject(new Error("ERR_NO_ACCESS_TOKEN"));
                                            this.closeWindow();
                                        }
                                    } else if (this.webOptions.responseType === "code") {
                                        // code flow
                                        let authorizationCode = urlParamObj.code;
                                        if (authorizationCode) {
                                            // TODO get access token by authorization code

                                            // TODO access resource by access token
                                        } else {
                                            reject(new Error("ERR_NO_AUTHORIZATION_CODE"));
                                        }
                                        this.closeWindow();
                                    } else {
                                        reject(new Error("ERR_INVALID_RESPONSE_TYPE"));
                                        this.closeWindow();
                                    }
                                } else {
                                    reject(new Error("ERR_STATES_NOT_MATCH"));
                                    this.closeWindow();
                                }
                            }
                        }
                    }
                }, this.intervalLength);
            }
        });
    }

    private requestResource(accessToken: string, urlParamObj: any, resolve: any, reject: (reason?: any) => void) {
        if (this.webOptions.resourceUrl) {
            const self = this;
            const request = new XMLHttpRequest();
            request.onload = function () {
                if (this.status === 200) {
                    let resp = JSON.parse(this.response);
                    if (resp) {
                        resp["access_token"] = accessToken;
                    }
                    resolve(resp);
                } else {
                    reject(new Error(this.statusText));
                }
                self.closeWindow();
            };
            request.onerror = function () {
                reject(new Error(this.statusText));
                self.closeWindow();
            };
            request.open("GET", this.webOptions.resourceUrl, true);
            request.setRequestHeader('Authorization', `Bearer ${accessToken}`);
            request.send();
        } else {
            // there is no refresh token allowed in implicit flow
            let resp = {
                access_token: accessToken,
            };
            resolve(resp);
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
        clearInterval(this.intervalId);
        this.windowHandle.close();
    }
}

const OAuth2Client = new OAuth2ClientPluginWeb();

export { OAuth2Client };
