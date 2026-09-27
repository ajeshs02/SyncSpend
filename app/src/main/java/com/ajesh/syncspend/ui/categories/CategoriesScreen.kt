package com.ajesh.syncspend.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.FlowToggle
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.HeaderAddButton
import com.ajesh.syncspend.ui.components.NameIconDialog
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.IconKind
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

@Composable
fun CategoriesScreen() {
    val container = LocalAppContainer.current
    val viewModel: CategoriesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CategoriesViewModel(container.categoryRepository, container.transactionRepository) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors
    val flowWord = if (state.type == FlowType.INCOME) "income" else "expense"

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(top = SyncSpendChrome.screenTopInset)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Categories",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
                color = colors.ink,
                modifier = Modifier.padding(vertical = 3.dp),
            )
            HeaderAddButton("New", onClick = { showAdd = true })
        }

        FlowToggle(
            type = state.type,
            onSelect = viewModel::setType,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp),
        )
        Text(
            "Use the arrows to reorder · tap a category to rename it, change its icon or delete it",
            fontSize = 10.5.sp,
            color = colors.sub,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 8.dp),
        )

        if (state.categories.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 40.dp, end = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No $flowWord categories yet", style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                Text(
                    "Tap New to create one and pick an icon for it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.sub,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = SyncSpendChrome.screenBottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(state.categories, key = { _, c -> c.id }, contentType = { _, _ -> "category" }) { index, cat ->
                    CategoryRow(
                        category = cat,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.categories.lastIndex,
                        onEdit = { editing = cat },
                        onUp = { viewModel.move(cat, -1) },
                        onDown = { viewModel.move(cat, 1) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showAdd) {
        NameIconDialog(
            title = "New category",
            body = "Name it and pick an icon. It will be added to the $flowWord list.",
            cta = "Create",
            initialName = "",
            initialIconKey = null,
            iconKind = IconKind.CATEGORY,
            namePlaceholder = if (state.type == FlowType.INCOME) "New Income ${state.categories.size + 1}" else "New Expense ${state.categories.size + 1}",
            onConfirm = { name, icon ->
                viewModel.add(name, icon)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
    editing?.let { cat ->
        NameIconDialog(
            title = "Edit category",
            body = "Rename it or pick a different icon. Past entries update along with it.",
            cta = "Save",
            initialName = cat.name,
            initialIconKey = cat.iconKey,
            iconKind = IconKind.CATEGORY,
            namePlaceholder = "Category name",
            onConfirm = { name, icon ->
                viewModel.update(cat, name, icon)
                editing = null
            },
            onDelete = {
                editing = null
                deleting = cat
            },
            deleteLabel = "Delete category",
            onDismiss = { editing = null },
        )
    }
    deleting?.let { cat ->
        ConfirmDialog(
            title = "Delete category?",
            body = "“${cat.name}” will be removed. Past entries keep their label.",
            cta = "Delete",
            onConfirm = {
                viewModel.delete(cat)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(16.dp))
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier.size(34.dp).background(colors.tile, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(SyncSpendIcons.iconFor(category.iconKey), null, tint = colors.ink, modifier = Modifier.size(16.dp)) }
            Text(
                category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        SquareIconButton(SyncSpendIcons.Up, onUp, enabled = canMoveUp)
        SquareIconButton(SyncSpendIcons.Down, onDown, enabled = canMoveDown)
    }
}
