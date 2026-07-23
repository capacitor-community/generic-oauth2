package com.getcapacitor.community.genericoauth2

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

data class AuthorizationRequest(
    val url: Uri,
    val state: String,
    val rawNonce: String,
    val verifier: String
)

object OAuth2Helper {

    fun generateAuthorizationRequest(
        authorizationEndpoint: String,
        clientId: String,
        redirectUri: String,
        scope: String,
        responseType: String,
        responseMode: String?,
        codeChallengeMethod: String
    ): AuthorizationRequest {
        val components = Uri.parse(authorizationEndpoint)
        if (components.scheme == null || components.host == null) {
            throw AuthError.InvalidUrl
        }

        // Generate State
        val state = UUID.randomUUID().toString()

        // Generate Nonce and Hash.
        // We hash the nonce as an extra layer of security.
        // The Firebase Auth docs also mentions doing this: https://firebase.google.com/docs/auth/ios/apple#sign_in_with_apple_and_authenticate_with_firebase
        // This means the backend should first SHA256 hash the `rawNonce` before comparing it to the `nonce` returned in the `idToken`.
        val (rawNonce, hashedNonce) = generateRandomStringAndHashStringPairInBase64UrlAlphabet()

        // Build the Authorization URL.
        // Append to any query items already present in `authorizationEndpoint`
        // (e.g. tenant/audience params) instead of overwriting them.
        val builder = components.buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", responseType)
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("state", state)
            .appendQueryParameter("nonce", hashedNonce)

        if (responseMode != null) {
            builder.appendQueryParameter("response_mode", responseMode)
        }

        if (codeChallengeMethod != "S256") {
            // Currently, only S256 code challenge method is supported.
            throw AuthError.InvalidCodeChallengeMethod
        }

        // Generate PKCE Code Verifier and Challenge
        val (verifier, challenge) = generateRandomStringAndHashStringPairInBase64UrlAlphabet()

        builder.appendQueryParameter("code_challenge", challenge)
        builder.appendQueryParameter("code_challenge_method", "S256")

        val url = try {
            builder.build()
        } catch (e: Exception) {
            throw AuthError.InvalidUrl
        }

        return AuthorizationRequest(url = url, state = state, rawNonce = rawNonce, verifier = verifier)
    }

    /**
     * Handles the callback from the browser.
     * Blocking network I/O — call from a background dispatcher.
     */
    fun handleCallback(
        tokenEndpoint: String,
        clientId: String,
        redirectUri: String,
        callbackUrl: Uri,
        state: String,
        verifier: String
    ): String {
        val code = extractCodeFromCallbackUrl(callbackUrl = callbackUrl, state = state)

        // Exchange Code for Tokens
        val tokenResult = exchangeCodeForToken(
            tokenEndpoint = tokenEndpoint,
            clientId = clientId,
            redirectUri = redirectUri,
            verifier = verifier,
            code = code
        )

        return tokenResult.idToken
    }

    private fun extractCodeFromCallbackUrl(
        callbackUrl: Uri,
        state: String
    ): String {
        if (callbackUrl.isOpaque) {
            throw AuthError.InvalidCallback
        }

        // `application/x-www-form-urlencoded` is a bit of a mess:
        // https://github.com/openid/AppAuth-iOS/pull/291
        // https://github.com/oauth-wg/oauth-v2-1/issues/128
        // https://github.com/apple/swift-http-types/issues/117
        //
        // As OAuth uses `application/x-www-form-urlencoded` encoding,
        // it interprets '+' as a space in addition to regular percent decoding.
        // https://url.spec.whatwg.org/#urlencoded-parsing
        //
        // On Android we get these semantics natively: `Uri.getQueryParameter` percent-decodes AND interprets '+' as a space (documented behavior)

        // Check for errors in the callback.
        callbackUrl.getQueryParameter("error")?.let { error ->
            // Include the optional human-readable `error_description` when the provider sends one.
            callbackUrl.getQueryParameter("error_description")?.let { errorDescription ->
                throw AuthError.ProviderError("$error: $errorDescription")
            }
            throw AuthError.ProviderError(error)
        }

        val stateFromUrl = callbackUrl.getQueryParameter("state")

        if (stateFromUrl == null) {
            throw AuthError.MissingState
        }

        if (stateFromUrl != state) {
            throw AuthError.InvalidState
        }

        val code = callbackUrl.getQueryParameter("code")

        if (code == null) {
            throw AuthError.MissingCode
        }

        return code
    }

