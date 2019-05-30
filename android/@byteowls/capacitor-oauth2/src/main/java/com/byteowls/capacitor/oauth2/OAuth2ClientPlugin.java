package com.byteowls.capacitor.oauth2;

import android.content.ActivityNotFoundException;
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
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;
import org.json.JSONException;

import java.util.Map;

@NativePlugin(requestCodes = { OAuth2ClientPlugin.REQ_OAUTH_AUTHORIZATION}, name = "OAuth2Client")
public class OAuth2ClientPlugin extends Plugin {

    static final int REQ_OAUTH_AUTHORIZATION = 2000;

    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_RESPONSE_TYPE = "responseType";
    private static final String PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
    private static final String PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
    private static final String PARAM_ADDITIONAL_PARAMETERS = "additionalParameters";
    private static final String PARAM_PKCE_DISABLED = "pkceDisabled";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_RESOURCE_URL = "resourceUrl";
    private static final String RESPONSE_TYPE_CODE = "code";
    private static final String RESPONSE_TYPE_TOKEN = "token";
    private static final String PARAM_ANDROID_CUSTOM_HANDLER_CLASS = "android.customHandlerClass";
    private static final String PARAM_ANDROID_CUSTOM_SCHEME = "android.customScheme";

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

    private static final String ERR_NO_ACCESS_TOKEN = "ERR_NO_ACCESS_TOKEN";
    private static final String ERR_ANDROID_NO_BROWSER = "ERR_ANDROID_NO_BROWSER";

    private static final String ERR_CUSTOM_HANDLER_LOGIN = "ERR_CUSTOM_HANDLER_LOGIN";
    private static final String ERR_CUSTOM_HANDLER_LOGOUT = "ERR_CUSTOM_HANDLER_LOGOUT";

    private static final String ERR_GENERAL = "ERR_GENERAL";
    private static final String ERR_STATES_NOT_MATCH = "ERR_STATES_NOT_MATCH";
    private static final String ERR_NO_AUTHORIZATION_CODE = "ERR_NO_AUTHORIZATION_CODE";


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
                        call.reject(USER_CANCELLED);
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(getLogTag(), ERR_CUSTOM_HANDLER_LOGIN + ": {}", error);
                        call.reject(ERR_CUSTOM_HANDLER_LOGIN);
                    }
                });
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                Log.e(getLogTag(), ERR_CUSTOM_HANDLER_LOGIN + ": {}", e);
                call.reject(ERR_CUSTOM_HANDLER_LOGIN);
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
                call.reject(ERR_ANDROID_NO_BROWSER);
            }
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
                    call.reject(ERR_CUSTOM_HANDLER_LOGOUT);
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                Log.e(getLogTag(), ERR_CUSTOM_HANDLER_LOGOUT, e);
                call.reject(ERR_CUSTOM_HANDLER_LOGOUT);
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
                TokenRequest tokenExchangeRequest = null;
                try {
                    tokenExchangeRequest = response.createTokenExchangeRequest();
                    this.authService.performTokenRequest(tokenExchangeRequest,
                        new AuthorizationService.TokenResponseCallback() {
                            @Override
                            public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
                                authState.update(response, ex);
                                if (ex != null) {
                                    savedCall.reject(ERR_GENERAL, ex);
                                } else {
                                    if (response != null) {
                                        if (oauth2Options.getResourceUrl() != null) {
                                            authState.performActionWithFreshTokens(authService, new AuthState.AuthStateAction() {
                                                @Override
                                                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                                                    new ResourceUrlAsyncTask(savedCall, oauth2Options, getLogTag()).execute(accessToken);
                                                }
                                            });
                                        } else {
                                            try {
                                                JSObject json = new JSObject(response.jsonSerializeString());
                                                savedCall.resolve(json);
                                            } catch (JSONException e) {
                                                savedCall.reject(ERR_GENERAL, e);
                                            }
                                        }
                                    } else {
                                        savedCall.reject(ERR_NO_ACCESS_TOKEN);
                                    }
                                }

                            }
                        });
                } catch (IllegalStateException e) {
                    savedCall.reject(ERR_NO_AUTHORIZATION_CODE);
                }
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

        Map<String, String> additionalParameters = getOverwritableParamMap(call, PARAM_ADDITIONAL_PARAMETERS);
        if (additionalParameters != null && !additionalParameters.isEmpty()) {
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
        o.setRedirectUrl(ConfigUtils.getCallString(call, PARAM_ANDROID_CUSTOM_SCHEME));
        o.setCustomHandlerClass(ConfigUtils.getCallString(call, PARAM_ANDROID_CUSTOM_HANDLER_CLASS));
        return o;
    }

    protected <T> T getOverwritableParam(Class<T> clazz, PluginCall call, String key) {
        T baseParam = ConfigUtils.getCallParam(clazz, call, key);
        T androidParam = ConfigUtils.getCallParam(clazz, call, "android." + key);
        if (androidParam != null) {
            baseParam = androidParam;
        }
        return baseParam;
    }

    protected Map<String, String> getOverwritableParamMap(PluginCall call, String key) {
        Map<String, String> baseParam = ConfigUtils.getCallParamMap(call, key);
        Map<String, String> androidParam = ConfigUtils.getCallParamMap(call, "android." + key);
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
