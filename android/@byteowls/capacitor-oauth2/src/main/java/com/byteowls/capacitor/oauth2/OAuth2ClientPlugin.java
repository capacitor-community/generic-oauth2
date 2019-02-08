package com.byteowls.capacitor.oauth2;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import com.byteowls.capacitor.oauth2.handler.AccessTokenCallback;
import com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@SuppressWarnings("ALL")
@NativePlugin(requestCodes = { OAuth2ClientPlugin.RC_OAUTH_AUTHORIZATION }, name = "OAuth2Client")
public class OAuth2ClientPlugin extends Plugin {

    static final int RC_OAUTH_AUTHORIZATION = 2000;

    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_ANDROID_APP_ID = "android.appId";
    private static final String PARAM_RESPONSE_TYPE = "responseType";
    private static final String PARAM_ANDROID_RESPONSE_TYPE = "android.responseType";
    private static final String PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
    private static final String PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
    private static final String PARAM_ANDROID_CUSTOM_HANDLER_CLASS = "android.customHandlerClass";
    private static final String PARAM_ANDROID_CUSTOM_SCHEME = "android.customScheme";
    private static final String PARAM_PKCE_DISABLED = "pkceDisabled";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_RESOURCE_URL = "resourceUrl";
    private static final String RESPONSE_TYPE_CODE = "code";
    private static final String RESPONSE_TYPE_TOKEN = "token";

    private OAuth2Options oauth2Options;
    private AuthorizationService authService;
    private AuthState authState;

    public OAuth2ClientPlugin() {}

    @PluginMethod()
    public void authenticate(final PluginCall call) {
        disposeAuthService();
        oauth2Options = buildOptions(call);
        if (oauth2Options.getCustomHandlerClass() != null && oauth2Options.getCustomHandlerClass().length() > 0) {
            try {
                Class<OAuth2CustomHandler> handlerClass = (Class<OAuth2CustomHandler>) Class.forName(oauth2Options.getCustomHandlerClass());
                OAuth2CustomHandler handler = handlerClass.newInstance();
                handler.getAccessToken(getActivity(), call, new AccessTokenCallback() {
                    @Override
                    public void onSuccess(String accessToken) {
                        new ResourceUrlAsyncTask(call, oauth2Options, getLogTag()).execute(accessToken);
                    }

                    @Override
                    public void onCancel() {
                        call.reject("Login canceled!");
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(getLogTag(), "Login failed!", error);
                        call.reject("Login failed!");
                    }
                });
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                Log.e(getLogTag(), "Custom handler problem", e);
            }
        } else {
            if (oauth2Options.getAppId() == null) {
                call.reject("Option '" + PARAM_APP_ID + "' or '" + PARAM_ANDROID_APP_ID + "' is required!");
                return;
            }
            if (oauth2Options.getAuthorizationBaseUrl() == null) {
                call.reject("Option '" + PARAM_AUTHORIZATION_BASE_URL + "' is required!");
                return;
            }

            if (oauth2Options.getRedirectUrl() == null) {
                call.reject("Option '" + PARAM_ANDROID_CUSTOM_SCHEME + "' is required!");
                return;
            }

            AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                Uri.parse(oauth2Options.getAuthorizationBaseUrl()),
                Uri.parse(oauth2Options.getAccessTokenEndpoint())
            );

            if (this.authState == null) {
                this.authState = new AuthState(config);
            }

            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                config,
                oauth2Options.getAppId(),
                oauth2Options.getResponseType(),
                Uri.parse(oauth2Options.getRedirectUrl())
            );

            // appauth always uses a state
            if (oauth2Options.getState() != null) {
                builder.setState(oauth2Options.getState());
            }
            builder.setScope(oauth2Options.getScope());
            if (!oauth2Options.isPkceDisabled()) {
                builder.setCodeVerifier(oauth2Options.getPkceCodeVerifier());
            } else {
                builder.setCodeVerifier(null);
            }

            AuthorizationRequest req = builder.build();

            this.authService = new AuthorizationService(getContext());
            Intent authIntent = this.authService.getAuthorizationRequestIntent(req);

