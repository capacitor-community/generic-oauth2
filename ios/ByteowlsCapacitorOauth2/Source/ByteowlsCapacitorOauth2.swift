import Foundation
import Capacitor
import OAuthSwift

typealias JSObject = [String:Any]

@objc(OAuth2ClientPlugin)
public class OAuth2ClientPlugin: CAPPlugin {

    let PARAM_APP_ID = "appId"
    let PARAM_RESPONSE_TYPE = "responseType"
    let PARAM_IOS_RESPONSE_TYPE = "ios.responseType"
    let PARAM_IOS_APP_ID = "ios.appId"
    let PARAM_IOS_CUSTOM_SCHEME = "ios.customScheme"
    let PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint"
    let PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl"
    let PARAM_CUSTOM_HANDLER_CLASS = "ios.customHandlerClass"
    let PARAM_SCOPE = "scope"
    let PARAM_STATE = "state"
    let PARAM_RESOURCE_URL = "resourceUrl"

    var oauthSwift: OAuth2Swift?
    var handlerClasses = [String: OAuth2CustomHandler.Type]()
    var handlerInstances = [String: OAuth2CustomHandler]()

    func registerHandlers() {
        var numClasses = UInt32(0);
        let classes = objc_copyClassList(&numClasses)
        for i in 0..<Int(numClasses) {
            let c: AnyClass = classes![i]
            if class_conformsToProtocol(c, OAuth2CustomHandler.self) {
                let className = NSStringFromClass(c)
                let pluginType = c as! OAuth2CustomHandler.Type
                handlerClasses[className] = pluginType
                print("@byteowls/capacitor-oauth2: custom handler class '\(className)' found!")
            }
        }
    }