    /**
     * Exchanges the authorization code for access/refresh tokens.
     * Blocking network I/O — call from a background dispatcher.
     */
    private fun exchangeCodeForToken(
        tokenEndpoint: String,
        clientId: String,
        redirectUri: String,
        verifier: String,
        code: String
    ): TokenResponse {
        val url = try {
            URL(tokenEndpoint)
        } catch (e: Exception) {
            throw AuthError.InvalidUrl
        }

        // OAuth 2.0 requires TLS on the token endpoint — the request body
        // contains the authorization code and PKCE verifier. `http` is
        // allowed for loopback hosts only, to ease local development
        // (10.0.2.2 is the Android emulator's alias for the host machine).
        // Android 9+ blocks cleartext by default anyway, but apps can opt
        // out via networkSecurityConfig; this keeps the guarantee
        // independent of app configuration and yields a clearer error.
        //
        // This check also guarantees the `HttpURLConnection` cast below:
        // for a non-http(s) protocol, that cast would throw an uncaught
        // `ClassCastException` instead of a clean `AuthError`.
        val isLoopback = url.host == "localhost" || url.host == "127.0.0.1" || url.host == "10.0.2.2"
        if (!(url.protocol == "https" || (url.protocol == "http" && isLoopback))) {
            throw AuthError.InsecureUrl
        }

        // `client_secret` is omitted entirely on purpose.
        // Saving a `client_secret` in a public client is considered unsafe.
        // And clients using PKCE shouldn't be sending it anyway.
        val bodyComponents = listOf(
            "code" to code,
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "grant_type" to "authorization_code",
            "code_verifier" to verifier
        )

        // `application/x-www-form-urlencoded` is still a bit of a mess.
        //
        // On Android, `URLEncoder.encode` implements `application/x-www-form-urlencoded` exactly (space -> '+', '+' -> %2B)
        val body = bodyComponents.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

        val (statusCode, dataString) = try {
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                // A token endpoint that redirects is a misconfiguration, and
                // `HttpURLConnection`'s redirect handling can mangle the POST
                // — fail loudly instead of following.
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 30_000
                connection.readTimeout = 30_000

                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                status to text
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            throw AuthError.TokenExchangeFailed(e.message ?: "Network error")
        }

        if (statusCode !in 200..299) {
            throw AuthError.TokenExchangeFailed("Incorrect status code: $statusCode. Payload: $dataString")
        }

        return try {
            TokenResponse(idToken = JSONObject(dataString).getString("id_token"))
        } catch (e: Exception) {
            // Don't echo the full payload here.
            // A 2xx doesn't necessarily mean that the response also contains an `id_token`.
            // That could happen if for example the `openid` scope is missing.
            // So the payload may contain LIVE tokens (e.g. `access_token`, `refresh_token`).
            // Reporting only the JSON keys keeps the error diagnosable without leaking credentials into JS, logs, or crash reporters.
            // The non-2xx branch above keeps the full payload, because error responses don't (or shouldn't) carry credentials.
            val keys = try {
                JSONObject(dataString).keys().asSequence().sorted().joinToString(", ")
            } catch (e2: Exception) {
                throw AuthError.TokenExchangeFailed("Decoding response failed: ${e.message}. Additionally, decoding the keys failed.")
            }
            throw AuthError.TokenExchangeFailed("Decoding response failed: ${e.message}. Response contained keys: $keys")
        }
    }

    private fun generateRandomStringAndHashStringPairInBase64UrlAlphabet(): Pair<String, String> {
        // Based on: https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow-with-pkce/call-your-api-using-the-authorization-code-flow-with-pkce#swift-5-sample
        // Improved a bit by utilizing more modern (Kotlin) APIs

        // 64 random bytes -> 86-character base64url string
        // (within the 43–128 character range PKCE requires)
        val randomStringBuffer = ByteArray(64)

        SecureRandom().nextBytes(randomStringBuffer)

        // Base64URL encode the buffer
        val randomString = randomStringBuffer.base64EncodedStringUsingUrlAlphabet()

        // SHA256 hash of the random string
        val hashStringBuffer = MessageDigest.getInstance("SHA-256")
            .digest(randomString.toByteArray(Charsets.UTF_8))

        // Base64URL encode the hash
        val hashString = hashStringBuffer.base64EncodedStringUsingUrlAlphabet()

        return randomString to hashString
    }
}

data class TokenResponse(
    val idToken: String
)

sealed class AuthError(val errorDescription: String) : Exception(errorDescription) {
    object InvalidUrl : AuthError("Invalid URL constructed.")
    object InsecureUrl : AuthError("`tokenEndpoint` must use https (http is allowed for localhost only).")
    object InvalidCallback : AuthError("Invalid callback URL format.")
    object InvalidCodeChallengeMethod :
        AuthError("Invalid `codeChallengeMethod` passed. Currently only `S256` is supported.")
    object InvalidParams : AuthError("Invalid params.")
    object MissingCallback : AuthError("Either `callbackScheme` or `callbackHttps` must be provided.")
    // NOTE: no `httpsCallbackUnavailable` equivalent here.
    // On Android `callbackHttps` is supported on all versions.
    class ProviderError(msg: String) : AuthError("Provider returned an error: $msg")
    object MissingState : AuthError("State missing in callback.")
    object InvalidState : AuthError("State doesn't match.")
    object MissingCode : AuthError("Authorization code missing in callback.")
    class TokenExchangeFailed(msg: String) : AuthError("Token exchange failed: $msg")
    object WebAuthCanceled : AuthError("Web authentication session canceled")
    object WebAuthFailed : AuthError("Web authentication session failed")
    // NOTE: message differs from iOS because the semantics differ:
    // iOS cancels the OLD flow and rejects its call; Android rejects the NEW call (see `authenticate`).
    object AuthInProgress : AuthError("An authentication session is already in progress.")
}

private fun ByteArray.base64EncodedStringUsingUrlAlphabet(): String {
    return Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
