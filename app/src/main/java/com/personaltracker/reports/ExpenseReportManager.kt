package com.personaltracker.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.personaltracker.data.database.dao.CategoryTotal
import com.personaltracker.data.database.entity.ExpenseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates CSV and PDF expense reports and saves them to the app's external Documents
 * directory (accessible via FileProvider for sharing).  No cloud storage is used.
 */
@Singleton
class ExpenseReportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val timestampFmt  = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val displayDateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val headerDateFmt  = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

    // ── CSV Export ────────────────────────────────────────────────────────────

    fun exportCsv(
        expenses: List<ExpenseEntity>,
        categoryTotals: List<CategoryTotal>,
        reportTitle: String,
        yearMonth: String
    ): File {
        val file = newExportFile("csv", yearMonth)
        val grandTotal = categoryTotals.sumOf { it.total }

        file.bufferedWriter().use { w ->
            w.write("SuryaWorld Personal Finance Tracker\n")
            w.write("Report: $reportTitle\n")
            w.write("Period: $yearMonth\n")
            w.write("Generated: ${LocalDateTime.now().format(headerDateFmt)}\n")
            w.write("All data is stored locally on device.\n")
            w.write("\n")

            // Category summary
            w.write("CATEGORY SUMMARY\n")
            w.write("Category,Amount (INR),Percentage\n")
            categoryTotals.sortedByDescending { it.total }.forEach { ct ->
                val pct = if (grandTotal > 0) ct.total / grandTotal * 100.0 else 0.0
                w.write("${csvEscape(ct.category)},${fmtAmount(ct.total)},${fmtPct(pct)}\n")
            }
            w.write("GRAND TOTAL,${fmtAmount(grandTotal)},100%\n")
            w.write("\n")

            // Detailed transactions
            w.write("DETAILED TRANSACTIONS\n")
            w.write("Date,Category,Sub-Category,Description,Amount (INR),Payment Method,Type,Notes\n")
            expenses.sortedByDescending { it.date }.forEach { e ->
                w.write(
                    "${e.date.format(displayDateFmt)}," +
                    "${csvEscape(e.category)}," +
                    "${csvEscape(e.subCategory ?: "")}," +
                    "${csvEscape(e.description ?: "")}," +
                    "${fmtAmount(e.amount)}," +
                    "${csvEscape(e.paymentMethod)}," +
                    "${csvEscape(e.expenseType)}," +
                    "${csvEscape(e.notes ?: "")}\n"
                )
            }
        }
        return file
    }

    // ── PDF Export ────────────────────────────────────────────────────────────

    fun exportPdf(
        expenses: List<ExpenseEntity>,
        categoryTotals: List<CategoryTotal>,
        reportTitle: String,
        yearMonth: String
    ): File {
        val file = newExportFile("pdf", yearMonth)
        val grandTotal = categoryTotals.sumOf { it.total }

        val pdfDocument = PdfDocument()
        val pageWidth   = 595   // A4 @ 72 dpi
        val pageHeight  = 842
        val margin      = 40f
        val contentW    = (pageWidth - margin * 2)

        var pageNum = 1
        var yPos    = margin
        var curPage: PdfDocument.Page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        )
        var canvas: Canvas = curPage.canvas

        fun ensureSpace(need: Float) {
            if (yPos + need > pageHeight - margin - 16f) {
                pdfDocument.finishPage(curPage)
                pageNum++
                curPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                )
                canvas = curPage.canvas
                yPos = margin
            }
        }

        // ── Paint objects ─────────────────────────────────────────────────────
        val brandPaint   = makePaint(Color.rgb(0x1a, 0x73, 0xe8), 22f, bold = true)
        val subPaint     = makePaint(Color.DKGRAY, 10f)
        val sectionPaint = makePaint(Color.rgb(0x1a, 0x73, 0xe8), 13f, bold = true)
        val hdrPaint     = makePaint(Color.WHITE, 9.5f, bold = true)
        val bodyPaint    = makePaint(Color.BLACK, 9f)
        val bodyBoldPaint= makePaint(Color.BLACK, 9f, bold = true)
        val errorPaint   = makePaint(Color.rgb(0xc0, 0x39, 0x2b), 18f, bold = true)
        val linePaint    = Paint().apply { color = Color.rgb(0xcc, 0xcc, 0xcc); strokeWidth = 0.5f }
        val hdrBgPaint   = Paint().apply { color = Color.rgb(0x1a, 0x73, 0xe8) }
        val altRowPaint  = Paint().apply { color = Color.rgb(0xf5, 0xf5, 0xf5) }
        val whitePaint   = Paint().apply { color = Color.WHITE }
        val totalRowPaint= Paint().apply { color = Color.rgb(0x2c, 0x3e, 0x50) }
        val summaryBgPaint = Paint().apply { color = Color.rgb(0xfe, 0xeb, 0xeb) }

        // ── Title block ───────────────────────────────────────────────────────
        canvas.drawText("SuryaWorld", margin, yPos + 20f, brandPaint)
        yPos += 26f
        canvas.drawText("$reportTitle — $yearMonth", margin, yPos, subPaint)
        yPos += 14f
        canvas.drawText("Generated: ${LocalDateTime.now().format(headerDateFmt)}", margin, yPos, subPaint)
        yPos += 6f
        linePaint.color = Color.rgb(0x1a, 0x73, 0xe8); linePaint.strokeWidth = 1.5f
        canvas.drawLine(margin, yPos, margin + contentW, yPos, linePaint)
        yPos += 20f

        // ── Grand total banner ────────────────────────────────────────────────
        canvas.drawRect(margin, yPos, margin + contentW, yPos + 52f, summaryBgPaint)
        canvas.drawText("Total Expenses for $yearMonth", margin + 10f, yPos + 16f, subPaint)
        canvas.drawText("₹${fmtAmount(grandTotal)}", margin + 10f, yPos + 38f, errorPaint)
        yPos += 64f

        // ── Category summary table ────────────────────────────────────────────
        ensureSpace(60f)
        canvas.drawText("Category Breakdown", margin, yPos, sectionPaint)
        yPos += 16f

        val c1 = margin; val c2 = margin + contentW * 0.48f
        val c3 = margin + contentW * 0.72f

        canvas.drawRect(c1, yPos, margin + contentW, yPos + 18f, hdrBgPaint)
        canvas.drawText("Category",   c1 + 4f, yPos + 13f, hdrPaint)
        canvas.drawText("Amount (₹)", c2,       yPos + 13f, hdrPaint)
        canvas.drawText("Share %",    c3,       yPos + 13f, hdrPaint)
        yPos += 18f

        categoryTotals.sortedByDescending { it.total }.forEachIndexed { i, ct ->
            ensureSpace(16f)
            val rowPaint = if (i % 2 == 0) whitePaint else altRowPaint
            canvas.drawRect(c1, yPos, margin + contentW, yPos + 15f, rowPaint)
            val pct = if (grandTotal > 0) ct.total / grandTotal * 100.0 else 0.0
            canvas.drawText(ct.category.take(32), c1 + 4f, yPos + 11f, bodyPaint)
            canvas.drawText(fmtAmount(ct.total),  c2,       yPos + 11f, bodyPaint)
            canvas.drawText(fmtPct(pct),           c3,       yPos + 11f, bodyPaint)
            yPos += 15f
        }

        ensureSpace(15f)
        canvas.drawRect(c1, yPos, margin + contentW, yPos + 15f, totalRowPaint)
        canvas.drawText("GRAND TOTAL",         c1 + 4f, yPos + 11f, hdrPaint)
        canvas.drawText(fmtAmount(grandTotal), c2,       yPos + 11f, hdrPaint)
        canvas.drawText("100%",                c3,       yPos + 11f, hdrPaint)
        yPos += 26f

        // ── Transactions table ────────────────────────────────────────────────
        ensureSpace(50f)
        canvas.drawText("Detailed Transactions (${expenses.size} items)", margin, yPos, sectionPaint)
        yPos += 16f

        val tc1 = margin; val tc2 = margin + 55f; val tc3 = margin + 145f
        val tc4 = margin + 260f; val tc5 = margin + 340f; val tc6 = margin + contentW - 60f

        canvas.drawRect(tc1, yPos, margin + contentW, yPos + 18f, hdrBgPaint)
        canvas.drawText("Date",     tc1 + 4f, yPos + 13f, hdrPaint)
        canvas.drawText("Category", tc2,      yPos + 13f, hdrPaint)
        canvas.drawText("Description", tc3,   yPos + 13f, hdrPaint)
        canvas.drawText("Method",   tc4,      yPos + 13f, hdrPaint)
        canvas.drawText("Type",     tc5,      yPos + 13f, hdrPaint)
        canvas.drawText("Amount",   tc6,      yPos + 13f, hdrPaint)
        yPos += 18f

        expenses.sortedByDescending { it.date }.forEachIndexed { i, e ->
            ensureSpace(14f)
            val rowPaint = if (i % 2 == 0) whitePaint else altRowPaint
            canvas.drawRect(tc1, yPos, margin + contentW, yPos + 13f, rowPaint)
            canvas.drawText(e.date.format(DateTimeFormatter.ofPattern("dd MMM")), tc1 + 4f, yPos + 10f, bodyPaint)
            canvas.drawText(e.category.take(13),    tc2, yPos + 10f, bodyPaint)
            val desc = (e.description ?: e.subCategory ?: "").take(14)
            canvas.drawText(desc,                   tc3, yPos + 10f, bodyPaint)
            canvas.drawText(e.paymentMethod.take(9),tc4, yPos + 10f, bodyPaint)
            canvas.drawText(e.expenseType.take(8),  tc5, yPos + 10f, bodyPaint)
            canvas.drawText(fmtAmount(e.amount),    tc6, yPos + 10f, bodyBoldPaint)
            yPos += 13f
        }

        // ── Footer ────────────────────────────────────────────────────────────
        ensureSpace(28f)
        yPos += 12f
        linePaint.color = Color.LTGRAY; linePaint.strokeWidth = 0.5f
        canvas.drawLine(margin, yPos, margin + contentW, yPos, linePaint)
        yPos += 10f
        canvas.drawText(
            "SuryaWorld  •  All data stored locally on device  •  No cloud storage",
            margin, yPos, subPaint
        )

        pdfDocument.finishPage(curPage)
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        return file
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun newExportFile(extension: String, yearMonth: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "exports").also { it.mkdirs() }
        if (!dir.exists()) dir.mkdirs()
        val timestamp = LocalDateTime.now().format(timestampFmt)
        return File(dir, "expense_report_${yearMonth.replace("-", "")}_$timestamp.$extension")
    }

    private fun makePaint(color: Int, textSize: Float, bold: Boolean = false) = Paint().apply {
        this.color    = color
        this.textSize = textSize
        typeface      = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun fmtAmount(amount: Double) = String.format("%,.2f", amount)
    private fun fmtPct(pct: Double)       = String.format("%.1f%%", pct)
    private fun csvEscape(s: String)      = "\"${s.replace("\"", "\"\"")}\""
}
