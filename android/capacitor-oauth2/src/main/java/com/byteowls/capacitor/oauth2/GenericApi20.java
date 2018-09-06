package com.byteowls.capacitor.oauth2;

import com.github.scribejava.core.builder.api.DefaultApi20;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public class GenericApi20 extends DefaultApi20 {
    private String accessTokenEndpoint;
    private String authorizationBaseUrl;

    public GenericApi20(String accessTokenEndpoint, String authorizationBaseUrl) {
        this.accessTokenEndpoint = accessTokenEndpoint;
        this.authorizationBaseUrl = authorizationBaseUrl;
    }


    @Override
    public String getAccessTokenEndpoint() {
        return accessTokenEndpoint;
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return authorizationBaseUrl;
    }
}