            startActivityForResult(call, authIntent, RC_OAUTH_AUTHORIZATION);
        }
    }

    @PluginMethod()
    public void logout(final PluginCall call) {
        String customHandlerClassname = ConfigUtils.getCallParam(String.class, call, PARAM_ANDROID_CUSTOM_HANDLER_CLASS);
        if (customHandlerClassname != null && customHandlerClassname.length() > 0) {
            try {
                Class<OAuth2CustomHandler> handlerClass = (Class<OAuth2CustomHandler>) Class.forName(customHandlerClassname);
                OAuth2CustomHandler handler = handlerClass.newInstance();
                boolean successful = handler.logout(call);
                if (successful) {
                    call.resolve();
                } else {
                    call.reject("Logout was not successful");
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                Log.e(getLogTag(), "Custom handler problem", e);
            }
        } else {
            this.disposeAuthService();
            this.discardAuthState();
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);
        if (RC_OAUTH_AUTHORIZATION == requestCode) {
            final PluginCall savedCall = getSavedCall();
            if (savedCall == null) {
                return;
            }

            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
            AuthorizationException error = AuthorizationException.fromIntent(data);
            this.authState.update(response, error);

            // get authorization code
            if (response != null) {
                this.authService = new AuthorizationService(getContext());
                this.authService.performTokenRequest(response.createTokenExchangeRequest(),
                    new AuthorizationService.TokenResponseCallback() {
                        @Override
                        public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
                            if (response != null) {
                                authState.update(response, ex);
                                if (oauth2Options.getResourceUrl() != null) {
                                    authState.performActionWithFreshTokens(authService, new AuthState.AuthStateAction() {
                                        @Override
                                        public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                                            new ResourceUrlAsyncTask(savedCall, oauth2Options, getLogTag()).execute(accessToken);
                                        }
                                    });
                                } else {
                                    // return only tokens if resourceUrl is empty
                                    try {
                                        JSObject json = new JSObject(response.jsonSerializeString());
                                        savedCall.resolve(json);
                                    } catch (JSONException e) {
                                        savedCall.reject("ERR_TOKENS_JSON");
                                    }
//                                    json.put("access_token", response.accessToken);
//                                    json.put("expires_in", response.accessTokenExpirationTime);
//                                    json.put("refresh_token", response.refreshToken);
//                                    json.put("id_token", response.idToken);
//                                    json.put("scope", response.scope);
//                                    json.put("additionalParameters", response.additionalParameters);
                                }

                            } else {
                                savedCall.reject("No authToken retrieved!");
                            }
                        }
                    });
            }
        }
    }

    protected OAuth2Options buildOptions(PluginCall call) {
        OAuth2Options o = new OAuth2Options();
        o.setAppId(getOverwritableParam(String.class, call, PARAM_APP_ID));
        o.setPkceDisabled(getOverwritableParam(Boolean.class, call, PARAM_PKCE_DISABLED));
        o.setAuthorizationBaseUrl(ConfigUtils.getCallString(call, PARAM_AUTHORIZATION_BASE_URL));
        o.setAccessTokenEndpoint(ConfigUtils.getCallString(call, PARAM_ACCESS_TOKEN_ENDPOINT));
        o.setResourceUrl(ConfigUtils.getCallString(call, PARAM_RESOURCE_URL));
        o.setResponseType(getOverwritableParam(String.class, call, PARAM_RESPONSE_TYPE));
        o.setScope(ConfigUtils.getCallString(call, PARAM_SCOPE));
        o.setState(ConfigUtils.getCallString(call, PARAM_STATE));
        if (o.getState() == null || o.getState().trim().length() == 0) {
            o.setState(ConfigUtils.getRandomString(20));
        }

        if (o.getResponseType() == null || o.getResponseType().length() == 0) {
            // fallback to token
            o.setResponseType(RESPONSE_TYPE_TOKEN);
        }

        if (RESPONSE_TYPE_CODE.equals(o.getResponseType())) {
            if (!o.isPkceDisabled()) {
                o.setPkceCodeVerifier(ConfigUtils.getRandomString(64));
            }
        }

        o.setRedirectUrl(ConfigUtils.getCallString(call, PARAM_ANDROID_CUSTOM_SCHEME));
        o.setCustomHandlerClass(ConfigUtils.getCallString(call, PARAM_ANDROID_CUSTOM_HANDLER_CLASS));
        return o;
    }

    /**
     * For use in #22
     */
    protected String getAuthorizationUrl(OAuth2Options options) {
        String url = options.getAuthorizationBaseUrl();
        url += "?client_id=" + options.getAppId();
        url += "&response_type=" + options.getResponseType();
        if (options.getRedirectUrl() != null) {
            url += "&redirect_uri=" + options.getRedirectUrl();
        }
        if (options.getScope() != null) {
            url += "&scope=" + options.getScope();
        }
        if (options.getState() != null) {
            url += "&state=" + options.getState();
        }
        try {
            url = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            // utf8 is always supported
        }
        return url;
    }

    protected <T> T getOverwritableParam(Class<T> clazz, PluginCall call, String key) {
        T baseParam = ConfigUtils.getCallParam(clazz, call, key);
        T androidParam = ConfigUtils.getCallParam(clazz, call, "android." + key);
        if (androidParam != null) {
            baseParam = androidParam;
        }
        return baseParam;
    }

    @Override
    protected void handleOnStop() {
        super.handleOnStop();
        disposeAuthService();
    }

    private void disposeAuthService() {
        if (authService != null) {
            authService.dispose();
            authService = null;
        }
    }

    public void discardAuthState() {
        if (this.authState != null) {
            this.authState = null;
        }
    }
}
