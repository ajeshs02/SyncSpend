package com.ajesh.syncspend.widget

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.domain.model.CalendarBounds
import com.ajesh.syncspend.MainActivity
import com.ajesh.syncspend.SyncSpendApp
import com.ajesh.syncspend.di.AppContainer
import com.ajesh.syncspend.domain.model.AppLink
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.ui.addentry.AddEntryViewModel
import com.ajesh.syncspend.ui.components.AmountEntryPad
import com.ajesh.syncspend.ui.components.AppLogo
import com.ajesh.syncspend.ui.components.CategoryField
import com.ajesh.syncspend.ui.components.CategoryPickerSheet
import com.ajesh.syncspend.ui.components.DatePickerSheet
import com.ajesh.syncspend.ui.components.PrimaryButton
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import kotlin.coroutines.cancellation.CancellationException
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The widget's quick-add panel: a translucent overlay over the home screen with
 * the Add Entry pattern (amount, category field + picker sheet, keypad with date
 * key, save) locked to Expense — the same lightweight "add without opening the
 * app" feel as Google Tasks' widget. It slides up, and slides back down (with
 * the scrim fading in step) before the activity finishes.
 */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SyncSpendApp).container
        setContent {
            // Always follows the phone's system theme, ignoring the app's own stored Light/Dark
            // override — this mirrors the home-screen widget itself, which is plain RemoteViews with
            // night-qualified resources and has no notion of an in-app theme preference at all. Since
            // this activity is freshly created on every widget tap (it finishes itself right after) and
            // isSystemInDarkTheme() is available immediately with no data load, there's no cold-start
            // flash to guard against either, unlike MainActivity's splash-gated path.
            val dark = isSystemInDarkTheme()
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            SyncSpendTheme(themeMode = ThemeMode.SYSTEM) {
                QuickAddPanel(
                    container = container,
                    onFinished = ::finishWithoutTransition,
                    onOpenApp = ::openApp,
                    onAddCategory = ::openCategories,
                )
            }
        }
    }

    /** The panel has already animated itself out; a system transition on top would read as a stutter. */
    private fun finishWithoutTransition() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        }
        finish()
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    /** This panel runs in its own Activity, so "+ add category" opens the main app via [AppLink] instead of a local nav push. */
    private fun openCategories(flow: FlowType) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(AppLink.EXTRA_KIND, AppLink.KIND_CATEGORIES)
                .putExtra(AppLink.EXTRA_ID, flow.ordinal.toLong()),
        )
    }
}

