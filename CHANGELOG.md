# Changelog

## [4.0.0] - 2022-09-18

### Fixed

* Detection of Network Errors when refreshing Tokens [#192](https://github.com/moberwasserlechner/capacitor-oauth2/issues/192)
* Popup blocked in Safari for pkce flow [#216](https://github.com/moberwasserlechner/capacitor-oauth2/issues/216)

### Breaking
* Minimum Capacitor version is **4.0.0**.! [#211](https://github.com/moberwasserlechner/capacitor-oauth2/issues/211)

## [3.0.1] - 2021-08-11

### Docs
* Where to securely save tokens [README](https://github.com/moberwasserlechner/capacitor-oauth2/#where-to-store-access-tokens) entry. [#139](https://github.com/moberwasserlechner/capacitor-oauth2/issues/139) Thank you [@RaphaelWoude](https://github.com/RaphaelWoude)

### Changed
* Chore: Use main instead of master branch. [#168](https://github.com/moberwasserlechner/capacitor-oauth2/issues/168)

### Fixed
* Android: Use json for responses instead of string. [#171](https://github.com/moberwasserlechner/capacitor-oauth2/issues/171) Thank you [@webflo](https://github.com/webflo)

## [3.0.0] - 2021-08-02

### Breaking
* Minimum Capacitor version is **3.0.0**. Only this plugin version supports Capacitor `3.x`!  [#138](https://github.com/moberwasserlechner/capacitor-oauth2/issues/138) [#140](https://github.com/moberwasserlechner/capacitor-oauth2/pull/140)

### Added
* Web: Add a new option `windowReplace` that defaults to undefined. Used in `window.open()` 4th param.
  This will fix https://bugs.chromium.org/p/chromium/issues/detail?id=1164959 [#153](https://github.com/moberwasserlechner/capacitor-oauth2/issues/153)
* Web, Android: Add "authorization_response" and "access_token_response" to the result returned to JS. On iOS it is not possible to extract the authorization response because of the used lib.  [#154](https://github.com/moberwasserlechner/capacitor-oauth2/issues/154)
* Web, Android: Added `additionalResourceHeaders` to base options
* Web, Android, iOS: Added `logsEnabled` to base options. If enabled extensive logs are written. All logs are prefixed with `I/Capacitor/OAuth2ClientPlugin` across all platforms.

### Changed
* Use `window.crypto` if available to generate random strings [#138](https://github.com/moberwasserlechner/capacitor-oauth2/issues/138) [#140](https://github.com/moberwasserlechner/capacitor-oauth2/pull/140)

### Fixed
* Web: # in URL causes parser to ignore ?  [#132](https://github.com/moberwasserlechner/capacitor-oauth2/issues/132) [#133](https://github.com/moberwasserlechner/capacitor-oauth2/pull/133)
* Android: Fix boolean param inheritance (#162) [#162](https://github.com/moberwasserlechner/capacitor-oauth2/issues/162)

## [2.1.0] - 2020-08-27

### Added

* ios: Sign in with Apple. Closes [#45](https://github.com/moberwasserlechner/capacitor-oauth2/issues/45).
The plugin will detect that the iOS 13+ buildin UI is needed, when `authorizationBaseUrl` contains `appleid.apple.com`.
This is needed for other platforms and iOS <=12 anyway. Android, web, iOS <12 are not supported in this release.

### Fixed

* web: Make web flow work if server and client are hosted on same server. Closes [#94](https://github.com/moberwasserlechner/capacitor-oauth2/issues/94). thx [@klot-git](https://github.com/klot-git)

### Changed

* iOS: Upgrade SwiftOAuth2 to head. Closes [#105](https://github.com/moberwasserlechner/capacitor-oauth2/issues/105)

## [2.0.0] - 2020-04-20

### Breaking
* Core: Capacitor 2.x is new minimum peer dependency. closes #80.
* `responseType` is required. Default values were removed. In favor of configuring anything. closes #86.
* `pkceDisabled` was replaced with `pkceEnabled`, which is NOT enabled by default. If you like to use PKCE set this to true.
* If a flow must not have a `accessTokenEndpoint` but you configured one as base parameter you have to
overwrite it in the according platform sections. `accessTokenEndpoint: ""` see Google example in README.
* Add `redirectUrl` to base parameter and make it overwritable in the platform sections. closes #84.
  * Android: `customScheme` replaced by `redirectUrl`
  * iOS: `customScheme` replaced by `redirectUrl`
* Additional method argument for `OAuth2CustomHandler#logout`. closes #58
  * Android: `activity` as 1st argument
  * iOS: `viewController` as 1st argument

### Added
* iOS: If the user touches "done" in safari without entering the credentials
the USER_CANCELLED error is sent. closes #71
* Web: Include all url params from the accessToken request if no resourceUrl is present. closes #72. thx [@sanjaywadhwani](https://github.com/sanjaywadhwani)
* Android: Add an alternative to handle the activity result intent.
This is controlled by Android specific parameters `handleResultOnNewIntent` for the alternative and `handleResultOnActivityResult` for the default. closes #52, #55.

### Changed
* Android: Allow no resource url and just return every we got until so far. closes #75. thx [@0x4AMiller](https://github.com/0x4AMiller)
* Web, iOS, Android: All base parameters are overwritable in the platform sections. closes #84.
* Restriction to the response type `code` and `token` was removed. Devs can configure anything but are responsible for it as well. closes #86.

### Fixed

* iOS: XCode 11.4 crash on app start. closes #73. thx [@macdja38](https://github.com/macdja38)

### Docs

* CustomHandler Facebook example logout fixed. closes #79. thx [@REPTILEHAUS](https://github.com/REPTILEHAUS)
* Facebook force authentication with FB App. closes #69. thx [@mrbatista](https://github.com/mrbatista)

## [1.1.0] - 2020-01-22
### Changed
- Docs for Facebook if using iOS 13 and Facebook pod 5.x #56
- Align Android behavior to iOS where the additional parameters are not overwritten #57 (thx @maggix)
- Upgrade dev dependencies to Capacitor 1.4.0

### Added
- Refresh token feature for iOS and Android #64 (thx @dennisameling)
- Detect when user cancels authentication on web (implicit flow) #25 (thx @michaeltintiuc)

## [1.0.1] - 2019-09-19
### Added
- Add OpenID not supported to README
- Add CHANGELOG file to project

### Fixed
- web/pwa: `pkceCodeChallenge` was always `undefined` because promise was not awaited properly #53 (thx @nicksteenstra)

## [1.0.0] - 2019-06-26

### Added
- Add minimum cap version to installation notice

### Changed
- Upgrade to Capacitor 1.0.0 #43,#39

### Fixed
- Android: Fix plugin does not send resource url response to app after specific steps #28
- Android: Fix Java compiler error #36 (thx @Anthbs)
- Fix github security error by updating Jest lib

[Unreleased]: https://github.com/moberwasserlechner/capacitor-oauth2/compare/4.0.0...main
[4.0.0]: https://github.com/moberwasserlechner/capacitor-oauth2/compare/3.0.1...4.0.0
[3.0.1]: https://github.com/moberwasserlechner/capacitor-oauth2/compare/3.0.0...3.0.1
[3.0.0]: https://github.com/moberwasserlechner/capacitor-oauth2/compare/2.1.0...3.0.0
[2.1.0]: https://github.com/moberwasserlechner/capacitor-oauth2/compare/2.0.0...2.1.0
[2.0.0]: https://github.com/moberwasserlechner/capacitor-oauth2/compare/1.1.0...2.0.0
[1.1.0]: https://github.com/moberwasserlechner/capacitor-oauth2/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/moberwasserlechner/capacitor-oauth2/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/moberwasserlechner/capacitor-oauth2/releases/tag/1.0.0
