package com.byteowls.capacitor.oauth2.api;

import android.app.Activity;
import com.getcapacitor.PluginCall;

/**
 *
 * @author m.oberwasserlechner@byteowls.com
 */
public interface OAuth2CustomHandler {

    void getAccessToken(final Activity activity, final PluginCall pluginCall, final AccessTokenCallback callback);

    boolean logout(final PluginCall pluginCall);

}
