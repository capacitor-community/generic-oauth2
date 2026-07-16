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
        responseMode: String,
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
            .appendQueryParameter("response_mode", responseMode)
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("state", state)
            .appendQueryParameter("nonce", hashedNonce)

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

        // Check for errors in the callback
        callbackUrl.getQueryParameter("error")?.let { error ->
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
            throw AuthError.TokenExchangeFailed("Decoding response failed: ${e.message}. Payload: $dataString")
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
    object InvalidCallback : AuthError("Invalid callback URL format.")
    object InvalidCodeChallengeMethod :
        AuthError("Invalid `codeChallengeMethod` passed. Currently only `S256` is supported.")
    object InvalidParams : AuthError("Invalid params.")
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