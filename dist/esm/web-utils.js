// import sha256 from "fast-sha256";
export class WebUtils {
    /**
     * Public only for testing
     */
    static getAppId(options) {
        return this.getOverwritableValue(options, 'appId');
    }
    static getOverwritableValue(options, key) {
        let base = options[key];
        if (options.web && key in options.web) {
            base = options.web[key];
        }
        return base;
    }
    /**
     * Public only for testing
     */
    static getAuthorizationUrl(options) {
        let url = options.authorizationBaseUrl + '?client_id=' + options.appId;
        url += '&response_type=' + options.responseType;
        if (options.redirectUrl) {
            url += '&redirect_uri=' + options.redirectUrl;
        }
        if (options.scope) {
            url += '&scope=' + options.scope;
        }
        url += '&state=' + options.state;
        if (options.additionalParameters) {
            for (const key in options.additionalParameters) {
                url += '&' + key + '=' + options.additionalParameters[key];
            }
        }
        if (options.pkceCodeChallenge) {
            url += '&code_challenge=' + options.pkceCodeChallenge;
            url += '&code_challenge_method=' + options.pkceCodeChallengeMethod;
        }
        return encodeURI(url);
    }
    static getTokenEndpointData(options, code) {
        let body = '';
        body +=
            encodeURIComponent('grant_type') +
                '=' +
                encodeURIComponent('authorization_code') +
                '&';
        body +=
            encodeURIComponent('client_id') +
                '=' +
                encodeURIComponent(options.appId) +
                '&';
        body +=
            encodeURIComponent('redirect_uri') +
                '=' +
                encodeURIComponent(options.redirectUrl) +
                '&';
        body += encodeURIComponent('code') + '=' + encodeURIComponent(code) + '&';
        body +=
            encodeURIComponent('code_verifier') +
                '=' +
                encodeURIComponent(options.pkceCodeVerifier);
        return body;
    }
    static setCodeVerifier(code) {
        try {
            window.sessionStorage.setItem(`I_Capacitor_GenericOAuth2Plugin_PKCE`, code);
            return true;
        }
        catch (err) {
            return false;
        }
    }
    static clearCodeVerifier() {
        window.sessionStorage.removeItem(`I_Capacitor_GenericOAuth2Plugin_PKCE`);
    }
    static getCodeVerifier() {
        return window.sessionStorage.getItem(`I_Capacitor_GenericOAuth2Plugin_PKCE`);
    }
    /**
     * Public only for testing
     */
    static getUrlParams(url) {
        const urlString = `${url !== null && url !== void 0 ? url : ''}`.trim();
        if (urlString.length === 0) {
            return;
        }
        const parsedUrl = new URL(urlString);
        if (!parsedUrl.search && !parsedUrl.hash) {
            return;
        }
        let urlParamStr;
        if (parsedUrl.search) {
            urlParamStr = parsedUrl.search.substr(1);
        }
        else {
            urlParamStr = parsedUrl.hash.substr(1);
        }
        const keyValuePairs = urlParamStr.split(`&`);
        return keyValuePairs.reduce((accumulator, currentValue) => {
            const [key, val] = currentValue.split(`=`);
            if (key && key.length > 0) {
                return Object.assign(Object.assign({}, accumulator), { [key]: decodeURIComponent(val) });
            }
        }, {});
    }
    static randomString(length = 10) {
        const haystack = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
        let randomStr;
        if (window.crypto) {
            let numberArray = new Uint32Array(length);
            window.crypto.getRandomValues(numberArray);
            numberArray = numberArray.map(x => haystack.charCodeAt(x % haystack.length));
            const stringArray = [];
            numberArray.forEach(x => {
                stringArray.push(haystack.charAt(x % haystack.length));
            });
            randomStr = stringArray.join('');
        }
        else {
            randomStr = '';
            for (let i = 0; i < length; i++) {
                randomStr += haystack.charAt(Math.floor(Math.random() * haystack.length));
            }
        }
        return randomStr;
    }
    static async buildWebOptions(configOptions) {
        var _a;
        const webOptions = new WebOptions();
        webOptions.appId = this.getAppId(configOptions);
        webOptions.authorizationBaseUrl = this.getOverwritableValue(configOptions, 'authorizationBaseUrl');
        webOptions.responseType = this.getOverwritableValue(configOptions, 'responseType');
        if (!webOptions.responseType) {
            webOptions.responseType = 'token';
        }
        webOptions.redirectUrl = this.getOverwritableValue(configOptions, 'redirectUrl');
        // controlling parameters
        webOptions.resourceUrl = this.getOverwritableValue(configOptions, 'resourceUrl');
        webOptions.accessTokenEndpoint = this.getOverwritableValue(configOptions, 'accessTokenEndpoint');
        webOptions.pkceEnabled = this.getOverwritableValue(configOptions, 'pkceEnabled');
        webOptions.sendCacheControlHeader =
            (_a = this.getOverwritableValue(configOptions, 'sendCacheControlHeader')) !== null && _a !== void 0 ? _a : webOptions.sendCacheControlHeader;
        if (webOptions.pkceEnabled) {
            const pkceCode = this.getCodeVerifier();
            if (pkceCode) {
                webOptions.pkceCodeVerifier = pkceCode;
            }
            else {
                webOptions.pkceCodeVerifier = this.randomString(64);
                this.setCodeVerifier(webOptions.pkceCodeVerifier);
            }
            if (CryptoUtils.HAS_SUBTLE_CRYPTO) {
                await CryptoUtils.deriveChallenge(webOptions.pkceCodeVerifier).then(c => {
                    webOptions.pkceCodeChallenge = c;
                    webOptions.pkceCodeChallengeMethod = 'S256';
                });
            }
            else {
                webOptions.pkceCodeChallenge = webOptions.pkceCodeVerifier;
                webOptions.pkceCodeChallengeMethod = 'plain';
            }
        }
        webOptions.scope = this.getOverwritableValue(configOptions, 'scope');
        webOptions.state = this.getOverwritableValue(configOptions, 'state');
        if (!webOptions.state || webOptions.state.length === 0) {
            webOptions.state = this.randomString(20);
        }
        const parametersMapHelper = this.getOverwritableValue(configOptions, 'additionalParameters');
        if (parametersMapHelper) {
            webOptions.additionalParameters = {};
            for (const key in parametersMapHelper) {
                if (key && key.trim().length > 0) {
                    const value = parametersMapHelper[key];
                    if (value && value.trim().length > 0) {
                        webOptions.additionalParameters[key] = value;
                    }
                }
            }
        }
        const headersMapHelper = this.getOverwritableValue(configOptions, 'additionalResourceHeaders');
        if (headersMapHelper) {
            webOptions.additionalResourceHeaders = {};
            for (const key in headersMapHelper) {
                if (key && key.trim().length > 0) {
                    const value = headersMapHelper[key];
                    if (value && value.trim().length > 0) {
                        webOptions.additionalResourceHeaders[key] = value;
                    }
                }
            }
        }
        webOptions.logsEnabled = this.getOverwritableValue(configOptions, 'logsEnabled');
        return webOptions;
    }
    static buildWindowOptions(configOptions) {
        const windowOptions = new WebOptions();
        if (configOptions.web) {
            if (configOptions.web.windowOptions) {
                windowOptions.windowOptions = configOptions.web.windowOptions;
            }
            if (configOptions.web.windowTarget) {
                windowOptions.windowTarget = configOptions.web.windowTarget;
            }
        }
        return windowOptions;
    }
}
export class CryptoUtils {
    static toUint8Array(str) {
        const buf = new ArrayBuffer(str.length);
        const bufView = new Uint8Array(buf);
        for (let i = 0; i < str.length; i++) {
            bufView[i] = str.charCodeAt(i);
        }
        return bufView;
    }
    static toBase64Url(base64) {
        return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
    }
    static toBase64(bytes) {
        const len = bytes.length;
        let base64 = '';
        for (let i = 0; i < len; i += 3) {
            base64 += this.BASE64_CHARS[bytes[i] >> 2];
            base64 += this.BASE64_CHARS[((bytes[i] & 3) << 4) | (bytes[i + 1] >> 4)];
            base64 +=
                this.BASE64_CHARS[((bytes[i + 1] & 15) << 2) | (bytes[i + 2] >> 6)];
            base64 += this.BASE64_CHARS[bytes[i + 2] & 63];
        }
        if (len % 3 === 2) {
            base64 = base64.substring(0, base64.length - 1) + '=';
        }
        else if (len % 3 === 1) {
            base64 = base64.substring(0, base64.length - 2) + '==';
        }
        return base64;
    }
    static deriveChallenge(codeVerifier) {
        if (codeVerifier.length < 43 || codeVerifier.length > 128) {
            return Promise.reject(new Error('ERR_PKCE_CODE_VERIFIER_INVALID_LENGTH'));
        }
        if (!CryptoUtils.HAS_SUBTLE_CRYPTO) {
            return Promise.reject(new Error('ERR_PKCE_CRYPTO_NOTSUPPORTED'));
        }
        return new Promise((resolve, reject) => {
            crypto.subtle.digest('SHA-256', this.toUint8Array(codeVerifier)).then(arrayBuffer => {
                return resolve(this.toBase64Url(this.toBase64(new Uint8Array(arrayBuffer))));
            }, error => reject(error));
        });
    }
}
CryptoUtils.BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
CryptoUtils.HAS_SUBTLE_CRYPTO = typeof window !== 'undefined' &&
    !!window.crypto &&
    !!window.crypto.subtle;
export class WebOptions {
    constructor() {
        this.sendCacheControlHeader = true;
        this.windowTarget = '_blank';
    }
}
//# sourceMappingURL=web-utils.js.map