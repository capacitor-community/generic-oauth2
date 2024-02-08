import Foundation
import Capacitor
import OAuthSwift
import CommonCrypto
import AuthenticationServices

typealias JSObject = [String:Any]

@objc(OAuth2ClientPlugin)
public class OAuth2ClientPlugin: CAPPlugin {

    var savedPluginCall: CAPPluginCall?

    let JSON_KEY_ACCESS_TOKEN = "access_token"
    let JSON_KEY_AUTHORIZATION_RESPONSE = "authorization_response"
    let JSON_KEY_ACCESS_TOKEN_RESPONSE = "access_token_response"

    let PARAM_REFRESH_TOKEN = "refreshToken"

    // required
    let PARAM_APP_ID = "appId"
    let PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl"
    let PARAM_RESPONSE_TYPE = "responseType"
    let PARAM_REDIRECT_URL = "redirectUrl"
    // controlling
    let PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint"
    let PARAM_RESOURCE_URL = "resourceUrl"
    let PARAM_ADDITIONAL_RESOURCE_HEADERS = "additionalResourceHeaders"

    let PARAM_ADDITIONAL_PARAMETERS = "additionalParameters"
    let PARAM_CUSTOM_HANDLER_CLASS = "ios.customHandlerClass"
    let PARAM_SCOPE = "scope"
    let PARAM_STATE = "state"
    let PARAM_PKCE_ENABLED = "pkceEnabled"
    let PARAM_IOS_USE_SCOPE = "ios.siwaUseScope"
    let PARAM_LOGOUT_URL = "logoutUrl"
    let PARAM_LOGS_ENABLED = "logsEnabled"

    let ERR_GENERAL = "ERR_GENERAL"

    // authenticate param validation
    let ERR_PARAM_NO_APP_ID = "ERR_PARAM_NO_APP_ID"
    let ERR_PARAM_NO_AUTHORIZATION_BASE_URL = "ERR_PARAM_NO_AUTHORIZATION_BASE_URL"
    let ERR_PARAM_NO_RESPONSE_TYPE = "ERR_PARAM_NO_RESPONSE_TYPE"
    let ERR_PARAM_NO_REDIRECT_URL = "ERR_PARAM_NO_REDIRECT_URL"

    // refreshToken param validation
    let ERR_PARAM_NO_REFRESH_TOKEN = "ERR_PARAM_NO_REFRESH_TOKEN"
    let ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT = "ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT"

    let ERR_CUSTOM_HANDLER_LOGIN = "ERR_CUSTOM_HANDLER_LOGIN"
    let ERR_CUSTOM_HANDLER_LOGOUT = "ERR_CUSTOM_HANDLER_LOGOUT"
    let ERR_STATES_NOT_MATCH = "ERR_STATES_NOT_MATCH"
    let ERR_NO_AUTHORIZATION_CODE = "ERR_NO_AUTHORIZATION_CODE"
    let ERR_AUTHORIZATION_FAILED = "ERR_AUTHORIZATION_FAILED"

    struct SharedConstants {
        static let ERR_USER_CANCELLED = "USER_CANCELLED"
    }

    var oauthSwift: OAuth2Swift?
    var oauth2SafariDelegate: OAuth2SafariDelegate?
    var handlerClasses = [String: OAuth2CustomHandler.Type]()
    var handlerInstances = [String: OAuth2CustomHandler]()

    func registerHandlers() {
        let classCount = objc_getClassList(nil, 0)
        let classes = UnsafeMutablePointer<AnyClass?>.allocate(capacity: Int(classCount))

        let releasingClasses = AutoreleasingUnsafeMutablePointer<AnyClass>(classes)
        let numClasses: Int32 = objc_getClassList(releasingClasses, classCount)

        for i in 0..<Int(numClasses) {
            if let c: AnyClass = classes[i] {
                if class_conformsToProtocol(c, OAuth2CustomHandler.self) {
                    let className = NSStringFromClass(c)
                    let pluginType = c as! OAuth2CustomHandler.Type
                    handlerClasses[className] = pluginType
                    log("Custom handler class '\(className)' found!")
                }
            }
        }

        classes.deallocate()
    }

