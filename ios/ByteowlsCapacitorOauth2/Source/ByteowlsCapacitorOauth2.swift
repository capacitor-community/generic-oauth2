import Foundation
import Capacitor
import OAuthSwift

typealias JSObject = [String:Any]

@objc(OAuth2ClientPlugin)
public class OAuth2ClientPlugin: CAPPlugin {

    let PARAM_APP_ID = "appId"
    let PARAM_RESPONSE_TYPE = "responseType"
    let PARAM_IOS_CUSTOM_SCHEME = "ios.customScheme"
    let PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint"
    let PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl"
    let PARAM_CUSTOM_HANDLER_CLASS = "ios.customHandlerClass"
    let PARAM_SCOPE = "scope"
    let PARAM_STATE = "state"
    let PARAM_RESOURCE_URL = "resourceUrl"
    let RESPONSE_TYPE_CODE = "code"
    let RESPONSE_TYPE_TOKEN = "token"

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
                log("Custom handler class '\(className)' found!")
            }
        }
    }

    public override func load() {
        NotificationCenter.default.addObserver(self, selector: #selector(self.handleRedirect(notification:)), name: Notification.Name(CAPNotifications.URLOpen.name()), object: nil)
        registerHandlers()
    }

    @objc func authenticate(_ call: CAPPluginCall) {
        guard let appId = getOverwritableString(call, PARAM_APP_ID) else {
            call.reject("ERR_PARAM_NO_APP_ID")
            return
        }
        let resourceUrl = getString(call, self.PARAM_RESOURCE_URL)

        if let handlerClassName = getString(call, PARAM_CUSTOM_HANDLER_CLASS) {
            if let handlerInstance = self.getOrLoadHandlerInstance(className: handlerClassName) {
                handlerInstance.getAccessToken(viewController: bridge.viewController, call: call,
                success: { (accessToken) in
                    
                    if resourceUrl != nil {
                        let client = OAuthSwiftClient(
                            consumerKey: appId,
                            consumerSecret: "",
                            oauthToken: accessToken,
                            oauthTokenSecret: "",
                            version: OAuthSwiftCredential.Version.oauth2)
                        
                        let _ = client.get(
                            resourceUrl!,
                            success: { (response) in
                                if var jsonObj = try? JSONSerialization.jsonObject(with: response.data, options: []) as! JSObject {
                                    // send the access token to the caller so e.g. it can be stored on a backend
                                    jsonObj.updateValue(accessToken, forKey: "access_token")
                                    call.resolve(jsonObj)
                                } else {
                                    call.reject("ERR_GENERAL")
                                }
                            },
                            failure: { (error) in
                                self.log("Resource url request error '\(error)'")
                                call.reject("ERR_CUSTOM_HANDLER_LOGIN");
                            })
                    } else {
                       // TODO handle no resource url same as android
                    }
                },
                cancelled: {
                    call.reject("USER_CANCELLED")
                },
                failure: { (error) in
                    self.log("Login failed because '\(error)'")
                    call.reject("ERR_CUSTOM_HANDLER_LOGIN")
                })
            } else {
                log("Handler class '\(handlerClassName)' not implements OAuth2CustomHandler protocol")
                call.reject("ERR_CUSTOM_HANDLER_LOGIN")
            }
        } else {
            guard let baseUrl = getString(call, PARAM_AUTHORIZATION_BASE_URL) else {
                call.reject("ERR_PARAM_NO_AUTHORIZATION_BASE_URL")
                return
            }
           
            guard let redirectUrl = getString(call, PARAM_IOS_CUSTOM_SCHEME) else {
                call.reject("ERR_PARAM_NO_REDIRECT_URL")
                return
            }
            
            var responseType = getOverwritableString(call, PARAM_RESPONSE_TYPE)
            if responseType == nil {
                // on native apps the response type is most probably "code"
                responseType = RESPONSE_TYPE_CODE
            }
            
            let accessTokenEndpoint = getString(call, PARAM_ACCESS_TOKEN_ENDPOINT)
            if accessTokenEndpoint == nil && responseType == RESPONSE_TYPE_CODE {
                call.reject("ERR_PARAM_NO_ACCESS_TOKEN_ENDPOINT")
                return
            }
            
            if responseType != RESPONSE_TYPE_CODE && responseType != RESPONSE_TYPE_TOKEN {
                call.reject("ERR_PARAM_INVALID_RESPONSE_TYPE")
                return
            }
            
            var oauthSwift: OAuth2Swift
            if (responseType == RESPONSE_TYPE_CODE) {
                oauthSwift = OAuth2Swift(
                    consumerKey: appId,
                    consumerSecret: "", // never ever store the app secret on client!
                    authorizeUrl: baseUrl,
                    accessTokenUrl: accessTokenEndpoint!,
                    responseType: responseType!
                )
            } else {
                oauthSwift = OAuth2Swift(
                    consumerKey: appId,
                    consumerSecret: "", // never ever store the app secret on client!
                    authorizeUrl: baseUrl,
                    responseType: responseType!
                )
            }

            self.oauthSwift = oauthSwift
            oauthSwift.authorizeURLHandler = SafariURLHandler(viewController: bridge.viewController, oauthSwift: oauthSwift)

            let requestState = getString(call, PARAM_STATE) ?? generateState(withLength: 20)
            let _ = oauthSwift.authorize(
                withCallbackURL: redirectUrl,
                scope: getString(call, PARAM_SCOPE) ?? "",
                state: requestState,
                success: { credential, response, parameters in
                    // oauthSwift internally checks the state if response type is code therefore I only need the token check
                    if responseType == self.RESPONSE_TYPE_TOKEN {
                        guard let responseState = parameters["state"] as? String, responseState == requestState else {
                            call.reject("ERR_STATES_NOT_MATCH")
                            return
                        }
                    }
                    
                    if resourceUrl != nil {
                        let _ = oauthSwift.client.get(
                            resourceUrl!,
                            parameters: parameters,
                            success: { (response) in
                                do {
                                    var jsonObj = try JSONSerialization.jsonObject(with: response.data, options: []) as! JSObject
                                    // send the access token to the caller so e.g. it can be stored on a backend
                                    jsonObj.updateValue(oauthSwift.client.credential.oauthToken, forKey: "access_token")
                                    call.resolve(jsonObj)
                                } catch {
                                    self.log("Invalid json in resource response \(error.localizedDescription)")
                                    call.reject("ERR_GENERAL")
                                }
                                
                            },
                            failure: { error in
                                self.log("Access resource request failed with \(error.localizedDescription)");
                                call.reject("ERR_GENERAL")
                            })
                    } else {
                        // TODO handle no resource url same as android
                    }
                },
                failure: { error in
                    switch error {
                    case .cancelled, .accessDenied(_, _):
                         call.reject("USER_CANCELLED")
                    case .stateNotEqual( _, _):
                        call.reject("ERR_STATES_NOT_MATCH")
                    default:
                        self.log("Authorization failed with \(error.localizedDescription)");
                        call.reject("ERR_NO_AUTHORIZATION_CODE")
                    }
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
                    call.reject("ERR_CUSTOM_HANDLER_LOGOUT")
                }
            } else {
                log("Handler instance not found! Bug!")
                call.reject("ERR_CUSTOM_HANDLER_LOGOUT")
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
    
    private func getOverwritableString(_ call: CAPPluginCall, _ key: String) -> String? {
        var base = getString(call, key)
        let ios = getString(call, "ios." + key)
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

    public func getOrLoadHandlerInstance(className: String) -> OAuth2CustomHandler? {
        guard let instance = self.getHandlerInstance(className: className) ?? self.loadHandlerInstance(className: className) else {
            return nil
        }
        return instance
    }

    public func getHandlerInstance(className: String) -> OAuth2CustomHandler? {
        return self.handlerInstances[className]
    }
    
    func log(_ msg: String) {
        print("@byteowls/capacitor-oauth2: \(msg).")
    }

    public func loadHandlerInstance(className: String) -> OAuth2CustomHandler? {
        guard let handlerClazz: OAuth2CustomHandler.Type = self.handlerClasses[className] else {
            log("Unable to load custom handler \(className). No such class found.")
            return nil
        }

        let instance: OAuth2CustomHandler = handlerClazz.init()

        self.handlerInstances[className] = instance
        return instance
    }

}
