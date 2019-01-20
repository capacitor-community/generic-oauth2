package com.byteowls.capacitor.oauth2.login;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import com.byteowls.capacitor.oauth2.login.ct.CustomTabMainActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoginManager {

    private static final int CUSTOM_TAB_REQUEST_CODE = 1;
    private static final int CHALLENGE_LENGTH = 20;
    private static final int API_EC_DIALOG_CANCEL = 4201;
    private static final String CUSTOM_TABS_SERVICE_ACTION =
        "android.support.customtabs.action.CustomTabsService";
    private static final String[] CHROME_PACKAGES = {
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
    };

    private static volatile LoginManager instance;
    private static final String PREFERENCE_LOGIN_MANAGER = "com.byteowls.capacitor.oauth2.loginManager";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private String currentPackage;

    LoginManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREFERENCE_LOGIN_MANAGER, Context.MODE_PRIVATE);
    }

    /**
     * Getter for the login manager.
     * @return The login manager.
     */
    public static LoginManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LoginManager.class) {
                if (instance == null) {
                    instance = new LoginManager(context);
                }
            }
        }
        return instance;
    }

    public void logIn(final Activity activity, final String authorizationUrl) {
        startLogin(activity, authorizationUrl);

    }

    void startLogin(final Activity activity, final String authorizationUrl) throws LoginException {
        boolean started = tryOAuth2Activity(activity, authorizationUrl);
        if (!started) {

        }
    }

    private boolean tryOAuth2Activity(final Activity activity, final String authorizationUrl) {
        Intent intent = getLoginIntent(authorizationUrl);

        if (!resolveIntent(intent)) {
            return false;
        }

        try {
            activity.startActivityForResult(
                intent,
                2000);
        } catch (ActivityNotFoundException e) {
            return false;
        }

        return true;
    }

    private Intent getLoginIntent(String authorizationUrl) {
        Intent intent = new Intent(this.context, CustomTabMainActivity.class);
        intent.putExtra(CustomTabMainActivity.EXTRA_AUTHORIZATION_URL, authorizationUrl);
        intent.putExtra(CustomTabMainActivity.EXTRA_CHROME_PACKAGE, getChromePackage());
        return intent;
    }

    private String getChromePackage() {
        if (currentPackage != null) {
            return currentPackage;
        }
        Intent serviceIntent = new Intent(CUSTOM_TABS_SERVICE_ACTION);
        List<ResolveInfo> resolveInfos =
            context.getPackageManager().queryIntentServices(serviceIntent, 0);
        if (resolveInfos != null) {
            Set<String> chromePackages = new HashSet<>(Arrays.asList(CHROME_PACKAGES));
            for (ResolveInfo resolveInfo : resolveInfos) {
                ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                if (serviceInfo != null && chromePackages.contains(serviceInfo.packageName)) {
                    currentPackage = serviceInfo.packageName;
                    return currentPackage;
                }
            }
        }
        return null;
    }

    private boolean resolveIntent(Intent intent) {
        ResolveInfo resolveInfo = this.context.getPackageManager()
            .resolveActivity(intent, 0);
        return resolveInfo != null;
    }

}
