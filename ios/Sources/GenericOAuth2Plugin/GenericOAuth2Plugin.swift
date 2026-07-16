import Foundation
import Capacitor

@objc(GenericOAuth2Plugin)
public class GenericOAuth2Plugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "GenericOAuth2Plugin"
    public let jsName = "GenericOAuth2"

    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "authenticate", returnType: CAPPluginReturnPromise)
    ]

    @objc func authenticate(_ call: CAPPluginCall) {
        call.resolve()
    }
}
