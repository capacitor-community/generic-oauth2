package com.getcapacitor.community.genericoauth2

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@CapacitorPlugin(name = "GenericOAuth2")
class GenericOAuth2Plugin : Plugin() {

    // All per-flow state lives in one nullable holder, so claiming or
    // rejecting a flow is a single assignment.
    private class PendingFlow(
        val call: PluginCall,
        val options: AuthenticateParams,
        val authorizationRequest: AuthorizationRequest
    )

    private var currentFlow: PendingFlow? = null

    private var launcher: ActivityResultLauncher<Intent>? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class AuthenticateCallbackHttpsParams(
        val host: String,
        val path: String
    )

    private data class AuthenticateParams(
        val authorizationEndpoint: String,
        val clientId: String,
        val redirectUri: String,
        val scope: String,
        val tokenEndpoint: String,
        val callbackScheme: String,
        val callbackHttps: AuthenticateCallbackHttpsParams?,
        val responseType: String,
        val responseMode: String,
        val codeChallengeMethod: String,

        // Ask the browser not to share cookies/website data,
        // which forces a fresh login. Defaults to false.
        // Best-effort: browsers that don't support ephemeral browsing ignore the hint.
        val preferEphemeralBrowsing: Boolean
    )

    override fun load() {
        super.load()
        // `AuthTabIntent.registerActivityResultLauncher` should be called unconditionally before the fragment or activity is created.
        // Capacitor's `load()` runs during bridge init in the activity's `onCreate`, which satisfies that.
        launcher = AuthTabIntent.registerActivityResultLauncher(activity) { result ->
            val callbackUrl = result.resultUri
            if (result.resultCode == AuthTabIntent.RESULT_OK && callbackUrl != null) {
                handleCallback(callbackUrl)
            } else if (result.resultCode == AuthTabIntent.RESULT_CANCELED) {
                rejectPendingFlow(AuthError.WebAuthCanceled)
            } else {
                rejectPendingFlow(AuthError.WebAuthFailed)
            }
        }
    }

    @PluginMethod
    fun authenticate(call: PluginCall) {
        val options = decodeParams(call) ?: run {
            call.reject(AuthError.InvalidParams.errorDescription)
            return
        }

        val launcher = this.launcher ?: run {
            call.reject(AuthError.WebAuthFailed.errorDescription)
            return
        }

        // If a flow is already in flight, reject the NEW call.
        //
        // NOTE: this deliberately diverges from iOS, which cancels the old
        // session and proceeds with the new one. There is no programmatic
        // equivalent of `ASWebAuthenticationSession.cancel()` for an Auth
        // Tab, and a replaced tab can deliver its (canceled) result AFTER the
        // new flow is stored — with no flow identity on the result to tell
        // the two apart. Single-flight semantics eliminate that race; the
        // pending flow always terminates (closing the tab yields
        // RESULT_CANCELED), so nothing gets stuck.
        if (currentFlow != null) {
            call.reject(AuthError.AuthInProgress.errorDescription)
            return
        }

        val authorizationRequest: AuthorizationRequest
        try {
            authorizationRequest = OAuth2Helper.generateAuthorizationRequest(
                authorizationEndpoint = options.authorizationEndpoint,
                clientId = options.clientId,
                redirectUri = options.redirectUri,
                scope = options.scope,
                responseType = options.responseType,
                responseMode = options.responseMode,
                codeChallengeMethod = options.codeChallengeMethod
            )
        } catch (e: AuthError) {
            call.reject(e.errorDescription)
            return
        }

        val authTabIntent = AuthTabIntent.Builder()
            .apply {
                if (options.preferEphemeralBrowsing) {
                    setEphemeralBrowsingEnabled(true)
                }
            }
            .build()

        currentFlow = PendingFlow(
            call = call,
            options = options,
            authorizationRequest = authorizationRequest
        )

        try {
            val callbackHttps = options.callbackHttps
            if (callbackHttps != null) {
                authTabIntent.launch(launcher, authorizationRequest.url, callbackHttps.host, callbackHttps.path)
            } else {
                authTabIntent.launch(launcher, authorizationRequest.url, options.callbackScheme)
            }
        } catch (e: Exception) {
            // No browser available, or launch failed.
            currentFlow = null
            call.reject(AuthError.WebAuthFailed.errorDescription)
        }
    }

