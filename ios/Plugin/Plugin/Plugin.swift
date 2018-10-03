import Foundation
import Capacitor

@objc(OAuth2ClientPlugin)
public class OAuth2ClientPlugin: CAPPlugin {

    @objc func authenticate(_ call: CAPPluginCall) {
        let customHandlerClassname = call.getString("ios.customHandlerClass")
        
        
        // ...
        
        call.resolve()
    }
    
}
