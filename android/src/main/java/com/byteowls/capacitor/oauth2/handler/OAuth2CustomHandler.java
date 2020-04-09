package com.byteowls.capacitor.oauth2.handler;

import android.app.Activity;
import com.getcapacitor.PluginCall;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public interface OAuth2CustomHandler {

    void getAccessToken(Activity activity, PluginCall pluginCall, final AccessTokenCallback callback);

    boolean logout(Activity activity, PluginCall pluginCall);

}
