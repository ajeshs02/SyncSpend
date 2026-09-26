package com.ajesh.syncspend.ui.addentry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.AmountEntryPad
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.CategoryPickerSheet
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.components.flowSegmentIcons
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import com.ajesh.syncspend.util.NativePickers

@Composable
fun AddEntryScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: AddEntryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AddEntryViewModel(container.transactionRepository, container.categoryRepository, container.preferencesRepository)
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = SyncSpendTheme.colors
    val draftColor = if (state.type == FlowType.INCOME) colors.pos else colors.neg

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(top = SyncSpendChrome.screenTopInset),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SquareIconButton(
                        SyncSpendIcons.Back, onBack, size = 36.dp, radius = 13.dp, iconSize = 17.dp,
                        background = colors.pill,
                        modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(13.dp)),
                    )
                    Text("New Entry", style = MaterialTheme.typography.titleLarge, color = colors.ink)
                }

                AnimatedSegmentedControl(
                    options = listOf("Expense", "Income"),
                    selectedIndex = if (state.type == FlowType.EXPENSE) 0 else 1,
                    onSelect = { viewModel.setType(if (it == 0) FlowType.EXPENSE else FlowType.INCOME) },
                    icons = flowSegmentIcons(),
                    height = 44.dp,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp),
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(draftColor, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.type == FlowType.INCOME) "INCOME" else "EXPENSE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.84.sp,
                            color = draftColor,
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(state.currencySymbol, fontSize = 24.sp, fontWeight = FontWeight.Normal, color = draftColor)
                        AnimatedContent(
                            targetState = state.amountText,
                            transitionSpec = {
                                (fadeIn(tween(120)) + slideInVertically(tween(120)) { it / 6 }) togetherWith
                                    (fadeOut(tween(90)) + slideOutVertically(tween(90)) { -it / 6 })
                            },
                            label = "draft-amount",
                        ) { amount ->
                            Text(amount, fontSize = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.3).sp, color = colors.ink)
                        }
                    }
                    Box(
                        Modifier
                            .padding(top = 12.dp)
                            .width(88.dp)
                            .height(2.dp)
                            .background(draftColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
                    )
                    Text(
                        DateUtils.longDate(state.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.sub,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(start = 22.dp, end = 22.dp, top = 20.dp)
                        .fillMaxWidth()
                        .background(colors.card, RoundedCornerShape(18.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            viewModel.openCategoryPicker()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).background(colors.tile, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            SyncSpendIcons.iconFor(state.selectedCategory?.iconKey ?: "layers"),
                            null,
                            tint = if (state.selectedCategory != null) colors.ink else colors.sub,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category", style = MaterialTheme.typography.labelSmall, color = colors.sub)
                        Text(
                            state.selectedCategory?.name ?: "Choose a category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Icon(SyncSpendIcons.Down, null, tint = colors.sub, modifier = Modifier.size(14.dp))
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = SyncSpendChrome.screenBottomContentPadding + 18.dp),
            ) {
                AmountEntryPad(
                    dateLabel = DateUtils.shortDate(state.date),
                    onDigit = viewModel::pressDigit,
                    onBackspace = viewModel::pressBackspace,
                    onDateClick = { NativePickers.showDate(context, state.date, colors.isDark, viewModel::setDate) },
                )
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(colors.dark, RoundedCornerShape(18.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            viewModel.save(onSaved)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(7.dp).background(draftColor, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Entry", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }

    if (state.categoryPickerOpen) {
        CategoryPickerSheet(
            flow = state.type,
            categories = state.categoriesForType,
            selectedId = state.selectedCategory?.id,
            onPick = { cat ->
                if (cat == null) viewModel.clearCategory() else viewModel.selectCategory(cat)
            },
            onCreate = { name, icon -> viewModel.createCategory(name, icon) {} },
            onDismiss = viewModel::closeCategoryPicker,
        )
    }
}
