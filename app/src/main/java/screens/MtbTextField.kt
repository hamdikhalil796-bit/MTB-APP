package com.mtb.mtb.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mtb.mtb.app.MtbSpacing
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors

/**
 * Single-line input styled like the website's `.mtb-input`: ~38dp tall (10dp/12dp padding,
 * 13sp text), 6dp corners, 1dp gray border that turns brown on focus. Material's
 * OutlinedTextField is a fixed 56dp tall, which made every search box look oversized next
 * to the website.
 */
@Composable
fun MtbTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .background(MtbWebColors.CardWhite, shape)
            .border(1.dp, if (focused) MtbWebColors.SecondaryBrown else MtbWebColors.BorderGray, shape)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = MtbSpacing.md, vertical = 10.dp),
        // lineHeight reset to Unspecified — see CompactTextField in AdminOrdersScreen.kt: otherwise
        // the app's 24sp base line height keeps the field tall regardless of fontSize.
        textStyle = LocalTextStyle.current.copy(
            fontSize = MtbTextSizes.body,
            lineHeight = TextUnit.Unspecified,
            color = MtbWebColors.TextDark
        ),
        // BasicTextField's default cursor is black, invisible on the dark theme's surfaces.
        cursorBrush = SolidColor(MtbWebColors.TextDark),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = MtbTextSizes.body, color = MtbWebColors.MutedText, maxLines = 1)
                }
                innerTextField()
            }
        }
    )
}
