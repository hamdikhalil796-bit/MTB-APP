package com.mtb.mtb.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbSpacing
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.ui.theme.NeumorphicCard

/**
 * Blocking full-screen overlay shown whenever the device has no validated internet
 * connection — both on cold launch and if the connection drops mid-session. Rendered as
 * a non-dismissible Dialog rather than swapping out the current screen, so whatever the
 * user was doing underneath (an in-progress order, a form) stays alive in composition
 * and simply reappears once connectivity returns.
 */
@Composable
fun NoNetworkScreen(onRetryClick: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MtbWebColors.BgPage)
                .padding(MtbSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = MtbWebColors.CardWhite,
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(MtbSpacing.xl)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mtb_logo),
                        contentDescription = "MTB Logo",
                        modifier = Modifier
                            .height(90.dp)
                            .width(170.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(MtbSpacing.lg))

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MtbWebColors.ErrorBannerBg, RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = MtbWebColors.DeleteRed, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.height(MtbSpacing.md))

                    Text(
                        text = stringResource(R.string.network_offline_title),
                        color = MtbWebColors.HeadingBrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.title
                    )

                    Spacer(modifier = Modifier.height(MtbSpacing.xs))

                    Text(
                        text = stringResource(R.string.network_offline_message),
                        color = MtbWebColors.MutedText,
                        fontSize = MtbTextSizes.body,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(MtbSpacing.lg))

                    MtbButton(
                        text = stringResource(R.string.network_retry_button),
                        onClick = onRetryClick,
                        containerColor = MtbWebColors.PrimaryBrown,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
