# Capacitor OAuth 2 client plugin

[![npm](https://img.shields.io/npm/v/@byteowls/capacitor-oauth2.svg)](https://www.npmjs.com/package/@byteowls/capacitor-oauth2)
[![Travis](https://img.shields.io/travis/moberwasserlechner/capacitor-oauth2/master.svg?maxAge=2592000)](https://travis-ci.org/moberwasserlechner/capacitor-oauth2)
[![npm](https://img.shields.io/npm/dt/@byteowls/capacitor-oauth2.svg?label=npm%20downloads)](https://www.npmjs.com/package/@byteowls/capacitor-oauth2)
[![Twitter Follow](https://img.shields.io/twitter/follow/michaelowl_web.svg?style=social&label=Follow&style=flat-square)](https://twitter.com/michaelowl_web)

This is a simple OAuth 2 client plugin.

It let you configure the oauth parameters yourself instead of using SDKs. Therefore it is usable with various providers.
See [providers](#list-of-providers) the community has already used this plugin with.

## Versions

| Plugin | Minimum Capacitor | Docs                                                                                   | Notes                          |
|--------|-------------------|----------------------------------------------------------------------------------------|--------------------------------|
| 2.x    | 2.0.0             | [README](https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/README.md) | Breaking changes see Changelog. XCode 11.4 needs this version  |
| 1.x    | 1.0.0             | [README](https://github.com/moberwasserlechner/capacitor-oauth2/blob/1.1.0/README.md)  |                                |

For further details on what has changed see the [CHANGELOG](https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/CHANGELOG.md).

## Supported flows

Starting with version **2.0.0** the plugin will no longer restrict the `responseType` to `token` or `code`.

Developers can configure anything. It is their responsibility to use the options the chosen OAuth Provider supports.

See the excellent article about OAuth2 response type combinations.

https://medium.com/@darutk/diagrams-of-all-the-openid-connect-flows-6968e3990660

The plugin on the other will behave differently depending on the existence of certain config parameters:

These parameters are:

* `accessTokenEndpoint`
* `resourceUrl`

e.g.

If `responseType=code`, `pkceDisable=true` and `accessTokenEndpoint` is missing the `authorizationCode` will be resolve along with the whole authorization response.
This only works for the Web and Android. On iOS the used lib does not allows to cancel after the authorization request see #13.

### Tested / working flows

These flows are already working and were tested by me.

#### Implicit flow

```
responseType: "token"
```

#### Code flow + PKCE

```
...
responseType: "code"
pkceEnable: true
...
```

Please be aware that some providers (OneDrive, Auth0) allow **Code Flow + PKCE** only for native apps. Web apps have to use implicit flow.

### Important
For security reasons this plugin does/will not support Code Flow without PKCE.

That would include storing your **client secret** in client code which is highly insecure and not recommended.
That flow should only be used on the backend (server).

## Installation

`npm i -E @byteowls/capacitor-oauth2`

Minimum Capacitor version is **2.0.0**

## Configuration

This example shows the common process of configuring this plugin.

Although it was taken from a Angular application, it should work in other frameworks as well.

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
   '<button (click)="onOAuthRefreshBtnClick()">Refresh token</button>' +
   '<button (click)="onLogoutClick()">Logout OAuth</button>'
})
export class SignupComponent {
    refreshToken: string;

    onOAuthBtnClick() {
        Plugins.OAuth2Client.authenticate(
            oauth2Options
        ).then(response => {
            let accessToken = response["access_token"];
            this.refreshToken = response["refresh_token"];

            // only if you include a resourceUrl protected user values are included in the response!
            let oauthUserId = response["id"];
            let name = response["name"];

            // go to backend
        }).catch(reason => {
            console.error("OAuth rejected", reason);
        });
    }

    // Refreshing tokens only works on iOS/Android for now
    onOAuthRefreshBtnClick() {
      if (!this.refreshToken) {
        console.error("No refresh token found. Log in with OAuth first.");
      }

      Plugins.OAuth2Client.refreshToken(
        oauth2RefreshOptions
      ).then(response => {
        let accessToken = response["access_token"];
        // Don't forget to store the new refresh token as well!
        this.refreshToken = response["refresh_token"];
        // Go to backend
      }).catch(reason => {
          console.error("Refreshing token failed", reason);
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

See the `oauth2Options` and `oauth2RefreshOptions` interfaces at https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/src/definitions.ts for details.

Example:
```
{
      authorizationBaseUrl: "https://accounts.google.com/o/oauth2/auth",
      accessTokenEndpoint: "https://www.googleapis.com/oauth2/v4/token",
      scope: "email profile",
      resourceUrl: "https://www.googleapis.com/userinfo/v2/me",
      web: {
        appId: environment.oauthAppId.google.web,
        responseType: "token", // implicit flow
        accessTokenEndpoint: "", // clear the tokenEndpoint as we know that implicit flow gets the accessToken from the authorizationRequest
        redirectUrl: "http://localhost:4200",
        windowOptions: "height=600,left=0,top=0"
      },
      android: {
        appId: environment.oauthAppId.google.android,
        responseType: "code", // if you configured a android app in google dev console the value must be "code"
        redirectUrl: "com.companyname.appname:/" // package name from google dev console
      },
      ios: {
        appId: environment.oauthAppId.google.ios,
        responseType: "code", // if you configured a ios app in google dev console the value must be "code"
        redirectUrl: "com.companyname.appname:/" // Bundle ID from google dev console
      }
    }
 ```


#### authenticate() and logout()

**Overwritable Base Parameter**

These parameters are overwritable in every platform

| parameter            	| default 	| required 	| description                                                                                                                                                                                                                            	| since 	|
|----------------------	|---------	|----------	|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|-------	|
| appId                	|         	| yes      	| aka clientId, serviceId, ...                                                                                                                                                                                                           	|       	|
| authorizationBaseUrl 	|         	| yes      	|                                                                                                                                                                                                                                        	|       	|
| responseType         	|         	| yes      	|                                                                                                                                                                                                                                        	|       	|
| redirectUrl          	|         	| yes      	|                                                                                                                                                                                                                                        	| 2.0.0 	|
| accessTokenEndpoint  	|         	|          	| If empty the authorization response incl code is return. Known issue: Not on iOS!                                                                                                                                                      	|       	|
| resourceUrl          	|         	|          	| If emtpy the tokens are return instead.                                                                                                                                                                                                	|       	|
| pkceEnabled          	| `false` 	|          	| Enable PKCE if you need it.                                                                                                                                                                                                            	|       	|
| scope                	|         	|          	|                                                                                                                                                                                                                                        	|       	|
| state                	|         	|          	| The plugin always uses a state.<br>If you don't provide one we generate it.                                                                                                                                                            	|       	|
| additionalParameters 	|         	|          	| Additional parameters for anything you might miss, like `none`, `response_mode`. <br><br>Just create a key value pair.<br>```{ "key1": "value", "key2": "value, "response_mode": "value"}``` 	|       	|

**Platform Web**

| parameter     	| default 	| required 	| description                            	| since 	|
|---------------	|---------	|----------	|----------------------------------------	|-------	|
| windowOptions 	|         	|          	| e.g. width=500,height=600,left=0,top=0 	|       	|
| windowTarget  	| `_blank`  |       	|                                        	|       	|

**Platform Android**

| parameter                    	| default 	| required 	| description                                                                                                              	| since 	|
|------------------------------	|---------	|----------	|--------------------------------------------------------------------------------------------------------------------------	|-------	|
| customHandlerClass           	|         	|          	| Provide a class name implementing `com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler`                            	|       	|
| handleResultOnNewIntent      	| `false` 	|          	| Alternative to handle the activity result. The `onNewIntent` method is only call if the App was killed while logging in. 	|       	|
| handleResultOnActivityResult 	| `true`  	|          	|                                                                                                                          	|       	|

**Platform iOS**

| parameter          	| default 	| required 	| description                                                                                    	| since 	|
|--------------------	|---------	|----------	|------------------------------------------------------------------------------------------------	|-------	|
| customHandlerClass 	|         	|          	| Provide a class name implementing `ByteowlsCapacitorOauth2.OAuth2CustomHandler`                	|       	|
| siwaUseScope       	|         	|          	| SiWA default scope is `name email` if you want to use the configured one set this param `true` 	| 2.1.0 	|


#### refreshToken()

| parameter           	| default 	| required 	| description                  	| since 	|
|---------------------	|---------	|----------	|------------------------------	|-------	|
| appId               	|         	| yes      	| aka clientId, serviceId, ... 	|       	|
| accessTokenEndpoint 	|         	| yes      	|                              	|       	|
| refreshToken        	|         	| yes      	|                              	|       	|
| scope               	|         	|          	|                              	|       	|

### Error Codes

#### authenticate()

* ERR_PARAM_NO_APP_ID ... The appId / clientId is missing. (web, android, ios)
* ERR_PARAM_NO_AUTHORIZATION_BASE_URL ... The authorization base url is missing. (web, android, ios)
* ERR_PARAM_NO_RESPONSE_TYPE ... The response type is missing. (web, android, ios)
* ERR_PARAM_NO_REDIRECT_URL ... The redirect url is missing. (web, android, ios)
* ERR_STATES_NOT_MATCH ... The state included in the authorization code request does not match the one in the redirect. Security risk! (web, android, ios)
* ERR_AUTHORIZATION_FAILED ... The authorization failed.
* ERR_NO_ACCESS_TOKEN ... No access_token found. (web, android)
* ERR_NO_AUTHORIZATION_CODE ... No authorization code was returned in the redirect response. (web, android, ios)
* USER_CANCELLED ... The user cancelled the login flow. (web, android, ios)
* ERR_CUSTOM_HANDLER_LOGIN ... Login through custom handler class failed. See logs and check your code. (android, ios)
* ERR_CUSTOM_HANDLER_LOGOUT ... Logout through custom handler class failed. See logs and check your code. (android, ios)
* ERR_ANDROID_NO_BROWSER ... No suitable browser could be found! (Android)
* ERR_ANDROID_RESULT_NULL ... The auth result is null. The intent in the ActivityResult is null. This might be a valid state but make sure you configured Android part correctly! See [Platform Android](#platform-android)
* ERR_GENERAL ... A unspecific error. Check the logs to see want exactly happened. (web, android, ios)

#### refreshToken()

* ERR_PARAM_NO_APP_ID ... The appId / clientId is missing. (android, ios)
* ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT ... The access token endpoint url is missing. It is only needed on refresh, on authenticate it is optional. (android, ios)
* ERR_PARAM_NO_REFRESH_TOKEN ... The refresh token is missing. (android, ios)
* ERR_NO_ACCESS_TOKEN ... No access_token found. (web, android)
* ERR_GENERAL ... A unspecific error. Check the logs to see want exactly happened. (android, ios)

## Platform: Web/PWA

This implementation just opens a browser window to let users enter their credentials.

As there is no provider SDK used to accomplish OAuth, no additional javascript files must be loaded and so there is no performance
impact using this plugin in a web application.

## Platform: Android

Prerequisite: [Capacitor Android Docs](https://capacitor.ionicframework.com/docs/android/configuration)

### Register the plugin

The plugin must be manually added to your `com.companyname.appname.MainActivity`.
See the [Capacitor Docs](https://capacitor.ionicframework.com/docs/plugins/android#export-to-capacitor) or below:

```java
// Other imports...
import com.byteowls.capacitor.oauth2.OAuth2ClientPlugin;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initializes the Bridge
        this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
            // Additional plugins you've installed go here
            // Ex: add(TotallyAwesomePlugin.class);
            add(OAuth2ClientPlugin.class);
        }});
    }
}
```

### Android Default Config

Skip this, if you use a [OAuth2CustomHandler](#custom-oauth-handler)

#### AndroidManifest.xml
The `AndroidManifest.xml` in your Capacitor Android project already contains
```xml
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="@string/custom_url_scheme" />
    </intent-filter>
```

Find the line
```xml
<data android:scheme="@string/custom_url_scheme" />
```
and change it to
```xml
<data android:scheme="@string/custom_url_scheme" android:host="oauth" />
```
Note: Actually any value for `android:host` will do. It does not has to be `oauth`.

This will fix an issues within the oauth workflow when the application is shown twice.
See [Issue #15](https://github.com/moberwasserlechner/capacitor-oauth2/issues/15) for details what happens.

#### android/app/build.gradle

```groovy
android.defaultConfig.manifestPlaceholders = [
  "appAuthRedirectScheme": "<@string/custom_url_scheme from string.xml>"
]
```

**Troubleshooting**

1) If your `appAuthRedirectScheme` does not get recognized because you are using a library that replaces it
(e.g.: onesignal-cordova-plugin), you will have to add it to your `buildTypes` like the following:

```groovy
android.buildTypes.debug.manifestPlaceholders =  [
  'appAuthRedirectScheme': '<@string/custom_url_scheme from string.xml>' // e.g. com.companyname.appname
]
android.buildTypes.release.manifestPlaceholders = [
  'appAuthRedirectScheme': '<@string/custom_url_scheme from string.xml>' // e.g. com.companyname.appname
]
```

2) "ERR_ANDROID_RESULT_NULL": See [Issue #52](https://github.com/moberwasserlechner/capacitor-oauth2/issues/52#issuecomment-525715515) for details.
I cannot reproduce this behaviour. Moreover there might be situation this state is valid. In other cases e.g. in the linked issue a configuration tweak fixed it.

### Custom OAuth Handler

Some OAuth provider (Facebook) force developers to use their SDK on Android.

This plugin should be as generic as possible so I don't want to include provider specific dependencies.

Therefore I created a mechanism which let developers integrate custom SDK features in this plugin.
Simply configure a full qualified classname in the option property `android.customHandlerClass`.
This class has to implement `com.byteowls.capacitor.oauth2.handler.OAuth2CustomHandler`.

See a full working example below!

## Platform: iOS

### Register plugin
On iOS the plugin is registered **automatically** by Capacitor.

### iOS Default Config

Skip this, if you use a [OAuth2CustomHandler](#custom-oauth-handler-1)

Open `ios/App/App/Info.plist` in XCode and add the value of `redirectUrl` from your config without `:/` like that

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

### Custom OAuth Handler

Some OAuth provider (e.g. Facebook) force developers to use their SDK on iOS.

This plugin should be as generic as possible so I don't want to include provider specific dependencies.

Therefore I created a mechanism which let developers integrate custom SDK features in this plugin.
Simply configure a the class name in the option property `ios.customHandlerClass`.
This class has to implement `ByteowlsCapacitorOauth2.OAuth2CustomHandler`.

See a full working example below!


## Platform: Electron

- No timeline.

## List of Providers

These are some of the providers that can be configured with this plugin. I'm happy to add others ot the list, if you let me know.

| Name     | Example (config,...)   | Notes |
|----------|------------------------|-------|
| Google   | [see below](#google)   |       |
| Facebook | [see below](#facebook) |       |
| Azure    | [see below](#azure-b2c)|       |


## Examples

### Azure B2C

In case of problems please read the discusssion leading to this config in [#91](https://github.com/moberwasserlechner/capacitor-oauth2/issues/91)

#### PWA
```typescript

azureLogin() {
  Plugins.OAuth2Client.authenticate({
    appId: "xxxxxxxxx",
    authorizationBaseUrl: "https://tenantb2c.b2clogin.com/tfp/tenantb2c.onmicrosoft.com/B2C_1_SignUpAndSignIn/oauth2/v2.0/authorize",
    accessTokenEndpoint: "",
    scope: "openid offline_access https://tenantb2c.onmicrosoft.com/capacitor-api/demo.read",
    responseType: "token",
    web: {
        redirectUrl: "http://localhost:8100/auth"
    },
    android: {
        pkceEnabled: true,
        responseType: "code",
        redirectUrl: "com.tenant.app://oauth/auth",
        accessTokenEndpoint: "https://tenantb2c.b2clogin.com/tfp/tenantb2c.onmicrosoft.com/B2C_1_SignUpAndSignIn/oauth2/v2.0/token",
        handleResultOnNewIntent: true,
        handleResultOnActivityResult: true
    },
    ios: {
        pkceEnabled: true,
        responseType: "code",
        redirectUrl: "msauth.com.tenant://oauth",
        accessTokenEndpoint: "https://tenantb2c.b2clogin.com/tfp/tenantb2c.onmicrosoft.com/B2C_1_SignUpAndSignIn/oauth2/v2.0/token",
    }
  }
}
```
#### Android

See [Android Default Config](#android-default-config)

#### iOS

See [iOS Default Config](#ios-default-config)

### Google

**PWA**
```typescript
googleLogin() {
    Plugins.OAuth2Client.authenticate({
      authorizationBaseUrl: "https://accounts.google.com/o/oauth2/auth",
      accessTokenEndpoint: "https://www.googleapis.com/oauth2/v4/token",
      scope: "email profile",
      resourceUrl: "https://www.googleapis.com/userinfo/v2/me",
      web: {
        appId: environment.oauthAppId.google.web,
        responseType: "token", // implicit flow
        accessTokenEndpoint: "", // clear the tokenEndpoint as we know that implicit flow gets the accessToken from the authorizationRequest
        redirectUrl: "http://localhost:4200",
        windowOptions: "height=600,left=0,top=0"
      },
      android: {
        appId: environment.oauthAppId.google.android,
        responseType: "code", // if you configured a android app in google dev console the value must be "code"
        redirectUrl: "com.companyname.appname:/" // package name from google dev console
      },
      ios: {
        appId: environment.oauthAppId.google.ios,
        responseType: "code", // if you configured a ios app in google dev console the value must be "code"
        redirectUrl: "com.companyname.appname:/" // Bundle ID from google dev console
      }
    }).then(resourceUrlResponse => {
      // do sth e.g. check with your backend
    }).catch(reason => {
      console.error("Google OAuth rejected", reason);
    });
  }
```

#### Android

See [Android Default Config](#android-default-config)

#### iOS

See [iOS Default Config](#ios-default-config)

### Facebook

**PWA**

```typescript
facebookLogin() {
    let fbApiVersion = "2.11";
    Plugins.OAuth2Client.authenticate({
      appId: "YOUR_FACEBOOK_APP_ID",
      authorizationBaseUrl: "https://www.facebook.com/v" + fbApiVersion + "/dialog/oauth",
      resourceUrl: "https://graph.facebook.com/v" + fbApiVersion + "/me",
      web: {
        responseType: "token",
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
import com.companyname.appname.MainActivity;
import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.DefaultAudience;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.getcapacitor.PluginCall;

import java.util.Collections;

public class YourAndroidFacebookOAuth2Handler implements OAuth2CustomHandler {

  @Override
  public void getAccessToken(Activity activity, PluginCall pluginCall, final AccessTokenCallback callback) {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    if (AccessToken.isCurrentAccessTokenActive()) {
      callback.onSuccess(accessToken.getToken());
    } else {
      LoginManager l = LoginManager.getInstance();
      l.logInWithReadPermissions(activity, Collections.singletonList("public_profile"));
      l.setLoginBehavior(LoginBehavior.WEB_ONLY);
      l.setDefaultAudience(DefaultAudience.NONE);
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
  public boolean logout(Activity activity, PluginCall pluginCall) {
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
platform :ios, '11.0'
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

    required override init() {
    }

    func getAccessToken(viewController: UIViewController, call: CAPPluginCall, success: @escaping (String) -> Void, cancelled: @escaping () -> Void, failure: @escaping (Error) -> Void) {
        if let accessToken = AccessToken.current {
            success(accessToken.tokenString)
        } else {
            DispatchQueue.main.async {
                let loginManager = LoginManager()
                // I only need the most basic permissions but others are available
                loginManager.logIn(permissions: [ .publicProfile ], viewController: viewController) { result in
                    switch result {
                    case .success(_, _, let accessToken):
                        success(accessToken.tokenString)
                    case .failed(let error):
                        failure(error)
                    case .cancelled:
                        cancelled()
                    }
                }
            }
        }
    }

    func logout(viewController: UIViewController, call: CAPPluginCall) -> Bool {
        let loginManager = LoginManager()
        loginManager.logOut()
        return true
    }
}
```

This handler will be automatically discovered up by the plugin and handles the login using the Facebook SDK.
See https://developers.facebook.com/docs/swift/login/#custom-login-button for details.

4) The users that have redirect problem after success grant add the following code to `ios/App/App/AppDelegate.swift`.
This code correctly delegate the FB redirect url to be managed by Facebook SDK.

```swift
import UIKit
import FacebookCore
import FacebookLogin
import Capacitor

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    // other methods

    func application(_ app: UIApplication, open url: URL, options: [UIApplicationOpenURLOptionsKey : Any] = [:]) -> Bool {
      // Called when the app was launched with a url. Feel free to add additional processing here,
      // but if you want the App API to support tracking app url opens, make sure to keep this call

      if let scheme = url.scheme, let host = url.host {
        let appId: String = Settings.appID!
        if scheme == "fb\(appId)" && host == "authorize" {
          return ApplicationDelegate.shared.application(app, open: url, options: options)
        }
      }

      return CAPBridge.handleOpenUrl(url, options)
    }

    // other methods
}
```

## Contribute

See [Contribution Guidelines](https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/.github/CONTRIBUTING.md).

## Changelog
See [CHANGELOG](https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/CHANGELOG.md).

## License

MIT. See [LICENSE](https://github.com/moberwasserlechner/capacitor-oauth2/blob/master/LICENSE).

## BYTEOWLS Software & Consulting

This plugin is powered by [BYTEOWLS Software & Consulting](https://byteowls.com) and was build for [Team Conductor](https://team-conductor.com/en/) - Next generation club management platform.
