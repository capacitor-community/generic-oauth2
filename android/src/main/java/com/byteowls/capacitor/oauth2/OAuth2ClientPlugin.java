package com.byteowls.capacitor.oauth2;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.activity.result.ActivityResult;

import com.byteowls.capacitor.oauth2.handler.AccessTokenCallback;
import com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.EndSessionRequest;
import net.openid.appauth.EndSessionResponse;
import net.openid.appauth.GrantTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.util.Map;

@CapacitorPlugin(name = "OAuth2Client")
public class OAuth2ClientPlugin extends Plugin {

    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
    private static final String PARAM_RESPONSE_TYPE = "responseType";
    private static final String PARAM_REDIRECT_URL = "redirectUrl";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";

    private static final String PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
    private static final String PARAM_PKCE_ENABLED = "pkceEnabled";
    private static final String PARAM_RESOURCE_URL = "resourceUrl";
    private static final String PARAM_ADDITIONAL_RESOURCE_HEADERS = "additionalResourceHeaders";
    private static final String PARAM_ADDITIONAL_PARAMETERS = "additionalParameters";
    private static final String PARAM_ANDROID_CUSTOM_HANDLER_CLASS = "android.customHandlerClass";
    // Activity result handling
    private static final String PARAM_ANDROID_HANDLE_RESULT_ON_NEW_INTENT = "android.handleResultOnNewIntent";
    private static final String PARAM_ANDROID_HANDLE_RESULT_ON_ACTIVITY_RESULT = "android.handleResultOnActivityResult";

    // Refresh token params
    private static final String PARAM_REFRESH_TOKEN = "refreshToken";

    // open id params
    private static final String PARAM_DISPLAY = "display";
    private static final String PARAM_LOGIN_HINT = "login_hint";
    private static final String PARAM_PROMPT = "prompt";
    private static final String PARAM_RESPONSE_MODE = "response_mode";
    private static final String PARAM_LOGS_ENABLED = "logsEnabled";

    private static final String PARAM_LOGOUT_URL = "logoutUrl";
    private static final String PARAM_ID_TOKEN = "id_token";

    private static final String USER_CANCELLED = "USER_CANCELLED";

