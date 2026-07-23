import Foundation
import AuthenticationServices
import CryptoKit

struct AuthorizationRequest {
    let url: URL
    let state: String
    let rawNonce: String
    let verifier: String
}

public class OAuth2Helper {
    static func generateAuthorizationRequest(
        authorizationEndpoint: String,
        clientId: String,
        redirectURI: String,
        scope: String,
        responseType: String,
        responseMode: String?,
        codeChallengeMethod: String
    ) throws(AuthError) -> AuthorizationRequest {
        guard var components = URLComponents(string: authorizationEndpoint) else {
            throw AuthError.invalidUrl
        }

        // Generate State
        let state = UUID().uuidString

        // Generate Nonce and Hash.
        // We hash the nonce as an extra layer of security.
        // The Firebase Auth docs also mentions doing this: https://firebase.google.com/docs/auth/ios/apple#sign_in_with_apple_and_authenticate_with_firebase
        // This means the backend should first SHA256 hash the `rawNonce` before comparing it to the `nonce` returned in the `idToken`.
        let (rawNonce, hashedNonce) = try generateRandomStringAndHashStringPairInBase64URLAlphabet()

        // Build the Authorization URL.
        // Append to any query items already present in `authorizationEndpoint`
        // (e.g. tenant/audience params) instead of overwriting them.
        var queryItems = components.queryItems ?? []
        queryItems.append(contentsOf: [
            URLQueryItem(name: "client_id", value: clientId),
            URLQueryItem(name: "redirect_uri", value: redirectURI),
            URLQueryItem(name: "response_type", value: responseType),
            URLQueryItem(name: "scope", value: scope),
            URLQueryItem(name: "state", value: state),
            URLQueryItem(name: "nonce", value: hashedNonce)
        ])

        if let responseMode {
            queryItems.append(URLQueryItem(name: "response_mode", value: responseMode))
        }

        if codeChallengeMethod != "S256" {
            // Currently, only S256 code challenge method is supported.
            throw AuthError.invalidCodeChallengeMethod
        }

        // Generate PKCE Code Verifier and Challenge
        let (verifier, challenge) = try generateRandomStringAndHashStringPairInBase64URLAlphabet()

        queryItems.append(URLQueryItem(name: "code_challenge", value: challenge))
        queryItems.append(URLQueryItem(name: "code_challenge_method", value: "S256"))

        components.queryItems = queryItems

        guard let url = components.url else {
            throw AuthError.invalidUrl
        }

        return AuthorizationRequest(url: url, state: state, rawNonce: rawNonce, verifier: verifier)
    }

    /// Handles the callback from the browser
    static func handleCallback(
        tokenEndpoint: String,
        clientId: String,
        redirectURI: String,
        callbackURL: URL,
        state: String,
        verifier: String
    ) async throws(AuthError) -> String {
        let code = try extractCodeFromCallbackURL(callbackURL: callbackURL, state: state)

        // Exchange Code for Tokens
        let tokenResult = try await exchangeCodeForToken(
            tokenEndpoint: tokenEndpoint,
            clientId: clientId,
            redirectURI: redirectURI,
            verifier: verifier,
            code: code
        )

        return tokenResult.idToken
    }

