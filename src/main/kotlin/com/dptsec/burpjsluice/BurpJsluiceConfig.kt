package com.dptsec.burpjsluice

import burp.api.montoya.MontoyaApi
import java.io.File

class BurpJsluiceConfig(private val montoyaApi: MontoyaApi) {
    /* Try to locate the jsluice binary on the system
     * TODO: Windows support
     */
    fun locateBinary() {
        val path = montoyaApi.persistence().preferences().getString(PATH_KEY)
        if (path != null && File(path).exists()) {
            return
        }

        val gopath = System.getenv("GOPATH")
        if (gopath == null) {
            montoyaApi.logging().logToError("Error: \$GOPATH not found")
        }

        val home = System.getenv("HOME")
        if (home == null) {
            montoyaApi.logging().logToError("Error: \$HOME not found")
        }

        val paths = arrayOf(
            "$gopath/bin/jsluice",
            "$home/go/bin/jsluice",
            "$home/bin/jsluice",
            "/usr/bin/jsluice",
            "/usr/local/bin/jsluice"
        )
        paths.forEach {
            val bin = File(it)
            if (bin.exists()) {
                montoyaApi.logging().logToError("Located binary: $it")
                saveBinaryPath(it)
            }
        }
        return
    }

    private fun saveBinaryPath(path: String) {
        montoyaApi.persistence().preferences().setString(PATH_KEY, path)
        montoyaApi.logging().logToOutput("Configuration: Saved jsluice path ($path) to settings.")
    }
}