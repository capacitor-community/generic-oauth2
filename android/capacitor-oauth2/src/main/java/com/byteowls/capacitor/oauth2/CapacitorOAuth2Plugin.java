package com.byteowls.capacitor.oauth2;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

@NativePlugin()
public class CapacitorOAuth2Plugin extends Plugin {

    @PluginMethod()
    public void authenticate(PluginCall call) {

        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }
}