    private static func extractCodeFromCallbackURL(
        callbackURL: URL,
        state: String
    ) throws(AuthError) -> String {
        guard var components = URLComponents(url: callbackURL, resolvingAgainstBaseURL: true) else {
            throw AuthError.invalidCallback
        }

        // `application/x-www-form-urlencoded` is a bit of a mess:
        // https://github.com/openid/AppAuth-iOS/pull/291
        // https://github.com/oauth-wg/oauth-v2-1/issues/128
        // https://github.com/apple/swift-http-types/issues/117
        //
        // As OAuth uses `application/x-www-form-urlencoded` encoding,
        // it interprets '+' as a space in addition to regular percent decoding.
        // https://url.spec.whatwg.org/#urlencoded-parsing
        // So we explicitly decode '+' to a space.
        // Same approach as AppAuth-iOS (`OIDURLQueryComponent.initWithURL:`).
        //
        // NOTE: in practice `code` and `state` should never contain spaces.
        components.percentEncodedQuery = components.percentEncodedQuery?.replacingOccurrences(of: "+", with: "%20")

        guard let queryItems = components.queryItems else {
            throw AuthError.invalidCallback
        }

        // Check for errors in the callback.
        if let error = queryItems.first(where: { $0.name == "error" })?.value {
            // Include the optional human-readable `error_description` when the provider sends one.
            if let errorDescription = queryItems.first(where: { $0.name == "error_description" })?.value {
                throw AuthError.providerError("\(error): \(errorDescription)")
            }
            throw AuthError.providerError(error)
        }

        guard let stateFromUrl = queryItems.first(where: { $0.name == "state" })?.value else {
            throw AuthError.missingState
        }

        guard stateFromUrl == state else {
            throw AuthError.invalidState
        }

        guard let code = queryItems.first(where: { $0.name == "code" })?.value else {
            throw AuthError.missingCode
        }

        return code
    }

    /// Exchanges the authorization code for access/refresh tokens
    private static func exchangeCodeForToken(
        tokenEndpoint: String,
        clientId: String,
        redirectURI: String,
        verifier: String,
        code: String
    ) async throws(AuthError) -> TokenResponse {
        guard let url = URL(string: tokenEndpoint) else {
            throw AuthError.invalidUrl
        }

        // OAuth 2.0 requires TLS on the token endpoint — the request body
        // contains the authorization code and PKCE verifier. `http` is
        // allowed for loopback hosts only, to ease local development.
        // (App Transport Security usually blocks plain http anyway, but apps
        // can carve exceptions; this keeps the guarantee independent of app
        // configuration and yields a clearer error.)
        let isLoopback = url.host == "localhost" || url.host == "127.0.0.1"
        guard url.scheme == "https" || (url.scheme == "http" && isLoopback) else {
            throw AuthError.insecureUrl
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 30 // Matches the Android implementation.
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        // `client_secret` is omitted entirely on purpose.
        // Saving a `client_secret` in a public client is considered unsafe.
        // And clients using PKCE shouldn't be sending it anyway.
        var bodyComponents = URLComponents()
        bodyComponents.queryItems = [
            URLQueryItem(name: "code", value: code),
            URLQueryItem(name: "client_id", value: clientId),
            URLQueryItem(name: "redirect_uri", value: redirectURI),
            URLQueryItem(name: "grant_type", value: "authorization_code"),
            URLQueryItem(name: "code_verifier", value: verifier)
        ]

        // `application/x-www-form-urlencoded` is still a bit of a mess.
        //
        // `percentEncodedQuery` creates a validly escaped URL query component,
        // but doesn't encode the '+', leading to potential ambiguity with `application/x-www-form-urlencoded` encoding.
        // Percent encode '+' explicitly to avoid this ambiguity.
        // Same approach as AppAuth-iOS (`OIDURLQueryComponent.URLEncodedParameters`).
        guard let encodedBody = bodyComponents.percentEncodedQuery?.replacingOccurrences(of: "+", with: "%2B") else {
            throw AuthError.invalidUrl
        }

        request.httpBody = encodedBody.data(using: .utf8)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw AuthError.tokenExchangeFailed(error.localizedDescription)
        }

        let dataString = String(data: data, encoding: .utf8) ?? ""

        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw AuthError.tokenExchangeFailed("Incorrect status code: \(http.statusCode). Payload: \(dataString)")
        }