    public override func load() {
        NotificationCenter.default.addObserver(self, selector: #selector(self.handleRedirect(notification:)), name: .capacitorOpenURL, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.handleRedirect(notification:)), name: .capacitorOpenUniversalLink, object: nil)
        registerHandlers()
    }

    @objc func handleRedirect(notification: NSNotification) {
        guard let object = notification.object as? [String: Any?] else {
            return
        }
        guard let url = object["url"] as? URL else {
            return
        }
        OAuth2Swift.handle(url: url);
    }

    /*
     * Plugin function to refresh tokens
     */
    @objc func refreshToken(_ call: CAPPluginCall) {
        guard let appId = getOverwritableString(call, PARAM_APP_ID) else {
            call.reject(self.ERR_PARAM_NO_APP_ID)
            return
        }

        guard let accessTokenEndpoint = getOverwritableString(call, PARAM_ACCESS_TOKEN_ENDPOINT) else {
            call.reject(self.ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT)
            return
        }

        guard let refreshToken = getOverwritableString(call, PARAM_REFRESH_TOKEN) else {
            call.reject(self.ERR_PARAM_NO_REFRESH_TOKEN)
            return
        }

        let oauthSwift = OAuth2Swift(
            consumerKey: appId,
            consumerSecret: "", // never ever store the app secret on client!
            authorizeUrl: "",
            accessTokenUrl: accessTokenEndpoint,
            responseType: "code"
        )

        self.oauthSwift = oauthSwift

        let scope = getOverwritableString(call, PARAM_SCOPE) ?? nil;
        var parameters: OAuthSwift.Parameters = [:];

        if scope != nil {
            parameters["scope"] = scope;
        }

        oauthSwift.renewAccessToken(withRefreshToken: refreshToken, parameters: parameters) { result in
            switch result {
            case .success(let tokenSuccess):
                do {
                    let jsonObj = try JSONSerialization.jsonObject(with: tokenSuccess.response!.data, options: []) as! JSObject
                    call.resolve(jsonObj)
                } catch {
                    self.log("Invalid json in renew access token response \(error.localizedDescription)")
                    call.reject(self.ERR_GENERAL)
                }
            case .failure(let error):
                switch error {
                case .cancelled, .accessDenied(_, _):
                    call.reject(SharedConstants.ERR_USER_CANCELLED)
                case .stateNotEqual( _, _):
                    self.log("The given state does not match the one in the respond!")
                    call.reject(self.ERR_STATES_NOT_MATCH)
                case .requestError(let underlyingError, _):
                    let nsError = (underlyingError as NSError);
                    let errorCode = nsError.code;
                    let responseBodyString = (nsError.userInfo["Response-Body"]) as? String;
                    self.log("Authorization failed with requestError \(responseBodyString ?? "")");

                    do {
                        let responseBody = Data((responseBodyString ?? "").utf8);
                        if let json = try JSONSerialization.jsonObject(with: responseBody, options: []) as? [String: Any] {
                            call.reject(json["error"] as? String ?? self.ERR_GENERAL, String(errorCode), underlyingError, json)
                        }
                    } catch  {
                        call.reject(self.ERR_GENERAL, String(errorCode), underlyingError)
                    }
                default:
                    self.log("Authorization failed with \(error.localizedDescription)");
                    call.reject(self.ERR_AUTHORIZATION_FAILED)
                }
            }
        }
    }

    /*
     * Plugin function to authenticate
     */
    @objc func authenticate(_ call: CAPPluginCall) {
        guard let appId = getOverwritableString(call, PARAM_APP_ID), !appId.isEmpty else {
            call.reject(self.ERR_PARAM_NO_APP_ID)
            return
        }
        let resourceUrl = getOverwritableString(call, self.PARAM_RESOURCE_URL)
        let logsEnabled: Bool = getOverwritable(call, self.PARAM_LOGS_ENABLED) as? Bool ?? false
        // #71
        self.oauth2SafariDelegate = OAuth2SafariDelegate(call)

        // ######### Custom Handler ########

        if let handlerClassName = getString(call, PARAM_CUSTOM_HANDLER_CLASS) {
            if let handlerInstance = self.getOrLoadHandlerInstance(className: handlerClassName) {
                log("Entering custom handler: " + handlerClassName)
                handlerInstance.getAccessToken(viewController: (bridge?.viewController)!, call: call, success: { (accessToken) in

                    if resourceUrl != nil {
                        let client = OAuthSwiftClient(
                            consumerKey: appId,
                            consumerSecret: "",
                            oauthToken: accessToken,
                            oauthTokenSecret: "",
                            version: OAuthSwiftCredential.Version.oauth2)

                        client.get(resourceUrl!) { result in
                            switch result {
                            case .success(let response):
                                if var jsonObj = try? JSONSerialization.jsonObject(with: response.data, options: []) as? JSObject {
                                    // send the access token to the caller so e.g. it can be stored on a backend
                                    jsonObj.updateValue(accessToken, forKey: self.JSON_KEY_ACCESS_TOKEN)
                                    call.resolve(jsonObj)
                                } else {
                                    self.log("Invalid json in resource response. '\(response.data)'")
                                    call.reject(self.ERR_GENERAL)
                                }
                            case .failure(let error):
                                self.log("Resource url request error '\(error)'")
                                call.reject(self.ERR_CUSTOM_HANDLER_LOGIN);
                            }
                        }
                    } else {
                        // create a json object with just the access tokens
                        var jsonObj = JSObject()
                        jsonObj.updateValue(accessToken, forKey: self.JSON_KEY_ACCESS_TOKEN)
                        call.resolve(jsonObj)
                    }
                }, cancelled: {
                    call.reject(SharedConstants.ERR_USER_CANCELLED)
                }, failure: { error in
                    if logsEnabled {
                        self.log("Login failed because '\(error)'")
                    }
                    call.reject(self.ERR_CUSTOM_HANDLER_LOGIN)
                })
            } else {
                log("Handler class '\(handlerClassName)' not implements OAuth2CustomHandler protocol")
                call.reject(self.ERR_CUSTOM_HANDLER_LOGIN)
            }
        } else {
            guard let baseUrl = getOverwritableString(call, PARAM_AUTHORIZATION_BASE_URL), !baseUrl.isEmpty else {
                call.reject(self.ERR_PARAM_NO_AUTHORIZATION_BASE_URL)
                return
            }

            // Sign in with Apple
            if baseUrl.contains("appleid.apple.com"), #available(iOS 13.0, *) {
                self.handleSignInWithApple(call)
            } else {
                guard let responseType = getOverwritableString(call, PARAM_RESPONSE_TYPE), !responseType.isEmpty else {
                    call.reject(self.ERR_PARAM_NO_RESPONSE_TYPE)
                    return
                }

                guard let redirectUrl = getOverwritableString(call, PARAM_REDIRECT_URL), !redirectUrl.isEmpty else {
                    call.reject(self.ERR_PARAM_NO_REDIRECT_URL)
                    return
                }


                var oauthSwift: OAuth2Swift
                if let accessTokenEndpoint = getOverwritableString(call, PARAM_ACCESS_TOKEN_ENDPOINT), !accessTokenEndpoint.isEmpty {
                    oauthSwift = OAuth2Swift(
                        consumerKey: appId,
                        consumerSecret: "", // never ever store the app secret on client!
                        authorizeUrl: baseUrl,
                        accessTokenUrl: accessTokenEndpoint,
                        responseType: responseType
                    )
                } else {
                    oauthSwift = OAuth2Swift(
                        consumerKey: appId,
                        consumerSecret: "", // never ever store the app secret on client!
                        authorizeUrl: baseUrl,
                        responseType: responseType
                    )
                }

                let urlHandler = SafariURLHandler(viewController: (bridge?.viewController)!, oauthSwift: oauthSwift)
                // if the user touches "done" in safari without entering the credentials the USER_CANCELLED error is sent #71
                urlHandler.delegate = self.oauth2SafariDelegate
                oauthSwift.authorizeURLHandler = urlHandler
                self.oauthSwift = oauthSwift

                // additional parameters #18
                let callParameter: [String: Any] = getOverwritable(call, PARAM_ADDITIONAL_PARAMETERS) as? [String: Any] ?? [:]
                let additionalParameters = buildStringDict(callParameter);

                let requestState = getOverwritableString(call, PARAM_STATE) ?? generateRandom(withLength: 20)
                let pkceEnabled: Bool = getOverwritable(call, PARAM_PKCE_ENABLED) as? Bool ?? false
                // if response type is code and pkce is not disabled
                if pkceEnabled {
                    let pkceCodeVerifier = generateRandom(withLength: 64)
                    let pkceCodeChallenge = pkceCodeVerifier.sha256().base64()

                    oauthSwift.authorize(
                        withCallbackURL: redirectUrl,
                        scope: getOverwritableString(call, PARAM_SCOPE) ?? "",
                        state: requestState,
                        codeChallenge: pkceCodeChallenge,
                        codeVerifier: pkceCodeVerifier,
                        parameters: additionalParameters) { result in
                            self.handleAuthorizationResult(result, call, responseType, requestState, logsEnabled, resourceUrl)
                    }
                } else {
                    oauthSwift.authorize(
                        withCallbackURL: redirectUrl,
                        scope: getOverwritableString(call, PARAM_SCOPE) ?? "",
                        state: requestState,
                        parameters: additionalParameters) { result in
                            self.handleAuthorizationResult(result, call, responseType, requestState, logsEnabled, resourceUrl)
                    }
                }
            }


        }
    }

