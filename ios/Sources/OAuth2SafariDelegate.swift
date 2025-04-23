import Foundation
import Capacitor
import SafariServices

class OAuth2SafariDelegate: NSObject, SFSafariViewControllerDelegate {

    var pluginCall: CAPPluginCall

    init(_ call: CAPPluginCall) {
        self.pluginCall = call
    }

    func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
        self.pluginCall.reject(GenericOAuth2Plugin.SharedConstants.ERR_USER_CANCELLED)
    }

}
