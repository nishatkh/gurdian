package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Softly rounded organic wellness card with minimal neumorphic lift,
 * pastel or cream surface, and optional delicate dotted editorial border.
 */
@Composable
fun GuardianCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    containerColor: Color = if (highlighted) SoftCreamSurface else SoftCreamSurface,
    dottedBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (highlighted) 3.dp else 1.5.dp,
                shape = shape,
                ambientColor = SoftShadow,
                spotColor = SoftShadow,
            )
            .clip(shape)
            .then(
                if (dottedBorder) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = DottedStroke,
                            cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                            style = Stroke(width = 1.4.dp.toPx(), pathEffect = dashEffect),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        border = if (dottedBorder) null else BorderStroke(1.dp, if (highlighted) SoftOutline else Color(0x14242322)),
    ) {
        Column(modifier = Modifier.padding(22.dp), content = content)
    }
}

/**
 * Playful editorial pastel card layered with soft organic blobs, irregular circles,
 * and a rounded geometric star in the background — zero gradients, minimal shadows.
 */
@Composable
fun OrganicEditorialCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ButterYellowSoft,
    blobColor: Color = ButterYellow,
    accentShapeColor: Color = PastelPink,
    dottedBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = shape,
                ambientColor = SoftShadow,
                spotColor = SoftShadow,
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        border = if (dottedBorder) null else BorderStroke(1.dp, SoftOutline),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Organic curved corner blob
                val blobPath = Path().apply {
                    moveTo(size.width * 0.62f, 0f)
                    cubicTo(
                        size.width * 0.72f,
                        size.height * 0.36f,
                        size.width * 0.90f,
                        size.height * 0.24f,
                        size.width,
                        size.height * 0.56f,
                    )
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(blobPath, color = blobColor.copy(alpha = 0.65f))

                // Soft irregular circle accent
                drawCircle(
                    color = accentShapeColor.copy(alpha = 0.55f),
                    radius = size.minDimension * 0.16f,
                    center = Offset(size.width * 0.88f, size.height * 0.82f),
                )

                // Rounded 4-point editorial star
                drawRoundedFourStar(
                    center = Offset(size.width * 0.76f, size.height * 0.24f),
                    outerRadius = 11.dp.toPx(),
                    innerRadius = 4.5.dp.toPx(),
                    color = SoftCharcoal.copy(alpha = 0.14f),
                )

                if (dottedBorder) {
                    drawRoundRect(
                        color = DottedStroke,
                        cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx()),
                        style = Stroke(width = 1.4.dp.toPx(), pathEffect = dashEffect),
                    )
                }
            }

            Column(modifier = Modifier.padding(22.dp), content = content)
        }
    }
}

/**
 * Large bold numerical circular progress indicator in soft organic wellness style.
 */
@Composable
fun SoftCircularProgressRing(
    progress: Float,
    valueText: String,
    captionText: String? = null,
    size: Dp = 96.dp,
    progressColor: Color = SoftCharcoal,
    trackColor: Color = OliveTrack,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)

            // Outer subtle dotted halo
            drawCircle(
                color = DottedStroke,
                radius = (this.size.minDimension / 2f) - 1.dp.toPx(),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                ),
            )

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = (progress.coerceIn(0f, 1f) * 360f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            if (captionText != null) {
                Text(
                    text = captionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

/**
 * Organic geometric illustration for onboarding and calm wellness hero spaces.
 */
@Composable
fun OrganicWellnessBadge(
    icon: ImageVector,
    primaryBlobColor: Color = ButterYellow,
    secondaryBlobColor: Color = PastelPink,
    tertiaryCircleColor: Color = SageGreen,
    size: Dp = 112.dp,
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Soft organic base blob
            val organicPath = Path().apply {
                moveTo(w * 0.50f, h * 0.08f)
                cubicTo(w * 0.85f, h * 0.05f, w * 0.96f, h * 0.38f, w * 0.88f, h * 0.66f)
                cubicTo(w * 0.80f, h * 0.94f, w * 0.42f, h * 0.96f, w * 0.22f, h * 0.82f)
                cubicTo(w * 0.04f, h * 0.68f, w * 0.12f, h * 0.12f, w * 0.50f, h * 0.08f)
                close()
            }
            drawPath(organicPath, color = primaryBlobColor)

            // Overlapping dusty pink circle
            drawCircle(
                color = secondaryBlobColor.copy(alpha = 0.78f),
                radius = w * 0.24f,
                center = Offset(w * 0.78f, h * 0.26f),
            )

            // Muted sage circle
            drawCircle(
                color = tertiaryCircleColor.copy(alpha = 0.72f),
                radius = w * 0.16f,
                center = Offset(w * 0.22f, h * 0.78f),
            )

            // Delicate dotted orbit ring
            drawCircle(
                color = SoftCharcoal.copy(alpha = 0.24f),
                radius = w * 0.44f,
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 9f)),
                ),
            )

            // Playful rounded star accent
            drawRoundedFourStar(
                center = Offset(w * 0.18f, h * 0.22f),
                outerRadius = w * 0.085f,
                innerRadius = w * 0.036f,
                color = SoftCharcoal,
            )
        }

        Surface(
            modifier = Modifier.size(size * 0.48f),
            shape = CircleShape,
            color = SoftCreamSurface,
            border = BorderStroke(1.dp, SoftOutline),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SoftCharcoal,
                    modifier = Modifier.size(size * 0.24f),
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color = SoftCharcoal,
    backgroundColor: Color = color.copy(alpha = 0.12f),
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(0.9.dp, color.copy(alpha = 0.22f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (color == ButterYellow || color == PowderBlue || color == SageGreen || color == PastelPink) SoftCharcoal else color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = ButterYellowSoft,
                border = BorderStroke(1.dp, SoftOutline),
                modifier = Modifier.clickable(onClick = onActionClick),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftCharcoal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    GuardianCard(
        modifier = modifier,
        dottedBorder = true,
        containerColor = SoftCreamSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OrganicWellnessBadge(
                icon = icon,
                primaryBlobColor = SageGreenSoft,
                secondaryBlobColor = ButterYellow,
                tertiaryCircleColor = PowderBlue,
                size = 86.dp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TimelineStepRow(
    title: String,
    subtitle: String,
    timestamp: String,
    isLast: Boolean = false,
    accentColor: Color = SageGreen,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(1.5.dp, SoftCharcoal, CircleShape),
            )
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp),
                ) {
                    drawLine(
                        color = DottedStroke,
                        start = Offset(size.width / 2f, 4f),
                        end = Offset(size.width / 2f, size.height - 4f),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(text = timestamp, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedFourStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
) {
    val path = Path()
    val points = 8
    for (i in 0 until points) {
        val angle = (i * Math.PI / 4.0) - Math.PI / 2.0
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}