    public override func load() {
        NotificationCenter.default.addObserver(self, selector: #selector(self.handleRedirect(notification:)), name: Notification.Name(CAPNotifications.URLOpen.name()), object: nil)
        registerHandlers()
    }

    @objc func authenticate(_ call: CAPPluginCall) {
        var appId = getString(call, PARAM_APP_ID)
        let iosAppId: String? = getString(call, PARAM_IOS_APP_ID)
        if iosAppId != nil {
            appId = iosAppId
        }
        guard let finalAppId = appId, appId != nil else {
            call.reject("Option '\(PARAM_APP_ID)' or '\(PARAM_IOS_APP_ID)' is required!")
            return
        }
        guard let resourceUrl = getString(call, PARAM_RESOURCE_URL) else {
            call.reject("Option '\(PARAM_RESOURCE_URL)' is required!")
            return
        }

        if let handlerClassName = getString(call, PARAM_CUSTOM_HANDLER_CLASS) {
            if let handlerInstance = self.getOrLoadHandlerInstance(className: handlerClassName) {
                handlerInstance.getAccessToken(viewController: bridge.viewController, call: call,
                success: { (accessToken) in
                    let client = OAuthSwiftClient(
                        consumerKey: finalAppId,
                        consumerSecret: "",
                        oauthToken: accessToken,
                        oauthTokenSecret: "",
                        version: OAuthSwiftCredential.Version.oauth2)
                    let _ = client.get(
                        resourceUrl,
                        success: { (response) in
                            if var jsonObj = try? JSONSerialization.jsonObject(with: response.data, options: []) as! JSObject {
                                // send the access token to the caller so e.g. it can be stored on a backend
                                jsonObj.updateValue(accessToken, forKey: "access_token")
                                call.success(jsonObj)
                            }
                    },
                        failure: { (error) in
                            call.reject("Access resource failed with \(error.localizedDescription)");
                    })
                },
                cancelled: {
                    call.reject("Login cancelled by user")
                },
                failure: { (error) in
                   call.reject("Login failed because '\(error)'")
                })
            } else {
                call.reject("Handler class '\(handlerClassName)' not implements OAuth2CustomHandler protocol")
            }
        } else {
            guard let baseUrl = getString(call, PARAM_AUTHORIZATION_BASE_URL) else {
                call.reject("Option '\(PARAM_AUTHORIZATION_BASE_URL)' is required!")
                return
            }
            guard let accessTokenEndpoint = getString(call, PARAM_ACCESS_TOKEN_ENDPOINT) else {
                call.reject("Option '\(PARAM_ACCESS_TOKEN_ENDPOINT)' is required!")
                return
            }
            guard let customScheme = getString(call, PARAM_IOS_CUSTOM_SCHEME) else {
                call.reject("Option '\(PARAM_IOS_CUSTOM_SCHEME)' is required!")
                return
            }
            var responseType = getString(call, PARAM_RESPONSE_TYPE)
            let iosResponseType: String? = getString(call, PARAM_IOS_RESPONSE_TYPE)
            if iosResponseType != nil {
                responseType = iosResponseType
            }
            if (responseType == "code") {
                print("@byteowls/capacitor-oauth2: Code flow with PKCE is not supported yet")
            } else {
                responseType = "token"
            }

            let oauthSwift = OAuth2Swift(
                consumerKey: finalAppId,
                consumerSecret: "", // never ever store the app secret on client!
                authorizeUrl: baseUrl,
                accessTokenUrl: accessTokenEndpoint,
                responseType: responseType!
            )

            self.oauthSwift = oauthSwift
            oauthSwift.authorizeURLHandler = SafariURLHandler(viewController: bridge.viewController, oauthSwift: oauthSwift)

            let defaultState = generateState(withLength: 20)
            let _ = oauthSwift.authorize(
                withCallbackURL: customScheme,
                scope: getString(call, PARAM_SCOPE) ?? "",
                state: getString(call, PARAM_STATE) ?? defaultState,
                success: { credential, response, parameters in
                    let _ = oauthSwift.client.get(
                        resourceUrl,
                        parameters: parameters,
                        success: { (response) in
                            if var jsonObj = try? JSONSerialization.jsonObject(with: response.data, options: []) as! JSObject {
                                // send the access token to the caller so e.g. it can be stored on a backend
                                jsonObj.updateValue(oauthSwift.client.credential.oauthToken, forKey: "access_token")
                                call.success(jsonObj)
                            }
                        },
                        failure: { (error) in
                            call.reject("Access resource failed with \(error.localizedDescription)");
                        })
                },
                failure: { error in
                    call.reject("Authorization failed with \(error.localizedDescription)");
                }
            )
        }
    }

    @objc func logout(_ call: CAPPluginCall) {
        if let handlerClassName = getString(call, PARAM_CUSTOM_HANDLER_CLASS) {
            if let handlerInstance = self.getOrLoadHandlerInstance(className: handlerClassName) {
                let success: Bool! = handlerInstance.logout(call: call)
                if success {
                    call.resolve();
                } else {
                    call.reject("Logout not successful")
                }
            } else {
                call.reject("Handler instance not found! Bug!")
            }
        } else {
            if self.oauthSwift != nil {
                self.oauthSwift = nil
            }
            call.resolve()
        }
    }

    @objc func handleRedirect(notification: NSNotification) {
        guard let object = notification.object as? [String:Any?] else {
            return
        }
        guard let url = object["url"] as? URL else {
            return
        }
        OAuth2Swift.handle(url: url);
    }

    private func getConfigObjectDeepest(_ options: [AnyHashable: Any?]!, key: String) -> [AnyHashable:Any?]? {
        let parts = key.split(separator: ".")

        var o = options
        for (_, k) in parts[0..<parts.count-1].enumerated() {
            if (o != nil) {
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

    public func getOrLoadHandlerInstance(className: String) -> OAuth2CustomHandler? {
        guard let instance = self.getHandlerInstance(className: className) ?? self.loadHandlerInstance(className: className) else {
            return nil
        }
        return instance
    }

    public func getHandlerInstance(className: String) -> OAuth2CustomHandler? {
        return self.handlerInstances[className]
    }

    public func loadHandlerInstance(className: String) -> OAuth2CustomHandler? {
        guard let handlerClazz: OAuth2CustomHandler.Type = self.handlerClasses[className] else {
            print("@byteowls/capacitor-oauth2: Unable to load custom handler \(className). No such class found.")
            return nil
        }

        let instance: OAuth2CustomHandler = handlerClazz.init()

        self.handlerInstances[className] = instance
        return instance
    }

}
