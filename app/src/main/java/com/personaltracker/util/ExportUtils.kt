package com.personaltracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.personaltracker.data.database.entity.ExpenseEntity
import java.io.File
import java.time.format.DateTimeFormatter

object ExportUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun exportExpensesToCsv(
        context: Context,
        expenses: List<ExpenseEntity>,
        fileName: String = "expenses_export.csv"
    ): File {
        val cacheDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(cacheDir, fileName)
        file.bufferedWriter().use { writer ->
            writer.write("Date,Category,Sub-Category,Description,Amount,Payment Method,Type\n")
            expenses.forEach { expense ->
                writer.write("${expense.date.format(dateFormatter)},${expense.category},${expense.subCategory ?: ""},${expense.description ?: ""},${expense.amount},${expense.paymentMethod},${expense.expenseType}\n")
            }
        }
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String = "text/csv") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }
}
