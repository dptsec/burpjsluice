package com.dptsec.burpjsluice

import burp.api.montoya.MontoyaApi
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.regex.PatternSyntaxException
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableRowSorter
import kotlin.concurrent.thread

/*
    Object for jsluice urls output
 */
data class UrlsObj(
    val url: String,
    var src: String,
    val queryParams: Array<String>,
    val bodyParams: Array<String>,
    val method: String,
    val type: String,
)

class BurpJsluiceTab(val montoyaApi: MontoyaApi) : JPanel() {
    val model = UrlsModel()
    val table = JTable(model)
    private val errorLabel: JLabel = JLabel("")
    val rowSorter = TableRowSorter(model)

    init {
        this.layout = GridBagLayout()
        addFilterRow()
        addResultsRow()
        addExportRow()
        addClearRow()
        addErrorRow()
    }

    private fun applyFilter(sorter: TableRowSorter<UrlsModel>, filter: String) {
        val rf: RowFilter<UrlsModel?, Any?>?
        try {
            rf = RowFilter.regexFilter(filter, 2)
            clearError()
        } catch (e: PatternSyntaxException) {
            throw e
        }
        sorter.rowFilter = rf
    }

    private fun addFilterRow() {
        val c = GridBagConstraints()

        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 0
        c.gridy = 0
        c.insets = Insets(0, 0, 10, 20)
        this.add(JLabel("Filter results (regular expression):"), c)

        val textField = JTextField("", 50)
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 1
        c.gridy = 0
        this.add(textField, c)

        val applyFilterButton = JButton("Apply filter to URLs")
        applyFilterButton.addActionListener {/* Disable button while we are in the thread */
            applyFilterButton.isEnabled = false
            textField.isEnabled = false

            thread(start = true) {
                try {
                    applyFilter(rowSorter, textField.text)
                } catch (ex: Exception) {
                    displayError("Error in regular expression: ${textField.text}")
                    montoyaApi.logging().logToError(ex.toString())
                    return@thread
                } finally {
                    applyFilterButton.isEnabled = true
                    textField.isEnabled = true
                }
            }
        }
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 3
        c.gridy = 0
        this.add(applyFilterButton, c)
    }

    private fun addResultsRow() {
        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 0
        c.gridy = 1
        c.gridwidth = 4
        c.insets = Insets(0, 0, 10, 0)

        BurpJsluiceActions(this)
        table.autoResizeMode = JTable.AUTO_RESIZE_NEXT_COLUMN
        table.columnModel.getColumn(0).preferredWidth = 5 // ID
        table.columnModel.getColumn(1).preferredWidth = 400 // Src
        table.columnModel.getColumn(2).preferredWidth = 300 // URL
        table.columnModel.getColumn(5).preferredWidth = 1 // Method
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        table.rowSorter = rowSorter
        table.autoscrolls = true

        val jsTable = JScrollPane(table)
        jsTable.preferredSize = Dimension(1600, 900)
        this.add(jsTable, c)
    }

    private fun addExportRow() {
        val c = GridBagConstraints()

        val exportButton = JButton("Export Results")
        exportButton.addActionListener {
            exportButton.isEnabled = false
            val fc = JFileChooser()
            val returnVal = fc.showSaveDialog(this)
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                val fileName = fc.selectedFile.path
                thread(start = true) {
                    try {
                        montoyaApi.logging().logToOutput("Exporting results to $fileName")
                        BurpJsluiceExport().exportToCSV(table, fileName)
                    } catch (ex: Exception) {
                        displayError("Internal error. Please double check the stack trace for more details.")
                        montoyaApi.logging().logToError(ex.toString())
                        return@thread
                    } finally {
                        exportButton.isEnabled = true
                    }
                }
            } else {
                exportButton.isEnabled = true
            }
        }
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 0
        c.gridy = 2
        c.gridwidth = 4
        c.insets = Insets(0, 0, 10, 0)

        this.add(exportButton, c)
    }

    private fun addClearRow() {
        val c = GridBagConstraints()

        val clearButton = JButton("Clear Results")
        clearButton.addActionListener {
            clearButton.isEnabled = false
            model.clearUrls()
            clearButton.isEnabled = true
        }
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 0
        c.gridy = 3
        c.gridwidth = 4
        c.insets = Insets(0, 0, 10, 0)

        this.add(clearButton, c)
    }

    private fun addErrorRow() {
        val c = GridBagConstraints()

        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 0
        c.gridy = 4
        c.gridwidth = 4
        this.add(errorLabel, c)
    }

    private fun displayError(error: String) {
        if (error == "") return
        errorLabel.text = "Error: $error"
    }

    private fun clearError() {
        errorLabel.text = ""
    }
}

class UrlsModel : AbstractTableModel() {
    private val columns =
        listOf(
            "ID",
            "Source",
            "URL",
            "Query Params",
            "Body Params",
            "Method",
            "Type",
        )
    private var urlsObjArr: MutableList<UrlsObj> = ArrayList()
    var displayedUrls: MutableList<UrlsObj> = ArrayList()
        private set

    override fun getRowCount(): Int = displayedUrls.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String {
        return columns[column]
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return when (columnIndex) {
            0 -> java.lang.Integer::class.java
            1 -> String::class.java
            2 -> String::class.java
            3 -> String::class.java
            4 -> String::class.java
            5 -> String::class.java
            6 -> String::class.java
            else -> throw RuntimeException()
        }
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val urls = displayedUrls[rowIndex]

        return when (columnIndex) {
            0 -> rowIndex
            1 -> urls.src
            2 -> urls.url
            3 -> urls.queryParams.joinToString()
            4 -> urls.bodyParams.joinToString()
            5 -> urls.method
            6 -> urls.type
            else -> ""
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return false
    }

    fun addUrl(jsObj: UrlsObj, src: String) {
        jsObj.src = src
        urlsObjArr.add(jsObj)
        displayedUrls = urlsObjArr
        fireTableRowsInserted(this.rowCount - 1, this.rowCount - 1)
        refreshUrls()
    }

    fun clearUrls() {
        urlsObjArr.clear()
        refreshUrls()
    }

    private fun refreshUrls() {
        fireTableDataChanged()
    }
}