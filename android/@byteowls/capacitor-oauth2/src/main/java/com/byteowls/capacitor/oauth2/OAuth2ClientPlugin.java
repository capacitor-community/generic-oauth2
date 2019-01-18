package com.byteowls.capacitor.oauth2;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.byteowls.capacitor.oauth2.handler.AccessTokenCallback;
import com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;

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
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_RESOURCE_URL = "resourceUrl";
    private static final String RESPONSE_TYPE_CODE = "code";
    private static final String RESPONSE_TYPE_TOKEN = "token";
    private static final String PARAM_STATE_DISABLED = "stateDisabled";
    private static final String PARAM_AUTHORIZATION_CODE_ONLY = "authorizationCodeOnly";

    private OAuth2Options oauth2Options;

    public OAuth2ClientPlugin() {}

    @PluginMethod()
    public void authenticate(final PluginCall call) {
        String customHandlerClassname = ConfigUtils.getCallParam(String.class, call, PARAM_ANDROID_CUSTOM_HANDLER_CLASS);
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
                Log.e(getLogTag(), "Custom handler problem", e);
            }
        } else {
            oauth2Options = buildOptions(call);

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


            Intent authIntent = new Intent(Intent.ACTION_VIEW);
            // maybe use the options from a customTabsIntent
//            authIntent.setPackage(getContext().getPackageName());
//            authIntent.setComponent(getActivity().getComponentName());
            authIntent.setData(Uri.parse(getAuthorizationUrl(oauth2Options)));
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
            // clear any
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

            Uri authorizationResponseUri = data.getData();
            String returnedState = authorizationResponseUri.getQueryParameter(PARAM_STATE);
            if (oauth2Options.isStateDisabled() || oauth2Options.getState().equals(returnedState)) {
                if (RESPONSE_TYPE_TOKEN.equals(oauth2Options.getResponseType())) {

                } else if (RESPONSE_TYPE_CODE.equals(oauth2Options.getResponseType())) {
                    // google uses code for android and ios but without requireing us to send the secret

                } else {
                    savedCall.reject("Not supported responseType");
                    return;
                }
            } else {
                savedCall.reject("State check not passed! Retrieved state does not match sent one!");
                return;
            }

            // TODO learn from authNet how to handle the rquest
//            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
//            AuthorizationException error = AuthorizationException.fromIntent(data);

            // get authorization code
//            if (response != null) {
//                this.authService = new AuthorizationService(getContext());
//                // TODO implicit code flow has never worked because this needs the authorizationCode
//                this.authService.performTokenRequest(response.createTokenExchangeRequest(),
//                    new AuthorizationService.TokenResponseCallback() {
//                        @Override
//                        public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
//                            if (response != null) {
//                                authState.update(response, ex);
//                                authState.performActionWithFreshTokens(authService, new AuthState.AuthStateAction() {
//                                    @Override
//                                    public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
//                                        new ResourceUrlAsyncTask(savedCall, getLogTag()).execute(accessToken);
//                                    }
//                                });
//                            } else {
//                                savedCall.reject("No authToken retrieved!");
//                            }
//                        }
//                    });
//            }
        }
    }

    protected OAuth2Options buildOptions(PluginCall call) {
        OAuth2Options o = new OAuth2Options();
        o.setAppId(getOverwritableParam(String.class, call, PARAM_APP_ID));
        o.setAuthorizationBaseUrl(ConfigUtils.getCallString(call, PARAM_AUTHORIZATION_BASE_URL));
        o.setAccessTokenEndpoint(ConfigUtils.getCallString(call, PARAM_ACCESS_TOKEN_ENDPOINT));
        o.setResourceUrl(ConfigUtils.getCallString(call, PARAM_RESOURCE_URL));
        o.setResponseType(getOverwritableParam(String.class, call, PARAM_RESPONSE_TYPE));
        o.setScope(ConfigUtils.getCallString(call, PARAM_SCOPE));
        o.setState(ConfigUtils.getCallString(call, PARAM_STATE));
        o.setStateDisabled(ConfigUtils.getCallParam(Boolean.class, call, PARAM_STATE_DISABLED, false));
        if (!o.isStateDisabled()) {
            if (o.getState() == null) {
                o.setState(ConfigUtils.getRandomString(20));
            }
        } else {
            o.setState(null);
        }
        o.setAuthorizationCodeOnly(ConfigUtils.getCallParam(Boolean.class, call, OAuth2ClientPlugin.PARAM_AUTHORIZATION_CODE_ONLY, false));
        if (o.isAuthorizationCodeOnly()) {
            if (!RESPONSE_TYPE_CODE.equals(o.getResponseType())) {
                Log.w(getLogTag(), "'" + PARAM_AUTHORIZATION_CODE_ONLY + "' is 'true' so '" + PARAM_RESPONSE_TYPE + "' must be 'code'! We fix that for you.");
            }
            o.setResponseType(RESPONSE_TYPE_CODE);
        }

        if (o.getResponseType() == null || o.getResponseType().length() == 0) {
            // fallback to token
            o.setResponseType(RESPONSE_TYPE_TOKEN);
        }
        o.setRedirectUrl(ConfigUtils.getCallString(call, PARAM_ANDROID_CUSTOM_SCHEME));
        o.setCustomHandlerClass(ConfigUtils.getCallString(call, PARAM_ANDROID_CUSTOM_HANDLER_CLASS));
        return o;
    }

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
}
