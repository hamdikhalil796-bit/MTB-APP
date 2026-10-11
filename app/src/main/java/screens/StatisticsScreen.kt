package com.mtb.mtb.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.SavedOrder
import com.mtb.mtb.ui.theme.NeumorphicCard
import kotlin.math.roundToLong

private data class ArticleStat(val id: String, val reference: String, val qty: Double, val orders: Int)

private data class ClientStat(
    val key: String,
    // Empty when the order was saved without a client name; the UI shows a placeholder.
    val client: String,
    val orders: Int,
    val totalQty: Double,
    val articles: List<ArticleStat>
)

private data class OrderStats(
    val clients: List<ClientStat>,
    val articles: List<ArticleStat>,
    val totalOrders: Int,
    val totalQty: Double
)

private class MutableArticle(val id: String, val reference: String) {
    var qty = 0.0
    var orders = 0
}

// Quantities are free text on the order form, so anything unparsable counts as 0.
private fun parseQty(raw: String): Double =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0

private fun formatQty(n: Double): String {
    val rounded = (n * 100).roundToLong() / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

private fun MutableMap<String, MutableArticle>.bump(id: String, reference: String, qty: Double, seenInOrder: MutableSet<String>) {
    val stat = getOrPut(id) { MutableArticle(id, reference) }
    stat.qty += qty
    if (seenInOrder.add(id)) stat.orders += 1
}

private fun Map<String, MutableArticle>.ranked(): List<ArticleStat> =
    values.map { ArticleStat(it.id, it.reference, it.qty, it.orders) }
        .sortedWith(compareByDescending<ArticleStat> { it.qty }.thenBy { it.reference.lowercase() })

// Every order counts (current and completed): an ordered article is demand either way.
private fun computeOrderStats(orders: List<SavedOrder>): OrderStats {
    class ClientAcc(val client: String) {
        var orders = 0
        val articles = LinkedHashMap<String, MutableArticle>()
    }

    val globalArticles = LinkedHashMap<String, MutableArticle>()
    val clients = LinkedHashMap<String, ClientAcc>()
    var totalQty = 0.0

    for (order in orders) {
        val name = order.client.trim()
        val acc = clients.getOrPut(name.lowercase()) { ClientAcc(name) }
        acc.orders += 1

        val seenGlobal = mutableSetOf<String>()
        val seenClient = mutableSetOf<String>()
        for (item in order.items) {
            val qty = parseQty(item.qty)
            val id = item.id.ifBlank { item.reference }.trim()
            if (id.isEmpty() || qty == 0.0) continue
            val reference = item.reference.ifBlank { id }
            globalArticles.bump(id, reference, qty, seenGlobal)
            acc.articles.bump(id, reference, qty, seenClient)
            totalQty += qty
        }
    }

    val clientStats = clients.map { (key, acc) ->
        val articles = acc.articles.ranked()
        ClientStat(key, acc.client, acc.orders, articles.sumOf { it.qty }, articles)
    }.sortedWith(compareByDescending<ClientStat> { it.totalQty }.thenBy { it.client.lowercase() })

    return OrderStats(clientStats, globalArticles.ranked(), orders.size, totalQty)
}

private enum class StatsTab { CLIENTS, ARTICLES }

/**
 * Statistics: per-client breakdown of every article ordered, plus a global ranking of the
 * most-ordered articles. Computed on the fly from the orders already streamed from Firestore,
 * so there is nothing extra to store or keep in sync.
 */
@Composable
fun StatisticsScreen(orders: List<SavedOrder>) {
    val stats = remember(orders) { computeOrderStats(orders) }
    var tab by remember { mutableStateOf(StatsTab.CLIENTS) }
    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(setOf<String>()) }

    val filteredClients = stats.clients.filter { it.client.contains(searchQuery.trim(), ignoreCase = true) }
    val filteredArticles = stats.articles.filter {
        it.id.contains(searchQuery.trim(), ignoreCase = true) || it.reference.contains(searchQuery.trim(), ignoreCase = true)
    }
    val maxArticleQty = stats.articles.firstOrNull()?.qty ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.stats_title),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.title
                )
                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(12.dp))

                if (stats.totalOrders == 0) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.stats_empty),
                            color = MtbWebColors.MutedText,
                            fontSize = MtbTextSizes.label,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryBox(stringResource(R.string.stats_summary_orders), stats.totalOrders.toString(), Modifier.weight(1f))
                        SummaryBox(stringResource(R.string.stats_summary_clients), stats.clients.size.toString(), Modifier.weight(1f))
                        SummaryBox(stringResource(R.string.stats_summary_articles), formatQty(stats.totalQty), Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MtbWebColors.BgBeige, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatsTabButton(
                            text = stringResource(R.string.stats_tab_clients),
                            selected = tab == StatsTab.CLIENTS,
                            onClick = { tab = StatsTab.CLIENTS; searchQuery = "" },
                            modifier = Modifier.weight(1f)
                        )
                        StatsTabButton(
                            text = stringResource(R.string.stats_tab_articles),
                            selected = tab == StatsTab.ARTICLES,
                            onClick = { tab = StatsTab.ARTICLES; searchQuery = "" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    MtbTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.padding(bottom = 12.dp),
                        placeholder = stringResource(
                            if (tab == StatsTab.CLIENTS) R.string.stats_search_clients else R.string.articles_search_placeholder
                        )
                    )

                    if (tab == StatsTab.CLIENTS) {
                        if (filteredClients.isEmpty()) {
                            EmptyHint(stringResource(R.string.stats_no_match))
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredClients, key = { it.key }) { client ->
                                    ClientCard(
                                        client = client,
                                        isOpen = client.key in expanded,
                                        onToggle = {
                                            expanded = if (client.key in expanded) expanded - client.key else expanded + client.key
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        if (filteredArticles.isEmpty()) {
                            EmptyHint(stringResource(R.string.articles_empty))
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(filteredArticles, key = { _, a -> a.id }) { index, article ->
                                    ArticleBarRow(article = article, max = maxArticleQty, rank = index + 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MtbWebColors.BgBeige, RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
        Text(
            label,
            color = MtbWebColors.MutedText,
            fontSize = MtbTextSizes.caption,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatsTabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MtbWebColors.PrimaryBrown else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else MtbWebColors.HeadingBrown,
            fontWeight = FontWeight.SemiBold,
            fontSize = MtbTextSizes.body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MtbWebColors.MutedText, fontSize = MtbTextSizes.label, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ClientCard(client: ClientStat, isOpen: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MtbWebColors.BorderGray, shape)
            .background(MtbWebColors.HoverBg, shape)
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MtbWebColors.MutedText,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (isOpen) 90f else 0f)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.client.ifBlank { stringResource(R.string.stats_unknown_client) },
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.stats_orders_count, client.orders.toString()) +
                        " | " + stringResource(R.string.stats_articles_count, formatQty(client.totalQty)),
                    color = MtbWebColors.MutedText,
                    fontSize = MtbTextSizes.label
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                formatQty(client.totalQty),
                color = MtbWebColors.HeadingBrown,
                fontWeight = FontWeight.Bold,
                fontSize = MtbTextSizes.title
            )
        }

        if (isOpen) {
            HorizontalDivider(color = MtbWebColors.BorderGray, thickness = 0.5.dp)
            Column(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val max = client.articles.firstOrNull()?.qty ?: 0.0
                client.articles.forEach { ArticleBarRow(article = it, max = max, rank = null) }
            }
        }
    }
}

@Composable
private fun ArticleBarRow(article: ArticleStat, max: Double, rank: Int?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (rank != null) {
            Text(
                rank.toString(),
                modifier = Modifier.width(28.dp),
                color = MtbWebColors.MutedText,
                fontWeight = FontWeight.Bold,
                fontSize = MtbTextSizes.body,
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = article.id,
                    modifier = Modifier
                        .background(MtbWebColors.IdBadgeBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    color = MtbWebColors.HeadingBrown,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (article.reference != article.id) article.reference else "",
                    modifier = Modifier.weight(1f),
                    color = MtbWebColors.TextDark,
                    fontSize = MtbTextSizes.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatQty(article.qty),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.title
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MtbWebColors.BgBeige)
            ) {
                val fraction = if (max > 0) (article.qty / max).coerceIn(0.02, 1.0).toFloat() else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(MtbWebColors.SecondaryBrown, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
