import Foundation
import Capacitor
import AuthenticationServices

@objc(GenericOAuth2Plugin)
public class GenericOAuth2Plugin: CAPPlugin, CAPBridgedPlugin, ASWebAuthenticationPresentationContextProviding {
    public let identifier = "GenericOAuth2Plugin"
    public let jsName = "GenericOAuth2"

    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "authenticate", returnType: CAPPluginReturnPromise)
    ]

    // All per-flow state lives in one nullable holder, so claiming or
    // rejecting a flow is a single assignment.
    //
    // We keep a strong reference to the session for its whole lifetime.
    // If it were to be a local variable, nothing guarantees it to stay alive after `session.start()` is called.
    // iOS appears to retain a presented session internally, but that's undocumented, so we shouldn't rely on it.
    // We also need this reference to be able to call `cancel()` on concurrent calls anyway.
    private struct PendingFlow {
        let session: ASWebAuthenticationSession
        let call: CAPPluginCall
    }

    private var currentFlow: PendingFlow?

    struct AuthenticateCallbackHttpsParams: Decodable {
        let host: String
        let path: String
    }

    struct AuthenticateParams: Decodable {
        let authorizationEndpoint: String
        let clientId: String
        let redirectURI: String
        let scope: String
        let tokenEndpoint: String
        let callbackScheme: String
        let callbackHttps: AuthenticateCallbackHttpsParams?
        let responseType: String
        let responseMode: String
        let codeChallengeMethod: String

        // Ask the system not to share cookies/website data with Safari,
        // which also suppresses the "wants to use X to sign in" prompt
        // and forces a fresh login. Defaults to false.
        let preferEphemeralBrowsing: Bool?

        // swiftlint:disable nesting
        enum CodingKeys: String, CodingKey {
            case authorizationEndpoint
            case clientId
            // Swift properties keep uppercase acronyms per the Swift API Design Guidelines.
            case redirectURI = "redirectUri"
            case scope
            case tokenEndpoint
            case callbackScheme
            case callbackHttps
            case responseType
            case responseMode
            case codeChallengeMethod
            case preferEphemeralBrowsing
        }
    }

    @objc func authenticate(_ call: CAPPluginCall) {
        let options: AuthenticateParams

        do {
            options = try call.decode(AuthenticateParams.self)
        } catch {
            call.reject(AuthError.invalidParams.errorDescription)
            return
        }

        // If a session is already in flight, cancel it and reject its
        // call instead of silently stacking a second session on top.
        if let existingFlow = currentFlow {
            existingFlow.session.cancel()
            existingFlow.call.reject(AuthError.authInProgress.errorDescription)
            currentFlow = nil
        }

        do throws(AuthError) {
            let authorizationRequest = try OAuth2Helper.generateAuthorizationRequest(
                authorizationEndpoint: options.authorizationEndpoint,
                clientId: options.clientId,
                redirectURI: options.redirectURI,
                scope: options.scope,
                responseType: options.responseType,
                responseMode: options.responseMode,
                codeChallengeMethod: options.codeChallengeMethod
            )

            let authSession: ASWebAuthenticationSession

            if #available(iOS 17.4, macOS 14.4, *), let callbackHttps = options.callbackHttps {
                authSession = ASWebAuthenticationSession(
                    url: authorizationRequest.url,
                    callback: .https(host: callbackHttps.host, path: callbackHttps.path),
                    completionHandler: { [weak self] callbackURL, error in
                        self?.handleCallback(
                            call,
                            options,
                            authorizationRequest,
                            callbackURL,
                            error
                        )
                    }
                )
            } else {
                authSession = ASWebAuthenticationSession(
                    url: authorizationRequest.url,
                    callbackURLScheme: options.callbackScheme,
                    completionHandler: { [weak self] callbackURL, error in
                        self?.handleCallback(
                            call,
                            options,
                            authorizationRequest,
                            callbackURL,
                            error
                        )
                    }
                )
            }

            authSession.presentationContextProvider = self
            authSession.prefersEphemeralWebBrowserSession = options.preferEphemeralBrowsing ?? false

            currentFlow = PendingFlow(session: authSession, call: call)

            DispatchQueue.main.async { [weak self] in
                if !authSession.start() {
                    guard let self, self.currentFlow?.call === call else { return }
                    self.currentFlow = nil
                    call.reject(AuthError.webAuthFailed.errorDescription)
                }
            }
        } catch let error {
            currentFlow = nil
            call.reject(error.errorDescription)
        }
    }

    private func handleCallback(_ call: CAPPluginCall, _ options: AuthenticateParams, _ authorizationRequest: AuthorizationRequest, _ callbackURL: URL?, _ error: Error?) {
        // Only release the stored flow if it still belongs to this
        // session's call — a stale completion (e.g. from a cancelled session)
        // must not tear down a newer in-flight session.
        if currentFlow?.call === call {
            currentFlow = nil
        }

        if let error = error as? ASWebAuthenticationSessionError {
            if error.code == .canceledLogin {
                call.reject(AuthError.webAuthCanceled.errorDescription)
                return
            } else {
                call.reject(AuthError.webAuthFailed.errorDescription)
                return
            }
        }

        guard let callbackURL = callbackURL else {
            call.reject(AuthError.webAuthFailed.errorDescription)
            return
        }

        Task {
            do throws(AuthError) {
                let idToken = try await OAuth2Helper.handleCallback(
                    tokenEndpoint: options.tokenEndpoint,
                    clientId: options.clientId,
                    redirectURI: options.redirectURI,
                    callbackURL: callbackURL,
                    state: authorizationRequest.state,
                    verifier: authorizationRequest.verifier
                )

                call.resolve([
                    "idToken": idToken,
                    "rawNonce": authorizationRequest.rawNonce
                ])
            } catch let error {
                call.reject(error.errorDescription)
            }
        }
    }

    public func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        var view: ASPresentationAnchor?
        if Thread.isMainThread {
            view = self.bridge?.webView?.window
        } else {
            DispatchQueue.main.sync {
                view = self.bridge?.webView?.window
            }
        }
        return view ?? ASPresentationAnchor()
    }
}
