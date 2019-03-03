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

    static getTokenEndpointUrl(options: WebOptions, code: string) {
        let data = "grant_type=authorization_code";
        data += "&client_id=" + options.appId;
        data += "&redirect_uri=" + options.redirectUrl;
        data += "&code=" + code;
        data += "&code_verifier=" + options.pkceCodeVerifier;
        return encodeURI(data);
    }

    static getTokenEndpointData(options: WebOptions, code: string): FormData {
        let data = new FormData();
        data.append('grant_type', 'authorization_code');
        data.append('client_id', options.appId);
        data.append('redirect_uri', options.redirectUrl);
        data.append('code', code);
        data.append('code_verifier', options.pkceCodeVerifier);
        // data.append('scope', options.scope);
        return data;
    }

    /**
     * Public only for testing
     */
    static getUrlParams(urlString: string): any | undefined {
        if (urlString && urlString.trim().length > 0) {
            urlString = urlString.trim();
            let idx = urlString.indexOf("#");
            if (idx == -1) {
                idx = urlString.indexOf("?");
            }
            if (idx != -1 && urlString.length > (idx + 1)) {
                const urlParamStr = urlString.slice(idx + 1);
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
                if (CryptoUtils.HAS_SUBTLE_CRYPTO) {
                    CryptoUtils.deriveChallenge(webOptions.pkceCodeVerifier).then(c => {
                        webOptions.pkceCodeChallenge = c;
                        webOptions.pkceCodeChallengeMethod = "S256";
                    });
                } else {
                    webOptions.pkceCodeChallenge = webOptions.pkceCodeVerifier;
                    webOptions.pkceCodeChallengeMethod = "plain";
                }
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

    // static b64EncodeUnicode(str: any) {
    //     // first we use encodeURIComponent to get percent-encoded UTF-8,
    //     // then we convert the percent encodings into raw bytes which
    //     // can be fed into btoa.
    //     return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g,
    //         // function toSolidBytes(match, p1) {
    //         (match, p1) => {
    //             // console.debug('match: ' + match);
    //             return String.fromCharCode(("0x" + p1) as any);
    //         }));
    // }
    //
    // static b64DecodeUnicode(str: string) {
    //     // Going backwards: from bytestream, to percent-encoding, to original string.
    //     return decodeURIComponent(atob(str).split('').map(function (c) {
    //         return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    //     }).join(''));
    // }
    //
    // static bufferToBase64(buffer: ArrayBuffer) {
    //     const binary = String.fromCharCode.apply(null, buffer);
    //     return window.btoa(binary);
    // }

}

/**
 *
 */
import * as base64 from 'base64-js';
export class CryptoUtils {
    static HAS_SUBTLE_CRYPTO: boolean = typeof window !== 'undefined' && !!(window.crypto as any) && !!(window.crypto.subtle as any);

    static textEncode(str: string) {
        const buf = new ArrayBuffer(str.length);
        const bufView = new Uint8Array(buf);

        for (let i = 0; i < str.length; i++) {
            bufView[i] = str.charCodeAt(i);
        }
        return bufView;
    }

    static urlSafe(buffer: Uint8Array): string {
        const encoded = base64.fromByteArray(new Uint8Array(buffer));
        return encoded.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
    }

    static deriveChallenge(codeVerifier: string): Promise<string> {
        if (codeVerifier.length < 43 || codeVerifier.length > 128) {
            console.log("Code verifier length:", codeVerifier);
            return Promise.reject(new Error('ERR_PKCE_CODE_VERIFIER_INVALID_LENGTH'));
        }
        if (!CryptoUtils.HAS_SUBTLE_CRYPTO) {
            return Promise.reject(new Error('ERR_PKCE_CRYPTO_NOTSUPPORTED'));
        }

        return new Promise((resolve, reject) => {
            crypto.subtle.digest('SHA-256', this.textEncode(codeVerifier)).then(buffer => {
                return resolve(this.urlSafe(new Uint8Array(buffer)));
            }, error => reject(error));
        });
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
    pkceCodeChallengeMethod: string;
}

