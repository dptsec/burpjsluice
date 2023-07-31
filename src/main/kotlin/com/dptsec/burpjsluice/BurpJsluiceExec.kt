package com.dptsec.burpjsluice

import burp.api.montoya.MontoyaApi
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities


class BurpJsluiceExec(private val api: MontoyaApi, private val gui: BurpJsluiceTab) {
    private fun String.runCommand(): String? {
        return try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(File("/tmp"))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            proc.waitFor(10, TimeUnit.SECONDS)
            val ret = proc.inputStream.bufferedReader().readText()
            proc.inputStream.close()
            proc.outputStream.close()
            proc.errorStream.close()
            ret
        } catch (e: IOException) {
            api.logging().logToError("Error: ${e.printStackTrace()}\n")
            null
        }
    }

    /* Write the contents of a script to a temporary file and run jsluice on it
     * Parse each line of the command output as JSON and update our tab
     */
    fun run(content: String, requestURL: String): Runnable {
        val bin = api.persistence().preferences().getString(PATH_KEY)
        if (bin != null) {
            val file: File = File.createTempFile("burpjsluice", null, File("/tmp"))
            File(file.absolutePath).writeText(content)

            val response = "$bin urls ${file.absolutePath}".runCommand()
            file.delete()

            response?.lines()?.forEach {
                if (it.isNotEmpty()) {
                    val url: UrlsObj = Gson().fromJson(it, UrlsObj::class.java)
                    SwingUtilities.invokeLater {
                        gui.model.addUrl(url, requestURL)
                    }
                }
            }
            file.delete()
        } else {
            api.logging().logToError("Error: No jsluice binary found (we shouldn't be here)")
        }
        return Runnable { }
    }
}