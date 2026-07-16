package com.getcapacitor.community.genericoauth2

import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "GenericOAuth2")
class GenericOAuth2Plugin : Plugin() {
    @PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
    fun authenticate(call: PluginCall) {
        call.resolve()
    }
}
