import Foundation
import Capacitor

@objc public protocol OAuth2CustomHandler: NSObjectProtocol {
    
    init()
    
    func getAccessToken(viewController: UIViewController, call: CAPPluginCall,
                        success: (_ accessToken: String) -> Void,
                        cancelled: () -> Void,
                        failure: (_ error: Error) -> Void)
}
