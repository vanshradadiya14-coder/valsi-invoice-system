package com.valsi.invoicesystem.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.graphics.scale
import com.valsi.invoicesystem.data.entity.AppSettings
import com.valsi.invoicesystem.data.entity.InvoiceDetail
import com.valsi.invoicesystem.data.entity.PaymentStatus
import com.valsi.invoicesystem.util.DateUtils
import com.valsi.invoicesystem.util.Money
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a finalized invoice to a professional single- or multi-page A4 PDF using the
 * platform [PdfDocument] API. Everything is drawn from the stored invoice data (price
 * snapshots), so a regenerated PDF always matches what was originally issued.
 */
@Singleton
class PdfInvoiceGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        const val PAGE_WIDTH = 595   // A4 @ 72dpi
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        val CONTENT_RIGHT = PAGE_WIDTH - MARGIN
        val GREEN = 0xFF1B5E20.toInt()
    }

    suspend fun generate(detail: InvoiceDetail, settings: AppSettings): File =
        withContext(Dispatchers.IO) {
            val document = PdfDocument()
            val symbol = settings.currencySymbol

            val titlePaint = paint(20f, bold = true, color = GREEN)
            val bigPaint = paint(24f, bold = true, color = GREEN)
            val labelPaint = paint(10f, bold = true)
            val textPaint = paint(11f)
            val smallPaint = paint(9f, color = Color.DKGRAY)
            val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

            var pageNumber = 1
            var page = document.startPage(pageInfo(pageNumber))
            var canvas = page.canvas
            var y = MARGIN

            // ---- Header: company + logo ----
            drawLogo(canvas, settings)
            canvas.drawText(settings.companyName.ifBlank { "Valsi Foods" }, MARGIN, y + 16f, titlePaint)
            y += 24f
            listOf(
                settings.companyAddress,
                settings.companyPhone,
                settings.companyEmail,
                if (settings.vatNumber.isNotBlank()) "VAT: ${settings.vatNumber}" else "",
            ).filter { it.isNotBlank() }.forEach { line ->
                canvas.drawText(line, MARGIN, y + 12f, smallPaint)
                y += 14f
            }

            // ---- Invoice title block (right aligned) ----
            canvas.drawText("INVOICE", CONTENT_RIGHT, MARGIN + 20f, right(bigPaint))
            canvas.drawText(detail.invoice.invoiceNumber, CONTENT_RIGHT, MARGIN + 38f, right(textPaint))
            canvas.drawText(
                DateUtils.formatDate(detail.invoice.createdAt),
                CONTENT_RIGHT, MARGIN + 52f, right(smallPaint),
            )

            y = maxOf(y, MARGIN + 66f) + 10f
            canvas.drawLine(MARGIN, y, CONTENT_RIGHT, y, linePaint)
            y += 20f

            // ---- Bill To ----
            canvas.drawText("BILL TO", MARGIN, y, labelPaint)
            y += 16f
            canvas.drawText(detail.customer.storeName, MARGIN, y, paint(12f, bold = true)); y += 15f
            canvas.drawText(detail.customer.ownerName, MARGIN, y, textPaint); y += 14f
            if (detail.customer.address.isNotBlank()) {
                canvas.drawText(detail.customer.address, MARGIN, y, textPaint); y += 14f
            }
            canvas.drawText(detail.customer.phoneNumber, MARGIN, y, textPaint); y += 20f

            // ---- Line item table header ----
            val colItem = MARGIN
            val colQty = 330f
            val colUnit = 400f
            val colTotal = CONTENT_RIGHT

            fun drawTableHeader(atY: Float): Float {
                canvas.drawText("ITEM", colItem, atY, labelPaint)
                canvas.drawText("QTY", colQty, atY, center(labelPaint))
                canvas.drawText("UNIT", colUnit, atY, right(labelPaint))
                canvas.drawText("TOTAL", colTotal, atY, right(labelPaint))
                val ny = atY + 6f
                canvas.drawLine(MARGIN, ny, CONTENT_RIGHT, ny, linePaint)
                return ny + 16f
            }
            y = drawTableHeader(y)

            // ---- Rows (with pagination) ----
            val bottomLimit = PAGE_HEIGHT - MARGIN - 120f // leave room for totals/footer
            for (item in detail.items) {
                if (y > bottomLimit) {
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(pageInfo(pageNumber))
                    canvas = page.canvas
                    y = MARGIN + 10f
                    y = drawTableHeader(y)
                }
                canvas.drawText(ellipsize(item.productNameSnapshot, 44), colItem, y, textPaint)
                canvas.drawText(item.quantity.toString(), colQty, y, center(textPaint))
                canvas.drawText(Money.format(item.unitPriceSnapshot, symbol), colUnit, y, right(textPaint))
                canvas.drawText(Money.format(item.lineTotal, symbol), colTotal, y, right(textPaint))
                y += 18f
            }

            y += 6f
            canvas.drawLine(320f, y, CONTENT_RIGHT, y, linePaint)
            y += 18f

            // ---- Totals ----
            fun totalRow(label: String, value: String, emphasize: Boolean = false) {
                val lp = if (emphasize) paint(13f, bold = true, color = GREEN) else textPaint
                val vp = if (emphasize) paint(13f, bold = true, color = GREEN) else textPaint
                canvas.drawText(label, 330f, y, lp)
                canvas.drawText(value, CONTENT_RIGHT, y, right(vp))
                y += 18f
            }
            val inv = detail.invoice
            totalRow("Subtotal", Money.format(inv.subtotal, symbol))
            if (inv.discountAmount > 0.0) totalRow("Discount", "- ${Money.format(inv.discountAmount, symbol)}")
            totalRow("VAT", Money.format(inv.vatAmount, symbol))
            totalRow("Grand Total", Money.format(inv.grandTotal, symbol), emphasize = true)

            y += 6f
            val statusText = when (inv.paymentStatus) {
                PaymentStatus.PAID -> "PAID"
                PaymentStatus.UNPAID -> "UNPAID"
                PaymentStatus.PARTIAL ->
                    "PARTIAL — Paid ${Money.format(inv.amountPaid, symbol)} of ${Money.format(inv.grandTotal, symbol)}"
            }
            canvas.drawText("Payment: $statusText", 330f, y, paint(11f, bold = true))
            y += 30f

            // ---- Signature + footer ----
            canvas.drawLine(MARGIN, y, MARGIN + 200f, y, linePaint)
            canvas.drawText("Received by", MARGIN, y + 14f, smallPaint)

            val footerY = PAGE_HEIGHT - MARGIN
            inv.notes?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText("Notes: $it", MARGIN, footerY - 24f, smallPaint)
            }
            canvas.drawText(
                "Thank you for your business",
                PAGE_WIDTH / 2f, footerY, center(paint(11f, bold = true, color = GREEN)),
            )

            document.finishPage(page)

            val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val file = File(dir, "${inv.invoiceNumber}.pdf")
            file.outputStream().use { document.writeTo(it) }
            document.close()
            file
        }

    private fun pageInfo(number: Int) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()

    private fun drawLogo(canvas: Canvas, settings: AppSettings): Unit? {
        val uriStr = settings.companyLogoUri ?: return null
        return try {
            val bitmap = context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                BitmapFactory.decodeStream(it)
            } ?: return null
            val size = 64
            val scaled = bitmap.scale(size, size)
            canvas.drawBitmap(scaled, CONTENT_RIGHT - size, MARGIN + 60f, null)
            if (scaled != bitmap) bitmap.recycle()
            Unit
        } catch (e: Exception) {
            null
        }
    }

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint().apply {
        this.color = color
        textSize = size
        isAntiAlias = true
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun right(base: Paint) = Paint(base).apply { textAlign = Paint.Align.RIGHT }
    private fun center(base: Paint) = Paint(base).apply { textAlign = Paint.Align.CENTER }

    private fun ellipsize(text: String, max: Int) =
        if (text.length <= max) text else text.take(max - 1) + "…"
}
