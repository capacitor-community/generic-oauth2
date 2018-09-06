package com.byteowls.capacitor.oauth2;

import android.content.Intent;
import android.net.Uri;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.github.scribejava.core.builder.ServiceBuilder;

@NativePlugin()
public class OAuth2ClientPlugin extends Plugin {

    public static final String PARAM_API_KEY = "apiKey";
    private static final int PLUGIN_RESULT_CODE = 654788;

    @PluginMethod()
    public void authenticate(PluginCall call) {

        ServiceBuilder serviceBuilder = new ServiceBuilder(call.getString(PARAM_API_KEY));

        Intent intent = new Intent();


//        JSObject ret = new JSObject();
//        ret.put("value", value);
//        call.success(ret);

        startActivityForResult(call, authIntent, PLUGIN_RESULT_CODE);
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
                JSObject response = new JSObject();
                response.put("url", url.toString());

                savedCall.resolve(response);
            } else {
                savedCall.reject("");
            }
        }
    }
}
