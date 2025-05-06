import { WebPlugin } from '@capacitor/core';
import { WebUtils } from './web-utils';
export class GenericOAuth2Web extends WebPlugin {
    constructor() {
        super(...arguments);
        this.loopCount = 2000;
        this.intervalLength = 100;
        this.MSG_RETURNED_TO_JS = 'Returned to JS:';
    }
    /**
     * Get a new access token using an existing refresh token.
     */
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    async refreshToken(_options) {
        return new Promise((_resolve, reject) => {
            reject(new Error('Functionality not implemented for PWAs yet'));
        });
    }
    async redirectFlowCodeListener(options) {
        this.webOptions = await WebUtils.buildWebOptions(options);
        return new Promise((resolve, reject) => {
            const urlParamObj = WebUtils.getUrlParams(options.response_url);
            if (urlParamObj) {
                const code = urlParamObj.code;
                if (code) {
                    this.getAccessToken(urlParamObj, resolve, reject, code);
                }
                else {
                    reject(new Error('Oauth Code parameter was not present in url.'));
                }
            }
            else {
                reject(new Error('Oauth Parameters where not present in url.'));
            }
        });
    }
    async authenticate(options) {
        const windowOptions = WebUtils.buildWindowOptions(options);
        // we open the window first to avoid popups being blocked because of
        // the asynchronous buildWebOptions call
        this.windowHandle = window.open('', windowOptions.windowTarget, windowOptions.windowOptions);
        this.webOptions = await WebUtils.buildWebOptions(options);
        return new Promise((resolve, reject) => {
            // validate
            if (!this.webOptions.appId || this.webOptions.appId.length == 0) {
                reject(new Error('ERR_PARAM_NO_APP_ID'));
            }
            else if (!this.webOptions.authorizationBaseUrl ||
                this.webOptions.authorizationBaseUrl.length == 0) {
                reject(new Error('ERR_PARAM_NO_AUTHORIZATION_BASE_URL'));
            }
            else if (!this.webOptions.redirectUrl ||
                this.webOptions.redirectUrl.length == 0) {
                reject(new Error('ERR_PARAM_NO_REDIRECT_URL'));
            }
            else if (!this.webOptions.responseType ||
                this.webOptions.responseType.length == 0) {
                reject(new Error('ERR_PARAM_NO_RESPONSE_TYPE'));
            }
            else {
                // init internal control params
                let loopCount = this.loopCount;
                this.windowClosedByPlugin = false;
                // open window
                const authorizationUrl = WebUtils.getAuthorizationUrl(this.webOptions);
                if (this.webOptions.logsEnabled) {
                    this.doLog('Authorization url: ' + authorizationUrl);
                }
                if (this.windowHandle) {
                    this.windowHandle.location.href = authorizationUrl;
                }
                // wait for redirect and resolve the
                this.intervalId = window.setInterval(() => {
                    var _a;
                    if (loopCount-- < 0) {
                        this.closeWindow();
                    }
                    else if (((_a = this.windowHandle) === null || _a === void 0 ? void 0 : _a.closed) && !this.windowClosedByPlugin) {
                        window.clearInterval(this.intervalId);
                        reject(new Error('USER_CANCELLED'));
                    }
                    else {
                        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                        let href = undefined;
                        try {
                            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                            href = this.windowHandle.location.href;
                        }
                        catch (ignore) {
                            // ignore DOMException: Blocked a frame with origin "http://localhost:4200" from accessing a cross-origin frame.
                        }
                        if (href != null &&
                            href.indexOf(this.webOptions.redirectUrl) >= 0) {
                            if (this.webOptions.logsEnabled) {
                                this.doLog('Url from Provider: ' + href);
                            }
                            const authorizationRedirectUrlParamObj = WebUtils.getUrlParams(href);
                            if (authorizationRedirectUrlParamObj) {
                                if (this.webOptions.logsEnabled) {
                                    this.doLog('Authorization response:', authorizationRedirectUrlParamObj);
                                }
                                window.clearInterval(this.intervalId);
                                // check state
                                if (authorizationRedirectUrlParamObj.state ===
                                    this.webOptions.state) {
                                    if (this.webOptions.accessTokenEndpoint) {
                                        const authorizationCode = authorizationRedirectUrlParamObj.code;
                                        if (authorizationCode) {
                                            this.getAccessToken(authorizationRedirectUrlParamObj, resolve, reject, authorizationCode);
                                        }
                                        else {
                                            reject(new Error('ERR_NO_AUTHORIZATION_CODE'));
                                        }
                                        this.closeWindow();
                                    }
                                    else {
                                        // if no accessTokenEndpoint exists request the resource
                                        this.requestResource(authorizationRedirectUrlParamObj.access_token, resolve, reject, authorizationRedirectUrlParamObj);
                                    }
                                }
                                else {
                                    if (this.webOptions.logsEnabled) {
                                        this.doLog('State from web options: ' + this.webOptions.state);
                                        this.doLog('State returned from provider: ' +
                                            authorizationRedirectUrlParamObj.state);
                                    }
                                    reject(new Error('ERR_STATES_NOT_MATCH'));
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
    getAccessToken(authorizationRedirectUrlParamObj, resolve, reject, authorizationCode) {
        const tokenRequest = new XMLHttpRequest();
        tokenRequest.onload = () => {
            WebUtils.clearCodeVerifier();
            if (tokenRequest.status === 200) {
                const accessTokenResponse = JSON.parse(tokenRequest.response);
                if (this.webOptions.logsEnabled) {
                    this.doLog('Access token response:', accessTokenResponse);
                }
                this.requestResource(accessTokenResponse.access_token, resolve, reject, authorizationRedirectUrlParamObj, accessTokenResponse);
            }
        };
        tokenRequest.onerror = () => {
            this.doLog('ERR_GENERAL: See client logs. It might be CORS. Status text: ' +
                tokenRequest.statusText);
            reject(new Error('ERR_GENERAL'));
        };
        tokenRequest.open('POST', this.webOptions.accessTokenEndpoint, true);
        tokenRequest.setRequestHeader('accept', 'application/json');
        if (this.webOptions.sendCacheControlHeader) {
            tokenRequest.setRequestHeader('cache-control', 'no-cache');
        }
        tokenRequest.setRequestHeader('content-type', 'application/x-www-form-urlencoded');
        tokenRequest.send(WebUtils.getTokenEndpointData(this.webOptions, authorizationCode));
    }
    requestResource(accessToken, resolve, reject, authorizationResponse, accessTokenResponse = null) {
        if (this.webOptions.resourceUrl) {
            const logsEnabled = this.webOptions.logsEnabled;
            if (logsEnabled) {
                this.doLog('Resource url: ' + this.webOptions.resourceUrl);
            }
            if (accessToken) {
                if (logsEnabled) {
                    this.doLog('Access token:', accessToken);
                }
                const self = this;
                const request = new XMLHttpRequest();
                request.onload = function () {
                    if (this.status === 200) {
                        const resp = JSON.parse(this.response);
                        if (logsEnabled) {
                            self.doLog('Resource response:', resp);
                        }
                        if (resp) {
                            self.assignResponses(resp, accessToken, authorizationResponse, accessTokenResponse);
                        }
                        if (logsEnabled) {
                            self.doLog(self.MSG_RETURNED_TO_JS, resp);
                        }
                        resolve(resp);
                    }
                    else {
                        reject(new Error(this.statusText));
                    }
                    self.closeWindow();
                };
                request.onerror = function () {
                    if (logsEnabled) {
                        self.doLog('ERR_GENERAL: ' + this.statusText);
                    }
                    reject(new Error('ERR_GENERAL'));
                    self.closeWindow();
                };
                request.open('GET', this.webOptions.resourceUrl, true);
                request.setRequestHeader('Authorization', `Bearer ${accessToken}`);
                if (this.webOptions.additionalResourceHeaders) {
                    for (const key in this.webOptions.additionalResourceHeaders) {
                        request.setRequestHeader(key, this.webOptions.additionalResourceHeaders[key]);
                    }
                }
                request.send();
            }
            else {
                if (logsEnabled) {
                    this.doLog('No accessToken was provided although you configured a resourceUrl. Remove the resourceUrl from the config.');
                }
                reject(new Error('ERR_NO_ACCESS_TOKEN'));
                this.closeWindow();
            }
        }
        else {
            // if no resource url exists just return the accessToken response
            const resp = {};
            this.assignResponses(resp, accessToken, authorizationResponse, accessTokenResponse);
            if (this.webOptions.logsEnabled) {
                this.doLog(this.MSG_RETURNED_TO_JS, resp);
            }
            resolve(resp);
            this.closeWindow();
        }
    }
    assignResponses(resp, accessToken, authorizationResponse, accessTokenResponse = null) {
        // #154
        if (authorizationResponse) {
            resp['authorization_response'] = authorizationResponse;
        }
        if (accessTokenResponse) {
            resp['access_token_response'] = accessTokenResponse;
        }
        resp['access_token'] = accessToken;
    }
    async logout(options) {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        return new Promise((resolve, _reject) => {
            localStorage.removeItem(WebUtils.getAppId(options));
            resolve(true);
        });
    }
    closeWindow() {
        var _a;
        window.clearInterval(this.intervalId);
        // #164 if the provider's login page is opened in the same tab or window it must not be closed
        // if (this.webOptions.windowTarget !== "_self") {
        //     this.windowHandle?.close();
        // }
        (_a = this.windowHandle) === null || _a === void 0 ? void 0 : _a.close();
        this.windowClosedByPlugin = true;
    }
    doLog(msg, obj = null) {
        console.log('I/Capacitor/GenericOAuth2Plugin: ' + msg, obj);
    }
}
//# sourceMappingURL=web.js.map