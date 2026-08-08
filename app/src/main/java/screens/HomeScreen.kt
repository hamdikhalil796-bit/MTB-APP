package com.mtb.mtb.app.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.UserRole

private data class HomeTile(
    val emoji: String,
    val label: String,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    userRole: UserRole,
    onNewOrderClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    onViewArticlesClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onViewStockClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val tiles = buildList {
        add(HomeTile("📋", stringResource(R.string.home_tile_order), MtbWebColors.PrimaryBrown, onNewOrderClick))
        add(HomeTile("📜", stringResource(R.string.home_tile_history), MtbWebColors.TertiaryBrown, onViewHistoryClick))
        add(HomeTile("📦", stringResource(R.string.home_tile_articles), MtbWebColors.SecondaryBrown, onViewArticlesClick))
        if (userRole == UserRole.ADMIN) {
            add(HomeTile("➕", stringResource(R.string.home_tile_add_product), MtbWebColors.MutedBrown, onAddProductClick))
            add(HomeTile("📊", stringResource(R.string.home_tile_stock), MtbWebColors.SuccessGreen, onViewStockClick))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MtbWebColors.CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
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
                            stringResource(if (userRole == UserRole.ADMIN) R.string.role_admin else R.string.role_guest)
                        ),
                        color = MtbWebColors.HeadingBrown,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onSettingsClick) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                }

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tiles.chunked(2).forEach { rowTiles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTiles.forEach { tile ->
                                HomeActionTile(
                                    emoji = tile.emoji,
                                    label = tile.label,
                                    color = tile.color,
                                    onClick = tile.onClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowTiles.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionTile(
    emoji: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tileScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .height(120.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MtbWebColors.TileSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = MtbWebColors.HeadingBrown,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
