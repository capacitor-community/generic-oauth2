package com.getcapacitor.community.genericoauth2;

public class OAuth2RefreshTokenOptions {

    private String appId;
    private String accessTokenEndpoint;
    private String refreshToken;
    private String scope;
    private String rawPkcs;
    private String pkcsPassword;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAccessTokenEndpoint() {
        return accessTokenEndpoint;
    }

    public void setAccessTokenEndpoint(String accessTokenEndpoint) {
        this.accessTokenEndpoint = accessTokenEndpoint;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRawPkcs() {
        return rawPkcs;
    }

    public void setRawPkcs(String rawPkcs) {
        this.rawPkcs = rawPkcs;
    }

    public String getPkcsPassword() {
        return pkcsPassword;
    }

    public void setPkcsPassword(String pkcsPassword) {
        this.pkcsPassword = pkcsPassword;
    }
}
