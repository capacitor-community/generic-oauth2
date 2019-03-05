# Capacitor OAuth 2 client plugin

[![npm](https://img.shields.io/npm/v/@byteowls/capacitor-oauth2.svg)](https://www.npmjs.com/package/@byteowls/capacitor-oauth2)
[![Travis](https://img.shields.io/travis/moberwasserlechner/capacitor-oauth2/master.svg?maxAge=2592000)](https://travis-ci.org/moberwasserlechner/capacitor-oauth2)
[![npm](https://img.shields.io/npm/dt/@byteowls/capacitor-oauth2.svg?label=npm%20downloads)](https://www.npmjs.com/package/@byteowls/capacitor-oauth2)
[![Twitter Follow](https://img.shields.io/twitter/follow/michaelowl_web.svg?style=social&label=Follow&style=flat-square)](https://twitter.com/michaelowl_web)
[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.me/moberwasserlechner)

This is a simple OAuth 2 client plugin.

It let you configure the oauth parameters yourself instead of using SDKs. Therefore it is usable with various providers.

## Supported flows

### Implicit flow (response type: token)

Status: **ok**

### Code flow + PKCE (response type: code)

Status: **ok**

Please be aware that some providers (OneDrive, Auth0) allow **Code Flow + PKCE** only for native apps. Web apps have to use implicit flow.

### Important
For security reasons this plugin does/will not support Code Flow without PKCE.

That would include storing your **client secret** in client code which is highly insecure and not recommended.
That flow should only be used on the backend (server).

## Installation

`npm i -E @byteowls/capacitor-oauth2`

## Configuration

This example shows the common process of configuring this plugin.

Although it was taken from a Angular 6 application, it should work in other frameworks as well.

### Register plugin

Find the init component of your app, which is in Angular `app.component.ts` and register this plugin by

```
import {registerWebPlugin} from "@capacitor/core";
import {OAuth2Client} from '@byteowls/capacitor-oauth2';

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
  template: '<button (click)="onOAuthBtnClick()">Login with OAuth</button>' +
   '<button (click)="onLogoutClick()">Logout OAuth</button>'
})
export class SignupComponent {
    onOAuthBtnClick() {
        Plugins.OAuth2Client.authenticate(
            oauth2Options
        ).then(resourceUrlResponse => {
            let accessToken = resourceUrlResponse["access_token"];
            let oauthUserId = resourceUrlResponse["id"];
            let name = resourceUrlResponse["name"];
            // go to backend
        }).catch(reason => {
            console.error("OAuth rejected", reason);
        });
    }

    onLogoutClick() {
            Plugins.OAuth2Client.logout(
                oauth2Options
            ).then(() => {
                // do something
            }).catch(reason => {
                console.error("OAuth logout failed", reason);
            });
        }
}
```

### Options

See the `oauth2Options` interface at https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/src/definitions.ts#L24

### Error Codes

* ERR_PARAM_NO_APP_ID ... The appId / clientId is missing. (web, android, ios)
* ERR_PARAM_NO_AUTHORIZATION_BASE_URL ... The authorization base url is missing. (web, android, ios)
* ERR_PARAM_NO_REDIRECT_URL ... The redirect url / custom scheme url is missing. (web, android, ios)
* ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT ... The access token endpoint url is missing. It is only needed if code flow is used. (web, android, ios)
* ERR_PARAM_INVALID_RESPONSE_TYPE ... You configured a invalid responseType. Only "token" or "code" are allowed. (web, android, ios)
* ERR_NO_ACCESS_TOKEN ... No access_token found. (web, android)
* ERR_NO_AUTHORIZATION_CODE ... No authorization code was returned in the redirect response. (web, android, ios)
* ERR_STATES_NOT_MATCH ... The state included in the authorization code request does not match the one in the redirect. Security risk! (web, android, ios)
* USER_CANCELLED ... The user cancelled the login flow. (android, ios)
* ERR_CUSTOM_HANDLER_LOGIN ... Login through custom handler class failed. See logs and check your code. (android, ios)
* ERR_CUSTOM_HANDLER_LOGOUT ... Logout through custom handler class failed. See logs and check your code. (android, ios)
* ERR_ANDROID_NO_BROWSER ... On Android not suitable browser could be found! (android)
* ERR_GENERAL ... A unspecific error. Check the logs to see want exactly happened. (web, android, ios)

## Platform: Web/PWA

This implementation just opens a browser window to let users enter their credentials.

As there is no provider SDK used to accomplish OAuth, no additional javascript files must be loaded and so there is no performance
impact using this plugin in a web application.

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

Therefore I created a mechanism which let developers integrate custom SDK features in this plugin.
Simply configure a full qualified classname in the option property `android.customHandlerClass`.
This class has to implement `com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler`.

See a full working example below!

## Platform: iOS

On iOS the plugin is registered automatically by Capacitor.

**Custom OAuth Handler**

Some OAuth provider (Facebook) force developers to use their SDK on iOS.

This plugin should be as generic as possible so I don't want to include provider specific dependencies.

Therefore I created a mechanism which let developers integrate custom SDK features in this plugin.
Simply configure a the class name in the option property `ios.customHandlerClass`.
This class has to implement `ByteowlsCapacitorOauth2.OAuth2CustomHandler`.

See a full working example below!


## Platform: Electron

- Maybe early 2019

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
        responseType: "code", // if you configured a android app in google dev console the value must be "code"
        customScheme: "com.companyname.appname:/" // package name from google dev console
      },
      ios: {
        appId: environment.oauthAppId.google.ios,
        responseType: "code", // if you configured a ios app in google dev console the value must be "code"
        customScheme: "com.companyname.appname:/" // Bundle ID from google dev console
      }
    }).then(resourceUrlResponse => {
      // do sth e.g. check with your backend
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

**iOS**

Open `ios/App/App/Info.plist` in a XML editor and add the customScheme without `:/` like that

```xml
	<key>CFBundleURLTypes</key>
	<array>
		<dict>
			<key>CFBundleURLSchemes</key>
			<array>
				<string>com.companyname.appname</string>
			</array>
		</dict>
	</array>
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
        customHandlerClass: "com.companyname.appname.YourAndroidFacebookOAuth2Handler",
      },
      ios: {
        customHandlerClass: "App.YourIOsFacebookOAuth2Handler",
      }
    }).then(resourceUrlResponse => {
      // do sth e.g. check with your backend
    }).catch(reason => {
      console.error("FB OAuth rejected", reason);
    });
  }
```

**Android and iOS**

Since October 2018 Strict Mode for Redirect Urls is always on.

>Use Strict Mode for Redirect URIs

>Only allow redirects that use the Facebook SDK or that exactly match the Valid OAuth Redirect URIs. Strongly recommended.

Before that it was able to use `fb<your_app_id>:/authorize` in a Android or iOS app and get the accessToken.

Unfortunately now we have to use the SDK for Facebook Login.

I don't want to have a dependency to facebook for users, who don't need Facebook OAuth.

To address this problem I created a integration with custom code in your app `customHandlerClass`

**Android**

See https://developers.facebook.com/docs/facebook-login/android/ for more background on how to configure Facebook in your Android app.

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

public class YourAndroidFacebookOAuth2Handler implements OAuth2CustomHandler {

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

  @Override
  public boolean logout(PluginCall pluginCall) {
    LoginManager.getInstance().logOut();
    return true;
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

**iOS**

See https://developers.facebook.com/docs/swift/getting-started and https://developers.facebook.com/docs/swift/login

1) Add Facebook pods to your app's Podfile `ios/App/App`

```
platform :ios, '10.0'
use_frameworks!

target 'App' do
  # Add your Pods here
  pod 'FacebookCore'
  pod 'FacebookLogin'

  # Automatic Capacitor Pod dependencies, do not delete
  pod 'Capacitor', :path => '../../node_modules/@capacitor/ios'
  pod 'CapacitorCordova', :path => '../../node_modules/@capacitor/ios'
  pod 'ByteowlsCapacitorOauth2', :path => '../../node_modules/@byteowls/capacitor-oauth2'
  pod 'CordovaPlugins', :path => '../../node_modules/@capacitor/cli/assets/capacitor-cordova-ios-plugins'

  # Do not delete
end

```

2) Add some Facebook configs to your `Info.plist`

```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>fb{your-app-id}</string>
    </array>
  </dict>
</array>
<key>FacebookAppID</key>
<string>{your-app-id}</string>
<key>FacebookDisplayName</key>
<string>{your-app-name}</string>
<key>LSApplicationQueriesSchemes</key>
<array>
  <string>fbapi</string>
  <string>fb-messenger-share-api</string>
  <string>fbauth2</string>
  <string>fbshareextension</string>
</array>
```

3) Create a custom handler class

```swift
 import Foundation
 import FacebookCore
 import FacebookLogin
 import Capacitor
 import ByteowlsCapacitorOauth2

 @objc class YourIOsFacebookOAuth2Handler: NSObject, OAuth2CustomHandler {

     var loginManager: LoginManager?;

     required override init() {
     }

     func getAccessToken(viewController: UIViewController, call: CAPPluginCall, success: @escaping (String) -> Void, cancelled: @escaping () -> Void, failure: @escaping (Error) -> Void) {

         if let accessToken = AccessToken.current {
             success(accessToken.authenticationToken)
         } else {
             DispatchQueue.main.async {
                 if self.loginManager == nil {
                     self.loginManager = LoginManager()
                 }

                 self.loginManager!.logIn(readPermissions: [ ReadPermission.publicProfile ],
                                          viewController: viewController, completion: { loginResult in
                                             switch loginResult {
                                             case .failed(let error):
                                                 failure(error)
                                             case .cancelled:
                                                 cancelled()
                                             case .success(_, _, let accessToken):
                                                 success(accessToken.authenticationToken)
                                             }
                 })
             }
         }
     }

     func logout(call: CAPPluginCall) -> Bool {
         self.loginManager?.logOut()
         return true
     }
 }
```

This handler will be automatically discovered up by the plugin and handles the login using the Facebook SDK.

4) Add the following to `ios/App/App/AppDelegate.swift`

```swift
import UIKit
import Capacitor

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    // other methods

    func application(_ app: UIApplication, open url: URL, options: [UIApplicationOpenURLOptionsKey : Any] = [:]) -> Bool {
      // Called when the app was launched with a url. Feel free to add additional processing here,
      // but if you want the App API to support tracking app url opens, make sure to keep this call

      if let scheme = url.scheme, let host = url.host {
        if scheme == "fb\(SDKSettings.appId)" && host == "authorize" {
          return SDKApplicationDelegate.shared.application(app, open: url, options: options)
        }
      }

      return CAPBridge.handleOpenUrl(url, options)
    }

    // other methods
}
```
This might not be needed but but users had an issue without it.

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

## BYTEOWLS Software & Consulting

This plugin is powered by BYTEOWLS Software & Consulting and was build for [Team Conductor](https://team-conductor.com/en/) - Next generation club management platform.

### Commercial support and consulting

We create plugins for apps we build and share them **as it is** with the community.

I you have a feature request, need support how to use the plugin or 
need a release breaking with our normal release cycle you have the possibility 
to sponsor the development by paying for this custom development or support.

See the wiki page for how to request a quote.
