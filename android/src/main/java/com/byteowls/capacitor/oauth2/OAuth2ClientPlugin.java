package com.byteowls.capacitor.oauth2;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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
import net.openid.appauth.GrantTypeValues;
import net.openid.appauth.TokenRequest;

import org.json.JSONException;

import java.util.Map;

@NativePlugin(requestCodes = {OAuth2ClientPlugin.REQ_OAUTH_AUTHORIZATION}, name = "OAuth2Client")
public class OAuth2ClientPlugin extends Plugin {

    static final int REQ_OAUTH_AUTHORIZATION = 2000;

    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
    private static final String PARAM_RESPONSE_TYPE = "responseType";
    // private static final String PARAM_REDIRECT_URL = "redirectUrl";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";

    private static final String PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
    private static final String PARAM_PKCE_DISABLED = "pkceDisabled";
    private static final String PARAM_RESOURCE_URL = "resourceUrl";
    private static final String RESPONSE_TYPE_CODE = "code";
    private static final String RESPONSE_TYPE_TOKEN = "token";
    private static final String PARAM_ADDITIONAL_PARAMETERS = "additionalParameters";
    private static final String PARAM_ANDROID_CUSTOM_HANDLER_CLASS = "android.customHandlerClass";
    private static final String PARAM_ANDROID_CUSTOM_SCHEME = "android.customScheme";

    // Refresh token params
    private static final String PARAM_REFRESH_TOKEN = "refreshToken";

    // open id params
    private static final String PARAM_DISPLAY = "display";
    private static final String PARAM_LOGIN_HINT = "login_hint";
    private static final String PARAM_PROMPT = "prompt";
    private static final String PARAM_RESPONSE_MODE = "response_mode";

    private static final String USER_CANCELLED = "USER_CANCELLED";

    private static final String ERR_PARAM_NO_APP_ID = "ERR_PARAM_NO_APP_ID";
    private static final String ERR_PARAM_NO_AUTHORIZATION_BASE_URL = "ERR_PARAM_NO_AUTHORIZATION_BASE_URL";
    private static final String ERR_PARAM_NO_REDIRECT_URL = "ERR_PARAM_NO_REDIRECT_URL";
    private static final String ERR_PARAM_INVALID_RESPONSE_TYPE = "ERR_PARAM_INVALID_RESPONSE_TYPE";
    private static final String ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT = "ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT";
    private static final String ERR_PARAM_NO_REFRESH_TOKEN = "ERR_PARAM_NO_REFRESH_TOKEN";

    private static final String ERR_NO_ACCESS_TOKEN = "ERR_NO_ACCESS_TOKEN";
    private static final String ERR_ANDROID_NO_BROWSER = "ERR_ANDROID_NO_BROWSER";
    private static final String ERR_ANDROID_RESULT_NULL = "ERR_ANDROID_NO_INTENT";

    private static final String ERR_CUSTOM_HANDLER_LOGIN = "ERR_CUSTOM_HANDLER_LOGIN";
    private static final String ERR_CUSTOM_HANDLER_LOGOUT = "ERR_CUSTOM_HANDLER_LOGOUT";

    private static final String ERR_GENERAL = "ERR_GENERAL";
    private static final String ERR_STATES_NOT_MATCH = "ERR_STATES_NOT_MATCH";
    private static final String ERR_NO_AUTHORIZATION_CODE = "ERR_NO_AUTHORIZATION_CODE";

    private OAuth2Options oauth2Options;
    private AuthorizationService authService;
    private AuthState authState;

    public OAuth2ClientPlugin() {
    }

    @PluginMethod()
    public void refreshToken(final PluginCall call) {
        disposeAuthService();
        OAuth2RefreshTokenOptions oAuth2RefreshTokenOptions = buildRefreshTokenOptions(call.getData());

        if (oAuth2RefreshTokenOptions.getAppId() == null) {
            call.reject(ERR_PARAM_NO_APP_ID);
            return;
        }

        if (oAuth2RefreshTokenOptions.getAccessTokenEndpoint() == null) {
            call.reject(ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT);
            return;
        }

        if (oAuth2RefreshTokenOptions.getRefreshToken() == null) {
            call.reject(ERR_PARAM_NO_REFRESH_TOKEN);
            return;
        }

        this.authService = new AuthorizationService(getContext());

        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
            Uri.parse(""),
            Uri.parse(oAuth2RefreshTokenOptions.getAccessTokenEndpoint())
        );

        if (this.authState == null) {
            this.authState = new AuthState(config);
        }

        TokenRequest tokenRequest = new TokenRequest.Builder(
            config,
            oAuth2RefreshTokenOptions.getAppId()
        ).setGrantType(GrantTypeValues.REFRESH_TOKEN)
            .setScope(oAuth2RefreshTokenOptions.getScope())
            .setRefreshToken(oAuth2RefreshTokenOptions.getRefreshToken())
            .build();

