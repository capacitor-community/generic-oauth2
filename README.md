# Capacitor OAuth 2 client plugin 

[![npm](https://img.shields.io/npm/v/@teamconductor/capacitor-oauth2.svg)](https://www.npmjs.com/package/@teamconductor/capacitor-oauth2) 
[![npm](https://img.shields.io/npm/dt/@teamconductor/capacitor-oauth2.svg?label=npm%20downloads)](https://www.npmjs.com/package/@teamconductor/capacitor-oauth2)

This is a simple OAuth 2 client plugin. 

It let you configure the oauth parameters yourself instead of using SDKs. Therefore it is usable with various providers.

## Installation

`npm i @teamconductor/capacitor-oauth2`

## Configuration

This example shows the common process of configuring this plugin. 

Although it was taken from a Angular 6 application, it should work in other frameworks as well. 

```typescript
import {OAuth2AuthenticateResult, OAuth2Client} from '@teamconductor/capacitor-oauth2';

@Component({
  template: '<button (click)="onFacebookBtnClick()">Login with Facebook</button>',
})
export class SignupComponent {
    onFacebookBtnClick() {
        OAuth2Client.authenticate({
            appId: "YOUR_FACEBOOK_APP_ID",
            authorizationBaseUrl: "https://www.facebook.com/v2.11/dialog/oauth",
            resourceUrl: "https://graph.facebook.com/v2.11/me",
            web: {
                redirectUrl: "http://localhost:4200/",
                // https://www.w3schools.com/jsref/met_win_open.asp
                windowOptions: "height=600,left=0,top=0"
            }
        }).then(resourceUrlResponse => {
            let oauthUserId = resourceUrlResponse["id"];
            let name = resourceUrlResponse["name"];
            // go to backend
        }).catch(reason => {
            console.error("OAuth rejected", reason);
        });
    }
}
```

Other working examples are:

**Google**

```typescript
OAuth2Client.authenticate({
    appId: "YOUR_GOOGLE_APP_ID",
    authorizationBaseUrl: "https://accounts.google.com/o/oauth2/auth",
    scope: "email profile",
    resourceUrl: "https://www.googleapis.com/userinfo/v2/me",
    web: {
        redirectUrl: "http://localhost:4200/"
    },
}).then(resourceUrlResponse => {
    let oauthUserId = resourceUrlResponse["id"];
    let name = resourceUrlResponse["name"];
    let email = resourceUrlResponse["email"];
    let fn = resourceUrlResponse["given_name"];
    let ln = resourceUrlResponse["family_name"];
    // go to backend
}).catch(reason => {
    console.error("Google OAuth rejected", reason);
});
```

**Amazon**

```typescript
OAuth2Client.authenticate({
    appId: "YOUR_AMAZON_APP_ID",
    authorizationBaseUrl: "https://www.amazon.com/ap/oa",
    scope: "profile:user_id",
    resourceUrl: "https://api.amazon.com/user/profile",
    web: {
        redirectUrl: "http://localhost:4200/"
    },
}).then(resourceUrlResponse => {
    let oauthUserId = resourceUrlResponse["user_id"];
    // go to backend
}).catch(reason => {
    console.error("Amazon OAuth rejected", reason);
});
``` 


## Platform: Web/PWA

This implementation just opens a browser window to let users enter their credentials.

As there is no provider SDK used to accomplish OAuth, no additional javascript files must be loaded and so there is no performance 
impact using this plugin in a web applications.

- Available since version: **1.0.0-alpha.16**

## Platform: Android

Add the following to your `androidManifest.xml`

```xml

```

- Available since version: **Work in progress**

## Platform: iOS

- ETA November 2018
 
## Platform: Electron
 
- No ETA yet
 
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

