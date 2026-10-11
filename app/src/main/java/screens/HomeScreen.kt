package com.mtb.mtb.app.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R
import com.mtb.mtb.app.AppTheme
import com.mtb.mtb.app.LocalAppTheme
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.Permissions
import com.mtb.mtb.ui.theme.NeumorphicCard

private data class HomeTile(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    permissions: Permissions,
    welcomeName: String,
    onNewOrderClick: () -> Unit,
    onViewCurrentOrderClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    onViewArticlesClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onViewStockClick: () -> Unit,
    onViewMaintenanceClick: () -> Unit,
    onViewStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val tiles = buildList {
        if (permissions.newOrder) add(HomeTile(Icons.Filled.Assignment, stringResource(R.string.home_tile_order), MtbWebColors.PrimaryBrown, onNewOrderClick))
        if (permissions.currentOrder) add(HomeTile(Icons.Filled.PendingActions, stringResource(R.string.home_tile_current_order), MtbWebColors.TertiaryBrown, onViewCurrentOrderClick))
        if (permissions.history) add(HomeTile(Icons.Filled.History, stringResource(R.string.home_tile_history), MtbWebColors.MutedBrown, onViewHistoryClick))
        if (permissions.articles) add(HomeTile(Icons.Filled.Inventory2, stringResource(R.string.home_tile_articles), MtbWebColors.SecondaryBrown, onViewArticlesClick))
        if (permissions.editArticles) add(HomeTile(Icons.Filled.AddBox, stringResource(R.string.home_tile_add_product), MtbWebColors.PrimaryBrown, onAddProductClick))
        if (permissions.stock) add(HomeTile(Icons.Filled.BarChart, stringResource(R.string.home_tile_stock), MtbWebColors.SuccessGreen, onViewStockClick))
        if (permissions.maintenance) add(HomeTile(Icons.Filled.Build, stringResource(R.string.home_tile_maintenance), MtbWebColors.TertiaryBrown, onViewMaintenanceClick))
        if (permissions.statistics) add(HomeTile(Icons.Filled.Insights, stringResource(R.string.home_tile_stats), MtbWebColors.SecondaryBrown, onViewStatisticsClick))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mtb_logo),
                    contentDescription = "MTB Logo",
                    modifier = Modifier
                        .height(130.dp)
                        .width(250.dp),
                    contentScale = ContentScale.Fit
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_welcome,
                            welcomeName
                        ),
                        color = MtbWebColors.HeadingBrown,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = MtbWebColors.HeadingBrown)
                    }
                }

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    tiles.forEach { tile ->
                        HomeActionRow(
                            icon = tile.icon,
                            label = tile.label,
                            color = tile.color,
                            onClick = tile.onClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionRow(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rowScale"
    )
    val shape = RoundedCornerShape(12.dp)
    // `color` is a "Fill" token (Color.kt) — meant for solid backgrounds with white
    // content on top, and deliberately stays dark in both themes. Using it as both
    // the icon tint AND its own translucent halo works in light mode (dark icon on
    // a near-white card), but in dark mode the halo sits on an already-dark card and
    // the same-hue icon nearly disappears into it. Boosting the halo's opacity and
    // switching to a white icon in dark mode keeps light mode untouched while giving
    // dark mode a solid color-badge-with-white-icon look instead.
    val isDark = LocalAppTheme.current == AppTheme.DARK

    Card(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .scale(scale),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MtbWebColors.TileSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = if (isDark) 0.5f else 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isDark) Color.White else color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = MtbWebColors.HeadingBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MtbWebColors.MutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
