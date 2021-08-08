package com.byteowls.capacitor.oauth2;

import com.getcapacitor.JSObject;

import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.TokenResponse;

public abstract class OAuth2Utils {

    public static void assignResponses(JSObject resp, String accessToken, AuthorizationResponse authorizationResponse, TokenResponse accessTokenResponse) {
        // #154
        if (authorizationResponse != null) {
            resp.put("authorization_response", authorizationResponse.jsonSerialize());
        }
        if (accessTokenResponse != null) {
            resp.put("access_token_response", accessTokenResponse.jsonSerialize());
        }
        if (accessToken != null) {
            resp.put("access_token", accessToken);
        }
    }
}
