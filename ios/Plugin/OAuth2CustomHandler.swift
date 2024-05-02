import Foundation
import Capacitor

@objc public protocol OAuth2CustomHandler: NSObjectProtocol {

    init()

    func getAccessToken(viewController: UIViewController, call: CAPPluginCall,
                        success: @escaping (_ accessToken: String) -> Void,
                        cancelled: @escaping () -> Void,
                        failure: @escaping (_ error: Error) -> Void)

    func logout(viewController: UIViewController, call: CAPPluginCall) -> Bool
}
