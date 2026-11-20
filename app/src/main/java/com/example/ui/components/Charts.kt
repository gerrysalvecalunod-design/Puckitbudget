package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryBreakdown
import com.example.data.model.CurrencyOption
import com.example.data.model.PeriodChartPoint
import com.example.ui.theme.ExpenseCoralLight
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.utils.formatMoney
import kotlin.math.max

@Composable
fun SpendingBarChart(
    points: List<PeriodChartPoint>,
    showIncomeComparison: Boolean,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxVal = max(
        points.maxOfOrNull { if (showIncomeComparison) max(it.expense, it.income) else it.expense } ?: 1.0,
        1.0
    )
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 550),
        label = "bar_chart_anim"
    )

    val expenseColor = if (showIncomeComparison) ExpenseCoralLight else MaterialTheme.colorScheme.primary
    val incomeColor = IncomeGreenLight
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (showIncomeComparison) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = incomeColor, label = "Income")
                Spacer(modifier = Modifier.width(14.dp))
                LegendDot(color = expenseColor, label = "Expenses")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            val width = size.width
            val height = size.height
            val slotCount = points.size.coerceAtLeast(1)
            val slotWidth = width / slotCount

            // Subtle horizontal guide lines
            for (i in 0..3) {
                val y = height * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            points.forEachIndexed { index, point ->
                val centerX = slotWidth * index + slotWidth / 2f
                if (showIncomeComparison) {
                    val barW = (slotWidth * 0.26f).coerceIn(8.dp.toPx(), 18.dp.toPx())
                    val gap = 3.dp.toPx()
                    val incH = ((point.income / maxVal).toFloat() * height * animProgress).coerceAtLeast(
                        if (point.income > 0) 4.dp.toPx() else 0f
                    )
                    val expH = ((point.expense / maxVal).toFloat() * height * animProgress).coerceAtLeast(
                        if (point.expense > 0) 4.dp.toPx() else 0f
                    )

                    if (incH > 0f) {
                        drawRoundRect(
                            color = incomeColor,
                            topLeft = Offset(centerX - barW - gap / 2f, height - incH),
                            size = Size(barW, incH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                    if (expH > 0f) {
                        drawRoundRect(
                            color = expenseColor,
                            topLeft = Offset(centerX + gap / 2f, height - expH),
                            size = Size(barW, expH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                } else {
                    val barW = (slotWidth * 0.42f).coerceIn(10.dp.toPx(), 28.dp.toPx())
                    val expH = ((point.expense / maxVal).toFloat() * height * animProgress).coerceAtLeast(
                        if (point.expense > 0) 5.dp.toPx() else 0f
                    )
                    if (expH > 0f) {
                        drawRoundRect(
                            color = expenseColor,
                            topLeft = Offset(centerX - barW / 2f, height - expH),
                            size = Size(barW, expH),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    } else {
                        drawRoundRect(
                            color = gridColor,
                            topLeft = Offset(centerX - barW / 2f, height - 3.dp.toPx()),
                            size = Size(barW, 3.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!showIncomeComparison) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Peak: ${formatMoney(points.maxOfOrNull { it.expense } ?: 0.0, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryDonutChart(
    breakdowns: List<CategoryBreakdown>,
    totalExpenses: Double,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    val animSweep by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650),
        label = "donut_anim"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(136.dp)) {
                val strokeWidth = 22.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                if (breakdowns.isEmpty() || totalExpenses <= 0.0) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    var startAngle = -90f
                    breakdowns.forEach { item ->
                        val sweep = item.percentage * 360f * animSweep
                        if (sweep > 1f) {
                            drawArc(
                                color = Color(item.category.colorHex),
                                startAngle = startAngle,
                                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        }
                        startAngle += sweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(totalExpenses, currency),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (breakdowns.isEmpty()) {
                Text(
                    text = "No category expenses logged yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                breakdowns.take(5).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(item.category.colorHex))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.category.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${(item.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
