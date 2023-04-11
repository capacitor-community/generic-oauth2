package com.byteowls.capacitor.oauth2;

import java.util.HashMap;
import java.util.Map;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public class OAuth2Options {

    // required
    private String appId;
    private String authorizationBaseUrl;
    private String responseType;
    private String redirectUrl;

    private String scope;
    private String state;

    private String accessTokenEndpoint;
    private String resourceUrl;
    private Map<String, String> additionalResourceHeaders;

    private boolean pkceEnabled;
    private boolean logsEnabled;
    private String pkceCodeVerifier;
    private Map<String, String> additionalParameters;

    private String customHandlerClass;
    // Activity result handling
    private boolean handleResultOnNewIntent;
    private boolean handleResultOnActivityResult = true;

    private String display;
    private String loginHint;
    private String prompt;
    private String responseMode;

    private String logoutUrl;


    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAuthorizationBaseUrl() {
        return authorizationBaseUrl;
    }

    public void setAuthorizationBaseUrl(String authorizationBaseUrl) {
        this.authorizationBaseUrl = authorizationBaseUrl;
    }

    public String getAccessTokenEndpoint() {
        return accessTokenEndpoint;
    }

    public void setAccessTokenEndpoint(String accessTokenEndpoint) {
        this.accessTokenEndpoint = accessTokenEndpoint;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public boolean isLogsEnabled() {
        return logsEnabled;
    }

    public void setLogsEnabled(boolean logsEnabled) {
        this.logsEnabled = logsEnabled;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getCustomHandlerClass() {
        return customHandlerClass;
    }

    public void setCustomHandlerClass(String customHandlerClass) {
        this.customHandlerClass = customHandlerClass;
    }

    public boolean isPkceEnabled() {
        return pkceEnabled;
    }

    public void setPkceEnabled(boolean pkceEnabled) {
        this.pkceEnabled = pkceEnabled;
    }

    public String getPkceCodeVerifier() {
        return pkceCodeVerifier;
    }

    public void setPkceCodeVerifier(String pkceCodeVerifier) {
        this.pkceCodeVerifier = pkceCodeVerifier;
    }

    public Map<String, String> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(Map<String, String> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    public void addAdditionalParameter(String key, String value) {
        if (key != null && value != null) {
            if (this.additionalParameters == null) {
                this.additionalParameters = new HashMap<>();
            }
            this.additionalParameters.put(key, value);
        }
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }


    public boolean isHandleResultOnNewIntent() {
        return handleResultOnNewIntent;
    }

    public void setHandleResultOnNewIntent(boolean handleResultOnNewIntent) {
        this.handleResultOnNewIntent = handleResultOnNewIntent;
    }

    public boolean isHandleResultOnActivityResult() {
        return handleResultOnActivityResult;
    }

    public void setHandleResultOnActivityResult(boolean handleResultOnActivityResult) {
        this.handleResultOnActivityResult = handleResultOnActivityResult;
    }

    public Map<String, String> getAdditionalResourceHeaders() {
        return additionalResourceHeaders;
    }

    public void setAdditionalResourceHeaders(Map<String, String> additionalResourceHeaders) {
        this.additionalResourceHeaders = additionalResourceHeaders;
    }

    public void addAdditionalResourceHeader(String key, String value) {
        if (key != null && value != null) {
            if (this.additionalResourceHeaders == null) {
                this.additionalResourceHeaders = new HashMap<>();
            }
            this.additionalResourceHeaders.put(key, value);
        }
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }
}
