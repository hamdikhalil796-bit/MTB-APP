package com.mtb.mtb.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Shared spacing/type scale so dense data screens (orders, stock, articles) stop
// hand-picking a different dp/sp value per screen. Pull from here instead of
// writing a new literal.
object MtbSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object MtbTextSizes {
    val caption = 11.sp
    val label = 12.sp
    val body = 13.sp
    val subtitle = 14.sp
    val title = 16.sp
    val heading = 18.sp
}
