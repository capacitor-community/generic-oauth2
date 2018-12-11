import {WebPlugin} from '@capacitor/core';
import {OAuth2AuthenticateOptions, OAuth2ClientPlugin} from "./definitions";

export class OAuth2ClientPluginWeb extends WebPlugin implements OAuth2ClientPlugin {

    private windowHandle: Window = null;
    private intervalId: number = null;
    private loopCount = 600;
    private intervalLength = 100;

    constructor() {
        super({
            name: 'OAuth2Client',
            platforms: ['web']
        });
    }

    async authenticate(options: OAuth2AuthenticateOptions): Promise<any> {
        return new Promise<any>((resolve, reject) => {
            if (!options.web || !options.web.redirectUrl) {
                reject(new Error("Required 'web.redirectUrl' not found!"))
            } else {
                let loopCount = this.loopCount;
                // open window

                let winOptions = null;
                if (options.web) {
                    winOptions = options.web.windowOptions;
                }
                this.windowHandle = window.open(this.getAuthorizationUrl(options), "_blank", winOptions);
                // wait for redirect and resolve the
                this.intervalId = setInterval(() => {
                    if (loopCount-- < 0) {
                        clearInterval(this.intervalId);
                        this.windowHandle.close();
                    } else {
                        let href: string;
                        try {
                            href = this.windowHandle.location.href;
                        } catch (ignore) {}

                        if (href != null) {
                            let urlParamObj = this.getUrlParams(href.substr(options.web.redirectUrl.length + 1));
                            if (options.authorizationCodeOnly && options.responseType === "code") {
                                let re = /code=(.*)/;
                                let authorizationCodeFound = href.match(re);
                                clearInterval(this.intervalId);
                                this.windowHandle.close();
                                if (authorizationCodeFound) {
                                    let authorizationCode = urlParamObj.code;
                                    if (authorizationCode) {
                                        let resp = {
                                            authorization_code: authorizationCode,
                                        };
                                        resolve(resp);
                                    } else {
                                        // this.authenticated = false; // we got the login callback just fine, but there was no token
                                        reject(new Error("No authorization code found!"));
                                    }
                                } else {
                                    if (href.indexOf(options.web.redirectUrl) === 0) {
                                        reject(new Error("No authorization code found!"));
                                    }
                                }
                            } else {
                                let re = /access_token=(.*)/;
                                let accessTokenFound = href.match(re);
                                if (accessTokenFound) {
                                    clearInterval(this.intervalId);
                                    this.windowHandle.close();
                                    let accessToken = urlParamObj.access_token;
                                    if (accessToken) {
                                        if (options.resourceUrl) {
                                            const request = new XMLHttpRequest();
                                            request.onload = function () {
                                                if (this.status === 200) {
                                                    let resp = JSON.parse(this.response);
                                                    if (resp) {
                                                        resp["access_token"] = accessToken;
                                                        resp["expires_in"] = urlParamObj.expires_in;
                                                    }
                                                    resolve(resp);
                                                } else {
                                                    reject(new Error(this.statusText));
                                                }
                                            };
                                            request.onerror = function () {
                                                reject(new Error('XMLHttpRequest Error: ' + this.statusText));
                                            };
                                            request.open("GET", options.resourceUrl, true);
                                            request.setRequestHeader('Authorization', `Bearer ${accessToken}`);
                                            request.send();
                                        } else {
                                            let resp = {
                                                access_token: accessToken,
                                                refresh_token: urlParamObj.refresh_token,
                                                expires_in: urlParamObj.expires_in
                                            };
                                            resolve(resp);
                                        }
                                    } else {
                                        // this.authenticated = false; // we got the login callback just fine, but there was no token
                                        reject(new Error("No access token! Authentication failed!"));
                                    }
                                } else {
                                    if (href.indexOf(options.web.redirectUrl) === 0) {
                                        clearInterval(this.intervalId);
                                        this.windowHandle.close();
                                        reject(new Error("Access token not found in redirect url!"));
                                    }
                                }
                            }
                        }
                    }
                }, this.intervalLength);
            }
        });
    }

    async logout(options: OAuth2AuthenticateOptions): Promise<void> {
        return new Promise<any>((resolve, reject) => {
            resolve();
        });
    }

    private getAuthorizationUrl(options: OAuth2AuthenticateOptions): string {
        let appId = options.appId;
        if (options.web && options.web.appId && options.web.appId.length > 0) {
            appId = options.web.appId;
        }

        let baseUrl = options.authorizationBaseUrl + "?client_id=" + appId;
        let responseType = "token";
        if (options.responseType === "code" && options.authorizationCodeOnly) {
            // console.log("@byteowls/capacitor-oauth2: Code flow + PKCE is not yet supported. See github #4")
            responseType = options.responseType;
        }
        baseUrl += "&response_type=" + responseType;

        if (options.web.redirectUrl) {
            baseUrl += "&redirect_uri=" + options.web.redirectUrl;
        }
        if (options.scope) {
            baseUrl += "&scope=" + options.scope;
        }
        if (options.state) {
            baseUrl += "&state=" + options.state;
        }
        return encodeURI(baseUrl);
    }

    private getUrlParams(search: string): any {
        let idx = search.indexOf("#");
        if (idx == -1) {
            idx = search.indexOf("?");
        }

        const hashes = search.slice(idx + 1).split(`&`);
        return hashes.reduce((acc, hash) => {
            const [key, val] = hash.split(`=`);
            return {
                ...acc,
                [key]: decodeURIComponent(val)
            }
        }, {});
    }
}

const OAuth2Client = new OAuth2ClientPluginWeb();

export { OAuth2Client };