        this.authService.performTokenRequest(tokenRequest, (response1, ex) -> {
            this.authState.update(response1, ex);
            if (ex != null) {
                call.reject(ERR_GENERAL, ex);
            } else {
                if (response1 != null) {
                    try {
                        JSObject json = new JSObject(response1.jsonSerializeString());
                        call.resolve(json);
                    } catch (JSONException e) {
                        call.reject(ERR_GENERAL, e);
                    }

                } else {
                    call.reject(ERR_NO_ACCESS_TOKEN);
                }
            }
        });
    }

    @PluginMethod()
    public void authenticate(final PluginCall call) {
        disposeAuthService();
        oauth2Options = buildAuthenticateOptions(call.getData());
        if (oauth2Options.hasCustomHandlerClass()) {
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
                        call.reject(USER_CANCELLED);
                    }

                    @Override
                    public void onError(Exception error) {
                        call.reject(ERR_CUSTOM_HANDLER_LOGIN, error);
                    }
                });
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                call.reject(ERR_CUSTOM_HANDLER_LOGIN, e);
            } catch (Exception e) {
                call.reject(ERR_GENERAL, e);
            }
        } else {
            if (oauth2Options.getAppId() == null) {
                call.reject(ERR_PARAM_NO_APP_ID);
                return;
            }
            if (oauth2Options.getAuthorizationBaseUrl() == null) {
                call.reject(ERR_PARAM_NO_AUTHORIZATION_BASE_URL);
                return;
            }

            if (oauth2Options.getRedirectUrl() == null) {
                call.reject(ERR_PARAM_NO_REDIRECT_URL);
                return;
            }

            if (!RESPONSE_TYPE_CODE.equals(oauth2Options.getResponseType()) && !RESPONSE_TYPE_TOKEN.equals(oauth2Options.getResponseType())) {
                call.reject(ERR_PARAM_INVALID_RESPONSE_TYPE);
                return;
            }

            if (RESPONSE_TYPE_CODE.equals(oauth2Options.getResponseType()) && oauth2Options.getAccessTokenEndpoint() == null) {
                call.reject(ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT);
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

            // app auth always uses a state
            if (oauth2Options.getState() != null) {
                builder.setState(oauth2Options.getState());
            }
            builder.setScope(oauth2Options.getScope());
            if (!oauth2Options.isPkceDisabled()) {
                builder.setCodeVerifier(oauth2Options.getPkceCodeVerifier());
            } else {
                builder.setCodeVerifier(null);
            }
            if (oauth2Options.getPrompt() != null) {
                builder.setPrompt(oauth2Options.getPrompt());
            }
            if (oauth2Options.getLoginHint() != null) {
                builder.setLoginHint(oauth2Options.getLoginHint());
            }
            if (oauth2Options.getResponseMode() != null) {
                builder.setResponseMode(oauth2Options.getResponseMode());
            }
            if (oauth2Options.getDisplay() != null) {
                builder.setDisplay(oauth2Options.getDisplay());
            }

            if (oauth2Options.getAdditionalParameters() != null) {
                try {
                    builder.setAdditionalParameters(oauth2Options.getAdditionalParameters());
                } catch (IllegalArgumentException e) {
                    // ignore all additional parameter on error
                    Log.e(getLogTag(), "Additional parameter error", e);
                }
            }

            AuthorizationRequest req = builder.build();

            this.authService = new AuthorizationService(getContext());
            try {
                Intent authIntent = this.authService.getAuthorizationRequestIntent(req);
                saveCall(call);
                startActivityForResult(call, authIntent, REQ_OAUTH_AUTHORIZATION);
            } catch (ActivityNotFoundException e) {
                call.reject(ERR_ANDROID_NO_BROWSER, e);
            }
        }
    }

    @PluginMethod()
    public void logout(final PluginCall call) {
        String customHandlerClassname = ConfigUtils.getParam(String.class, call.getData(), PARAM_ANDROID_CUSTOM_HANDLER_CLASS);
        if (customHandlerClassname != null && customHandlerClassname.length() > 0) {
            try {
                Class<OAuth2CustomHandler> handlerClass = (Class<OAuth2CustomHandler>) Class.forName(customHandlerClassname);
                OAuth2CustomHandler handler = handlerClass.newInstance();
                boolean successful = handler.logout(getActivity(), call);
                if (successful) {
                    call.resolve();
                } else {
                    call.reject(ERR_CUSTOM_HANDLER_LOGOUT);
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                call.reject(ERR_CUSTOM_HANDLER_LOGOUT, e);
            } catch (Exception e) {
                call.reject(ERR_GENERAL, e);
            }
        } else {
            this.disposeAuthService();
            this.discardAuthState();
            call.resolve();
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQ_OAUTH_AUTHORIZATION == requestCode) {
            final PluginCall savedCall = getSavedCall();
            // there are valid situation when the Intent is null, but
            if (data != null) {

                AuthorizationResponse response;
                AuthorizationException error;
                try {
                    response = AuthorizationResponse.fromIntent(data);
                    error = AuthorizationException.fromIntent(data);
                    this.authState.update(response, error);
                } catch (IllegalArgumentException e) {
                    savedCall.reject(ERR_GENERAL, e);
                    return;
                }

                if (error != null) {
                    if (error.code == AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.code) {
                        savedCall.reject(USER_CANCELLED);
                    } else if (error.code == AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH.code) {
                        savedCall.reject(ERR_STATES_NOT_MATCH);
                    } else {
                        savedCall.reject(ERR_GENERAL, error);
                    }
                    return;
                }

                // get authorization code
                if (response != null) {
                    this.authService = new AuthorizationService(getContext());
                    TokenRequest tokenExchangeRequest;
                    try {
                        tokenExchangeRequest = response.createTokenExchangeRequest();
                        this.authService.performTokenRequest(tokenExchangeRequest, (response1, ex) -> {
                            authState.update(response1, ex);
                            if (ex != null) {
                                savedCall.reject(ERR_GENERAL, ex);
                            } else {
                                if (response1 != null) {
                                    if (oauth2Options.getResourceUrl() != null) {
                                        authState.performActionWithFreshTokens(authService, (accessToken, idToken, ex1)
                                            -> new ResourceUrlAsyncTask(savedCall, oauth2Options, getLogTag()).execute(accessToken));
                                    } else {
                                        try {
                                            JSObject json = new JSObject(response1.jsonSerializeString());
                                            savedCall.resolve(json);
                                        } catch (JSONException e) {
                                            savedCall.reject(ERR_GENERAL, e);
                                        }
                                    }
                                } else {
                                    savedCall.reject(ERR_NO_ACCESS_TOKEN);
                                }
                            }

                        });
                    } catch (IllegalStateException e) {
                        savedCall.reject(ERR_NO_AUTHORIZATION_CODE);
                    }
                }
            } else {
                // the intent is null because the provider send the redirect to the server, which would be valid
                // the intent is null because the plugin user configured sth wrong incl.
                // the provider does not support redirecting to a android app, which would be invalid
                savedCall.reject(ERR_ANDROID_RESULT_NULL);
            }
        }
    }

    protected OAuth2Options buildAuthenticateOptions(JSObject callData) {
        OAuth2Options o = new OAuth2Options();
        o.setAppId(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_APP_ID));
        o.setPkceDisabled(ConfigUtils.getOverwrittenAndroidParam(Boolean.class, callData, PARAM_PKCE_DISABLED));
        o.setAuthorizationBaseUrl(ConfigUtils.getParamString(callData, PARAM_AUTHORIZATION_BASE_URL));
        o.setAccessTokenEndpoint(ConfigUtils.getParamString(callData, PARAM_ACCESS_TOKEN_ENDPOINT));
        o.setResourceUrl(ConfigUtils.getParamString(callData, PARAM_RESOURCE_URL));
        o.setResponseType(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_RESPONSE_TYPE));
        o.setScope(ConfigUtils.getParamString(callData, PARAM_SCOPE));
        o.setState(ConfigUtils.getParamString(callData, PARAM_STATE));
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

        Map<String, String> additionalParameters = ConfigUtils.getOverwrittenAndroidParamMap(callData, PARAM_ADDITIONAL_PARAMETERS);
        if (!additionalParameters.isEmpty()) {
            for (Map.Entry<String, String> entry : additionalParameters.entrySet()) {
                String key = entry.getKey();
                if (PARAM_DISPLAY.equals(key)) {
                    o.setDisplay(entry.getValue());
                } else if (PARAM_LOGIN_HINT.equals(key)) {
                    o.setLoginHint(entry.getValue());
                } else if (PARAM_PROMPT.equals(key)) {
                    o.setPrompt(entry.getValue());
                } else if (PARAM_RESPONSE_MODE.equals(key)) {
                    o.setResponseMode(entry.getValue());
                } else {
                    o.addAdditionalParameter(key, entry.getValue());
                }
            }
        }
        o.setRedirectUrl(ConfigUtils.getParamString(callData, PARAM_ANDROID_CUSTOM_SCHEME));
        o.setCustomHandlerClass(ConfigUtils.getParamString(callData, PARAM_ANDROID_CUSTOM_HANDLER_CLASS));
        return o;
    }

    protected OAuth2RefreshTokenOptions buildRefreshTokenOptions(JSObject callData) {
        OAuth2RefreshTokenOptions o = new OAuth2RefreshTokenOptions();
        o.setAppId(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_APP_ID));
        o.setAccessTokenEndpoint(ConfigUtils.getParamString(callData, PARAM_ACCESS_TOKEN_ENDPOINT));
        o.setScope(ConfigUtils.getParamString(callData, PARAM_SCOPE));
        o.setRefreshToken(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_REFRESH_TOKEN));
        return o;
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

    private void discardAuthState() {
        if (this.authState != null) {
            this.authState = null;
        }
    }
}
