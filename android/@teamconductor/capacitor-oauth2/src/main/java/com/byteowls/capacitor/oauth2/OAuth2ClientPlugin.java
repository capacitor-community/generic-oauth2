package com.byteowls.capacitor.oauth2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
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
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.MODE_PRIVATE;

@NativePlugin(requestCodes = { OAuth2ClientPlugin.RC_OAUTH }, name = "OAuth2Client")
public class OAuth2ClientPlugin extends Plugin {

    static final int RC_OAUTH = 2000;

    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_ANDROID_APP_ID = "android.appId";
    private static final String PARAM_ANDROID_CUSTOM_HANDLER_CLASS = "android.customHandlerClass";
    private static final String PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
    private static final String PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
    private static final String PARAM_CUSTOM_SCHEME = "android.customScheme";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_RESOURCE_URL = "resourceUrl";

    private AuthorizationService authService;
    private AuthState authState;

    public OAuth2ClientPlugin() {}

    @PluginMethod()
    public void authenticate(final PluginCall call) {
        String customHandlerClassname = getCallString(call, PARAM_ANDROID_CUSTOM_HANDLER_CLASS);

        if (customHandlerClassname != null && customHandlerClassname.length() > 0) {
            try {
                Class<OAuth2CustomHandler> handlerClass = (Class<OAuth2CustomHandler>) Class.forName(customHandlerClassname);
                OAuth2CustomHandler handler = handlerClass.newInstance();
                handler.getAccessToken(getActivity(), call, new AccessTokenCallback() {
                    @Override
                    public void onSuccess(String accessToken) {
                        new ResourceUrlAsyncTask(call, getLogTag()).execute(accessToken);
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
                e.printStackTrace();
            }

        } else {
            String appId = getCallString(call, PARAM_APP_ID);
            String androidAppId = getCallString(call, PARAM_ANDROID_APP_ID);
            if (androidAppId != null && !androidAppId.isEmpty()) {
                appId = androidAppId;
            }
            String baseUrl = getCallString(call, PARAM_AUTHORIZATION_BASE_URL);
            String accessTokenEndpoint = getCallString(call, PARAM_ACCESS_TOKEN_ENDPOINT); // placeholder
            String customScheme = getCallString(call, PARAM_CUSTOM_SCHEME);

            AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                Uri.parse(baseUrl),
                Uri.parse(accessTokenEndpoint)
            );

            if (this.authState == null) {
                this.authState = new AuthState(config);
            }

            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                config,
                appId,
                ResponseTypeValues.CODE,
                Uri.parse(customScheme)
            );

            AuthorizationRequest req = builder
                .setScope(call.getString(PARAM_SCOPE))
                .setState(call.getString(PARAM_STATE))
                .build();

            this.authService = new AuthorizationService(getContext());
            Intent authIntent = this.authService.getAuthorizationRequestIntent(req);

            startActivityForResult(call, authIntent, RC_OAUTH);
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);
        if (RC_OAUTH == requestCode) {
            final PluginCall savedCall = getSavedCall();
            if (savedCall == null) {
                return;
            }

            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
            AuthorizationException error = AuthorizationException.fromIntent(data);
            this.authState.update(response, error);

            // get authorization code
            if (response != null) {
                this.authService.performTokenRequest(response.createTokenExchangeRequest(),
                    new AuthorizationService.TokenResponseCallback() {
                        @Override
                        public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
                            if (response != null) {
                                authState.update(response, ex);
                                authState.performActionWithFreshTokens(authService, new AuthState.AuthStateAction() {
                                    @Override
                                    public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                                        new ResourceUrlAsyncTask(savedCall, getLogTag()).execute(accessToken);
                                    }
                                });
                            } else {
                                savedCall.reject("No authToken retrieved!");
                            }
                        }
                    });
            }
        }
    }

    private String getCallString(PluginCall call, String key) {
        return getCallString(call, key, null);
    }

    private String getCallString(PluginCall call, String key, String defaultValue) {
        String k = getDeepestKey(key);
        try {
            JSONObject o = getDeepestObject(call.getData(), key);

            String value = o.getString(k);
            if (value == null) {
                return defaultValue;
            }
            return value;
        } catch (Exception ignore) {}
        return defaultValue;
    }

    private String getDeepestKey(String key) {
        String[] parts = key.split("\\.");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    private JSObject getDeepestObject(JSObject o, String key) throws JSONException {
        // Split on periods
        String[] parts = key.split("\\.");
        // Search until the second to last part of the key
        for (int i = 0; i < parts.length-1; i++) {
            String k = parts[i];
            o = o.getJSObject(k);
        }
        return o;
    }

    @NonNull
    public AuthState readAuthState() {
        SharedPreferences authPrefs = getContext().getSharedPreferences("auth", MODE_PRIVATE);
        String stateJson = authPrefs.getString("stateJson", "");
        try {
            return AuthState.jsonDeserialize(stateJson);
        } catch (JSONException ignore) {}
        return new AuthState();
    }

    public void writeAuthState(@NonNull AuthState state) {
        SharedPreferences authPrefs = getContext().getSharedPreferences("auth", MODE_PRIVATE);
        authPrefs
            .edit()
            .putString("stateJson", state.jsonSerializeString())
            .apply();
    }

}
