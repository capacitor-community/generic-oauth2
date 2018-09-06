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
        let loopCount = this.loopCount;
        // open window
        this.windowHandle = window.open(this.getAuthorizationUrl(options), "auth", "width=500,height=600,left=0,top=0");
        // wait for redirect and resolve the
        this.intervalId = setInterval(() => {
            if (loopCount-- < 0) {
                clearInterval(this.intervalId);
                // this.emitAuthStatus(false);
                this.windowHandle.close();
            } else {
                var href: string;
                try {
                    href = this.windowHandle.location.href;
                } catch (ignore) {
                    //console.log('Error:', e);
                }

                if (href != null) {
                    let re = /access_token=(.*)/;
                    let accessTokenFound = href.match(re);
                    if (accessTokenFound) {
                        clearInterval(this.intervalId);
                        let parsed = this.getUrlParams(href.substr(options.redirectUrl.length + 1));

                        this.token = parsed.access_token;
                        if (this.token) {
                            this.authenticated = true;
                            let expiresSeconds = +parsed.expires_in || 1800;
                            this.startExpiresTimer(expiresSeconds);
                            this.expires = new Date();
                            this.expires = this.expires.setSeconds(this.expires.getSeconds() + expiresSeconds);
                            this.windowHandle.close();
                            // this.emitAuthStatus(true);
                            this.fetchUserInfo();
                        } else {
                            this.authenticated = false; // we got the login callback just fine, but there was no token
                            // this.emitAuthStatus(false); // so we are still going to fail the login
                        }

                    } else {
                        // http://localhost:3000/auth/callback#error=access_denied
                        if (href.indexOf(options.redirectUrl) == 0) {
                            clearInterval(this.intervalId);
                            // parsed = this.getUrlParams(href.substr(options.redirectUrl.length + 1));
                            this.windowHandle.close();
                            // this.emitAuthStatusError(false, parsed);
                        }
                    }
                }
            }
        }, this.intervalLength);

        return Promise.reject();
    }

    private getAuthorizationUrl(options: OAuth2AuthenticateOptions): string {
        let baseUrl = options.authorizationBaseUrl + "?response_type=token&client_id="+options.appId;
        if (options.redirectUrl) {
            baseUrl += "&redirect_url="+options.redirectUrl;
        }
        if (options.scope) {
            baseUrl += "&scope="+options.scope;
        }
        if (options.state) {
            baseUrl += "&state="+options.state;
        }
        return baseUrl;
    }

    private getUrlParams(search: string): any {
        const hashes = search.slice(search.indexOf(`?`) + 1).split(`&`);
        return hashes.reduce((acc, hash) => {
            const [key, val] = hash.split(`=`);
            return {
                ...acc,
                key: decodeURIComponent(val)
            }
        }, {})
    }

    private startExpiresTimer(seconds: number) {
        if (this.expiresTimerId != null) {
            clearTimeout(this.expiresTimerId);
        }
        this.expiresTimerId = setTimeout(() => {
            console.log('Session has expired');
            this.doLogout();
        }, seconds * 1000); // seconds * 1000
        console.log('Token expiration timer set for', seconds, "seconds");
    }

    public doLogout() {
        this.authenticated = false;
        this.expiresTimerId = null;
        this.expires = 0;
        this.token = null;
        // this.emitAuthStatus(true);
        console.log('Session has been cleared');
    }

    private fetchUserInfo() {
        if (this.token != null) {
            let headers = new Headers();
            headers.append('Authorization', `Bearer ${this.token}`);
            //noinspection TypeScriptUnresolvedFunction
            // this.http.get(this.oAuthUserUrl, {headers: headers})
            //     .subscribe(info => {
            //         this.userInfo = info;
            //     }, err => {
            //         console.error("Failed to fetch user info:", err);
            //     });
        }
    }
}

const OAuth2Client = new OAuth2ClientPluginWeb();

export { OAuth2Client };