    private static final String ERR_PARAM_NO_APP_ID = "ERR_PARAM_NO_APP_ID";
    private static final String ERR_PARAM_NO_AUTHORIZATION_BASE_URL = "ERR_PARAM_NO_AUTHORIZATION_BASE_URL";
    private static final String ERR_PARAM_NO_REDIRECT_URL = "ERR_PARAM_NO_REDIRECT_URL";
    private static final String ERR_PARAM_NO_RESPONSE_TYPE = "ERR_PARAM_NO_RESPONSE_TYPE";

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
    private String callbackId;

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
                String message = ex.error != null ? ex.error : ERR_GENERAL;
                call.reject(message, String.valueOf(ex.code), ex);
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
        this.callbackId = call.getCallbackId();
        disposeAuthService();
        oauth2Options = buildAuthenticateOptions(call.getData());
        if (oauth2Options.getCustomHandlerClass() != null) {
            if (oauth2Options.isLogsEnabled()) {
                Log.i(getLogTag(), "Entering custom handler: " + oauth2Options.getCustomHandlerClass().getClass().getName());
            }
            try {
                Class<OAuth2CustomHandler> handlerClass = (Class<OAuth2CustomHandler>) Class.forName(oauth2Options.getCustomHandlerClass());
                OAuth2CustomHandler handler = handlerClass.newInstance();
                handler.getAccessToken(getActivity(), call, new AccessTokenCallback() {
                    @Override
                    public void onSuccess(String accessToken) {
                        new ResourceUrlAsyncTask(call, oauth2Options, getLogTag(), null, null).execute(accessToken);
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

            // ###################################
            // ### Validate required parameter ###
            // ###################################

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

            // ### Configure

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
            if (oauth2Options.isPkceEnabled()) {
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
                this.bridge.saveCall(call);
                startActivityForResult(call, authIntent, "handleIntentResult");
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
            String idToken = ConfigUtils.getParam(String.class, call.getData(), PARAM_ID_TOKEN);
            if (idToken == null) {
                this.disposeAuthService();
                this.discardAuthState();
                call.resolve();
                return;
            }

            oauth2Options = buildAuthenticateOptions(call.getData());

            Uri authorizationUri = Uri.parse(oauth2Options.getAuthorizationBaseUrl());
            Uri accessTokenUri;
            if (oauth2Options.getAccessTokenEndpoint() != null) {
                accessTokenUri = Uri.parse(oauth2Options.getAccessTokenEndpoint());
            } else {
                // appAuth does not allow to be the accessTokenUri empty although it is not used unit performTokenRequest
                accessTokenUri = authorizationUri;
            }
            Uri logoutUri = Uri.parse(oauth2Options.getLogoutUrl());

            AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(authorizationUri, accessTokenUri);

            EndSessionRequest endSessionRequest =
                new EndSessionRequest.Builder(config)
                    .setIdTokenHint(idToken)
                    .setPostLogoutRedirectUri(logoutUri)
                    .build();

            this.authService = new AuthorizationService(getContext());

            try {
                Intent endSessionIntent = authService.getEndSessionRequestIntent(endSessionRequest);
                this.bridge.saveCall(call);
                startActivityForResult(call, endSessionIntent, "handleEndSessionIntentResult");
            } catch (ActivityNotFoundException e) {
                call.reject(ERR_ANDROID_NO_BROWSER, e);
            } catch (Exception e) {
                Log.e(getLogTag(), "Unexpected exception on open browser for logout request!");
                call.reject(ERR_GENERAL, e);
            }
        }
    }

    @Override
    protected void handleOnNewIntent(Intent intent) {
        // this is a experimental hook and only usable if the android system kills the app between
        if (this.oauth2Options != null && this.oauth2Options.isHandleResultOnNewIntent()) {
            // with this I have no way to check if this intent is for this plugin
            PluginCall savedCall = this.bridge.getSavedCall(this.callbackId);
            if (savedCall == null) {
                return;
            }
            handleAuthorizationRequestActivity(intent, savedCall);
        }
    }

    @ActivityCallback
    private void handleIntentResult(PluginCall call, ActivityResult result) {
        if (this.oauth2Options != null && this.oauth2Options.isHandleResultOnActivityResult()) {
            if (result.getResultCode() == Activity.RESULT_CANCELED) {
                call.reject(USER_CANCELLED);
            } else {
                handleAuthorizationRequestActivity(result.getData(), call);
            }
        }
    }

    @ActivityCallback
    private void handleEndSessionIntentResult(PluginCall call, ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_CANCELED) {
            call.reject(USER_CANCELLED);
        } else {
            if (result.getData() != null) {
                try {
                    EndSessionResponse resp = EndSessionResponse.fromIntent(result.getData());
                    JSObject json = new JSObject(resp.jsonSerializeString());

                    this.disposeAuthService();
                    this.discardAuthState();

                    call.resolve(json);
                } catch (Exception e) {
                    Log.e(getLogTag(), "Unexpected exception on handling result for logout request!");
                    call.reject(ERR_GENERAL, e);
                    return;
                }
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
                    if (oauth2Options.isLogsEnabled()) {
                        Log.i(getLogTag(), "State from web options: " + oauth2Options.getState());
                        if (authorizationResponse != null) {
                            Log.i(getLogTag(), "State returned from provider: " + authorizationResponse.state);
                        }
                    }
                    savedCall.reject(ERR_STATES_NOT_MATCH);
                } else {
                    savedCall.reject(ERR_GENERAL, error);
                }
                return;
            }

            // this response may contain the authorizationCode but also idToken and accessToken depending on the flow chosen by responseType
            if (authorizationResponse != null) {
                if (oauth2Options.isLogsEnabled()) {
                    Log.i(getLogTag(), "Authorization response:\n" + authorizationResponse.jsonSerializeString());
                }
                // if there is a tokenEndpoint configured try to get the accessToken from it.
                // it might be already in the authorizationResponse but tokenEndpoint might deliver other tokens.
                if (oauth2Options.getAccessTokenEndpoint() != null) {
                    this.authService = new AuthorizationService(getContext());
                    TokenRequest tokenExchangeRequest;
                    try {
                        tokenExchangeRequest = authorizationResponse.createTokenExchangeRequest();
                        this.authService.performTokenRequest(tokenExchangeRequest, (accessTokenResponse, exception) -> {
                            authState.update(accessTokenResponse, exception);
                            if (exception != null) {
                                savedCall.reject(ERR_AUTHORIZATION_FAILED, String.valueOf(exception.code), exception);
                            } else {
                                if (accessTokenResponse != null) {
                                    if (oauth2Options.isLogsEnabled()) {
                                        Log.i(getLogTag(), "Access token response:\n" + accessTokenResponse.jsonSerializeString());
                                    }
                                    authState.performActionWithFreshTokens(authService,
                                        (accessToken, idToken, ex1) -> {
                                            AsyncTask<String, Void, ResourceCallResult> asyncTask =
                                                new ResourceUrlAsyncTask(
                                                    savedCall,
                                                    oauth2Options,
                                                    getLogTag(),
                                                    authorizationResponse,
                                                    accessTokenResponse);
                                            asyncTask.execute(accessToken);
                                        });
                                } else {
                                    resolveAuthorizationResponse(savedCall, authorizationResponse);
                                }
                            }
                        });
                    } catch (Exception e) {
                        savedCall.reject(ERR_NO_AUTHORIZATION_CODE, e);
                    }
                } else {
                    resolveAuthorizationResponse(savedCall, authorizationResponse);
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

    private void resolveAuthorizationResponse(PluginCall savedCall, AuthorizationResponse authorizationResponse) {
        JSObject json = new JSObject();
        OAuth2Utils.assignResponses(json, null, authorizationResponse, null);
        savedCall.resolve(json);
    }

    OAuth2Options buildAuthenticateOptions(JSObject callData) {
        OAuth2Options o = new OAuth2Options();
        // required
        o.setAppId(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_APP_ID)));
        o.setAuthorizationBaseUrl(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_AUTHORIZATION_BASE_URL)));
        o.setResponseType(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_RESPONSE_TYPE)));
        o.setRedirectUrl(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_REDIRECT_URL)));

        // optional
        Boolean logsEnabled = ConfigUtils.getOverwrittenAndroidParam(Boolean.class, callData, PARAM_LOGS_ENABLED);
        o.setLogsEnabled(logsEnabled != null && logsEnabled);
        o.setResourceUrl(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_RESOURCE_URL)));
        o.setAccessTokenEndpoint(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_ACCESS_TOKEN_ENDPOINT)));
        Boolean pkceEnabledObj = ConfigUtils.getOverwrittenAndroidParam(Boolean.class, callData, PARAM_PKCE_ENABLED);
        o.setPkceEnabled(pkceEnabledObj != null && pkceEnabledObj);
        if (o.isPkceEnabled()) {
            o.setPkceCodeVerifier(ConfigUtils.getRandomString(64));
        }

        o.setScope(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_SCOPE)));
        o.setState(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_STATE)));
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
        o.setAdditionalResourceHeaders(ConfigUtils.getOverwrittenAndroidParamMap(callData, PARAM_ADDITIONAL_RESOURCE_HEADERS));
        // android only
        o.setCustomHandlerClass(ConfigUtils.trimToNull(ConfigUtils.getParamString(callData, PARAM_ANDROID_CUSTOM_HANDLER_CLASS)));
        o.setHandleResultOnNewIntent(ConfigUtils.getParam(Boolean.class, callData, PARAM_ANDROID_HANDLE_RESULT_ON_NEW_INTENT, false));
        o.setHandleResultOnActivityResult(ConfigUtils.getParam(Boolean.class, callData, PARAM_ANDROID_HANDLE_RESULT_ON_ACTIVITY_RESULT, false));
        if (!o.isHandleResultOnNewIntent() && !o.isHandleResultOnActivityResult()) {
            o.setHandleResultOnActivityResult(true);
        }
        return o;
    }

    OAuth2RefreshTokenOptions buildRefreshTokenOptions(JSObject callData) {
        OAuth2RefreshTokenOptions o = new OAuth2RefreshTokenOptions();
        o.setAppId(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_APP_ID)));
        o.setAccessTokenEndpoint(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_ACCESS_TOKEN_ENDPOINT)));
        o.setScope(ConfigUtils.trimToNull(ConfigUtils.getOverwrittenAndroidParam(String.class, callData, PARAM_SCOPE)));
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
