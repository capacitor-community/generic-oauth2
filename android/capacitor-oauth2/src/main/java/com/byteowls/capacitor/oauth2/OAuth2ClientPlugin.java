package com.byteowls.capacitor.oauth2;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;

@NativePlugin()
public class OAuth2ClientPlugin extends Plugin {

    private static final String PARAM_API_KEY = "apiKey";
    private static final String PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
    private static final String PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
    private static final int PLUGIN_RESULT_CODE = 654788;

    private OAuth20Service service;

    @PluginMethod()
    public void authenticate(PluginCall call) {
        String apiKey = call.getString(PARAM_API_KEY);
        String endpoint = call.getString(PARAM_ACCESS_TOKEN_ENDPOINT);
        String baseUrl = call.getString(PARAM_AUTHORIZATION_BASE_URL);

        ServiceBuilder serviceBuilder = new ServiceBuilder(apiKey)
            .responseType("token");
        GenericApi20 genericApi20 = new GenericApi20(endpoint, baseUrl);
        this.service = serviceBuilder.build(genericApi20);

        Intent intent = new Intent();
//        JSObject ret = new JSObject();
//        ret.put("value", value);
//        call.success(ret);
        startActivityForResult(call, intent, PLUGIN_RESULT_CODE);
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        PluginCall savedCall = getSavedCall();

        if (savedCall == null) {
            return;
        }
        if (PLUGIN_RESULT_CODE == resultCode) {
            Uri url = data.getData();
            if (url != null) {
                Log.i(getLogTag(), url.toString());
                JSObject response = new JSObject();
                response.put("url", url.toString());
                savedCall.resolve(response);
            } else {
                savedCall.reject("");
            }
        }
    }
}
