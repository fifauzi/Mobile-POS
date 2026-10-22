package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

fun getProductInitials(name: String): String {
    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "??"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

fun getInitialsColors(name: String): Pair<Color, Color> {
    // Soft pastel backgrounds with matching rich text colors
    val pastelPairs = listOf(
        Pair(Color(0xFFE0F2FE), Color(0xFF0369A1)), // Soft Sky Blue
        Pair(Color(0xFFFEF3C7), Color(0xFFB45309)), // Soft Amber
        Pair(Color(0xFFDCFCE7), Color(0xFF15803D)), // Soft Emerald Mint
        Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE)), // Soft Lavender Purple
        Pair(Color(0xFFFFE4E6), Color(0xFFBE123C)), // Soft Rose Pink
        Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9)), // Soft Indigo Violet
        Pair(Color(0xFFFEF9C3), Color(0xFFA16207)), // Soft Warm Cream
        Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))  // Soft Cyan Teal
    )
    val index = abs(name.hashCode()) % pastelPairs.size
    return pastelPairs[index]
}

@Composable
fun ProductFallbackAvatar(
    productName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 14.dp,
    fontSize: TextUnit = 16.sp
) {
    val (bgColor, textColor) = getInitialsColors(productName)
    val initials = getProductInitials(productName)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}