    /*
     * Plugin function to refresh tokens
     */
    @objc func logout(_ call: CAPPluginCall) {
        if let handlerClassName = getString(call, PARAM_CUSTOM_HANDLER_CLASS) {
            if let handlerInstance = self.getOrLoadHandlerInstance(className: handlerClassName) {
                let success: Bool! = handlerInstance.logout(viewController: (bridge?.viewController!)!, call: call)
                if success {
                    call.resolve();
                } else {
                    self.log("Custom handler logout failed!")
                    call.reject(self.ERR_CUSTOM_HANDLER_LOGOUT)
                }
            } else {
                log("Handler instance not found! Bug!")
                call.reject(self.ERR_CUSTOM_HANDLER_LOGOUT)
            }
        } else {
            if self.oauthSwift != nil {
                self.oauthSwift = nil
            }
            call.resolve()
        }
    }

    // #################################
    // ### Helper functions
    // #################################

    private func handleAuthorizationResult(_ result: Result<OAuthSwift.TokenSuccess, OAuthSwiftError>,
                                           _ call: CAPPluginCall,
                                           _ responseType: String,
                                           _ requestState: String,
                                           _ logsEnabled: Bool,
                                           _ resourceUrl: String?) {
        switch result {
        case .success(let (credential, response, parameters)):
            if logsEnabled, let accessTokenResponse = response {
                logDataObj("Authorization or Access token response:", accessTokenResponse.data)
            }

            // state is aready checked by the lib
            if resourceUrl != nil && !resourceUrl!.isEmpty {
                if logsEnabled {
                    log("Resource url: \(resourceUrl!)")
                    log("Access token:\n\(credential.oauthToken)")
                }
                // resource url request headers
                let callParameter: [String: Any] = getOverwritable(call, PARAM_ADDITIONAL_RESOURCE_HEADERS) as? [String: Any] ?? [:]
                let additionalHeadersDict = buildStringDict(callParameter);

                self.oauthSwift!.client.get(resourceUrl!,
                                            headers: additionalHeadersDict) { result in
                        switch result {
                        case .success(let resourceResponse):
                            do {
                                if logsEnabled {
                                    self.logDataObj("Resource response:", resourceResponse.data)
                                }

                                var jsonObj = try JSONSerialization.jsonObject(with: resourceResponse.data, options: []) as! JSObject
                                // send the access token to the caller so e.g. it can be stored on a backend
                                // #154
                                if let accessTokenResponse = response {
                                    let accessTokenJsObject = try? JSONSerialization.jsonObject(with: accessTokenResponse.data, options: []) as? JSObject
                                    jsonObj.updateValue(accessTokenJsObject!, forKey: self.JSON_KEY_ACCESS_TOKEN_RESPONSE)
                                }

                                jsonObj.updateValue(credential.oauthToken, forKey: self.JSON_KEY_ACCESS_TOKEN)

                                if logsEnabled {
                                    self.log("Returned to JS:\n\(jsonObj)")
                                }

                                call.resolve(jsonObj)
                            } catch {
                                self.log("Invalid json in resource response:\n \(error.localizedDescription)")
                                call.reject(self.ERR_GENERAL)
                            }
                        case .failure(let error):
                            self.log("Resource url request failed:\n\(error.description)");
                            call.reject(self.ERR_GENERAL)
                        }
                }
            // no resource url
            } else if let responseData = response?.data {
                do {
                    var jsonObj = JSObject()
                    let accessTokenJsObject = try? JSONSerialization.jsonObject(with: responseData, options: []) as? JSObject
                    jsonObj.updateValue(accessTokenJsObject!, forKey: self.JSON_KEY_ACCESS_TOKEN_RESPONSE)

                    if logsEnabled {
                        self.log("Returned to JS:\n\(jsonObj)")
                    }
                    call.resolve(jsonObj)
                } catch {
                    self.log("Invalid json in response \(error.localizedDescription)")
                    call.reject(self.ERR_GENERAL)
                }
            } else {
                // `parameters` will be response parameters
                var result = parameters
                result.updateValue(credential.oauthToken, forKey: self.JSON_KEY_ACCESS_TOKEN)
                call.resolve(parameters)
            }
        case .failure(let error):
            switch error {
            case .cancelled, .accessDenied(_, _):
                call.reject(SharedConstants.ERR_USER_CANCELLED)
            case .stateNotEqual(_, _):
                self.log("The given state does not match the one in the respond!")
                call.reject(self.ERR_STATES_NOT_MATCH)
            default:
                self.log("Authorization failed with \(error.localizedDescription)");
                call.reject(self.ERR_NO_AUTHORIZATION_CODE)
            }
        }
    }

