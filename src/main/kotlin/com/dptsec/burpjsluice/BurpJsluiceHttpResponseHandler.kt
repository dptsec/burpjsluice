package com.dptsec.burpjsluice

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.MimeType
import burp.api.montoya.proxy.http.InterceptedResponse
import burp.api.montoya.proxy.http.ProxyResponseHandler
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction
import kotlinx.coroutines.*

class BurpJsluiceHttpResponseHandler(
    private val api: MontoyaApi,
    private val gui: BurpJsluiceTab
) :
    ProxyResponseHandler {
    private val dispatcher = newFixedThreadPoolContext(1, "App Background")
    private fun isScript(interceptedResponse: InterceptedResponse): Boolean {
        return interceptedResponse.inferredMimeType() == MimeType.SCRIPT || interceptedResponse.statedMimeType() == MimeType.SCRIPT
    }

    override fun handleResponseReceived(interceptedResponse: InterceptedResponse?): ProxyResponseReceivedAction {
        /* This should never happen */
        if (interceptedResponse == null) {
            api.logging().logToError("Null response received. Dropping message.")
            return ProxyResponseReceivedAction.drop()
        }

        /* Condition to determine if we should ignore this response */
        if (!this.api.scope()
                .isInScope(interceptedResponse.initiatingRequest().url()) || !isScript(interceptedResponse)
        ) {
            return ProxyResponseReceivedAction.continueWith(
                interceptedResponse
            )
        }

        /* Launch coroutine for background processing */
        GlobalScope.launch(dispatcher) {
            BurpJsluiceExec(api, gui).run(
                interceptedResponse.bodyToString(),
                interceptedResponse.initiatingRequest().url()
            )
        }

        return ProxyResponseReceivedAction.continueWith(
            interceptedResponse
        )
    }

    override fun handleResponseToBeSent(interceptedResponse: InterceptedResponse?): ProxyResponseToBeSentAction {
        return ProxyResponseToBeSentAction.continueWith(interceptedResponse)
    }
}