        do {
            let decoder = JSONDecoder()
            return try decoder.decode(TokenResponse.self, from: data)
        } catch {
            // Don't echo the full payload here.
            // A 2xx doesn't necessarily mean that the response also contains an `id_token`.
            // That could happen if for example the `openid` scope is missing.
            // So the payload may contain LIVE tokens (e.g. `access_token`, `refresh_token`).
            // Reporting only the JSON keys keeps the error diagnosable without leaking credentials into JS, logs, or crash reporters.
            // The non-2xx branch above keeps the full payload, because error responses don't (or shouldn't) carry credentials.
            let keys: String
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                keys = json.keys.sorted().joined(separator: ", ")
            } else {
                throw AuthError.tokenExchangeFailed("Decoding response failed: \(error.localizedDescription). Additionally, decoding the keys failed.")
            }
            throw AuthError.tokenExchangeFailed("Decoding response failed: \(error.localizedDescription). Response contained keys: \(keys)")
        }
    }

    private static func generateRandomStringAndHashStringPairInBase64URLAlphabet() throws(AuthError) -> (randomString: String, hashString: String) {
        // Based on: https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow-with-pkce/call-your-api-using-the-authorization-code-flow-with-pkce#swift-5-sample
        // Improved by using `SHA256` and `Data(string.utf8)`

        // 64 random bytes -> 86-character base64url string
        // (within the 43–128 character range PKCE requires)
        var randomStringBuffer = [UInt8](repeating: 0, count: 64)

        let status = SecRandomCopyBytes(kSecRandomDefault, randomStringBuffer.count, &randomStringBuffer)
        guard status == errSecSuccess else {
            // This should never really happen in reality.
            // We should still safeguard for it though.
            throw AuthError.randomGenerationFailed
        }

        // Base64URL encode the buffer
        let randomString = Data(randomStringBuffer).base64EncodedStringUsingURLAlphabet()

        // SHA256 hash of the random string
        let hashStringBuffer = SHA256.hash(data: Data(randomString.utf8))

        // Base64URL encode the hash
        let hashString = Data(hashStringBuffer).base64EncodedStringUsingURLAlphabet()

        return (randomString, hashString)
    }
}

struct TokenResponse: Decodable {
    let idToken: String

    enum CodingKeys: String, CodingKey {
        case idToken = "id_token"
    }
}

enum AuthError: LocalizedError {
    case invalidUrl
    case insecureUrl
    case invalidCallback
    case invalidCodeChallengeMethod
    case invalidParams
    case missingCallback
    case httpsCallbackUnavailable
    case providerError(String)
    case missingState
    case invalidState
    case missingCode
    case tokenExchangeFailed(String)
    case webAuthCanceled
    case webAuthFailed
    case randomGenerationFailed
    case authInProgress

    var errorDescription: String {
        switch self {
        case .invalidUrl: return "Invalid URL constructed."
        case .insecureUrl: return "`tokenEndpoint` must use https (http is allowed for localhost only)."
        case .invalidCallback: return "Invalid callback URL format."
        case .invalidCodeChallengeMethod: return "Invalid `codeChallengeMethod` passed. Currently only `S256` is supported."
        case .invalidParams: return "Invalid params."
        case .missingCallback: return "Either `callbackScheme` or `callbackHttps` must be provided."
        case .httpsCallbackUnavailable: return "`callbackHttps` is not supported on this OS version (requires iOS 17.4+ / macOS 14.4+) and no `callbackScheme` fallback was provided."
        case .providerError(let msg): return "Provider returned an error: \(msg)"
        case .missingState: return "State missing in callback."
        case .invalidState: return "State doesn't match."
        case .missingCode: return "Authorization code missing in callback."
        case .tokenExchangeFailed(let msg): return "Token exchange failed: \(msg)"
        case .webAuthCanceled: return "Web authentication session canceled"
        case .webAuthFailed: return "Web authentication session failed"
        case .randomGenerationFailed: return "Failed to generate secure random bytes."
        case .authInProgress: return "Another authentication session was started."
        }
    }
}

extension Data {
    func base64EncodedStringUsingURLAlphabet() -> String {
        // Replace with `anyAppleOS 26.4, *` when widely available
        if #available(iOS 26.4, macOS 26.4, watchOS 26.4, tvOS 26.4, visionOS 26.4, *) {
            return self.base64EncodedString(options: [.base64URLAlphabet, .omitPaddingCharacter])
        } else {
            return self.base64EncodedString()
                .replacingOccurrences(of: "+", with: "-")
                .replacingOccurrences(of: "/", with: "_")
                .replacingOccurrences(of: "=", with: "")
        }
    }
}