    private func getConfigObjectDeepest(_ options: [AnyHashable: Any?]!, key: String) -> [AnyHashable:Any?]? {
        let parts = key.split(separator: ".")

        var o = options
        for (_, k) in parts[0..<parts.count-1].enumerated() {
            if o != nil {
                o = o?[String(k)] as? [String:Any?] ?? nil
            }
        }
        return o
    }

    private func getConfigKey(_ key: String) -> String {
        let parts = key.split(separator: ".")
        if parts.last != nil {
            return String(parts.last!)
        }
        return ""
    }

    private func getOverwritableString(_ call: CAPPluginCall, _ key: String) -> String? {
        var base = getString(call, key)
        let ios = getString(call, "ios." + key)
        if ios != nil {
            base = ios
        }
        return base;
    }

    private func getOverwritable(_ call: CAPPluginCall, _ key: String) -> Any? {
        var base = getValue(call, key)
        let ios = getValue(call, "ios." + key)
        if ios != nil {
            base = ios
        }
        return base;
    }

    private func getValue(_ call: CAPPluginCall, _ key: String) -> Any? {
        let k = getConfigKey(key)
        let o = getConfigObjectDeepest(call.options, key: key)
        return o?[k] ?? nil
    }

    private func getString(_ call: CAPPluginCall, _ key: String) -> String? {
        let value = getValue(call, key)
        if value == nil {
            return nil
        }
        return value as? String
    }

