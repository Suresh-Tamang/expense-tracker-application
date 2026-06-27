package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.Expense
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateExpenseReport(
        context: Context,
        monthKey: String,
        expenses: List<Expense>,
        budget: Double,
        currencySymbol: String = "$",
        userName: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(33, 150, 243) // Modern blue accent
            isAntiAlias = true
        }

        // Page setup constants
        val pageWidth = 595 // A4 Width in points
        val pageHeight = 842 // A4 Height in points
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = currentPage.canvas

        // Header Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 100f, headerPaint)
        
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("FINFLOW BUDGET REPORT", 40f, 45f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        val reportDateStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthKey) ?: Date()
        )
        canvas.drawText("Monthly Period: $reportDateStr", 40f, 75f, paint)
        if (userName.isNotEmpty()) {
            canvas.drawText("User: $userName", 400f, 45f, paint)
        }
        canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 400f, 75f, paint)

        // Summary Calculations
        val totalSpent = expenses.sumOf { it.amount }
        val categoryTotals = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }

        var yPos = 140f

        // Overview Box
        paint.color = Color.rgb(245, 247, 250)
        canvas.drawRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + 70f, paint)

        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("FINANCIAL OVERVIEW", 50f, yPos + 25f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 11f
        canvas.drawText("Total Budget: $currencySymbol${String.format("%.2f", budget)}", 50f, yPos + 48f, paint)
        canvas.drawText("Total Spent: $currencySymbol${String.format("%.2f", totalSpent)}", 220f, yPos + 48f, paint)

        val balance = budget - totalSpent
        if (balance >= 0) {
            paint.color = Color.rgb(46, 125, 50) // Green
            canvas.drawText("Remaining: $currencySymbol${String.format("%.2f", balance)}", 390f, yPos + 48f, paint)
        } else {
            paint.color = Color.rgb(198, 40, 40) // Red
            canvas.drawText("Over Budget: $currencySymbol${String.format("%.2f", -balance)}", 390f, yPos + 48f, paint)
        }

        yPos += 100f

        // Category Breakdown Header
        paint.color = Color.BLACK
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("SPENDING BY CATEGORY", 40f, yPos, paint)
        yPos += 15f

        paint.strokeWidth = 1f
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, yPos, (pageWidth - 40).toFloat(), yPos, paint)
        yPos += 20f

        // Category list
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY

        var catColumn = 0
        val columnWidth = 170f
        categoryTotals.forEach { (cat, amt) ->
            val colX = 40f + (catColumn * columnWidth)
            canvas.drawText("• $cat: $currencySymbol${String.format("%.2f", amt)}", colX, yPos, paint)
            catColumn++
            if (catColumn >= 3) {
                catColumn = 0
                yPos += 18f
            }
        }
        if (catColumn > 0) {
            yPos += 25f
        } else {
            yPos += 10f
        }

        // --- DRAW VISUAL CATEGORY CHART IN PDF ---
        if (categoryTotals.isNotEmpty()) {
            val chartCardHeight = (categoryTotals.size * 22f) + 30f
            
            // Draw chart container card background
            paint.color = Color.rgb(245, 247, 250)
            canvas.drawRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + chartCardHeight, paint)

            // Draw a subtle border around the chart card
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = Color.rgb(220, 224, 230)
            paint.strokeWidth = 1f
            canvas.drawRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + chartCardHeight, paint)
            
            // Reset paint style
            paint.style = android.graphics.Paint.Style.FILL

            // Chart title
            paint.color = Color.BLACK
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("CATEGORY SPENDING DISTRIBUTION CHART", 55f, yPos + 22f, paint)

            paint.isFakeBoldText = false
            var barY = yPos + 36f
            val maxCatValue = categoryTotals.values.maxOrNull() ?: 1.0
            val chartAvailableWidth = pageWidth - 280f // leave ample space for names on left and amounts on right

            categoryTotals.toList().sortedByDescending { it.second }.forEach { (cat, amt) ->
                val percentage = if (totalSpent > 0) (amt / totalSpent) * 100 else 0.0
                
                // Draw category name label
                paint.color = Color.rgb(40, 44, 52)
                paint.textSize = 9f
                paint.isFakeBoldText = true
                canvas.drawText(cat, 55f, barY + 8f, paint)

                // Draw the grey background track for the bar
                paint.color = Color.rgb(230, 235, 240)
                canvas.drawRect(140f, barY, 140f + chartAvailableWidth, barY + 10f, paint)

                // Draw the actual category colored progress bar
                paint.color = getCategoryNativeColor(cat)
                val barWidth = ((amt / maxCatValue) * chartAvailableWidth).toFloat()
                canvas.drawRect(140f, barY, 140f + barWidth, barY + 10f, paint)

                // Draw value & percentage text on the right of the bar
                paint.color = Color.rgb(60, 64, 72)
                paint.isFakeBoldText = false
                paint.textSize = 9f
                canvas.drawText("$currencySymbol${String.format("%.2f", amt)} (${String.format("%.1f", percentage)}%)", 148f + chartAvailableWidth, barY + 8f, paint)

                barY += 22f
            }

            yPos += chartCardHeight + 25f
        }

        // Transactions Header
        paint.color = Color.BLACK
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("TRANSACTION DETAIL", 40f, yPos, paint)
        yPos += 15f

        paint.color = Color.rgb(33, 150, 243)
        canvas.drawRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + 22f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Date", 50f, yPos + 15f, paint)
        canvas.drawText("Category", 140f, yPos + 15f, paint)
        canvas.drawText("Description", 260f, yPos + 15f, paint)
        canvas.drawText("Amount", 480f, yPos + 15f, paint)

        yPos += 22f
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        expenses.forEachIndexed { index, expense ->
            // Check page height limit to paginate dynamically
            if (yPos > 770f) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                yPos = 50f

                // Sub-page transaction header
                paint.color = Color.rgb(33, 150, 243)
                canvas.drawRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + 22f, paint)

                paint.color = Color.WHITE
                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("Date", 50f, yPos + 15f, paint)
                canvas.drawText("Category", 140f, yPos + 15f, paint)
                canvas.drawText("Description", 260f, yPos + 15f, paint)
                canvas.drawText("Amount", 480f, yPos + 15f, paint)
                yPos += 22f
            }

            // Alternating backgrounds for data table rows
            if (index % 2 == 1) {
                paint.color = Color.rgb(245, 247, 250)
                canvas.drawRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + 20f, paint)
            }

            paint.color = Color.BLACK
            paint.isFakeBoldText = false
            paint.textSize = 9f

            val dateStr = sdf.format(Date(expense.timestamp))
            canvas.drawText(dateStr, 50f, yPos + 14f, paint)
            canvas.drawText(expense.category, 140f, yPos + 14f, paint)
            
            // Truncate long descriptions to fit table column
            var desc = expense.description
            if (desc.length > 32) {
                desc = desc.take(29) + "..."
            }
            canvas.drawText(desc, 260f, yPos + 14f, paint)
            
            canvas.drawText("$currencySymbol${String.format("%.2f", expense.amount)}", 480f, yPos + 14f, paint)

            yPos += 20f
        }

        // Draw footer on the last page
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, 790f, (pageWidth - 40).toFloat(), 790f, paint)
        paint.color = Color.GRAY
        paint.textSize = 8f
        canvas.drawText("Expense Tracker - Offline Local Finance. All data is securely encrypted.", 40f, 805f, paint)
        canvas.drawText("Page $pageNumber", 500f, 805f, paint)

        pdfDocument.finishPage(currentPage)

        // Save PDF to cache directory so we can share it easily
        return try {
            val directory = File(context.cacheDirs, "reports")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "Expense_Report_${monthKey}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    // Helper property to work with cache dirs properly
    private val Context.cacheDirs: File
        get() = this.cacheDir

    private fun getCategoryNativeColor(category: String): Int {
        return when (category) {
            "Food" -> Color.rgb(229, 115, 115)        // Red-Orange
            "Transport" -> Color.rgb(100, 181, 246)   // Blue
            "Entertainment" -> Color.rgb(186, 104, 200) // Purple
            "Shopping" -> Color.rgb(255, 183, 77)    // Orange
            "Bills" -> Color.rgb(77, 182, 172)       // Teal
            "Health" -> Color.rgb(255, 138, 101)      // Peach-Orange
            "Education" -> Color.rgb(144, 164, 174)   // Blue-Grey
            else -> Color.rgb(161, 136, 127)          // Brown
        }
    }

    fun renderPdfToBitmaps(context: Context, file: File): List<android.graphics.Bitmap> {
        val bitmaps = mutableListOf<android.graphics.Bitmap>()
        try {
            val fileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = (page.width * 1.5f).toInt()
                val height = (page.height * 1.5f).toInt()
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmaps
    }
}
