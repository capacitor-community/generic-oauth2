# Capacitor OAuth 2 client plugin

<a href="#sponsors"><img src="https://img.shields.io/badge/plugin-Sponsors-blue?style=flat-square" /></a>
<a href="https://github.com/moberwasserlechner/capacitor-oauth2/actions?query=workflow%3ACI"><img src="https://img.shields.io/github/workflow/status/moberwasserlechner/capacitor-oauth2/CI?style=flat-square" /></a>
<a href="https://www.npmjs.com/package/@byteowls/capacitor-oauth2"><img src="https://img.shields.io/npm/dw/@byteowls/capacitor-oauth2?style=flat-square" /></a>
<a href="https://www.npmjs.com/package/@byteowls/capacitor-oauth2"><img src="https://img.shields.io/npm/v/@byteowls/capacitor-oauth2?style=flat-square" /></a>
<a href="LICENSE"><img src="https://img.shields.io/npm/l/@byteowls/capacitor-oauth2?style=flat-square" /></a>

This is a **generic OAuth 2 client** plugin. It let you configure the oauth parameters yourself instead of using SDKs. Therefore it is usable with various providers.
See [identity providers](#list-of-providers) the community has already used this plugin with.

## How to install

For Capacitor v4
```bash
npm i @byteowls/capacitor-oauth2
npx cap sync
```
For Capacitor v3 use `3.0.1`
```bash
npm i @byteowls/capacitor-oauth2@3.0.1
npx cap sync
```
For Capacitor v2 use `2.1.0`
```bash
npm i @byteowls/capacitor-oauth2@2.1.0
npx cap sync
```

## Versions

| Plugin | For Capacitor         | Docs                                                                                        | Notes                          |
|--------|-------------------|---------------------------------------------------------------------------------------------|--------------------------------|
| 4.x    | 4.x.x             | [README](./README.md)                                                                       | Breaking changes see Changelog. XCode 12.0 needs this version  |
| 3.x    | 3.x.x             | [README](https://github.com/moberwasserlechner/capacitor-oauth2/tree/release/3.x/README.md) | Breaking changes see Changelog. XCode 12.0 needs this version  |
| 2.x    | 2.x.x             | [README](https://github.com/moberwasserlechner/capacitor-oauth2/tree/release/2.x/README.md) | Breaking changes see Changelog. XCode 11.4 needs this version  |
| 1.x    | 1.x.x             | [README](https://github.com/moberwasserlechner/capacitor-oauth2/blob/1.1.0/README.md)       |                                |

For further details on what has changed see the [CHANGELOG](https://github.com/moberwasserlechner/capacitor-oauth2/blob/main/CHANGELOG.md).

## Sponsors

I would like to especially thank some people and companies for supporting my work on this plugin and therefore improving it for everybody.

* [Mark Laurence](https://github.com/UnclearMaker) and the [Royal Veterinary College](https://www.rvc.ac.uk/) - Thanks for supporting open source.

## Maintainers

| Maintainer | GitHub | Social |
| -----------| -------| -------|
| Michael Oberwasserlechner | [moberwasserlechner](https://github.com/moberwasserlechner) | [@michaelowl_web](https://twitter.com/michaelowl_web) |

Actively maintained: YES

## Supported flows

See the excellent article about OAuth2 response type combinations.

https://medium.com/@darutk/diagrams-of-all-the-openid-connect-flows-6968e3990660

The plugin on the other will behave differently depending on the existence of certain config parameters:

These parameters are:

* `accessTokenEndpoint`
* `resourceUrl`

e.g.

1)
If `responseType=code`, `pkceDisable=true` and `accessTokenEndpoint` is missing the `authorizationCode` will be resolve along with the whole authorization response.
This only works for the Web and Android. On iOS the used lib does not allows to cancel after the authorization request see #13.

2)
If you just need the `id_token` JWT you have to set `accessTokenEndpoint` and `resourceUrl` to `null`.


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

## Configuration

Starting with version 3.0.0, the plugin is registered automatically on all platforms.

### Use it

```typescript
import {OAuth2Client} from "@byteowls/capacitor-oauth2";

@Component({
  template: '<button (click)="onOAuthBtnClick()">Login with OAuth</button>' +
   '<button (click)="onOAuthRefreshBtnClick()">Refresh token</button>' +
   '<button (click)="onLogoutClick()">Logout OAuth</button>'
})
export class SignupComponent {
    refreshToken: string;

    onOAuthBtnClick() {
        OAuth2Client.authenticate(
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

      OAuth2Client.refreshToken(
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
            OAuth2Client.logout(
                oauth2LogoutOptions
            ).then(() => {
                // do something
            }).catch(reason => {
                console.error("OAuth logout failed", reason);
            });
        }
}
```

### Options

See the `oauth2Options` and `oauth2RefreshOptions` interfaces at https://github.com/moberwasserlechner/capacitor-oauth2/blob/main/src/definitions.ts for details.

Example:
```
{
      authorizationBaseUrl: "https://accounts.google.com/o/oauth2/auth",
      accessTokenEndpoint: "https://www.googleapis.com/oauth2/v4/token",
      scope: "email profile",
      resourceUrl: "https://www.googleapis.com/userinfo/v2/me",
      logsEnabled: true,
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

**Overrideable Base Parameter**

These parameters are overrideable in every platform

| parameter            	    | default 	| required 	| description                                                                                                                                                                                                                            	| since 	|
|----------------------	    |---------	|----------	|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|-------	|
| appId                	    |         	| yes      	| aka clientId, serviceId, ...                                                                                                                                                                                                           	|       	|
| authorizationBaseUrl 	    |         	| yes      	|                                                                                                                                                                                                                                        	|       	|
| responseType         	    |         	| yes      	|                                                                                                                                                                                                                                        	|       	|
| redirectUrl          	    |         	| yes      	|                                                                                                                                                                                                                                        	| 2.0.0 	|
| accessTokenEndpoint  	    |         	|          	| If empty the authorization response incl code is returned. Known issue: Not on iOS!                                                                                                                                                    	|       	|
| resourceUrl          	    |         	|          	| If empty the tokens are return instead. If you need just the `id_token` you have to set both `accessTokenEndpoint` and `resourceUrl` to `null` or empty ``.                                                                               |       	|
| additionalResourceHeaders	|         	|          	| Additional headers for the resource request                                                                                                                                                                                               | 3.0.0     |
| pkceEnabled          	    | `false` 	|          	| Enable PKCE if you need it. Note: On iOS because of #111 boolean values are not overwritten. You have to explicitly define the param in the subsection.                                                                                   |        	|
| logsEnabled          	    | `false` 	|          	| Enable extensive logging. All plugin outputs are prefixed with `I/Capacitor/OAuth2ClientPlugin: ` across all platforms. Note: On iOS because of #111 boolean values are not overwritten. You have to explicitly define the param in the subsection.   | 3.0.0     |
| scope                	    |         	|          	|                                                                                                                                                                                                                                        	|       	|
| state                	    |         	|          	| The plugin always uses a state.<br>If you don't provide one we generate it.                                                                                                                                                            	|       	|
| additionalParameters 	    |         	|          	| Additional parameters for anything you might miss, like `none`, `response_mode`. <br><br>Just create a key value pair.<br>```{ "key1": "value", "key2": "value, "response_mode": "value"}``` 	                                            |       	|

**Platform Web**

| parameter     	| default 	| required 	| description                            	| since 	|
|---------------	|---------	|----------	|----------------------------------------	|-------	|
| windowOptions 	|         	|          	| e.g. width=500,height=600,left=0,top=0 	|       	|
| windowTarget  	| `_blank`  |       	|                                        	|       	|
| windowReplace  	|           |       	|                                        	| 3.0.0   	|

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

### Register plugin
On Web/PWA the plugin is registered **automatically** by Capacitor.

## Platform: Android

Prerequisite: [Capacitor Android Docs](https://capacitor.ionicframework.com/docs/android/configuration)

### Register plugin
On Android the plugin is registered **automatically** by Capacitor.

### Android Default Config

Skip this, if you use a [OAuth2CustomHandler](#custom-oauth-handler)

#### android/app/src/main/res/AndroidManifest.xml

The `AndroidManifest.xml` in your Capacitor Android project already contains
```xml
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="@string/custom_url_scheme" />
    </intent-filter>
```

Find the following line in your `AndroidManifest.xml`
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

#### android/app/src/main/res/values/strings.xml

In your `strings.xml` change the `custom_url_scheme` string to your actual scheme value. Do NOT include `://oauth/redirect` or other endpoint urls here!

```xml
<string name="custom_url_scheme">com.example.yourapp</string>

<!-- wrong -->
<!-- <string name="custom_url_scheme">com.example.yourapp://endpoint/path</string> -->
```

#### android/app/build.gradle

```groovy
android.defaultConfig.manifestPlaceholders = [
  // change to the 'custom_url_scheme' value in your strings.xml. They need to be the same. e.g.
  "appAuthRedirectScheme": "com.example.yourapp"
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

Open `ios/App/App/Info.plist` in XCode (Context menu -> Open as -> Source) and add the value of `redirectUrl` from your config without `:/` like that

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

Some OAuth provider (e.g., Facebook) force developers to use their SDK on iOS.

This plugin should be as generic as possible, so I don't want to include provider specific dependencies.

Therefore, I created a mechanism which let developers integrate custom SDK features in this plugin.
Simply configure the class name in the option property `ios.customHandlerClass`.
This class has to implement `ByteowlsCapacitorOauth2.OAuth2CustomHandler`.

See a full working example below!


## Platform: Electron

- No timeline.


## Where to store access tokens?

You can use the [capacitor-secure-storage](https://www.npmjs.com/package/capacitor-secure-storage-plugin) plugin for this.

This plugin stores data in secure locations for natives devices.
- For Android, it will store data in a [`AndroidKeyStore`](https://developer.android.com/training/articles/keystore) and a [`SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences).
- For iOS, it will store data in a [`SwiftKeychainWrapper`](https://github.com/jrendel/SwiftKeychainWrapper).


## List of Providers

These are some of the providers that can be configured with this plugin. I'm happy to add others ot the list, if you let me know.

| Name      | Example (config,...)   | Notes |
|-----------|------------------------|-------|
| Google    | [see below](#google)   |       |
| Facebook  | [see below](#facebook) |       |
| Azure     | [see below](#azure-active-directory--azure-ad-b2c)|       |
| Apple     | [see below](#apple)    | ios only |


## Examples

### Apple

#### iOS 13+
Minimum config

```typescript
appleLogin() {
  OAuth2Client.authenticate({
    appId: "xxxxxxxxx",
    authorizationBaseUrl: "https://appleid.apple.com/auth/authorize",
  });
}
```

The plugin requires `authorizationBaseUrl` as it triggers the native support and because it is needed for other platforms anyway. Those platforms are not supported yet.

`appId` is required as well for internal, generic reasons and any not blank value is fine.

It is also possible to control the scope although Apple only supports `email` and/or `fullName`. Add `siwaUseScope: true` to the ios section.
Then you can use `scope: "fullName"`, `scope: "email"` or both but the latter is the default one if `siwaUseScope` is not set or false.

```typescript
appleLogin() {
  OAuth2Client.authenticate({
    appId: "xxxxxxxxx",
    authorizationBaseUrl: "https://appleid.apple.com/auth/authorize",
    ios: {
      siwaUseScope: true,
      scope: "fullName"
    }
  });
}
```

As "Signin with Apple" is only supported since iOS 13 you should show the according button only in that case.

In Angular do sth like
```typescript
import {Component, OnInit} from '@angular/core';
import {Device, DeviceInfo} from "@capacitor/device";
import {OAuth2Client} from "@byteowls/capacitor-oauth2";

@Component({
  templateUrl: './siwa.component.html'
})
export class SiwaComponent implements OnInit {

  ios: boolean;
  siwaSupported: boolean;
  deviceInfo: DeviceInfo;

  async ngOnInit() {
      this.deviceInfo = await Device.getInfo();
      this.ios = this.deviceInfo.platform === "ios";
      if (this.ios) {
          const majorVersion: number = +this.deviceInfo.osVersion.split(".")[0];
          this.siwaSupported = majorVersion >= 13;
      }
  }
}

```

And show the button only if `siwaSupported` is `true`.

The response contains these fields:

```
"id"
"given_name"
"family_name"
"email"
"real_user_status"
"state"
"id_token"
"code"
```

#### iOS <12

not supported

#### PWA

not supported

#### Android

not supported

### Azure Active Directory / Azure AD B2C

It's important to use the urls you see in the Azure portal for the specific platform.

Note: Don't be confused by the fact that the Azure portal shows "Azure Active Directory" and "Azure AD B2C" services.
They share the same core features and therefore the plugin should work either way.

#### PWA

```typescript
import {OAuth2AuthenticateOptions, OAuth2Client} from "@byteowls/capacitor-oauth2";

export class AuthService {

  getAzureB2cOAuth2Options(): OAuth2AuthenticateOptions {
    return {
        appId: environment.oauthAppId.azureBc2.appId,
        authorizationBaseUrl: `https://login.microsoftonline.com/${environment.oauthAppId.azureBc2.tenantId}/oauth2/v2.0/authorize`,
        scope: "https://graph.microsoft.com/User.Read", // See Azure Portal -> API permission
        accessTokenEndpoint: `https://login.microsoftonline.com/${environment.oauthAppId.azureBc2.tenantId}/oauth2/v2.0/token`,
        resourceUrl: "https://graph.microsoft.com/v1.0/me/",
        responseType: "code",
        pkceEnabled: true,
        logsEnabled: true,
        web: {
            redirectUrl: environment.redirectUrl,
            windowOptions: "height=600,left=0,top=0",
        },
        android: {
            redirectUrl: "msauth://{package-name}/{url-encoded-signature-hash}" // See Azure Portal -> Authentication -> Android Configuration "Redirect URI"
        },
        ios: {
            pkceEnabled: true, // workaround for bug #111
            redirectUrl: "msauth.{package-name}://auth"
        }
    };
  }
}
```

##### Custom Scopes

If you need to use **custom scopes** configured in "API permissions" and created in "Expose an API" in Azure Portal you might need
to remove the `resourceUrl` parameter if your scopes are not included in the response. I can not give a clear advise on those Azure specifics.
Try to experiment with the config until Azure includes everything you need in the response.

<details>
<summary>A configuration with custom scopes might look like this:</summary>

```typescript
import {OAuth2Client} from "@byteowls/capacitor-oauth2";

  getAzureB2cOAuth2Options(): OAuth2AuthenticateOptions {
    return {
        appId: environment.oauthAppId.azureBc2.appId,
        authorizationBaseUrl: `https://login.microsoftonline.com/${environment.oauthAppId.azureBc2.tenantId}/oauth2/v2.0/authorize`,
        scope: "api://uuid-created-by-azure/scope.name1 api://uuid-created-by-azure/scope.name2", // See Azure Portal -> API permission / Expose an API
        accessTokenEndpoint: `https://login.microsoftonline.com/${environment.oauthAppId.azureBc2.tenantId}/oauth2/v2.0/token`,
        // no resourceURl!
        responseType: "code",
        pkceEnabled: true,
        logsEnabled: true,
        web: {
            redirectUrl: environment.redirectUrl,
            windowOptions: "height=600,left=0,top=0",
        },
        android: {
            redirectUrl: "msauth://{package-name}/{url-encoded-signature-hash}" // See Azure Portal -> Authentication -> Android Configuration "Redirect URI"
        },
        ios: {
            pkceEnabled: true, // workaround for bug #111
            redirectUrl: "msauth.{package-name}://auth"
        }
    };
  }
}
```
</details>

##### Prior configs
<details>
<summary>Other configs that works in prior versions</summary>

```typescript
import {OAuth2Client} from "@byteowls/capacitor-oauth2";

azureLogin() {
  OAuth2Client.authenticate({
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
        redirectUrl: "com.tenant.app://oauth/auth", // Use the value from Azure config. Platform "Android"
        accessTokenEndpoint: "https://tenantb2c.b2clogin.com/tfp/tenantb2c.onmicrosoft.com/B2C_1_SignUpAndSignIn/oauth2/v2.0/token",
        handleResultOnNewIntent: true,
        handleResultOnActivityResult: true
    },
    ios: {
        pkceEnabled: true,
        responseType: "code",
        redirectUrl: "msauth.BUNDLE_ID://oauth", // Use the value from Azure config. Platform "iOS/Mac"
        accessTokenEndpoint: "https://tenantb2c.b2clogin.com/tfp/tenantb2c.onmicrosoft.com/B2C_1_SignUpAndSignIn/oauth2/v2.0/token",
    }
  });
}
```

```typescript
import {OAuth2Client} from "@byteowls/capacitor-oauth2";

azureLogin() {
  OAuth2Client.authenticate({
    appId: 'XXXXXXXXXX-XXXXXXXXXX-XXXXXXXXX',
    authorizationBaseUrl: 'https://TENANT.b2clogin.com/tfp/TENANT.onmicrosoft.com/B2C_1_policy-signin-signup-web/oauth2/v2.0/authorize',
    accessTokenEndpoint: '',
    scope: 'https://XXXXXXX.onmicrosoft.com/TestApi4/demo.read',
    responseType: 'token',
    web: {
      redirectUrl: 'http://localhost:8100/'
    },
    android: {
      pkceEnabled: true,
      responseType: 'code',
      redirectUrl: 'com.company.project://oauth/redirect',
      accessTokenEndpoint: 'https://TENANT.b2clogin.com/TENANT.onmicrosoft.com/B2C_1_policy-signin-signup-web',
      handleResultOnNewIntent: true,
      handleResultOnActivityResult: true
    },
    ios: {
      pkceEnabled: true,
      responseType: 'code',
      redirectUrl: 'com.company.project://oauth',
      accessTokenEndpoint: 'https://TENANT.b2clogin.com/TENANT.onmicrosoft.com/B2C_1_policy-signin-signup-web',
    }
  });
}
```

</details>

#### Android

If you have **only** Azure B2C as identity provider you have to add a new `intent-filter` to your main activity in `AndroidManifest.xml`.

```xml
<!-- azure ad b2c -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="@string/azure_b2c_scheme" android:host="@string/package_name" android:path="@string/azure_b2c_signature_hash" />
</intent-filter>
```

If you have **multiple** identity providers **or** your logins always ends in a `USER_CANCELLED` error like in [#178](https://github.com/moberwasserlechner/capacitor-oauth2/issues/178)
you have to create an additional Activity in `AndroidManifest.xml`.

These are both activities! Make sure to replace `com.company.project.MainActivity` with your real qualified class path!
```xml
<activity
      android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|smallestScreenSize|screenLayout|uiMode"
      android:name="com.company.project.MainActivity"
      android:label="@string/title_activity_main"
      android:launchMode="singleTask"
      android:theme="@style/AppTheme.NoActionBarLaunch">

      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>

      <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="@string/custom_url_scheme" android:host="@string/custom_host" />
      </intent-filter>

    </activity>

    <activity android:name="net.openid.appauth.RedirectUriReceiverActivity" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="@string/custom_url_scheme" android:host="@string/custom_host" />
      </intent-filter>

      <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="@string/azure_b2c_scheme" android:host="@string/package_name" android:path="@string/azure_b2c_signature_hash" />
      </intent-filter>
    </activity>
```

Values for `android/app/src/main/res/values/string.xml`. Replace the example values!
```
  <string name="title_activity_main">Your Project's Name/string>
  <string name="custom_url_scheme">com.company.project</string>
  <string name="custom_host">foo</string><!-- any value is fine -->
  <string name="package_name">com.company.project</string>
  <string name="azure_b2c_scheme">msauth</string>
  <string name="azure_b2c_signature_hash">/your-signature-hash</string><!-- The leading slash is required. Copied from Azure Portal Android Config "Signature hash" field -->
```

See [Android Default Config](#android-default-config)

#### iOS

Open `Info.plist` in XCode by clicking right on that file -> Open as -> Source Code. Note: XCode does not "like" files opened and changed externally.

```xml
	<key>CFBundleURLTypes</key>
	<array>
		<dict>
			<key>CFBundleURLSchemes</key>
			<array>
				<!-- msauth.BUNDLE_ID -->
				<string>msauth.com.yourcompany.yourproject</string>
			</array>
		</dict>
	</array>
```

**Important:**

* Do not enter `://` as part of your redirect url
* Make sure the `msauth.` prefix is present

#### Troubleshooting
In case of problems please read [#91](https://github.com/moberwasserlechner/capacitor-oauth2/issues/91)
and [#96](https://github.com/moberwasserlechner/capacitor-oauth2/issues/96)

See this [example repo](https://github.com/loonix/capacitor-oauth2-azure-example) by @loonix.

### Google

#### PWA
```typescript
import {OAuth2Client} from "@byteowls/capacitor-oauth2";

googleLogin() {
    OAuth2Client.authenticate({
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

#### PWA

```typescript
import {OAuth2Client} from "@byteowls/capacitor-oauth2";

facebookLogin() {
    let fbApiVersion = "2.11";
    OAuth2Client.authenticate({
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

#### Android

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
    // Initialize Facebook SDK
    FacebookSdk.sdkInitialize(this.getApplicationContext());
    callbackManager = CallbackManager.Factory.create();
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

1) Add Facebook pods to `ios/App/Podfile` and run `pod install` afterwards

```
platform :ios, '13.0'
use_frameworks!

# workaround to avoid Xcode caching of Pods that requires
# Product -> Clean Build Folder after new Cordova plugins installed
# Requires CocoaPods 1.6 or newer
install! 'cocoapods', :disable_input_output_paths => true

def capacitor_pods
  pod 'Capacitor', :path => '../../node_modules/@capacitor/ios'
  pod 'CapacitorCordova', :path => '../../node_modules/@capacitor/ios'
  pod 'ByteowlsCapacitorOauth2', :path => '../../node_modules/@byteowls/capacitor-oauth2'
  # core plugins
  pod 'CapacitorApp', :path => '../../node_modules/@capacitor/app'
  pod 'CapacitorDevice', :path => '../../node_modules/@capacitor/device'
  pod 'CapacitorKeyboard', :path => '../../node_modules/@capacitor/keyboard'
  pod 'CapacitorSplashScreen', :path => '../../node_modules/@capacitor/splash-screen'
  pod 'CapacitorStatusBar', :path => '../../node_modules/@capacitor/status-bar'
end

target 'App' do
  capacitor_pods
  # Add your Pods here
  pod 'FacebookCore'
  pod 'FacebookLogin'
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

See [Contribution Guidelines](https://github.com/moberwasserlechner/capacitor-oauth2/blob/main/.github/CONTRIBUTING.md).

## Changelog
See [CHANGELOG](https://github.com/moberwasserlechner/capacitor-oauth2/blob/main/CHANGELOG.md).

## License

MIT. See [LICENSE](https://github.com/moberwasserlechner/capacitor-oauth2/blob/main/LICENSE).

## BYTEOWLS Software & Consulting

This plugin is powered by [BYTEOWLS Software & Consulting](https://byteowls.com).

If you need extended support for this project like critical changes or releases ahead of schedule. Feel free to contact us for a consulting offer.

## Disclaimer

We have no business relation to Ionic.
