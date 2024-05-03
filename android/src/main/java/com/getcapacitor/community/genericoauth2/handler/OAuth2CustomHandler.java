package com.getcapacitor.community.genericoauth2.handler;

import android.app.Activity;
import com.getcapacitor.PluginCall;

public interface OAuth2CustomHandler {
    void getAccessToken(Activity activity, PluginCall pluginCall, final AccessTokenCallback callback);

    boolean logout(Activity activity, PluginCall pluginCall);
}
