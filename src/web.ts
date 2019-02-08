import {WebPlugin} from '@capacitor/core';
import {OAuth2AuthenticateOptions, OAuth2ClientPlugin} from "./definitions";
import {WebUtils} from "./web-utils";

export class OAuth2ClientPluginWeb extends WebPlugin implements OAuth2ClientPlugin {

    private windowHandle: Window = null;
    private intervalId: number = null;
    private loopCount = 1000;
    private intervalLength = 100;

    constructor() {
        super({
            name: 'OAuth2Client',
            platforms: ['web']
        });
    }

    async authenticate(options: OAuth2AuthenticateOptions): Promise<any> {
        return new Promise<any>((resolve, reject) => {
            // validate
            if (!WebUtils.getAppId(options)) {
                reject(new Error("Required 'appId' or 'web.appId' not found!"))
            } else if (!options.authorizationBaseUrl) {
                reject(new Error("Required 'authorizationBaseUrl' not found!"))
            } else if (!options.web || !options.web.redirectUrl) {
                reject(new Error("Required 'web.redirectUrl' not found!"))
            } else {

                if (options.responseType === "token") {
                    // check if access token is stored
                    // TODO get access token and expires in
                    // const storageItemName = options.authorizationBaseUrl
                    // const storedAccessTokenObj: {accessToken: string, expiresAt: Date} = localStorage.getItem(this.getAppId(options))
                    // if (storedAccessTokenObj) {
                    //     if (storedAccessTokenObj.expiresAt && storedAccessTokenObj.expiresAt < new Date()) {
                    //         this.requestResource(options, storedAccessTokenObj.accessToken, storedAccessTokenObj.expiresAt, resolve, reject);
                    //     }
                    // }
                }

                let loopCount = this.loopCount;
                // open window

                let winOptions = null;
                if (options.web) {
                    winOptions = options.web.windowOptions;
                }
                this.windowHandle = window.open(WebUtils.getAuthorizationUrl(options), "_blank", winOptions);
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
                                if (urlParamObj.state === options.state) {
                                    // implicit flow
                                    if (options.responseType === "token") {
                                        let accessToken = urlParamObj.access_token;
                                        if (accessToken) {
                                            const expiresIn = urlParamObj.expires_in;
                                            // TODO store access token and expires in
                                            this.requestResource(options, accessToken, expiresIn, resolve, reject);
                                        } else {
                                            reject(new Error("No access token! Authentication failed!"));
                                            this.closeWindow();
                                        }
                                    } else if (options.responseType === "code") {
                                        // code flow
                                        let authorizationCode = urlParamObj.code;
                                        if (authorizationCode) {
                                            // TODO PKCE
                                        } else {
                                            reject(new Error("No authorization code found!"));
                                        }
                                        this.closeWindow();
                                    } else {
                                        reject(new Error("Not supported responseType"));
                                        this.closeWindow();
                                    }
                                } else {
                                    reject(new Error("State check not passed! Retrieved state does not match sent one!"));
                                    this.closeWindow();
                                }
                            }
                        }
                    }
                }, this.intervalLength);
            }
        });
    }

    private requestResource(options: OAuth2AuthenticateOptions, accessToken: string, expiresIn: Date, resolve: any, reject: (reason?: any) => void) {
        if (options.resourceUrl) {
            const self = this;
            const request = new XMLHttpRequest();
            request.onload = function () {
                if (this.status === 200) {
                    let resp = JSON.parse(this.response);
                    if (resp) {
                        resp["access_token"] = accessToken;
                        resp["expires_in"] = expiresIn;
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
            request.open("GET", options.resourceUrl, true);
            request.setRequestHeader('Authorization', `Bearer ${accessToken}`);
            request.send();
        } else {
            // there is no refresh token allowed in implicit flow
            let resp = {
                access_token: accessToken,
                expires_in: expiresIn
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