@Composable
private fun QuickAddPanel(
    container: AppContainer,
    onFinished: () -> Unit,
    onOpenApp: () -> Unit,
    onAddCategory: (FlowType) -> Unit,
) {
    val viewModel: AddEntryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AddEntryViewModel(
                    container.transactionRepository,
                    container.categoryRepository,
                    container.preferencesRepository,
                    container.selectionState,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val earliest by container.transactionRepository.getEarliestDate().collectAsStateWithLifecycle(null)
    val colors = SyncSpendTheme.colors
    val negative = colors.neg
    val scope = rememberCoroutineScope()

    // One 0..1 progress drives both the scrim and the panel, so they always move together
    // (enter: 0 -> 1, exit: 1 -> 0). It is only ever read in draw/layer blocks.
    val progress = remember { Animatable(0f) }
    val enterAlpha = remember { Animatable(0f) }
    var panelHeightPx by remember { mutableIntStateOf(1) }
    var closing by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { enterAlpha.animateTo(1f, tween(200)) }
        progress.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
    }

    fun dismiss() {
        if (closing) return
        closing = true
        scope.launch {
            progress.animateTo(0f, tween(240, easing = FastOutLinearInEasing))
            onFinished()
        }
    }

    // Back gesture: the panel follows the finger (Android 14+ predictive back), then completes or springs back.
    PredictiveBackHandler(enabled = !closing) { events ->
        try {
            events.collect { event -> progress.snapTo(1f - 0.35f * event.progress) }
            dismiss()
        } catch (_: CancellationException) {
            progress.animateTo(1f, tween(180))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color(0xFF080E0A), alpha = 0.28f * progress.value) }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::dismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.94f)
                .onSizeChanged { panelHeightPx = it.height.coerceAtLeast(1) }
                .graphicsLayer {
                    translationY = (1f - progress.value) * size.height
                    alpha = enterAlpha.value
                }
                .background(colors.sheet, SyncSpendCorners.sheetTop)
                // Swallow taps so they don't fall through to the scrim.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 20.dp),
        ) {
            // Header doubles as the drag handle: pull the panel down to dismiss it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (progress.value < 0.8f) dismiss() else scope.launch { progress.animateTo(1f, tween(160)) }
                            },
                            onDragCancel = { scope.launch { progress.animateTo(1f, tween(160)) } },
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch { progress.snapTo((progress.value - dragAmount / panelHeightPx).coerceIn(0f, 1f)) }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Shortcut into the app itself — the real launcher icon.
                    AppLogo(
                        size = 30.dp,
                        modifier = Modifier
                            .semantics { contentDescription = "Open SyncSpend" }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                onOpenApp()
                                dismiss()
                            },
                    )
                    Text("New expense", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                }
                SquareIconButton(SyncSpendIcons.Close, { dismiss() }, size = 30.dp)
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(state.currencySymbol, fontSize = 22.sp, color = negative)
                        Text(
                            CurrencyFormatter.groupTyped(state.amountText),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-1.2).sp,
                            color = colors.ink,
                        )
                    }
                    Box(Modifier.padding(top = 10.dp).width(80.dp).height(2.dp).background(negative.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                    Text(DateUtils.longDate(state.date), style = MaterialTheme.typography.bodySmall, color = colors.sub, modifier = Modifier.padding(top = 9.dp))
                }

                CategoryField(
                    category = state.selectedCategory,
                    onClick = {
                        hint = null
                        viewModel.openCategoryPicker()
                    },
                    modifier = Modifier.padding(top = 16.dp),
                )

                hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = negative, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(if (hint == null) 16.dp else 8.dp))

                AmountEntryPad(
                    dateLabel = DateUtils.shortDate(state.date),
                    onDigit = {
                        hint = null
                        viewModel.pressDigit(it)
                    },
                    onBackspace = viewModel::pressBackspace,
                    onDateClick = { showDatePicker = true },
                    showDecimalKey = state.allowDecimalInput,
                )
                PrimaryButton(
                    text = if (saved) "Saved" else "Save Entry",
                    enabled = !saved,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                    leading = {
                        if (saved) {
                            Icon(SyncSpendIcons.Spark, null, tint = colors.brand, modifier = Modifier.size(14.dp))
                        } else {
                            Box(Modifier.size(7.dp).background(colors.expenseOnSelected, RoundedCornerShape(2.dp)))
                        }
                        Spacer(Modifier.width(8.dp))
                    },
                    onClick = {
                        when {
                            // No warning needed here — the picker popping open is the feedback.
                            state.selectedCategory == null -> viewModel.openCategoryPicker()
                            (state.amountText.toDoubleOrNull() ?: 0.0) <= 0.0 -> hint = "Enter an amount."
                            else -> viewModel.save(note = "") {
                                saved = true
                                scope.launch {
                                    delay(420)
                                    dismiss()
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    if (state.categoryPickerOpen) {
        CategoryPickerSheet(
            flow = FlowType.EXPENSE,
            categories = state.categoriesForType,
            selectedId = state.selectedCategory?.id,
            onPick = { cat -> if (cat == null) viewModel.clearCategory() else viewModel.selectCategory(cat) },
            onDismiss = viewModel::closeCategoryPicker,
            onAddCategory = { onAddCategory(FlowType.EXPENSE) },
        )
    }
    if (showDatePicker) {
        DatePickerSheet(
            initial = state.date,
            minMonth = CalendarBounds.entryLowerMonth(YearMonth.now(), earliest),
            onApply = viewModel::setDate,
            onDismiss = { showDatePicker = false },
        )
    }
}