    private func getOrLoadHandlerInstance(className: String) -> OAuth2CustomHandler? {
        guard let instance = self.getHandlerInstance(className: className) ?? self.loadHandlerInstance(className: className) else {
            return nil
        }
        return instance
    }

    private func getHandlerInstance(className: String) -> OAuth2CustomHandler? {
        return self.handlerInstances[className]
    }

    private func log(_ msg: String) {
        print("I/Capacitor/OAuth2ClientPlugin: \(msg)")
    }

    private func logDataObj(_ msg: String, _ data: Data) {
        let json = try? JSONSerialization.jsonObject(with: data, options: [])
        log("\(msg)\n\(json ?? "")")
    }

    private func buildStringDict(_ callParameter: [String: Any]) -> [String: String]  {
        var dict: [String: String] = [:]
        for (key, value) in callParameter {
            // only non empty string values are allowed
            if !key.isEmpty && value is String {
                let str = value as! String;
                if !str.isEmpty {
                    dict[key] = str
                }
            }
        }
        return dict;
    }

    private func loadHandlerInstance(className: String) -> OAuth2CustomHandler? {
        guard let handlerClazz: OAuth2CustomHandler.Type = self.handlerClasses[className] else {
            log("Unable to load custom handler \(className). No such class found.")
            return nil
        }

        let instance: OAuth2CustomHandler = handlerClazz.init()

        self.handlerInstances[className] = instance
        return instance
    }

