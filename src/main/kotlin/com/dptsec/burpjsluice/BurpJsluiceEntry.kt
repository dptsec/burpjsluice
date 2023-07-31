package com.dptsec.burpjsluice

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import java.util.concurrent.Executors

class BurpJsluiceEntry : BurpExtension {
    /**
     * The entry point for the BurpCage extension.
     *
     * @param api An instance of the MontoyaApi
     */
    override fun initialize(api: MontoyaApi?) {
        /* Null safety check. PortSwigger didn't add the sufficient
         * annotations to its MontoyaApi interface, so Kotlin thinks
         * that it is possible for this object to be null. This is
         * just to make Kotlin happy.
         */
        if (api == null) {
            return
        }

        api.extension().setName("BurpJsluice")
        api.logging().logToOutput("Starting..")
        BurpJsluiceConfig(api).locateBinary()
        val gui = BurpJsluiceTab(api)
        api.userInterface().registerSuiteTab("BurpJsluice", gui)
        val executor = Executors.newSingleThreadExecutor()
        api.proxy().registerResponseHandler(BurpJsluiceHttpResponseHandler(api, executor, gui))
    }
}