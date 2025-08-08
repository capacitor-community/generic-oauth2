import Foundation
import Security

public class URLSessionMTLSDelegate: NSObject, URLSessionDelegate {
    private let secIdentity: SecIdentity

    public init(secIdentity: SecIdentity) {
        self.secIdentity = secIdentity
        super.init()
    }

    public func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust {
            if let serverCertificate = challenge.protectionSpace.serverTrust {
                let credential = URLCredential(trust: serverCertificate)
                completionHandler(.useCredential, credential)
            }
            else {
                completionHandler(.cancelAuthenticationChallenge, nil)
            }
        } else if challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodClientCertificate {
            let credential = URLCredential(identity: secIdentity, certificates: nil, persistence: .forSession)
            completionHandler(.useCredential, credential)
        } else {
            completionHandler(.performDefaultHandling, nil)
        }
    }
}