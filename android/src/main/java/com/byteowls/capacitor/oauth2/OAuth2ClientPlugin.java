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

    static final int REQ_OAUTH_AUTHORIZATION = 12548894;

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
    private static final String PARAM_ANDROID_HANDLE_RESULT_METHOD = "android.handleResultMethod";

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
    private static final String ERR_PARAM_NO_RESPONSE_TYPE = "ERR_PARAM_NO_RESPONSE_TYPE";

    private static final String ERR_PARAM_INVALID_RESPONSE_TYPE = "ERR_PARAM_INVALID_RESPONSE_TYPE";

    private static final String ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT = "ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT";
    private static final String ERR_PARAM_NO_REFRESH_TOKEN = "ERR_PARAM_NO_REFRESH_TOKEN";

    private static final String ERR_AUTHORIZATION_FAILED = "ERR_AUTHORIZATION_FAILED";
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
        if (oauth2Options.getCustomHandlerClass() != null) {
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

            if (oauth2Options.getResponseType() == null) {
                call.reject(ERR_PARAM_NO_RESPONSE_TYPE);
                return;
            }

            if (oauth2Options.getRedirectUrl() == null) {
                call.reject(ERR_PARAM_NO_REDIRECT_URL);
                return;
            }

            // TODO remove check #46, #48, #49
            if (!RESPONSE_TYPE_CODE.equals(oauth2Options.getResponseType()) && !RESPONSE_TYPE_TOKEN.equals(oauth2Options.getResponseType())) {
                call.reject(ERR_PARAM_INVALID_RESPONSE_TYPE);
                return;
            }

            // TODO remove check. If there is a accessTokenEndpoint given the plugin will try it regardless of the given responseType
            if (RESPONSE_TYPE_CODE.equals(oauth2Options.getResponseType()) && oauth2Options.getAccessTokenEndpoint() == null) {
                call.reject(ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT);
                return;
            }

            Uri authorizationUri = Uri.parse(oauth2Options.getAuthorizationBaseUrl());
            Uri accessTokenUri;
            if (oauth2Options.getAccessTokenEndpoint() != null) {
                accessTokenUri = Uri.parse(oauth2Options.getAccessTokenEndpoint());
            } else {
                // appAuth does not allow to be the accessTokenUri empty although it is not used unit performTokenRequest
                accessTokenUri = authorizationUri;
            }

            AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(authorizationUri, accessTokenUri);

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
            } catch (Exception e) {
                Log.e(getLogTag(), "Unexpected exception on open browser for authorization request!");
                call.reject(ERR_GENERAL, e);
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
    protected void handleOnNewIntent(Intent intent) {
        if (this.oauth2Options.getHandleResultMethod() == OAuth2Options.HandleResultMethod.NEW || this.oauth2Options.getHandleResultMethod() == OAuth2Options.HandleResultMethod.BOTH) {
            // with this I have no way to check if this intent is for this plugin
            PluginCall savedCall = getSavedCall();
            if (savedCall == null) {
                return;
            }
            handleAuthorizationRequestActivity(intent, getSavedCall());
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent intent) {
        super.handleOnActivityResult(requestCode, resultCode, intent);
        if (this.oauth2Options.getHandleResultMethod() == OAuth2Options.HandleResultMethod.OLD || this.oauth2Options.getHandleResultMethod() == OAuth2Options.HandleResultMethod.BOTH) {
            if (REQ_OAUTH_AUTHORIZATION == requestCode) {
                PluginCall savedCall = getSavedCall();

                if (savedCall == null) {
                    return;
                }
                handleAuthorizationRequestActivity(intent, getSavedCall());
            }
        }
    }

    void handleAuthorizationRequestActivity(Intent intent, PluginCall savedCall) {
        // there are valid situation when the Intent is null, but
        if (intent != null) {
            AuthorizationResponse authorizationResponse;
            AuthorizationException error;
            try {
                authorizationResponse = AuthorizationResponse.fromIntent(intent);
                error = AuthorizationException.fromIntent(intent);
                this.authState.update(authorizationResponse, error);
            } catch (Exception e) {
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

            // this response may contain the authorizationCode but also idToken and accessToken depending on the flow chosen by responseType
            if (authorizationResponse != null) {
                // if the accessToken already exists because of the implicit flow
                if (authorizationResponse.accessToken != null) {
                    // we try to make the resource call if there was a resource url provided
                    if (oauth2Options.getResourceUrl() != null) {
                        ResourceUrlAsyncTask asyncTask = new ResourceUrlAsyncTask(savedCall, oauth2Options, getLogTag());
                        asyncTask.execute(authorizationResponse.accessToken);
                    } else {
                        // if there is no resource url we resolve the call with the whole response incl. the accessToken
                        createJsObjAndResolve(savedCall, authorizationResponse.jsonSerializeString());
                    }
                } else {
                    // if no accessToken was found it was
                    if (oauth2Options.getAccessTokenEndpoint() != null) {

                        this.authService = new AuthorizationService(getContext());
                        TokenRequest tokenExchangeRequest;
                        try {
                            tokenExchangeRequest = authorizationResponse.createTokenExchangeRequest();
                            this.authService.performTokenRequest(tokenExchangeRequest, (accessTokenResponse, exception) -> {
                                authState.update(accessTokenResponse, exception);
                                if (exception != null) {
                                    savedCall.reject(ERR_AUTHORIZATION_FAILED, exception);
                                } else {
                                    if (accessTokenResponse != null) {
                                        if (oauth2Options.getResourceUrl() != null) {
                                            authState.performActionWithFreshTokens(authService, (accessToken, idToken, ex1)
                                                -> new ResourceUrlAsyncTask(savedCall, oauth2Options, getLogTag()).execute(accessToken));
                                        } else {
                                            createJsObjAndResolve(savedCall, accessTokenResponse.jsonSerializeString());
                                        }
                                    } else {
                                        savedCall.reject(ERR_NO_ACCESS_TOKEN);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            savedCall.reject(ERR_NO_AUTHORIZATION_CODE, e);
                        }
                    } else {
                        createJsObjAndResolve(savedCall, authorizationResponse.jsonSerializeString());
                    }
                }
            } else {
                savedCall.reject(ERR_NO_AUTHORIZATION_CODE);
            }
        } else {
            // the intent is null because the provider send the redirect to the server, which would be valid
            // the intent is null because the plugin user configured sth wrong incl.
            // the provider does not support redirecting to a android app, which would be invalid
            savedCall.reject(ERR_ANDROID_RESULT_NULL);
        }
    }

    void createJsObjAndResolve(PluginCall call, String jsonStr) {
        try {
            JSObject json = new JSObject(jsonStr);
            call.resolve(json);
        } catch (JSONException e) {
            call.reject(ERR_GENERAL, e);
        }
    }

    OAuth2Options buildAuthenticateOptions(JSObject callData) {
        OAuth2Options o = new OAuth2Options();
        // required
        o.setAppId(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_APP_ID)));
        o.setAuthorizationBaseUrl(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_AUTHORIZATION_BASE_URL)));
        o.setResponseType(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_RESPONSE_TYPE)));
        if (o.getResponseType() == null) {
            // fallback to token
            o.setResponseType(RESPONSE_TYPE_TOKEN);
        }
        // TODO #84 base redirectUrl
        o.setRedirectUrl(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_ANDROID_CUSTOM_SCHEME)));

        // optional
        o.setResourceUrl(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_RESOURCE_URL)));
        o.setAccessTokenEndpoint(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_ACCESS_TOKEN_ENDPOINT)));
        o.setPkceDisabled(ConfigUtils.getOverwrittenAndroidParam(Boolean.class, callData, PARAM_PKCE_DISABLED));
        if (RESPONSE_TYPE_CODE.equals(o.getResponseType())) {
            if (!o.isPkceDisabled()) {
                o.setPkceCodeVerifier(ConfigUtils.getRandomString(64));
            }
        }

        o.setScope(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_SCOPE)));
        o.setState(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_STATE)));
        if (o.getState() == null) {
            o.setState(ConfigUtils.getRandomString(20));
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
        o.setCustomHandlerClass(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_ANDROID_CUSTOM_HANDLER_CLASS)));

        String methodStr = ConfigUtils.getParamString(callData, PARAM_ANDROID_HANDLE_RESULT_METHOD);
        if (methodStr != null) {
            try {
                OAuth2Options.HandleResultMethod handleResultMethod = OAuth2Options.HandleResultMethod.valueOf(methodStr.toUpperCase());
                o.setHandleResultMethod(handleResultMethod);
            } catch (IllegalArgumentException ignore) {

            }
        }

        if (o.getHandleResultMethod() == null) {
            o.setHandleResultMethod(OAuth2Options.HandleResultMethod.NEW);
        }
        return o;
    }

    OAuth2RefreshTokenOptions buildRefreshTokenOptions(JSObject callData) {
        OAuth2RefreshTokenOptions o = new OAuth2RefreshTokenOptions();
        o.setAppId(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_APP_ID)));
        o.setAccessTokenEndpoint(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_ACCESS_TOKEN_ENDPOINT)));
        o.setScope(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_SCOPE)));
        o.setRefreshToken(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_REFRESH_TOKEN)));
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
