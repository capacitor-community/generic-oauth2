package com.byteowls.capacitor.oauth2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONException;

import static android.content.Context.MODE_PRIVATE;

@NativePlugin(requestCodes = { OAuth2ClientPlugin.RC_OAUTH }, name = "OAuth2Client")
public class OAuth2ClientPlugin extends Plugin {

    public static final int RC_OAUTH = 654788;

    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
    private static final String PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
    private static final String PARAM_CUSTOM_SCHEME = "android.customScheme";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_RESOURCE_URL = "resourceUrl";

    private AuthorizationService authService;

    @PluginMethod()
    public void authenticate(PluginCall call) {
        String appId = call.getString(PARAM_APP_ID);
        String baseUrl = call.getString(PARAM_AUTHORIZATION_BASE_URL);
        String accessTokenEndpoint = call.getString(PARAM_ACCESS_TOKEN_ENDPOINT, "https://idp.example.com/token"); // placeholder
        String customScheme = call.getString(PARAM_CUSTOM_SCHEME);

        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
            Uri.parse(baseUrl), // authorization endpoint
            Uri.parse(accessTokenEndpoint)
        );

        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
            config, // the authorization service configuration
            appId, // the client ID, typically pre-registered and static
            ResponseTypeValues.TOKEN, // TODO maybe the response_type value: we want a code
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

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (RC_OAUTH == resultCode) {
            final PluginCall savedCall = getSavedCall();
            if (savedCall == null) {
                return;
            }

            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
            AuthorizationException error = AuthorizationException.fromIntent(data);
            final AuthState authState = new AuthState(response, error);
            authState.performActionWithFreshTokens(this.authService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                    new ResourceTask(savedCall, getLogTag()).execute(accessToken);
                }
            });
        }
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


    private static class ResourceTask extends AsyncTask<String, Void, JSObject> {

        private PluginCall pluginCall;
        private String logTag;

        private ResourceTask(PluginCall pluginCall, String logTag) {
            this.pluginCall = pluginCall;
            this.logTag = logTag;
        }

        @Override
        protected JSObject doInBackground(String... tokens) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                .url(pluginCall.getString(PARAM_RESOURCE_URL))
                .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                .build();
            try {
                okhttp3.Response response = client.newCall(request).execute();
                String jsonBody = response.body().string();
                Log.i(logTag, String.format("User Info Response %s", jsonBody));
                return new JSObject(jsonBody);
            } catch (Exception exception) {
                Log.w(logTag, exception);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSObject jsObject) {
            pluginCall.resolve(jsObject);
        }

    }
}
