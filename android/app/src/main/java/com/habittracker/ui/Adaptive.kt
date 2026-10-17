package com.habittracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val TabletBreakpoint = 600.dp
private val WideContentMax = 1040.dp
private val RailWidth = 104.dp

data class NavTab(val label: String, val icon: ImageVector, val group: String? = null)

@Composable
fun HabitScaffold(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    expanded: Boolean,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!expanded) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { onSelect(index) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = floatingActionButton,
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (expanded) {
                NavigationRail(
                    modifier = Modifier.width(RailWidth),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = { Spacer(Modifier.height(16.dp)) },
                ) {
                    var lastGroup: String? = null
                    tabs.forEachIndexed { index, tab ->
                        val group = tab.group
                        if (group != null && group != lastGroup) {
                            RailGroupLabel(group)
                            lastGroup = group
                        }
                        NavigationRailItem(
                            selected = selected == index,
                            onClick = { onSelect(index) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
            Box(
                Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    Modifier.fillMaxSize().widthIn(max = WideContentMax)
                        .padding(horizontal = if (expanded) 12.dp else 0.dp),
                ) {
                    content(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun RailGroupLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
fun TwoPane(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    if (expanded) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) { leading() }
            Column(Modifier.weight(1f)) { trailing() }
        }
    } else {
        Column(modifier) {
            leading()
            Spacer(Modifier.height(4.dp))
            trailing()
        }
    }
}
