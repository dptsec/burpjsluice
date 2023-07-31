package com.dptsec.burpjsluice

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi

class BurpJsluiceEntry : BurpExtension {
    override fun initialize(api: MontoyaApi?) {
        if (api == null) {
            return
        }

        api.extension().setName("BurpJsluice")
        api.logging().logToOutput("Starting..")
        BurpJsluiceConfig(api).locateBinary()

        val gui = BurpJsluiceTab(api)
        api.userInterface().registerSuiteTab("BurpJsluice", gui)
        api.proxy().registerResponseHandler(BurpJsluiceHttpResponseHandler(api, gui))
    }
}