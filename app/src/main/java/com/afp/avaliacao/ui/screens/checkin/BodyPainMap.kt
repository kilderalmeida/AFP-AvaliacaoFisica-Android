package com.afp.avaliacao.ui.screens.checkin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afp.avaliacao.R

data class BodyRegion(
    val id: String,
    val name: String,
    val x: Float, // Coordenada X normalizada (0.0 a 1.0) relativa à imagem
    val y: Float, // Coordenada Y normalizada (0.0 a 1.0) relativa à imagem
    val width: Float = 0.15f, // Largura normalizada
    val height: Float = 0.06f, // Altura normalizada
    val isCircle: Boolean = false,
    val radius: Float = 0.06f // Raio normalizado (relativo à largura da imagem)
)

@Composable
fun BodyPainMap(
    selectedRegions: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val frontRegions = remember {
        listOf(
            BodyRegion("L", "Ombro Esquerdo", 0.72f, 0.22f, isCircle = true),
            BodyRegion("M", "Ombro Direito", 0.28f, 0.22f, isCircle = true),
            BodyRegion("N", "Antebraço Esquerdo", 0.82f, 0.38f),
            BodyRegion("O", "Antebraço Direito", 0.18f, 0.38f),
            BodyRegion("A", "Abdômen", 0.5f, 0.42f, width = 0.25f, height = 0.1f),
            BodyRegion("1", "Coxa Direita", 0.38f, 0.55f, width = 0.18f, height = 0.1f),
            BodyRegion("2", "Coxa Esquerda", 0.62f, 0.55f, width = 0.18f, height = 0.1f),
            BodyRegion("3", "Adutor Direito", 0.46f, 0.58f, width = 0.08f, height = 0.08f),
            BodyRegion("4", "Adutor Esquerdo", 0.54f, 0.58f, width = 0.08f, height = 0.08f),
            BodyRegion("B", "Joelho Direito", 0.38f, 0.68f, isCircle = true, radius = 0.07f),
            BodyRegion("C", "Joelho Esquerdo", 0.62f, 0.68f, isCircle = true, radius = 0.07f),
            BodyRegion("5", "Canela Direita", 0.38f, 0.83f, width = 0.14f, height = 0.12f),
            BodyRegion("6", "Canela Esquerda", 0.62f, 0.83f, width = 0.14f, height = 0.12f),
            BodyRegion("D", "Tornozelo Direito", 0.38f, 0.95f, isCircle = true, radius = 0.05f),
            BodyRegion("E", "Tornozelo Esquerdo", 0.62f, 0.95f, isCircle = true, radius = 0.05f)
        )
    }

    val backRegions = remember {
        listOf(
            BodyRegion("P", "Cervical", 0.5f, 0.12f, isCircle = true, radius = 0.1f),
            BodyRegion("17", "Dorsal Sup. Esquerdo", 0.32f, 0.28f, width = 0.22f),
            BodyRegion("18", "Dorsal Sup. Direito", 0.68f, 0.28f, width = 0.22f),
            BodyRegion("21", "Tríceps Esquerdo", 0.18f, 0.36f),
            BodyRegion("22", "Tríceps Direito", 0.82f, 0.36f),
            BodyRegion("Q", "Cotovelo Esquerdo", 0.12f, 0.42f, isCircle = true, radius = 0.06f),
            BodyRegion("R", "Cotovelo Direito", 0.88f, 0.42f, isCircle = true, radius = 0.06f),
            BodyRegion("F", "Lombar", 0.5f, 0.44f, width = 0.35f, height = 0.08f),
            BodyRegion("12", "Glúteo Direito", 0.68f, 0.56f, width = 0.25f, height = 0.1f),
            BodyRegion("8", "Post. Coxa Esquerda", 0.38f, 0.69f, width = 0.18f, height = 0.12f),
            BodyRegion("9", "Post. Coxa Direita", 0.62f, 0.69f, width = 0.18f, height = 0.12f),
            BodyRegion("G", "Joelho Post. Esquerdo", 0.38f, 0.78f, isCircle = true, radius = 0.06f),
            BodyRegion("H", "Joelho Post. Direito", 0.62f, 0.78f, isCircle = true, radius = 0.06f),
            BodyRegion("10", "Panturrilha Esquerda", 0.38f, 0.88f, width = 0.16f, height = 0.12f),
            BodyRegion("11", "Panturrilha Direita", 0.62f, 0.88f, width = 0.16f, height = 0.12f),
            BodyRegion("I", "Tornozelo Esquerdo", 0.38f, 0.96f, isCircle = true, radius = 0.05f),
            BodyRegion("J", "Tornozelo Direito", 0.62f, 0.96f, isCircle = true, radius = 0.05f)
        )
    }

    val allRegions = remember { frontRegions + backRegions }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .height(420.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            InteractiveBodyPart(
                imageRes = R.drawable.body_front,
                regions = frontRegions,
                selectedRegions = selectedRegions,
                onToggle = onToggle,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.3f)))
            InteractiveBodyPart(
                imageRes = R.drawable.body_back,
                regions = backRegions,
                selectedRegions = selectedRegions,
                onToggle = onToggle,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Selecione as Regiões",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(allRegions) { region ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(region.id) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = selectedRegions.contains(region.id),
                        onCheckedChange = { onToggle(region.id) }
                    )
                    Text(
                        text = "${region.id}: ${region.name}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (selectedRegions.contains(region.id)) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveBodyPart(
    imageRes: Int,
    regions: List<BodyRegion>,
    selectedRegions: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val painter = painterResource(id = imageRes)
        val imageIntrinsicSize = painter.intrinsicSize
        
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()
        
        // Calcula a escala e o offset da imagem dentro do Box (ContentScale.Fit)
        val scale = minOf(boxWidth / imageIntrinsicSize.width, boxHeight / imageIntrinsicSize.height)
        val imageWidth = imageIntrinsicSize.width * scale
        val imageHeight = imageIntrinsicSize.height * scale
        val imageLeft = (boxWidth - imageWidth) / 2
        val imageTop = (boxHeight - imageHeight) / 2

        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Converte o offset do toque para coordenadas normalizadas da imagem (0.0 a 1.0)
                        val relativeX = (offset.x - imageLeft) / imageWidth
                        val relativeY = (offset.y - imageTop) / imageHeight
                        
                        if (relativeX in 0f..1f && relativeY in 0f..1f) {
                            regions.forEach { region ->
                                val hit = if (region.isCircle) {
                                    val dx = (relativeX - region.x)
                                    val dy = (relativeY - region.y) * (imageHeight / imageWidth)
                                    (dx * dx + dy * dy) <= (region.radius * region.radius)
                                } else {
                                    relativeX in (region.x - region.width / 2)..(region.x + region.width / 2) &&
                                    relativeY in (region.y - region.height / 2)..(region.y + region.height / 2)
                                }
                                if (hit) {
                                    onToggle(region.id)
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
                }
        ) {
            regions.forEach { region ->
                val isSelected = selectedRegions.contains(region.id)
                
                // Mapeia coordenadas normalizadas de volta para pixels relativos ao Canvas/Box
                val centerX = imageLeft + (region.x * imageWidth)
                val centerY = imageTop + (region.y * imageHeight)

                if (isSelected) {
                    if (region.isCircle) {
                        drawCircle(
                            color = Color.Red.copy(alpha = 0.5f),
                            radius = region.radius * imageWidth,
                            center = Offset(centerX, centerY)
                        )
                    } else {
                        val w = region.width * imageWidth
                        val h = region.height * imageHeight
                        drawRect(
                            color = Color.Red.copy(alpha = 0.5f),
                            topLeft = Offset(centerX - w / 2, centerY - h / 2),
                            size = Size(w, h)
                        )
                    }
                }

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 14.sp.toPx()
                        isFakeBoldText = true
                    }
                    drawText(
                        region.id,
                        centerX,
                        centerY + (paint.textSize / 3),
                        paint
                    )
                }
            }
        }
    }
}
