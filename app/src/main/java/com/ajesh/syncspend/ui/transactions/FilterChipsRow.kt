package com.ajesh.syncspend.ui.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.ui.components.ChipsRow

@Composable
fun FilterChipsRow(
    options: List<EntryFilter>,
    selected: EntryFilter,
    onSelect: (EntryFilter) -> Unit,
    modifier: Modifier = Modifier,
    /** Shown on the Custom chip once a range has been picked, e.g. "12 Aug – 3 Sep". */
    customLabel: String? = null,
) {
    val labels = remember(options, selected, customLabel) {
        options.map { if (it == EntryFilter.CUSTOM && it == selected && customLabel != null) customLabel else it.label }
    }
    ChipsRow(
        labels = labels,
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
        modifier = modifier,
    )
}
