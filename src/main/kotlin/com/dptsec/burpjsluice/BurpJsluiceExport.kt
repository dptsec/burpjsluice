package com.dptsec.burpjsluice

import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.swing.JTable

class BurpJsluiceExport {
    fun exportToCSV(tableToExport: JTable, pathToExportTo: String): Boolean {
        try {
            val model = tableToExport.model
            val csv = FileWriter(File(pathToExportTo))
            for (i in 0 until model.columnCount) {
                csv.write(model.getColumnName(i) + ",")
            }
            csv.write("\n")
            for (i in 0 until model.rowCount) {
                for (j in 0 until model.columnCount) {
                    csv.write(model.getValueAt(i, j).toString() + ",")
                }
                csv.write("\n")
            }
            csv.close()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }
}