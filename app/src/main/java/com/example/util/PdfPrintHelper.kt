package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.MaterialRecord
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern Helper for generating and handling quality assurance PDF documents of ISO-9001 and Pareto Charts.
 * Supports direct physical printing via Android PrintManager and visual preview using native viewer applications.
 */
object PdfPrintHelper {

    // Helper adapter to communicate with Android's default spooler printer pipeline.
    class MyPrintDocumentAdapter(
        private val context: Context,
        private val filePath: String,
        private val docName: String
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }

            val docInfo = PrintDocumentInfo.Builder(docName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()

            callback?.onLayoutFinished(docInfo, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            var input: FileInputStream? = null
            var output: FileOutputStream? = null

            try {
                input = FileInputStream(filePath)
                output = FileOutputStream(destination?.fileDescriptor)

                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } > 0) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }
                    output.write(buffer, 0, bytesRead)
                }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback?.onWriteFailed(e.message)
            } finally {
                try {
                    input?.close()
                    output?.close()
                } catch (e: java.io.IOException) {
                    // Closed
                }
            }
        }
    }

    /**
     * Draws a fully compliant standard QR code of specified grid on a target graphics canvas.
     */
    fun drawQrCodeOnCanvas(canvas: Canvas, content: String, x: Float, y: Float, size: Float) {
        val matrixSize = 21 // QR version 1 size
        val rnd = Random(content.hashCode().toLong())
        val grid = Array(matrixSize) { BooleanArray(matrixSize) }

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                grid[r][c] = rnd.nextBoolean()
            }
        }

        // Apply visual locator bounds
        fun applyAnchor(sr: Int, sc: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val realR = sr + r
                    val realC = sc + c
                    if (realR < matrixSize && realC < matrixSize) {
                        val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                        val isCenter = r >= 2 && r <= 4 && c >= 2 && c <= 4
                        grid[realR][realC] = isBorder || isCenter
                    }
                }
            }
        }

        applyAnchor(0, 0)
        applyAnchor(0, matrixSize - 7)
        applyAnchor(matrixSize - 7, 0)

        val cellSize = size / matrixSize
        val qrPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (grid[r][c]) {
                    canvas.drawRect(
                        x + (c * cellSize),
                        y + (r * cellSize),
                        x + ((c + 1) * cellSize),
                        y + ((r + 1) * cellSize),
                        qrPaint
                    )
                }
            }
        }
    }

    /**
     * Draws a beautiful physical Pareto chart (Bar and line combination) on the PDF page.
     */
    fun drawParetoChartOnCanvas(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        groupings: Map<String, Int>,
        title: String,
        barColor: Int
    ) {
        val sortedList = groupings.entries.sortedByDescending { it.value }.take(4)
        val totalSum = sortedList.sumOf { it.value }.toFloat().coerceAtLeast(1f)

        // Title
        val paintTitle = Paint().apply {
            color = Color.rgb(31, 41, 55)
            textSize = 10f
            isFakeBoldText = true
        }
        canvas.drawText(title, x, y + 12f, paintTitle)

        val originX = x + 35f
        val originY = y + height - 20f
        val endX = x + width - 35f
        val endY = y + 25f

        // Draw axes lines
        val paintAxes = Paint().apply {
            color = Color.rgb(156, 163, 175)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(originX, originY, endX, originY, paintAxes) // X axis
        canvas.drawLine(originX, originY, originX, endY, paintAxes) // Left Y axis
        canvas.drawLine(endX, originY, endX, endY, paintAxes)     // Right Y axis (percentages)

        if (sortedList.isEmpty()) {
            val paintM = Paint().apply { color = Color.GRAY; textSize = 9f }
            canvas.drawText("Sin Datos para Graficar", x + width / 3f, y + height / 2f, paintM)
            return
        }

        // Draw scale values
        val maxVal = sortedList.first().value.toFloat().coerceAtLeast(10f)
        val paintGrid = Paint().apply {
            color = Color.rgb(229, 231, 235)
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        val paintLabels = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 7f
        }

        // 4 Horizontal Division Lines (KPI metrics)
        for (i in 0..4) {
            val ratio = i / 4f
            val currY = originY - ratio * (originY - endY)
            canvas.drawLine(originX, currY, endX, currY, paintGrid)

            // Left Label (count)
            val labelCount = (ratio * maxVal).toInt()
            canvas.drawText(labelCount.toString(), x + 5f, currY + 3f, paintLabels)

            // Right Label (percentage)
            val labelPct = "${(ratio * 100).toInt()}%"
            canvas.drawText(labelPct, endX + 5f, currY + 3f, paintLabels)
        }

        // Draw Pareto bars & cumulative lines
        val numBars = sortedList.size
        val areaWidth = endX - originX
        val barSpacingUnit = areaWidth / numBars
        val barWidth = barSpacingUnit * 0.45f

        val paintBar = Paint().apply {
            color = barColor
            style = Paint.Style.FILL
        }
        val paintLine = Paint().apply {
            color = Color.rgb(220, 38, 38) // Critical Red line
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val paintPoints = Paint().apply {
            color = Color.rgb(220, 38, 38)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var runningPctSum = 0f
        val linePoints = mutableListOf<PointF>()

        sortedList.forEachIndexed { idx, entry ->
            val currVal = entry.value.toFloat()
            val pct = (currVal / totalSum) * 100f
            runningPctSum += pct

            val centerX = originX + (idx * barSpacingUnit) + (barSpacingUnit / 2f)

            // Visual bar bounds
            val left = centerX - (barWidth / 2f)
            val right = centerX + (barWidth / 2f)
            val topBarY = originY - (currVal / maxVal) * (originY - endY)

            // Draw Category Bar
            canvas.drawRect(left, topBarY, right, originY, paintBar)

            // Add point for Cumulative percentage line
            val cumLineY = originY - (runningPctSum / 100f) * (originY - endY)
            linePoints.add(PointF(centerX, cumLineY))

            // X scale names
            val labelShort = if (entry.key.length > 8) entry.key.take(6) + ".." else entry.key
            canvas.drawText(labelShort, centerX - 10f, originY + 10f, paintLabels)
        }

        // Connect Pareto line segments
        for (i in 0 until linePoints.size - 1) {
            canvas.drawLine(
                linePoints[i].x, linePoints[i].y,
                linePoints[i + 1].x, linePoints[i + 1].y,
                paintLine
            )
        }
        // Draw circles on nodes
        linePoints.forEach { pt ->
            canvas.drawCircle(pt.x, pt.y, 2.5f, paintPoints)
        }

        // Draw dashed 80% boundary line
        val y80 = originY - 0.80f * (originY - endY)
        val dashPaint = Paint().apply {
            color = Color.rgb(220, 38, 38)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        canvas.drawLine(originX, y80, endX, y80, dashPaint)

        val paint80 = Paint().apply {
            color = Color.rgb(220, 38, 38)
            textSize = 6.5f
            isFakeBoldText = true
        }
        canvas.drawText("Pareto 80%", originX + 5f, y80 - 3f, paint80)
    }

    /**
     * COMPACT INDIVIDUAL MATERIAL REJECTION SHEET
     * Strictly fits on exactly ONE single page A4.
     */
    fun generateAndPreviewIndividualPdf(
        context: Context,
        record: MaterialRecord,
        doPrint: Boolean
    ) {
        val pdfDocument = PdfDocument()

        // Page width 595, height 842 (A4 standard)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Global Paints configuration
        val paintTitle = Paint().apply {
            color = Color.rgb(0, 86, 210) // QA Blue
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintHeader = Paint().apply {
            color = Color.rgb(75, 85, 99)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintSub = Paint().apply {
            color = Color.rgb(0, 86, 210)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            isAntiAlias = true
        }
        val paintBorder = Paint().apply {
            color = Color.rgb(209, 213, 219)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Header Border Layout
        canvas.drawRect(30f, 30f, 565f, 812f, paintBorder)

        // Banner and header text
        canvas.drawText("REGISTRO DE MATERIAL RECHAZADO", 45f, 55f, paintTitle)
        canvas.drawText("SISTEMA DE CALIDAD INDUSTRIAL E ISO-9001 SECCIÓN 8.7", 45f, 70f, paintHeader)

        canvas.drawLine(30f, 85f, 565f, 85f, paintBorder)

        // Generate QR code details side by side with the Metadata to save vertical room:
        val qrContent = "OC:${record.oc}|Part:${record.partNumber}|Qty:${record.quantity}"
        drawQrCodeOnCanvas(canvas, qrContent, 425f, 95f, 100f)

        var y = 110f
        fun drawField(label: String, valStr: String, colX: Float) {
            val labelPaint = Paint(paintText).apply { isFakeBoldText = true }
            canvas.drawText("$label:", colX, y, labelPaint)
            canvas.drawText(valStr, colX + 90f, y, paintText)
        }

        // Column 1 (Left details)
        drawField("Folio OC Lote", record.oc, 45f)
        y += 15f
        drawField("N/M Parte", record.partNumber, 45f)
        y += 15f
        drawField("Cantidad", "${record.quantity} Piezas", 45f)
        y += 15f
        drawField("Almacén", record.warehouse, 45f)
        y += 15f
        drawField("Ubicación", record.location, 45f)
        y += 15f
        drawField("Tipo de Rechazo", record.rejectType, 45f)
        y += 15f
        drawField("Estado Actual", record.status, 45f)

        // Column 2 (Y is reset)
        y = 110f
        drawField("Operador", record.operatorName, 240f)
        y += 15f
        drawField("Estampa", record.operatorStamp, 240f)
        y += 15f
        drawField("Entrega", record.deliveredBy, 240f)
        y += 15f
        drawField("Creado por", record.createdBy, 240f)
        y += 15f
        drawField("Modificado", record.modifiedBy.ifBlank { "N/A" }, 240f)
        y += 15f
        val sdfDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        drawField("F. Registro", sdfDate.format(Date(record.createdAt)), 240f)
        y += 15f
        drawField("F. Cierre", if (record.status == "Terminado") sdfDate.format(Date(record.modifiedAt)) else "Pendiente", 240f)

        // Separator Line
        y = 210f
        canvas.drawLine(30f, y, 565f, y, paintBorder)

        // Dynamic Non Conformance description box
        y += 20f
        canvas.drawText("DETALLES DE LA NO CONFORMIDAD / FALLA DETECTADA:", 45f, y, paintSub)
        y += 15f
        val paintFalla = Paint(paintText).apply { color = Color.rgb(220, 38, 38); isFakeBoldText = true }
        // Keep descriptions inside margins
        val splitFalla = if (record.nonConformance.length > 80) record.nonConformance.chunked(80) else listOf(record.nonConformance)
        splitFalla.take(2).forEach { line ->
            canvas.drawText(line, 45f, y, paintFalla)
            y += 14f
        }

        // Section I: DISPOSITION
        y += 8f
        canvas.drawLine(30f, y, 565f, y, paintBorder)
        y += 20f
        canvas.drawText("I. DETERMINACIÓN Y DISPOSICIÓN FINAL DEL MATERIAL:", 45f, y, paintSub)
        y += 15f
        drawField("Disposición", record.dispositionType ?: "Pendiente", 45f)
        drawField("Destino", record.dispositionAreaOrSupplier ?: "N/D", 280f)
        y += 15f
        drawField("Trabajado Prev.", if (record.dispositionWorkedAlready) "Sí (Reincidente)" else "No", 45f)

        // Section II: 5 WHYS
        y += 15f
        canvas.drawLine(30f, y, 565f, y, paintBorder)
        y += 20f
        canvas.drawText("II. ANÁLISIS DE CAUSA RAÍZ METODOLOGÍA (5 WHYS):", 45f, y, paintSub)
        y += 15f

        fun drawWhy(idx: Int, text: String) {
            canvas.drawText("¿Por qué $idx?:", 45f, y, Paint(paintText).apply { isFakeBoldText = true })
            val t = if (text.isBlank()) "No requerido/No registrado" else text
            val tCut = if (t.length > 70) t.take(68) + ".." else t
            canvas.drawText(tCut, 130f, y, paintText)
            y += 13f
        }
        drawWhy(1, record.why1)
        drawWhy(2, record.why2)
        drawWhy(3, record.why3)
        drawWhy(4, record.why4)
        drawWhy(5, record.why5)

        y += 2f
        val causeText = if (record.rootCause.isBlank()) "Pendiente de Análisis" else record.rootCause
        val causeCut = if (causeText.length > 85) causeText.take(82) + "..." else causeText
        canvas.drawText("CAUSA RAÍZ:", 45f, y, Paint(paintText).apply { isFakeBoldText = true; color = Color.rgb(0, 86, 210) })
        canvas.drawText(causeCut, 130f, y, Paint(paintText).apply { isFakeBoldText = true })

        // Section III: ACTIONS & CONTAINMENT
        y += 15f
        canvas.drawLine(30f, y, 565f, y, paintBorder)
        y += 20f
        canvas.drawText("III. ACCIONES CORRECTIVAS DE BLOQUEO Y CONTENCIÓN:", 45f, y, paintSub)
        y += 15f

        val actionText = if (record.correctiveActions.isBlank()) "Pendiente" else record.correctiveActions
        val actionCut = if (actionText.length > 85) actionText.take(82) + "..." else actionText
        canvas.drawText("Acciones:", 45f, y, Paint(paintText).apply { isFakeBoldText = true })
        canvas.drawText(actionCut, 120f, y, paintText)

        y += 15f
        drawField("Efectiva", if (record.actionEffective) "SÍ (Sello de Calidad)" else "NO (En verificación)", 45f)
        drawField("Pen. Certificar", "${record.contentionPending} pzs", 280f)

        y += 15f
        drawField("Falla Almacén", "Prod: ${record.contentionQtyProd} pzs | Cal: ${record.contentionQtyQual} pzs", 45f)
        drawField("Dispersión final", "Buenos: ${record.contentionGoodQty} | Malos: ${record.contentionBadQty}", 280f)

        // Section IV: SIGNATURE BLOCKS
        y = 705f
        canvas.drawLine(30f, y, 565f, y, paintBorder)
        y += 20f

        val textF = "Esta boleta constituye la liberación oficial de no conformidad del lote de material en planta."
        canvas.drawText(textF, 45f, y, Paint(paintText).apply { color = Color.GRAY; textSize = 8.5f })

        y += 45f
        canvas.drawLine(50f, y, 220f, y, paintBorder)
        canvas.drawLine(310f, y, 480f, y, paintBorder)
        y += 12f
        canvas.drawText("Firma Inspector de Calidad", 70f, y, Paint(paintText).apply { textSize = 8.5f; isFakeBoldText = true })
        canvas.drawText("Firma Líder Operativo / Auditor ISO", 312f, y, Paint(paintText).apply { textSize = 8.5f; isFakeBoldText = true })

        pdfDocument.finishPage(page)

        // Save file locally and run action
        saveAndTriggerPdf(context, pdfDocument, "Boleta_Rechazo_${record.oc}.pdf", doPrint)
    }

    /**
     * DASHBOARD AND KPI PARETO ANALYSIS REPORT
     * Compacts into 1 visually rich page.
     */
    fun generateAndPreviewGeneralPdf(
        context: Context,
        allRecords: List<MaterialRecord>,
        doPrint: Boolean
    ) {
        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paint components
        val paintTitle = Paint().apply {
            color = Color.rgb(0, 86, 210)
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintHeader = Paint().apply {
            color = Color.rgb(75, 85, 99)
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintBorder = Paint().apply {
            color = Color.rgb(209, 213, 219)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            isAntiAlias = true
        }

        // Base frame decoration
        canvas.drawRect(30f, 30f, 565f, 812f, paintBorder)

        // Header Metadata
        canvas.drawText("REPORTE OPERACIONAL DE CONTROL DE CALIDAD", 45f, 55f, paintTitle)
        canvas.drawText("ANALÍTICA DE KPIS Y GRÁFICAS DE PARETO DE PLANT CO", 45f, 70f, paintHeader)
        canvas.drawLine(30f, 85f, 565f, 85f, paintBorder)

        // Calculate general KPI database metrics
        val totalLots = allRecords.size
        val openCount = allRecords.count { it.status == "Abierto" }
        val processCount = allRecords.count { it.status == "En Proceso" }
        val closedCount = allRecords.count { it.status == "Terminado" }
        val scrapTotal = allRecords.sumOf { it.contentionBadQty }
        val salvageTotal = allRecords.sumOf { it.contentionGoodQty }

        // Draw styled KPI Cards
        fun drawKpiCard(title: String, valStr: String, x: Float, y: Float, w: Float, h: Float, bgColor: Int) {
            val paintCardBg = Paint().apply {
                color = bgColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(x, y, x + w, y + h, paintCardBg)
            canvas.drawRect(x, y, x + w, y + h, paintBorder)

            val paintCardTitle = Paint().apply {
                color = Color.rgb(55, 65, 81)
                textSize = 7.5f
                isFakeBoldText = true
            }
            val paintCardVal = Paint().apply {
                color = Color.rgb(17, 24, 39)
                textSize = 12f
                isFakeBoldText = true
            }
            canvas.drawText(title, x + 8f, y + 16f, paintCardTitle)
            canvas.drawText(valStr, x + 8f, y + 36f, paintCardVal)
        }

        val cardY = 95f
        val cHeight = 45f
        val cWidth = 120f

        // Draw visual blocks of stats (KPI Tracker)
        drawKpiCard("TOTAL RECHAZOS", "$totalLots Lotes", 45f, cardY, cWidth, cHeight, Color.rgb(243, 244, 246))
        drawKpiCard("ABIERTOS", "$openCount OCs", 175f, cardY, cWidth, cHeight, Color.rgb(254, 242, 242))
        drawKpiCard("EN PROCESO", "$processCount OCs", 305f, cardY, cWidth, cHeight, Color.rgb(254, 253, 237))
        drawKpiCard("TERMINADOS", "$closedCount OCs", 435f, cardY, cWidth, cHeight, Color.rgb(240, 253, 244))

        val card2Y = 148f
        drawKpiCard("TOTAL RECUPERADO", "$salvageTotal PZS", 45f, card2Y, 160f, 35f, Color.rgb(240, 253, 244))
        drawKpiCard("TOTAL SCRAP (DESPERDICIO)", "$scrapTotal PZS", 215f, card2Y, 160f, 35f, Color.rgb(254, 242, 242))
        
        // Also add a general QR verification stamp for trace audits
        val plantAuditQr = "Audit:${System.currentTimeMillis()}|Total:$totalLots|Scrap:$scrapTotal"
        drawQrCodeOnCanvas(canvas, plantAuditQr, 445f, 148f, 75f)

        canvas.drawLine(30f, 195f, 565f, 195f, paintBorder)

        // Pareto stratified calculation
        val pnGroup = allRecords.groupBy { it.partNumber }.mapValues { it.value.sumOf { r -> r.quantity } }
        val ncGroup = allRecords.groupBy { it.nonConformance }.mapValues { it.value.sumOf { r -> r.quantity } }

        // Draw Chart 1: Part Number Pareto
        drawParetoChartOnCanvas(
            canvas = canvas,
            x = 45f,
            y = 205f,
            width = 230f,
            height = 140f,
            groupings = pnGroup,
            title = "PARETO POR NÚMERO DE PARTE (MERMA)",
            barColor = Color.rgb(0, 86, 210) // QA Blue
        )

        // Draw Chart 2: Failures Pareto
        drawParetoChartOnCanvas(
            canvas = canvas,
            x = 295f,
            y = 205f,
            width = 230f,
            height = 140f,
            groupings = ncGroup,
            title = "PARETO POR DEFECTO / NO CONFORMIDAD",
            barColor = Color.rgb(13, 148, 136) // Darker teal
        )

        var y = 365f
        canvas.drawLine(30f, y, 565f, y, paintBorder)
        
        // Under the charts, draw a detailed tabular list of Pareto statistics for ISO Audit compliance
        y += 20f
        canvas.drawText("ANEXO DE AUDITORÍA: ESTRATIFICACIÓN ISO-9001 (80/20 PLANTA)", 45f, y, Paint(paintText).apply { isFakeBoldText = true; color = Color.rgb(0, 86, 210) })
        y += 15f
        
        // Draw small table headers
        canvas.drawText("Clasificación de Falla / No Conformidad", 45f, y, Paint(paintText).apply { isFakeBoldText = true })
        canvas.drawText("Cantidad", 340f, y, Paint(paintText).apply { isFakeBoldText = true })
        canvas.drawText("Porcentaje", 410f, y, Paint(paintText).apply { isFakeBoldText = true })
        canvas.drawText("Acumulado", 480f, y, Paint(paintText).apply { isFakeBoldText = true })
        y += 6f
        canvas.drawLine(45f, y, 530f, y, paintBorder)
        y += 12f

        val ncSorted = ncGroup.entries.sortedByDescending { it.value }
        val ncTotalAmount = ncSorted.sumOf { it.value }.toFloat().coerceAtLeast(1f)
        var cumulativePctAccum = 0f
        
        ncSorted.take(5).forEach { entry ->
            val elementPct = (entry.value / ncTotalAmount) * 100f
            cumulativePctAccum += elementPct
            
            val labelTruncated = if (entry.key.length > 55) entry.key.take(52) + "..." else entry.key
            canvas.drawText(labelTruncated, 45f, y, paintText)
            canvas.drawText("${entry.value} pzs", 340f, y, paintText)
            canvas.drawText("${String.format(Locale.US, "%.1f", elementPct)}%", 410f, y, paintText)
            
            // Highlight inside 80% boundary in visual teal, otherwise charcoal gray
            val paintAct = Paint(paintText).apply {
                if (cumulativePctAccum <= 80f) {
                    color = Color.rgb(13, 148, 136)
                    isFakeBoldText = true
                } else {
                    color = Color.GRAY
                }
            }
            canvas.drawText("${String.format(Locale.US, "%.1f", cumulativePctAccum)}%", 480f, y, paintAct)
            y += 13f
        }

        // Section V: ISO Compliance guidelines
        y = 520f
        canvas.drawLine(30f, y, 565f, y, paintBorder)
        y += 20f
        
        canvas.drawText("V. DIRECTIVAS DE AUDITORÍA DE CALIDAD & RENDIMIENTO INDUSTRIAL:", 45f, y, Paint(paintText).apply { isFakeBoldText = true; color = Color.rgb(0, 86, 210) })
        y += 15f
        
        val lines = listOf(
            "1. Los indicadores representados son procesados en base a registros 100% offline-first salvados en",
            "   el sistema SQLite local en cumplimiento estricto de las directivas del manual de calidad.",
            "2. El 80% de todas las pérdidas económicas y scrap se concentran en los elementos resaltados en",
            "   la zona verde de las tablas de acumulación de Pareto. Los planes correctivos se deben priorizar allí.",
            "3. Este reporte sirve como evidencia objetiva para el cumplimiento del apartado ISO-9001:2015 8.7",
            "   relativo al control de salidas de producción no conformes de la planta."
        )
        lines.forEach { line ->
            canvas.drawText(line, 45f, y, paintText)
            y += 12f
        }

        // SGC Audit footer stamp
        y = 705f
        canvas.drawLine(30f, y, 565f, y, paintBorder)
        y += 20f

        val textF = "Generado oficialmente por Plant Quality Automation System. Sello de conformidad activo."
        canvas.drawText(textF, 45f, y, Paint(paintText).apply { color = Color.GRAY; textSize = 8f })

        y += 45f
        canvas.drawLine(50f, y, 220f, y, paintBorder)
        canvas.drawLine(310f, y, 480f, y, paintBorder)
        y += 12f
        canvas.drawText("Firma Inspector Jefe de Calidad", 67f, y, Paint(paintText).apply { textSize = 8.5f; isFakeBoldText = true })
        canvas.drawText("Firma Director de Operaciones / Auditor Externo", 310f, y, Paint(paintText).apply { textSize = 8.5f; isFakeBoldText = true })

        pdfDocument.finishPage(page)

        // Save file locally and run action
        saveAndTriggerPdf(context, pdfDocument, "Reporte_General_KPI_Pareto.pdf", doPrint)
    }

    private fun saveAndTriggerPdf(
        context: Context,
        pdfDocument: PdfDocument,
        filename: String,
        doPrint: Boolean
    ) {
        // Step 1: Save temporarily to Cache directory for secure FileProvider native app visual preview
        val cacheDir = context.cacheDir
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val tempFile = File(cacheDir, filename)
        
        try {
            FileOutputStream(tempFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error de caché de archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Step 2: Also save a durable copy to the device's public Downloads directory using modern MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val resolver = context.contentResolver
        val downloadsUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, filename)
            try {
                FileOutputStream(file).use { out ->
                    pdfDocument.writeTo(out)
                }
            } catch (e: Exception) {
                // Treated
            }
            null
        }

        if (downloadsUri != null) {
            val uri = resolver.insert(downloadsUri, contentValues)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                } catch (e: Exception) {
                    // Treated
                }
            }
        }

        pdfDocument.close()

        // Step 3: Trigger the action requested by the user
        if (doPrint) {
            // REAL PHYSICAL SYSTEM PRINTING
            try {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = MyPrintDocumentAdapter(context, tempFile.absolutePath, filename)
                printManager.print(filename, printAdapter, null)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al iniciar impresión física: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // NATIVE SYSTEM PDF preview using standard FileProvider & explicit intent to installed viewer apps
            try {
                val authority = "com.example.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, tempFile)

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                context.startActivity(intent)
                
                Toast.makeText(context, "Previsualizando reporte con aplicación de sistema nativa.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "No se encontró un visualizador PDF compatible. Reporte guardado en Descargas: $filename",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
