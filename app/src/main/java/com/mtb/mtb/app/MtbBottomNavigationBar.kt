package com.mtb.mtb.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mtb.mtb.R

private data class NavDestination(
    val screen: String,
    val icon: ImageVector,
    val labelRes: Int,
    val isAllowed: (Permissions) -> Boolean = { true }
)

private val HOME_DESTINATION = NavDestination("HOME", Icons.Filled.Home, R.string.nav_home)

// Kept to the daily-use screens only — with Add Product, Stock, and Maintenance folded
// in too this bar was 8 icons wide for admins, too cramped for a bottom bar. Those three
// stay one tap away via the Home screen's list instead of being duplicated here.
private val OTHER_DESTINATIONS = listOf(
    NavDestination("NEW_ORDER", Icons.Filled.Assignment, R.string.nav_orders) { it.newOrder },
    NavDestination("CURRENT_ORDER", Icons.Filled.PendingActions, R.string.nav_current_order) { it.currentOrder },
    NavDestination("HISTORY", Icons.Filled.History, R.string.nav_history) { it.history },
    NavDestination("ARTICLES", Icons.Filled.Inventory2, R.string.nav_articles) { it.articles }
)

private val BAR_HEIGHT = 52.dp
private val BOTTOM_MARGIN = 8.dp
private val HORIZONTAL_MARGIN = 14.dp
private val CORNER_RADIUS = 22.dp
private val TAB_ICON_SIZE = 22.dp
private val INDICATOR_SIZE = 38.dp
private val INDICATOR_SHAPE = RoundedCornerShape(14.dp)

/**
 * Persistent tab bar for jumping directly between the app's main sections. Hidden only
 * on the login screen. Home is inserted at the midpoint of the other destinations, which
 * splits evenly and centers it for whatever tabs the signed-in profile is allowed to see.
 *
 * The selected tab is highlighted by a pill that slides behind its icon and springs to its
 * new position when the selection changes. It stays flush inside the bar instead of
 * floating above it, so the bar doesn't need to reserve headroom for anything to pop out of.
 */
@Composable
fun MtbBottomNavigationBar(
    currentScreen: String,
    permissions: Permissions,
    onNavigate: (String) -> Unit
) {
    val others = OTHER_DESTINATIONS.filter { it.isAllowed(permissions) }
    // Round-half-up instead of floor: with an odd total tab count, floor division would put
    // Home one slot left of true center. This keeps it as close to centered as possible
    // for whatever count `others` ends up being.
    val homeIndex = (others.size + 1) / 2
    val destinations = others.toMutableList().apply { add(homeIndex, HOME_DESTINATION) }
    val selectedIndex = destinations.indexOfFirst { it.screen == currentScreen }.coerceAtLeast(0)

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val targetCenterXPx = if (barWidthPx > 0f) {
        (selectedIndex + 0.5f) * (barWidthPx / destinations.size)
    } else {
        0f
    }
    val animatedCenterXPx by animateFloatAsState(
        targetValue = targetCenterXPx,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "indicatorX"
    )

    val density = LocalDensity.current
    val animatedCenterXDp = with(density) { animatedCenterXPx.toDp() }

    val barShape: Shape = RoundedCornerShape(CORNER_RADIUS)
    val barModifier = Modifier
        .navigationBarsPadding()
        .padding(horizontal = HORIZONTAL_MARGIN, vertical = BOTTOM_MARGIN)
        .fillMaxWidth()
        .height(BAR_HEIGHT)
        .onSizeChanged { barWidthPx = it.width.toFloat() }

    val barContent: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = animatedCenterXDp - INDICATOR_SIZE / 2)
                    .size(INDICATOR_SIZE)
                    .background(MtbWebColors.PrimaryBrown, INDICATOR_SHAPE)
            )
            Row(modifier = Modifier.fillMaxSize()) {
                destinations.forEachIndexed { index, destination ->
                    val label = stringResource(destination.labelRes)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigate(destination.screen) }
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            destination.icon,
                            contentDescription = null,
                            tint = if (index == selectedIndex) Color.White else MtbWebColors.MutedText,
                            modifier = Modifier.size(TAB_ICON_SIZE)
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = barModifier
            .shadow(elevation = 10.dp, shape = barShape)
            .background(MtbWebColors.CardWhite, barShape)
    ) {
        barContent()
    }
}
