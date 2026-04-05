package com.afp.avaliacao.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {

    fun generateRelatorioAtleta(
        nomeAtleta: String,
        periodo: String,
        metrics: Map<String, Any>,
        sessoes: List<Map<String, Any>>
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
        }

        // Page 1
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var yPos = 50f

        // Header
        canvas.drawText("Relatório de Performance - AFP", 50f, yPos, titlePaint)
        yPos += 30f
        canvas.drawText("Atleta: $nomeAtleta", 50f, yPos, bodyPaint)
        yPos += 20f
        canvas.drawText("Período: $periodo", 50f, yPos, bodyPaint)
        yPos += 40f

        // Summary Cards Section
        canvas.drawText("Resumo do Período", 50f, yPos, headerPaint)
        yPos += 25f
        canvas.drawText("Carga Total: ${metrics["cargaTotal"]}", 60f, yPos, bodyPaint)
        yPos += 20f
        canvas.drawText("PSE Médio: ${metrics["pseMedio"]}", 60f, yPos, bodyPaint)
        yPos += 20f
        canvas.drawText("Duração Média: ${metrics["duracaoMedia"]} min", 60f, yPos, bodyPaint)
        yPos += 40f

        // Table Header
        canvas.drawText("Detalhamento das Sessões", 50f, yPos, headerPaint)
        yPos += 25f
        canvas.drawText("Data", 50f, yPos, headerPaint)
        canvas.drawText("Modalidade", 150f, yPos, headerPaint)
        canvas.drawText("Carga", 300f, yPos, headerPaint)
        canvas.drawText("PSE", 400f, yPos, headerPaint)
        canvas.drawText("Duração", 480f, yPos, headerPaint)
        yPos += 10f
        canvas.drawLine(50f, yPos, 550f, yPos, paint)
        yPos += 20f

        // Table Rows
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sessoes.take(25).forEach { sessao -> // Limit to 25 to fit one page for now
            val data = (sessao["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
            val modalidade = (sessao["atividades"] as? List<*>)?.firstOrNull()?.toString() ?: "N/A"
            val carga = sessao["carga"]?.toString() ?: "0"
            val pse = sessao["pseFoster"]?.toString() ?: "0"
            val duracao = sessao["duracaoMin"]?.toString() ?: "0"

            canvas.drawText(sdf.format(data), 50f, yPos, bodyPaint)
            canvas.drawText(modalidade, 150f, yPos, bodyPaint)
            canvas.drawText(carga, 300f, yPos, bodyPaint)
            canvas.drawText(pse, 400f, yPos, bodyPaint)
            canvas.drawText(duracao + "m", 480f, yPos, bodyPaint)
            yPos += 20f

            if (yPos > 800) return@forEach // Basic overflow check
        }

        pdfDocument.finishPage(page)

        // Save file
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Relatorio_${nomeAtleta.replace(" ", "_")}_$timeStamp.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF salvo em: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        pdfDocument.close()
    }
}
