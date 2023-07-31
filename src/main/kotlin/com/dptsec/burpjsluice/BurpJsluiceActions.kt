package com.dptsec.burpjsluice

import burp.api.montoya.http.message.requests.HttpRequest.httpRequestFromUrl
import org.apache.commons.validator.routines.UrlValidator
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.net.URL
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class BurpJsluiceActions(private val tab: BurpJsluiceTab) : ActionListener {
    private val menu = JPopupMenu()
    private val sendRepeater = JMenuItem("Send to Repeater")
    private val deleteURL = JMenuItem("Delete URL(s)")
    private val copyURL = JMenuItem("Copy URL to clipboard")
    private val copySrc = JMenuItem("Copy source URL to clipboard")

    init {
        sendRepeater.addActionListener(this)
        deleteURL.addActionListener(this)
        copyURL.addActionListener(this)
        copySrc.addActionListener(this)
        menu.add(sendRepeater)
        menu.add(deleteURL)
        menu.add(copyURL)
        menu.add(copySrc)
        tab.table.componentPopupMenu = menu
    }

    override fun actionPerformed(e: ActionEvent?) {
        if (tab.table.selectedRow == -1) return
        val selected = getSelected()
        when (e?.source) {
            sendRepeater -> {
                selected.forEach {
                    sendToRepeater(it)
                }
            }

            deleteURL -> {
                TODO("Not Implemented")
            }

            copyURL -> {
                val urls = selected.joinToString("\n") { it.url }
                val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(urls), null)
            }

            copySrc -> {
                val urls = selected.joinToString("\n") { it.src }
                val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(urls), null)
            }
        }
    }

    private fun getSelected(): MutableList<UrlsObj> {
        val selected: MutableList<UrlsObj> = ArrayList()
        for (index in tab.table.selectedRows) {
            val row = tab.rowSorter.convertRowIndexToModel(index)
            selected.add(tab.model.displayedUrls[row])
        }
        return selected
    }

    private fun sendToRepeater(entry: UrlsObj) {
        val method = if (entry.method != "") entry.method else "GET"

        val req = httpRequestFromUrl(parse(entry))
            .withMethod(method)
            .withBody(entry.bodyParams.joinToString("&"))
        tab.montoyaApi.repeater().sendToRepeater(req)
    }

    private fun parse(entry: UrlsObj): String {
        val u = if (entry.url.startsWith("//")) "https:${entry.url}" else entry.url

        if (UrlValidator().isValid(u)) {
            return u
        }
        val src = URL(entry.src)
        val base = src.protocol + "://" + src.host
        return if (!u.startsWith("/")) "$base/$u" else "$base$u"
    }
}