    private func generateRandom(withLength len: Int) -> String {
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        let length = UInt32(letters.count)

        var randomString = ""
        for _ in 0..<len {
            let rand = arc4random_uniform(length)
            let idx = letters.index(letters.startIndex, offsetBy: Int(rand))
            let letter = letters[idx]
            randomString += String(letter)
        }
        return randomString
    }

}

// see https://auth0.com/docs/api-auth/tutorials/authorization-code-grant-pkce

extension String {
    func sha256() -> Data {
        let data = self.data(using: .utf8)!
        var buffer = [UInt8](repeating: 0,  count: Int(CC_SHA256_DIGEST_LENGTH))
        data.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &buffer)
        }
        return Data(buffer)
    }
}

extension Data {
    func base64() -> String {
        return self.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
            .trimmingCharacters(in: .whitespaces)
    }
}

@available(iOS 13.0, *)
extension OAuth2ClientPlugin: ASAuthorizationControllerDelegate {

    func handleSignInWithApple(_ call: CAPPluginCall) {
        self.savedPluginCall = call

        let appleIDProvider = ASAuthorizationAppleIDProvider()
        let request = appleIDProvider.createRequest()

        if let _: Bool = getValue(call, PARAM_IOS_USE_SCOPE) as? Bool {
            if let scopeStr = getOverwritableString(call, PARAM_SCOPE), !scopeStr.isEmpty {
                var scopeArr: Array<ASAuthorization.Scope> = []
                if scopeStr.localizedCaseInsensitiveContains("fullName")
                   || scopeStr.localizedCaseInsensitiveContains("name") {
                   scopeArr.append(.fullName)
               }

                if scopeStr.localizedCaseInsensitiveContains("email") {
                    scopeArr.append(.email)
                }

                request.requestedScopes = scopeArr
            }
        } else {
            request.requestedScopes = [.fullName, .email]
        }

        let authorizationController = ASAuthorizationController(authorizationRequests: [request])
        authorizationController.delegate = self
        authorizationController.performRequests()
    }

    public func authorizationController(controller: ASAuthorizationController,
                                        didCompleteWithAuthorization authorization: ASAuthorization) {

        switch authorization.credential {
        case let appleIDCredential as ASAuthorizationAppleIDCredential:
            var realUserStatus: String
            switch appleIDCredential.realUserStatus {
            case .likelyReal:
                realUserStatus = "likelyReal"
            case .unknown:
                realUserStatus = "unknown"
            case .unsupported:
                realUserStatus = "unsupported"
            @unknown default:
                realUserStatus = ""
            }

            let result = [
                "id": appleIDCredential.user,
                "given_name": appleIDCredential.fullName?.givenName as Any,
                "family_name": appleIDCredential.fullName?.familyName as Any,
                "email": appleIDCredential.email as Any,
                "real_user_status": realUserStatus,
                "state": appleIDCredential.state  as Any,
                "id_token": String(data: appleIDCredential.identityToken!, encoding: .utf8) as Any,
                "code": String(data: appleIDCredential.authorizationCode!, encoding: .utf8) as Any
            ] as [String : Any]
            self.savedPluginCall?.resolve(result as PluginCallResultData)
        default:
            self.log("SIWA: Authorization failed!")
            self.savedPluginCall?.reject(self.ERR_AUTHORIZATION_FAILED)
        }
    }

    public func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        // Handle error.
        guard let error = error as? ASAuthorizationError else {
            return
        }

        switch error.code {
        case .canceled:
            self.savedPluginCall?.reject(SharedConstants.ERR_USER_CANCELLED)
        case .unknown:
            self.log("SIWA: Error.unknown.")
            self.savedPluginCall?.reject(SharedConstants.ERR_USER_CANCELLED)
        case .invalidResponse:
            self.log("SIWA: Error.invalidResponse")
            self.savedPluginCall?.reject(self.ERR_AUTHORIZATION_FAILED)
        case .notHandled:
            self.log("SIWA: Error.notHandled")
            self.savedPluginCall?.reject(self.ERR_AUTHORIZATION_FAILED)
        case .failed:
            self.log("SIWA: Error.failed")
            self.savedPluginCall?.reject(self.ERR_AUTHORIZATION_FAILED)
        @unknown default:
            self.log("SIWA: Error.default")
            self.savedPluginCall?.reject(self.ERR_GENERAL)
        }
    }



}
