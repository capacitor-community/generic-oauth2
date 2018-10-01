# Capacitor OAuth 2 client plugin

[![npm](https://img.shields.io/npm/v/@teamconductor/capacitor-oauth2.svg)](https://www.npmjs.com/package/@teamconductor/capacitor-oauth2)
[![npm](https://img.shields.io/npm/dt/@teamconductor/capacitor-oauth2.svg?label=npm%20downloads)](https://www.npmjs.com/package/@teamconductor/capacitor-oauth2) [![Twitter Follow](https://img.shields.io/twitter/follow/michaelowl_web.svg?style=social&label=Follow&style=flat-square)](https://twitter.com/michaelowl_web)

This is a simple OAuth 2 client plugin.

It let you configure the oauth parameters yourself instead of using SDKs. Therefore it is usable with various providers.

## Installation

`npm i @teamconductor/capacitor-oauth2`

## Configuration

This example shows the common process of configuring this plugin.

Although it was taken from a Angular 6 application, it should work in other frameworks as well.

### Register plugin

Find the init component of your app, which is in Angular `app.component.ts` and register this plugin by

```
import {registerWebPlugin} from "@capacitor/core";
import {OAuth2Client} from '@teamconductor/capacitor-oauth2';

@Component()
export class AppComponent implements OnInit {

    ngOnInit() {
        console.log("Register custom capacitor plugins");
        registerWebPlugin(OAuth2Client);
        // other stuff
    }
}
```

### Use it

```typescript
import {
  Plugins
} from '@capacitor/core';

@Component({
  template: '<button (click)="onOAuthBtnClick()">Login with OAuth</button>',
})
export class SignupComponent {
    onOAuthBtnClick() {
        Plugins.OAuth2Client.authenticate(
            oauth2Options
        ).then(resourceUrlResponse => {
            let oauthUserId = resourceUrlResponse["id"];
            let name = resourceUrlResponse["name"];
            // go to backend
        }).catch(reason => {
            console.error("OAuth rejected", reason);
        });
    }
}
```

### Options

See the `oauth2Options` interface at https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/src/definitions.ts#L16

## Platform: Web/PWA

This implementation just opens a browser window to let users enter their credentials.

As there is no provider SDK used to accomplish OAuth, no additional javascript files must be loaded and so there is no performance
impact using this plugin in a web applications.

- Available since version: **1.0.0-alpha.16**

## Platform: Android

**Register the plugin** in `com.companyname.appname.MainActivity#onCreate`

```
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<Class<? extends Plugin>> additionalPlugins = new ArrayList<>();
        // Additional plugins you've installed go here
        // Ex: additionalPlugins.add(TotallyAwesomePlugin.class);
        additionalPlugins.add(OAuth2ClientPlugin.class);

        // Initializes the Bridge
        this.init(savedInstanceState, additionalPlugins);
    }
```

**Custom OAuth Handler**

Some OAuth provider (Facebook) force developers to use their SDK on Android.

This plugin should be as generic as possible so I don't want to include provider specific dependencies.

Therefore I created a mechanism which let developers integrate custom SDK features in this plugin. Simply configure a full qualified classname in the option property `android.customHandlerClass`. This class has to implement `com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler`.

See a full working example below!

- Available since version: **1.0.0-alpha.38**

## Platform: iOS

- ETA October 2018

## Platform: Electron

- No ETA yet

## Full examples

### Google

**PWA**
```typescript
googleLogin() {
    Plugins.OAuth2Client.authenticate({
      appId: environment.oauthAppId.google.web,
      authorizationBaseUrl: "https://accounts.google.com/o/oauth2/auth",
      accessTokenEndpoint: "https://www.googleapis.com/oauth2/v4/token",
      scope: "email profile",
      resourceUrl: "https://www.googleapis.com/userinfo/v2/me",
      web: {
        redirectUrl: "http://localhost:4200",
        windowOptions: "height=600,left=0,top=0"
      },
      android: {
        appId: environment.oauthAppId.google.android,
        customScheme: "com.companyname.appname:/"
      }
    }).then(resourceUrlResponse => {
      this.authenticateBackend("GOOGLE", resourceUrlResponse);
    }).catch(reason => {
      console.error("Google OAuth rejected", reason);
    });
  }
```
**Android**

Add customScheme in `android/app/build.gradle` as well. Without `:/`.

```
android.defaultConfig.manifestPlaceholders = [
  'appAuthRedirectScheme': 'com.companyname.appname'
]
```

### Facebook

**PWA**

```typescript
facebookLogin() {
    let fbApiVersion = "2.11";
    Plugins.OAuth2Client.authenticate({
      appId: "YOUR_FACEBOOK_APP_ID",
      authorizationBaseUrl: "https://www.facebook.com/v" + fbApiVersion + "/dialog/oauth",
      accessTokenEndpoint:  "https://graph.facebook.com/v" + fbApiVersion + "/oauth/access_token",
      resourceUrl: "https://graph.facebook.com/v" + fbApiVersion + "/me",
      web: {
        redirectUrl: "http://localhost:4200",
        windowOptions: "height=600,left=0,top=0"
      },
      android: {
        customHandlerClass: "com.companyname.appname.YourFacebookOAuth2Handler",
      }
    }).then(resourceUrlResponse => {
      // check with your backend
    }).catch(reason => {
      console.error("FB OAuth rejected", reason);
    });
  }
```

**Android**

Facebook forces us to use their SDK and as I don't want to have a dependency to facebook for users, who dont need Facebook OAuth. I created the `customHandlerClass` integration.

See https://developers.facebook.com/docs/facebook-login/android/ for more background on how to configure Facebook in your Android app.

Here is what I did, which seems like a lot but it's basically the same you would have to I you do not use the plugin at all.

1) Add `implementation 'com.facebook.android:facebook-login:4.36.0'` to `android/app/build.gradle` as dependency.

2) Add to `string.xml`

```xml
    <string name="facebook_app_id"><YOUR_FACEBOOK_APP_ID></string>
    <string name="fb_login_protocol_scheme">fb<YOUR_FACEBOOK_APP_ID></string>
```

3) Add to `AndroidManifest.xml`

```xml
<meta-data android:name="com.facebook.sdk.ApplicationId" android:value="@string/facebook_app_id"/>

<activity android:name="com.facebook.FacebookActivity"
  android:configChanges=
    "keyboard|keyboardHidden|screenLayout|screenSize|orientation"
  android:label="@string/app_name" />

<activity android:name="com.facebook.CustomTabActivity" android:exported="true">
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="@string/fb_login_protocol_scheme" />
  </intent-filter>
</activity>
```
4) Create a custom handler class

