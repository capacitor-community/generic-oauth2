import {OAuth2AuthenticateOptions} from "./definitions";
// import sha256 from "fast-sha256";


export class WebUtils {
    /**
     * Public only for testing
     */
    static getAppId(options: OAuth2AuthenticateOptions): string {
        return this.getOverwritableValue(options, "appId");
    }

    static getOverwritableValue<T>(options: OAuth2AuthenticateOptions | any, key: string): T {
        let base = options[key];
        if (options.web && options.web[key]) {
            base = options.web[key];
        }
        return base;
    }

    /**
     * Public only for testing
     */
    static getAuthorizationUrl(options: WebOptions): string {
        let url = options.authorizationBaseUrl + "?client_id=" + options.appId;
        url += "&response_type=" + options.responseType;

        if (options.redirectUrl) {
            url += "&redirect_uri=" + options.redirectUrl;
        }
        if (options.scope) {
            url += "&scope=" + options.scope;
        }
        url += "&state=" + options.state;

        if (options.pkceCodeChallenge) {
            url += "&code_challenge=" + options.pkceCodeChallenge;
            url += "&code_challenge_method=" + options.pkceCodeChallengeMethod;
        }
        return encodeURI(url);
    }

    /**
     * Public only for testing
     */
    static getUrlParams(search: string): any | undefined {
        if (search && search.trim().length > 0) {
            search = search.trim();
            let idx = search.indexOf("#");
            if (idx == -1) {
                idx = search.indexOf("?");
            }
            if (idx != -1 && search.length > (idx + 1)) {
                const urlParamStr = search.slice(idx + 1);
                const keyValuePairs: string[] = urlParamStr.split(`&`);
                return keyValuePairs.reduce((acc, hash) => {
                    const [key, val] = hash.split(`=`);
                    if (key && key.length > 0) {
                        return {
                            ...acc,
                            [key]: decodeURIComponent(val)
                        }
                    }
                }, {});
            }

        }
        return undefined;
    }

    static randomString(length: number = 10) {
        const possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        let text = "";
        for (let i = 0; i < length; i++) {
            text += possible.charAt(Math.floor(Math.random() * possible.length));
        }

        return text;
    }

    static buildWebOptions(configOptions: OAuth2AuthenticateOptions): WebOptions {
        const webOptions = new WebOptions();
        webOptions.appId = this.getAppId(configOptions);
        webOptions.responseType = this.getOverwritableValue(configOptions, "responseType");
        webOptions.pkceDisabled = this.getOverwritableValue(configOptions, "pkceDisabled");
        if (!webOptions.responseType) {
            webOptions.responseType = "token";
        }

        if (webOptions.responseType == "code") {
            if (!webOptions.pkceDisabled) {
                webOptions.pkceCodeVerifier = this.randomString(64);
                // webOptions.pkceCodeChallenge = sha256(webOptions.pkceCodeVerifier);
                webOptions.pkceCodeChallenge = webOptions.pkceCodeVerifier;
                webOptions.pkceCodeChallengeMethod = "plain";
            }
        }

        webOptions.authorizationBaseUrl = configOptions.authorizationBaseUrl;
        webOptions.accessTokenEndpoint = configOptions.accessTokenEndpoint;
        webOptions.resourceUrl = configOptions.resourceUrl;
        webOptions.scope = configOptions.scope;
        webOptions.state = configOptions.state;
        if (!webOptions.state || webOptions.state.length === 0) {
            webOptions.state = this.randomString(20);
        }
        webOptions.redirectUrl = configOptions.web.redirectUrl;
        if (configOptions.web) {
            if (configOptions.web.windowOptions) {
                webOptions.windowOptions = configOptions.web.windowOptions;
            }
            if (configOptions.web.windowTarget) {
                webOptions.windowTarget = configOptions.web.windowTarget;
            }
        }
        return webOptions;

    }

}

export class WebOptions {
    appId: string;
    authorizationBaseUrl: string;
    accessTokenEndpoint: string;
    resourceUrl: string;
    responseType: string;
    scope: string;
    state: string;
    redirectUrl: string;
    windowOptions: string;
    windowTarget: string = "_blank";

    pkceDisabled: boolean;
    pkceCodeVerifier: string;
    pkceCodeChallenge: string;
    pkceCodeChallengeMethod: string = "S256";
}

