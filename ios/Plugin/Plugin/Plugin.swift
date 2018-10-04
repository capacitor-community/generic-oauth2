import Foundation
import Capacitor
import OAuthSwift

typealias JSObject = [String:Any]

@objc(OAuth2ClientPlugin)
public class OAuth2ClientPlugin: CAPPlugin {
    
    let PARAM_APP_ID = "appId";
    let PARAM_IOS_APP_ID = "ios.appId";
    let PARAM_IOS_CUSTOM_SCHEME = "ios.customScheme";
    let PARAM_ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
    let PARAM_AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
    let PARAM_SCOPE = "scope";
    let PARAM_STATE = "state";
    let PARAM_RESOURCE_URL = "resourceUrl";

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
        guard let resourceUrl = getString(call, PARAM_RESOURCE_URL) else {
            call.reject("Option '\(PARAM_RESOURCE_URL)' is required!")
            return
        }
        
        let oauthSwift = OAuth2Swift(
            consumerKey: finalAppId,
            consumerSecret: "",
            authorizeUrl: baseUrl,
            accessTokenUrl: accessTokenEndpoint,
            responseType: "code"
        )
        
        oauthSwift.allowMissingStateCheck = true
        
        oauthSwift.authorizeURLHandler = SafariURLHandler(viewController: bridge.viewController, oauthSwift: oauthSwift)
        
        oauthSwift.authorize(
            withCallbackURL: customScheme,
            scope: getString(call, PARAM_SCOPE) ?? "",
            state: getString(call, PARAM_STATE) ?? "",
            success: { credential, response, parameters in
                oauthSwift.client.get(
                    resourceUrl,
                    parameters: parameters,
                    success: { (response) in
                        if let jsonObj = try? JSONSerialization.jsonObject(with: response.data, options: []) as! JSObject {
                            call.success(jsonObj)
                        }
                    },
                    failure: { (error) in
                        print(error.localizedDescription);
                        call.reject(error.localizedDescription);
                    })
            },
            failure: { error in
                print(error.localizedDescription)
                call.reject(error.localizedDescription);
            }
        )
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
    
    // Handle callback url which contains now token information
    public static func handleRedirectUrl(_ url: URL) {
        OAuthSwift.handle(url: url)
    }
    
}