```java

package com.companyname.appname;

import android.app.Activity;

import com.byteowls.capacitor.oauth2.handler.AccessTokenCallback;
import com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler;
import com.byteowls.teamconductor.MainActivity;
import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.getcapacitor.PluginCall;

public class YourFacebookOAuth2Handler implements OAuth2CustomHandler {

  @Override
  public void getAccessToken(Activity activity, PluginCall pluginCall, final AccessTokenCallback callback) {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    if (AccessToken.isCurrentAccessTokenActive()) {
      callback.onSuccess(accessToken.getToken());
    } else {
      LoginManager.getInstance().logInWithReadPermissions(activity, null);

      LoginManager.getInstance().registerCallback(((MainActivity) activity).getCallbackManager(), new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
          callback.onSuccess(loginResult.getAccessToken().getToken());
        }

        @Override
        public void onCancel() {
          callback.onCancel();
        }

        @Override
        public void onError(FacebookException error) {
          callback.onCancel();
        }
      });
    }
  }
}

```
5) Change your MainActivity like

```java
public class MainActivity extends BridgeActivity {

  private CallbackManager callbackManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FacebookSdk.sdkInitialize(this.getApplicationContext());

    callbackManager = CallbackManager.Factory.create();

    // add my plugins here
    List<Class<? extends Plugin>> additionalPlugins = new ArrayList<>();
    // Additional plugins you've installed go here
    additionalPlugins.add(OAuth2ClientPlugin.class);
    // Ex: additionalPlugins.add(TotallyAwesomePlugin.class);

    // Initializes the Bridge
    this.init(savedInstanceState, additionalPlugins);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (callbackManager.onActivityResult(requestCode, resultCode, data)) {
      return;
    }
  }

  public CallbackManager getCallbackManager() {
    return callbackManager;
  }

}
```

## Contribute

### Fix a bug or create a new feature

Please do not mix more than one issue in a feature branch. Each feature/bugfix should have its own branch and its own Pull Request (PR).

1. Create a issue and describe what you want to do at [Issue Tracker](https://github.com/moberwasserlechner/capacitor-oauth2/issues)
2. Create your feature branch (`git checkout -b feature/my-feature` or `git checkout -b bugfix/my-bugfix`)
3. Test your changes to the best of your ability.
5. Commit your changes (`git commit -m 'Describe feature or bug'`)
6. Push to the branch (`git push origin feature/my-feature`)
7. Create a Github pull request

### Code Style

This repo includes a .editorconfig file, which your IDE should pickup automatically.

If not please use the sun coding convention. Please do not use tabs at all!

Try to change only parts your feature or bugfix requires.

## License

MIT. Please see [LICENSE](https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/LICENSE).

## Team Condcutor

This plugin is powered by [Team Conductor](https://team-conductor.com/en/) - Next generation club management platform.