    // Fallback path: on browsers without Auth Tab support the intent is launched as a plain Custom Tab,
    // and the redirect (if the app declares a matching intent filter) arrives here instead of through the launcher.
    override fun handleOnNewIntent(intent: Intent) {
        super.handleOnNewIntent(intent)
        val callbackUrl = intent.data ?: return
        val flow = currentFlow ?: return

        // We need to somehow find out if this `handleOnNewIntent` was meant for us.
        // Because apps commonly use one custom scheme for ALL their deep links,
        // and a scheme-only match would let an unrelated deep link arriving mid-flow
        // (e.g. from a tapped notification) claim and kill the pending auth flow.
        //
        // We do this by comparing if the `state` param is present in the callbackUrl and equals our known `state`.
        // This is better than comparing to the full path of the known `redirectUri`,
        // because the redirect that intents to close the Auth Tab
        // is not necessarily the same as the actual `callbackUrl` (because of redirects for example).
        // Any URI failing the `state` check would only have failed the regular state validation later on anyway.
        //
        // Note that this matching is done only for routing accuracy, not for security.
        // For security, we have the `state` and PKCE checks in place.
        //
        // Another note: we deliberately do NOT apply this pre-check to the launcher path.
        // Not only because it's not needed there, but also because its one result would then already be spent,
        // so declining the claim would hang the flow forever instead of throwing the `MissingState` or `InvalidState` error.
        val stateMatches = !callbackUrl.isOpaque && callbackUrl.getQueryParameter("state") == flow.authorizationRequest.state
        if (!stateMatches) {
            return
        }

        handleCallback(callbackUrl)
    }

    private fun handleCallback(callbackUrl: Uri) {
        val flow = currentFlow ?: return

        currentFlow = null

        scope.launch {
            try {
                val idToken = OAuth2Helper.handleCallback(
                    tokenEndpoint = flow.options.tokenEndpoint,
                    clientId = flow.options.clientId,
                    redirectUri = flow.options.redirectUri,
                    callbackUrl = callbackUrl,
                    state = flow.authorizationRequest.state,
                    verifier = flow.authorizationRequest.verifier
                )

                val result = JSObject()
                result.put("idToken", idToken)
                result.put("rawNonce", flow.authorizationRequest.rawNonce)
                flow.call.resolve(result)
            } catch (e: AuthError) {
                flow.call.reject(e.errorDescription)
            } catch (e: Exception) {
                flow.call.reject(AuthError.WebAuthFailed.errorDescription)
            }
        }
    }

    private fun rejectPendingFlow(error: AuthError) {
        val flow = currentFlow ?: return
        currentFlow = null
        flow.call.reject(error.errorDescription)
    }

    override fun handleOnDestroy() {
        super.handleOnDestroy()
        scope.cancel()
    }

    private fun decodeParams(call: PluginCall): AuthenticateParams? {
        val callbackHttps = call.getObject("callbackHttps")?.let {
            AuthenticateCallbackHttpsParams(
                host = it.getString("host") ?: return null,
                path = it.getString("path") ?: return null
            )
        }

        return AuthenticateParams(
            authorizationEndpoint = call.getString("authorizationEndpoint") ?: return null,
            clientId = call.getString("clientId") ?: return null,
            redirectUri = call.getString("redirectUri") ?: return null,
            scope = call.getString("scope") ?: return null,
            tokenEndpoint = call.getString("tokenEndpoint") ?: return null,
            callbackScheme = call.getString("callbackScheme") ?: return null,
            callbackHttps = callbackHttps,
            responseType = call.getString("responseType") ?: return null,
            responseMode = call.getString("responseMode") ?: return null,
            codeChallengeMethod = call.getString("codeChallengeMethod") ?: return null,
            preferEphemeralBrowsing = call.getBoolean("preferEphemeralBrowsing") ?: false
        )
    }
}