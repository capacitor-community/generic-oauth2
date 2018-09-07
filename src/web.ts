import { WebPlugin } from '@capacitor/core';
import {OAuth2AuthenticateOptions, OAuth2AuthenticateResult, OAuth2ClientPlugin} from "./definitions";

export class OAuth2ClientPluginWeb extends WebPlugin implements OAuth2ClientPlugin {

    private authenticated: boolean = false;
    private token: string;
    private expires: any = 0;
    private userInfo: any = {};
    private windowHandle: any = null;
    private intervalId: any = null;
    private expiresTimerId: any = null;
    private loopCount = 600;
    private intervalLength = 100;

    constructor() {
        super({
            name: 'OAuth2Client',
            platforms: ['web']
        });
    }
    async authenticate(options: OAuth2AuthenticateOptions): Promise<OAuth2AuthenticateResult> {
        return new Promise<OAuth2AuthenticateResult>((resolve, reject) => {
            let loopCount = this.loopCount;
            // open window
            let windowOptions = "width=500,height=600,left=0,top=0";
            this.windowHandle = window.open(this.getAuthorizationUrl(options), "capacitor-oauth");
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
                        let urlParamObj = this.getUrlParams(href.substr(options.redirectUrl.length + 1));
                        let re = /access_token=(.*)/;
                        let accessTokenFound = href.match(re);
                        if (accessTokenFound) {
                            clearInterval(this.intervalId);
                            this.windowHandle.close();
                            this.token = urlParamObj.access_token;
                            if (this.token) {
                                this.authenticated = true;
                                let expiresSeconds = +urlParamObj.expires_in || 1800;
                                this.startExpiresTimer(expiresSeconds);
                                this.expires = new Date();
                                this.expires = this.expires.setSeconds(this.expires.getSeconds() + expiresSeconds);

                                const request = new XMLHttpRequest();
                                request.onload = function () {
                                    if (this.status === 200) {
                                        let token = JSON.parse(this.response);
                                        resolve(token);
                                    } else {
                                        reject(new Error(this.statusText));
                                    }
                                };
                                request.onerror = function () {
                                    reject(new Error('XMLHttpRequest Error: ' + this.statusText));
                                };
                                request.open("GET", options.resourceUrl, true);
                                request.setRequestHeader('Authorization', `Bearer ${this.token}`);
                                request.send();
                            } else {
                                this.authenticated = false; // we got the login callback just fine, but there was no token
                                reject(new Error("Authentication failed"));
                            }
                        } else {
                            if (href.indexOf(options.redirectUrl) == 0) {
                                clearInterval(this.intervalId);
                                this.windowHandle.close();
                                reject(new Error("Not found"));
                            }
                        }
                    }
                }
            }, this.intervalLength);
        });
    }

    private getAuthorizationUrl(options: OAuth2AuthenticateOptions): string {
        let baseUrl = options.authorizationBaseUrl + "?response_type=token&client_id="+options.appId;
        if (options.redirectUrl) {
            baseUrl += "&redirect_uri="+options.redirectUrl;
        }
        if (options.scope) {
            baseUrl += "&scope="+options.scope;
        }
        if (options.state) {
            baseUrl += "&state="+options.state;
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

    private startExpiresTimer(seconds: number) {
        if (this.expiresTimerId != null) {
            clearTimeout(this.expiresTimerId);
        }
        this.expiresTimerId = setTimeout(() => {
            this.doLogout();
        }, seconds * 1000); // seconds * 1000
    }

    public doLogout() {
        this.authenticated = false;
        this.expiresTimerId = null;
        this.expires = 0;
        this.token = null;
    }

    fetchUserInfo(options: OAuth2AuthenticateOptions): Promise<OAuth2AuthenticateResult> {
        let tkn = this.token;
        return new Promise<OAuth2AuthenticateResult>(
            function (resolve, reject) {

            });
    }
}

const OAuth2Client = new OAuth2ClientPluginWeb();

export { OAuth2Client };
