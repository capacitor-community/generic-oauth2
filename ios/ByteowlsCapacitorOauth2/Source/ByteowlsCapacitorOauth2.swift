import Foundation
import Capacitor
import OAuthSwift
import CommonCrypto

typealias JSObject = [String:Any]

@objc(OAuth2ClientPlugin)
public class OAuth2ClientPlugin: CAPPlugin {
    
    let JSON_KEY_ACCESS_TOKEN = "access_token"
    
    let PARAM_REFRESH_TOKEN = "refreshToken"
    
    // required
    let PARAM_APP_ID = "appId"
    let PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl"
    let PARAM_RESPONSE_TYPE = "responseType"
    let PARAM_REDIRECT_URL = "redirectUrl"
    // controlling
    let PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint"
    let PARAM_RESOURCE_URL = "resourceUrl"
    
    let PARAM_ADDITIONAL_PARAMETERS = "additionalParameters"
    let PARAM_CUSTOM_HANDLER_CLASS = "ios.customHandlerClass"
    let PARAM_SCOPE = "scope"
    let PARAM_STATE = "state"
    let PARAM_PKCE_ENABLED = "pkceEnabled"
    
    let ERR_GENERAL = "ERR_GENERAL"
    
    let ERR_PARAM_NO_APP_ID = "ERR_PARAM_NO_APP_ID"
    let ERR_PARAM_NO_AUTHORIZATION_BASE_URL = "ERR_PARAM_NO_AUTHORIZATION_BASE_URL"
    let ERR_PARAM_NO_RESPONSE_TYPE = "ERR_PARAM_NO_RESPONSE_TYPE"
    let ERR_PARAM_NO_REDIRECT_URL = "ERR_PARAM_NO_REDIRECT_URL"
    
    
    let ERR_CUSTOM_HANDLER_LOGIN = "ERR_CUSTOM_HANDLER_LOGIN"
    let ERR_CUSTOM_HANDLER_LOGOUT = "ERR_CUSTOM_HANDLER_LOGOUT"
    let ERR_STATES_NOT_MATCH = "ERR_STATES_NOT_MATCH"
    let ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT = "ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT"
    let ERR_NO_AUTHORIZATION_CODE = "ERR_NO_AUTHORIZATION_CODE"
    let ERR_PARAM_NO_REFRESH_TOKEN = "ERR_PARAM_NO_REFRESH_TOKEN"
    
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
        NotificationCenter.default.addObserver(self, selector: #selector(self.handleRedirect(notification:)), name: Notification.Name(CAPNotifications.URLOpen.name()), object: nil)
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
                    call.reject(self.ERR_GENERAL)
                }
            case .failure(let error):
                switch error {
                case .cancelled, .accessDenied(_, _):
                    call.reject(SharedConstants.ERR_USER_CANCELLED)
                case .stateNotEqual( _, _):
                    call.reject(self.ERR_STATES_NOT_MATCH)
                default:
                    self.log("Authorization failed with \(error.localizedDescription)");
                    call.reject(self.ERR_NO_AUTHORIZATION_CODE)
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
        // Github issue #71
        self.oauth2SafariDelegate = OAuth2SafariDelegate(call)
        
        // ######### Custom Handler ########
        
        if let handlerClassName = getString(call, PARAM_CUSTOM_HANDLER_CLASS) {
            if let handlerInstance = self.getOrLoadHandlerInstance(className: handlerClassName) {
                handlerInstance.getAccessToken(viewController: bridge.viewController, call: call, success: { (accessToken) in
                    
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
                    self.log("Login failed because '\(error)'")
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
            
            let urlHandler = SafariURLHandler(viewController: bridge.viewController, oauthSwift: oauthSwift)
            // if the user touches "done" in safari without entering the credentials the USER_CANCELLED error is sent #71
            urlHandler.delegate = self.oauth2SafariDelegate
            oauthSwift.authorizeURLHandler = urlHandler
            self.oauthSwift = oauthSwift
            
            // additional parameters #18
            let callParameter: [String: Any] = getOverwritable(call, PARAM_ADDITIONAL_PARAMETERS) as? [String: Any] ?? [:]
            var additionalParameters: [String: String] = [:]
            for (key, value) in callParameter {
                // only non empty string values are allowed
                if !key.isEmpty && value is String {
                    let str = value as! String;
                    if !str.isEmpty {
                        additionalParameters[key] = str
                    }
                }
            }
            
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
                        self.handleAuthorizationResult(result, call, responseType, requestState, resourceUrl)
                }
            } else {
                oauthSwift.authorize(
                    withCallbackURL: redirectUrl,
                    scope: getOverwritableString(call, PARAM_SCOPE) ?? "",
                    state: requestState,
                    parameters: additionalParameters) { result in
                        self.handleAuthorizationResult(result, call, responseType, requestState, resourceUrl)
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
                let success: Bool! = handlerInstance.logout(viewController: bridge.viewController, call: call)
                if success {
                    call.resolve();
                } else {
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
    
    private func handleAuthorizationResult(_ result: Result<OAuthSwift.TokenSuccess, OAuthSwiftError>, _ call: CAPPluginCall, _ responseType: String, _ requestState: String, _ resourceUrl: String?) {
        switch result {
        case .success(let (credential, response, parameters)):
            // state is aready checked by the lib
            if resourceUrl != nil && !resourceUrl!.isEmpty {
                self.oauthSwift!.client.get(
                    resourceUrl!,
                    parameters: parameters) { result in
                        switch result {
                        case .success(let response):
                            do {
                                var jsonObj = try JSONSerialization.jsonObject(with: response.data, options: []) as! JSObject
                                // send the access token to the caller so e.g. it can be stored on a backend
                                jsonObj.updateValue(credential.oauthToken, forKey: self.JSON_KEY_ACCESS_TOKEN)
                                call.resolve(jsonObj)
                            } catch {
                                self.log("Invalid json in resource response \(error.localizedDescription)")
                                call.reject(self.ERR_GENERAL)
                            }
                        case .failure(let error):
                            self.log("Access resource request failed with \(error.localizedDescription)");
                            call.reject(self.ERR_GENERAL)
                        }
                }
            } else {
                do {
                    let jsonObj = try JSONSerialization.jsonObject(with: response!.data, options: []) as! JSObject
                    call.resolve(jsonObj)
                } catch {
                    call.reject(self.ERR_GENERAL)
                }
            }
        case .failure(let error):
            switch error {
            case .cancelled, .accessDenied(_, _):
                call.reject(SharedConstants.ERR_USER_CANCELLED)
            case .stateNotEqual(_, _):
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
        print("@byteowls/capacitor-oauth2: \(msg).")
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
            _ = CC_SHA256($0, CC_LONG(data.count), &buffer)
        }
        let hash = Data(buffer)
        return hash;
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
