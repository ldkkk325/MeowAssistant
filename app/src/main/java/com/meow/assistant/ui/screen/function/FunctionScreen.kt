package com.meow.assistant.ui.screen.function

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.meow.assistant.R
import com.meow.assistant.assistant.AssistantConfig
import com.meow.assistant.assistant.AssistantDebugLog
import com.meow.assistant.assistant.AssistantViewModel
import com.meow.assistant.assistant.ProcessingMode
import com.meow.assistant.ui.LocalUiMode
import com.meow.assistant.ui.UiMode
import com.meow.assistant.ui.component.material.ExpressiveScaffold
import com.meow.assistant.ui.component.material.SegmentedColumn
import com.meow.assistant.ui.component.material.SegmentedDropdownItem
import com.meow.assistant.ui.component.material.SegmentedListItem
import com.meow.assistant.ui.component.material.SegmentedSwitchItem
import com.meow.assistant.ui.component.material.expressiveTopAppBarColors
import com.meow.assistant.ui.util.BlurredBar
import com.meow.assistant.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
private fun SmoothAnimatedVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) +
            slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { -it / 5 } +
            expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(tween(120)) +
            slideOutVertically(animationSpec = tween(160)) { -it / 6 } +
            shrinkVertically(animationSpec = tween(180)),
    ) {
        content()
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun FunctionPager(bottomInnerPadding: Dp) {
    val viewModel = viewModel<AssistantViewModel>()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(viewModel.exportConfig().toByteArray()) }
        }.onFailure { Toast.makeText(context, R.string.assistant_config_export_failed, Toast.LENGTH_SHORT).show() }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { viewModel.importConfig(it.readText()) }
            Toast.makeText(context, R.string.assistant_config_imported, Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(context, R.string.assistant_config_import_failed, Toast.LENGTH_SHORT).show() }
    }
    val logExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) runCatching {
            val log = AssistantDebugLog.export(context).ifBlank { context.getString(R.string.assistant_log_empty) + "\n" }
            context.contentResolver.openOutputStream(uri)?.use { it.write(log.toByteArray()) }
        }.onFailure { Toast.makeText(context, R.string.assistant_log_export_failed, Toast.LENGTH_SHORT).show() }
    }
    LifecycleResumeEffect(config.processingMode) {
        if (config.processingMode == ProcessingMode.FLOATING && Settings.canDrawOverlays(context)) {
            context.startService(Intent(context, com.meow.assistant.assistant.AssistantFloatBallService::class.java))
        } else if (config.processingMode != ProcessingMode.FLOATING) {
            context.stopService(Intent(context, com.meow.assistant.assistant.AssistantFloatBallService::class.java))
        }
        onPauseOrDispose { }
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> FunctionPagerMiuix(config, viewModel, bottomInnerPadding, exportLauncher, importLauncher, logExportLauncher)
        UiMode.Material -> FunctionPagerMaterial(config, viewModel, bottomInnerPadding, exportLauncher, importLauncher, logExportLauncher)
    }
}

@Composable
private fun FunctionPagerMaterial(
    config: AssistantConfig,
    viewModel: AssistantViewModel,
    bottomInnerPadding: Dp,
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    logExportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var editingField by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<EditorField?>(null) }
    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.function)) },
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = bottomInnerPadding)
        ) {
            SegmentedColumn(title = stringResource(R.string.assistant_section_general)) {
                item {
                    SegmentedDropdownItem(
                        icon = Icons.Rounded.TextFields,
                        title = stringResource(R.string.assistant_processing_mode),
                        summary = stringResource(R.string.assistant_processing_mode_summary),
                        items = listOf(
                            stringResource(R.string.assistant_mode_punctuation),
                            stringResource(R.string.assistant_mode_realtime),
                            stringResource(R.string.assistant_mode_floating),
                        ),
                        selectedIndex = processingModeIndex(config.processingMode),
                        onItemSelected = { selectProcessingMode(context, viewModel, it) },
                    )
                }
            }
            SegmentedColumn(title = stringResource(R.string.assistant_section_style)) {
                item {
                    SegmentedSwitchItem(
                        icon = Icons.Rounded.FormatPaint,
                        title = stringResource(R.string.assistant_append),
                        summary = stringResource(R.string.assistant_append_summary),
                        checked = config.enableAppend,
                        onCheckedChange = viewModel::setAppendEnabled,
                    )
                }
                item {
                    SmoothAnimatedVisibility(visible = config.enableAppend) {
                        SegmentedListItem(
                            onClick = { editingField = EditorField.Append },
                            headlineContent = { Text(stringResource(R.string.assistant_append_text)) },
                            supportingContent = {
                                Text(config.appendText.ifBlank { "—" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
                item {
                    SegmentedSwitchItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.assistant_emoticon),
                        summary = stringResource(R.string.assistant_emoticon_summary),
                        checked = config.enableEmoticon,
                        onCheckedChange = viewModel::setEmoticonEnabled,
                    )
                }
                item {
                    SmoothAnimatedVisibility(visible = config.enableEmoticon) {
                        EmoticonProbabilityMaterial(config, viewModel)
                    }
                }
                item {
                    SmoothAnimatedVisibility(visible = config.enableEmoticon && config.enableRandomText) {
                        ProbabilityMaterial(R.string.assistant_random_text_probability, R.string.assistant_random_text_probability_summary, config.randomTextProbability, true, viewModel::setRandomTextProbability)
                    }
                }
                item {
                    SmoothAnimatedVisibility(visible = config.enableAppend) {
                        ProbabilityMaterial(R.string.assistant_append_probability, R.string.assistant_append_probability_summary, config.appendProbability, true, viewModel::setAppendProbability)
                    }
                }
                item { SegmentedSwitchItem(title = stringResource(R.string.assistant_protect_input_methods), summary = stringResource(R.string.assistant_protect_input_methods_summary), checked = config.protectInputMethods, onCheckedChange = viewModel::setProtectInputMethods) }
                item { SegmentedSwitchItem(title = stringResource(R.string.assistant_protect_passwords), summary = stringResource(R.string.assistant_protect_passwords_summary), checked = config.protectPasswords, onCheckedChange = viewModel::setProtectPasswords) }
                item {
                    SmoothAnimatedVisibility(visible = config.enableEmoticon) {
                        SegmentedSwitchItem(
                            title = stringResource(R.string.assistant_smart_emoticon),
                            summary = stringResource(R.string.assistant_smart_emoticon_summary),
                            checked = config.enableSmartEmoticon,
                            onCheckedChange = viewModel::setSmartEmoticonEnabled,
                        )
                    }
                }
                item {
                    SmoothAnimatedVisibility(visible = config.enableEmoticon) {
                        SegmentedSwitchItem(
                            title = stringResource(R.string.assistant_random_text),
                            summary = stringResource(R.string.assistant_random_text_summary),
                            checked = config.enableRandomText,
                            onCheckedChange = viewModel::setRandomTextEnabled,
                        )
                    }
                }
            }
            SegmentedColumn(title = stringResource(R.string.assistant_section_rules)) {
                item {
                    SegmentedListItem(
                        onClick = { editingField = EditorField.Rules },
                        headlineContent = { Text(stringResource(R.string.assistant_rules)) },
                        supportingContent = { Text(editorSummary(AssistantConfig.rulesToText(config.rules))) },
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = { editingField = EditorField.Emoticons },
                        headlineContent = { Text(stringResource(R.string.assistant_custom_emoticons)) },
                        supportingContent = { Text(editorSummary(config.customEmoticons.joinToString("\n"))) },
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = { editingField = EditorField.Texts },
                        headlineContent = { Text(stringResource(R.string.assistant_custom_texts)) },
                        supportingContent = { Text(editorSummary(config.customTexts.joinToString("\n"))) },
                    )
                }
            }
            ConfigTransferMaterial(exportLauncher, importLauncher, logExportLauncher)
            SmoothAnimatedVisibility(visible = config.processingMode == ProcessingMode.FLOATING) {
                FloatingSettingsMaterial(config = config, viewModel = viewModel)
            }
        }
    }
    editingField?.let { field ->
        MaterialEditorDialog(
            field = field,
            initialValue = editorValue(field, config),
            onDismiss = { editingField = null },
            onConfirm = { value ->
                when (field) {
                    EditorField.Append -> viewModel.setAppendText(value)
                    EditorField.Rules -> viewModel.setRules(value)
                    EditorField.Emoticons -> viewModel.setCustomEmoticons(value)
                    EditorField.Texts -> viewModel.setCustomTexts(value)
                }
                editingField = null
            },
        )
    }
}

@Composable
private fun FunctionPagerMiuix(
    config: AssistantConfig,
    viewModel: AssistantViewModel,
    bottomInnerPadding: Dp,
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    logExportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(enableBlur = true)
    val blurActive = backdrop != null
    val barColor = colorScheme.surface.copy(alpha = if (blurActive) 0.72f else 0.82f)
    var editingField by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<EditorField?>(null) }
    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, tintAlpha = 0.62f) {
                TopAppBar(
                    title = stringResource(R.string.function),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().let { modifier ->
            if (backdrop != null) modifier.layerBackdrop(backdrop) else modifier
        }) {
            Column(
                modifier = Modifier.padding(innerPadding).nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp).padding(bottom = bottomInnerPadding)
            ) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                OverlayDropdownPreference(
                    title = stringResource(R.string.assistant_processing_mode),
                    summary = stringResource(R.string.assistant_processing_mode_summary),
                    items = listOf(
                        stringResource(R.string.assistant_mode_punctuation),
                        stringResource(R.string.assistant_mode_realtime),
                        stringResource(R.string.assistant_mode_floating),
                    ),
                    selectedIndex = processingModeIndex(config.processingMode),
                    onSelectedIndexChange = { selectProcessingMode(context, viewModel, it) },
                    startAction = { Icon(Icons.Rounded.TextFields, null, tint = colorScheme.onBackground) },
                )
                SwitchPreference(
                    title = stringResource(R.string.assistant_append),
                    summary = stringResource(R.string.assistant_append_summary),
                    checked = config.enableAppend,
                    onCheckedChange = viewModel::setAppendEnabled,
                    startAction = { Icon(Icons.Rounded.FormatPaint, null, tint = colorScheme.onBackground) },
                )
                SmoothAnimatedVisibility(visible = config.enableAppend) {
                    ArrowPreference(
                        title = stringResource(R.string.assistant_append_text),
                        summary = config.appendText.ifBlank { "—" },
                        onClick = { editingField = EditorField.Append },
                    )
                }
                SwitchPreference(
                    title = stringResource(R.string.assistant_emoticon),
                    summary = stringResource(R.string.assistant_emoticon_summary),
                    checked = config.enableEmoticon,
                    onCheckedChange = viewModel::setEmoticonEnabled,
                    startAction = { Icon(Icons.Rounded.AutoAwesome, null, tint = colorScheme.onBackground) },
                )
                SmoothAnimatedVisibility(visible = config.enableEmoticon) {
                    EmoticonProbabilityMiuix(config, viewModel)
                }
                SmoothAnimatedVisibility(visible = config.enableEmoticon && config.enableRandomText) {
                    ProbabilityMiuix(R.string.assistant_random_text_probability, R.string.assistant_random_text_probability_summary, config.randomTextProbability, true, viewModel::setRandomTextProbability)
                }
                SmoothAnimatedVisibility(visible = config.enableAppend) {
                    ProbabilityMiuix(R.string.assistant_append_probability, R.string.assistant_append_probability_summary, config.appendProbability, true, viewModel::setAppendProbability)
                }
                SwitchPreference(title = stringResource(R.string.assistant_protect_input_methods), summary = stringResource(R.string.assistant_protect_input_methods_summary), checked = config.protectInputMethods, onCheckedChange = viewModel::setProtectInputMethods)
                SwitchPreference(title = stringResource(R.string.assistant_protect_passwords), summary = stringResource(R.string.assistant_protect_passwords_summary), checked = config.protectPasswords, onCheckedChange = viewModel::setProtectPasswords)
                SmoothAnimatedVisibility(visible = config.enableEmoticon) {
                    SwitchPreference(
                        title = stringResource(R.string.assistant_smart_emoticon),
                        summary = stringResource(R.string.assistant_smart_emoticon_summary),
                        checked = config.enableSmartEmoticon,
                        onCheckedChange = viewModel::setSmartEmoticonEnabled,
                    )
                }
                SmoothAnimatedVisibility(visible = config.enableEmoticon) {
                    SwitchPreference(
                        title = stringResource(R.string.assistant_random_text),
                        summary = stringResource(R.string.assistant_random_text_summary),
                        checked = config.enableRandomText,
                        onCheckedChange = viewModel::setRandomTextEnabled,
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                ArrowPreference(
                    title = stringResource(R.string.assistant_rules),
                    summary = editorSummary(AssistantConfig.rulesToText(config.rules)),
                    onClick = { editingField = EditorField.Rules },
                )
                ArrowPreference(
                    title = stringResource(R.string.assistant_custom_emoticons),
                    summary = editorSummary(config.customEmoticons.joinToString("\n")),
                    onClick = { editingField = EditorField.Emoticons },
                )
                ArrowPreference(
                    title = stringResource(R.string.assistant_custom_texts),
                    summary = editorSummary(config.customTexts.joinToString("\n")),
                    onClick = { editingField = EditorField.Texts },
                )
            }
            ConfigTransferMiuix(exportLauncher, importLauncher, logExportLauncher)
            SmoothAnimatedVisibility(visible = config.processingMode == ProcessingMode.FLOATING) {
                FloatingSettingsMiuix(config = config, viewModel = viewModel)
            }
            }
        }
    }
    editingField?.let { field ->
        MiuixEditorDialog(
            field = field,
            initialValue = editorValue(field, config),
            onDismiss = { editingField = null },
            onConfirm = { value ->
                when (field) {
                    EditorField.Append -> viewModel.setAppendText(value)
                    EditorField.Rules -> viewModel.setRules(value)
                    EditorField.Emoticons -> viewModel.setCustomEmoticons(value)
                    EditorField.Texts -> viewModel.setCustomTexts(value)
                }
                editingField = null
            },
        )
    }
}

@Composable
private fun EmoticonProbabilityMaterial(config: AssistantConfig, viewModel: AssistantViewModel) {
    var probability by remember(config.emoticonProbability) { mutableFloatStateOf(config.emoticonProbability.toFloat()) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.assistant_emoticon_probability), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.assistant_emoticon_probability_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${probability.toInt()}%", color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = probability,
            onValueChange = { probability = it },
            onValueChangeFinished = { viewModel.setEmoticonProbability(probability.toInt()) },
            valueRange = 0f..100f,
            enabled = config.enableEmoticon,
        )
    }
}

@Composable
private fun EmoticonProbabilityMiuix(config: AssistantConfig, viewModel: AssistantViewModel) {
    var probability by remember(config.emoticonProbability) { mutableFloatStateOf(config.emoticonProbability.toFloat()) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.assistant_emoticon_probability), color = colorScheme.onSurface)
                Text(stringResource(R.string.assistant_emoticon_probability_summary), color = colorScheme.onSurfaceVariantSummary)
            }
            Text("${probability.toInt()}%", color = colorScheme.primary)
        }
        MiuixSlider(
            value = probability,
            onValueChange = { probability = it },
            onValueChangeFinished = { viewModel.setEmoticonProbability(probability.toInt()) },
            valueRange = 0f..100f,
            enabled = config.enableEmoticon,
        )
    }
}

@Composable
private fun ProbabilityMaterial(titleRes: Int, summaryRes: Int, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(summaryRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${sliderValue.toInt()}%", color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = sliderValue, onValueChange = { sliderValue = it }, onValueChangeFinished = { onChange(sliderValue.toInt()) }, valueRange = 0f..100f, enabled = enabled)
    }
}

@Composable
private fun ProbabilityMiuix(titleRes: Int, summaryRes: Int, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(titleRes), color = colorScheme.onSurface)
                Text(stringResource(summaryRes), color = colorScheme.onSurfaceVariantSummary)
            }
            Text("${sliderValue.toInt()}%", color = colorScheme.primary)
        }
        MiuixSlider(value = sliderValue, onValueChange = { sliderValue = it }, onValueChangeFinished = { onChange(sliderValue.toInt()) }, valueRange = 0f..100f, enabled = enabled)
    }
}

@Composable
private fun ConfigTransferMaterial(
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    logExportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    SegmentedColumn(title = stringResource(R.string.assistant_config_section)) {
        item {
            SegmentedListItem(leadingContent = { androidx.compose.material3.Icon(Icons.Rounded.FileUpload, null) }, headlineContent = { Text(stringResource(R.string.assistant_config_import)) }, onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) })
        }
        item {
            SegmentedListItem(leadingContent = { androidx.compose.material3.Icon(Icons.Rounded.FileDownload, null) }, headlineContent = { Text(stringResource(R.string.assistant_config_export)) }, onClick = { exportLauncher.launch("miao-helper-config.json") })
        }
        item {
            SegmentedListItem(leadingContent = { androidx.compose.material3.Icon(Icons.Rounded.FileDownload, null) }, headlineContent = { Text(stringResource(R.string.assistant_log_export)) }, onClick = { logExportLauncher.launch("miao-helper-debug.log") })
        }
    }
}

@Composable
private fun ConfigTransferMiuix(
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    logExportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        ArrowPreference(title = stringResource(R.string.assistant_config_import), onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, startAction = { Icon(Icons.Rounded.FileUpload, null, tint = colorScheme.onBackground) })
        ArrowPreference(title = stringResource(R.string.assistant_config_export), onClick = { exportLauncher.launch("miao-helper-config.json") }, startAction = { Icon(Icons.Rounded.FileDownload, null, tint = colorScheme.onBackground) })
        ArrowPreference(title = stringResource(R.string.assistant_log_export), onClick = { logExportLauncher.launch("miao-helper-debug.log") }, startAction = { Icon(Icons.Rounded.FileDownload, null, tint = colorScheme.onBackground) })
    }
}

@Composable
private fun FloatingSettingsMaterial(config: AssistantConfig, viewModel: AssistantViewModel) {
    var size by remember(config.floatBallSize) { mutableFloatStateOf(config.floatBallSize.toFloat()) }
    var alpha by remember(config.floatBallAlpha) { mutableFloatStateOf(config.floatBallAlpha) }
    SegmentedColumn(title = stringResource(R.string.assistant_float_ball_settings)) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.assistant_float_ball_size),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.assistant_float_ball_size_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${size.toInt()}dp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = size,
                        onValueChange = { size = it },
                        onValueChangeFinished = { viewModel.setFloatBallSize(size.toInt()) },
                        valueRange = AssistantConfig.MIN_FLOAT_BALL_SIZE.toFloat()..AssistantConfig.MAX_FLOAT_BALL_SIZE.toFloat(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.assistant_float_ball_alpha),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.assistant_float_ball_alpha_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${(alpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        onValueChangeFinished = { viewModel.setFloatBallAlpha(alpha) },
                        valueRange = AssistantConfig.MIN_FLOAT_BALL_ALPHA..AssistantConfig.MAX_FLOAT_BALL_ALPHA,
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingSettingsMiuix(config: AssistantConfig, viewModel: AssistantViewModel) {
    var size by remember(config.floatBallSize) { mutableFloatStateOf(config.floatBallSize.toFloat()) }
    var alpha by remember(config.floatBallAlpha) { mutableFloatStateOf(config.floatBallAlpha) }
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.assistant_float_ball_settings),
                color = colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.assistant_float_ball_size), color = colorScheme.onSurface)
                    Text(
                        text = stringResource(R.string.assistant_float_ball_size_summary),
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
                Text(text = "${size.toInt()}dp", color = colorScheme.primary)
            }
            MiuixSlider(
                value = size,
                onValueChange = { size = it },
                onValueChangeFinished = { viewModel.setFloatBallSize(size.toInt()) },
                valueRange = AssistantConfig.MIN_FLOAT_BALL_SIZE.toFloat()..AssistantConfig.MAX_FLOAT_BALL_SIZE.toFloat(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.assistant_float_ball_alpha), color = colorScheme.onSurface)
                    Text(
                        text = stringResource(R.string.assistant_float_ball_alpha_summary),
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
                Text(text = "${(alpha * 100).toInt()}%", color = colorScheme.primary)
            }
            MiuixSlider(
                value = alpha,
                onValueChange = { alpha = it },
                onValueChangeFinished = { viewModel.setFloatBallAlpha(alpha) },
                valueRange = AssistantConfig.MIN_FLOAT_BALL_ALPHA..AssistantConfig.MAX_FLOAT_BALL_ALPHA,
            )
        }
    }
}

private fun processingModeIndex(mode: ProcessingMode): Int = when (mode) {
    ProcessingMode.PUNCTUATION -> 0
    ProcessingMode.REALTIME -> 1
    ProcessingMode.FLOATING -> 2
}

private fun selectProcessingMode(context: Context, viewModel: AssistantViewModel, index: Int) {
    val mode = when (index) {
        1 -> ProcessingMode.REALTIME
        2 -> ProcessingMode.FLOATING
        else -> ProcessingMode.PUNCTUATION
    }
    viewModel.setMode(mode)
    if (mode == ProcessingMode.FLOATING && !Settings.canDrawOverlays(context)) {
        openOverlaySettings(context)
    }
}

private enum class EditorField {
    Append, Rules, Emoticons, Texts,
}

private fun editorValue(field: EditorField, config: AssistantConfig): String = when (field) {
    EditorField.Append -> config.appendText
    EditorField.Rules -> AssistantConfig.rulesToText(config.rules)
    EditorField.Emoticons -> config.customEmoticons.joinToString("\n")
    EditorField.Texts -> config.customTexts.joinToString("\n")
}

private fun editorSummary(value: String): String = value.lineSequence()
    .filter(String::isNotBlank)
    .take(2)
    .joinToString(" · ")
    .ifBlank { "—" }

@Composable
private fun MaterialEditorDialog(
    field: EditorField,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by androidx.compose.runtime.remember(field, initialValue) { androidx.compose.runtime.mutableStateOf(initialValue) }
    val multiline = field != EditorField.Append
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(editorTitle(field)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = !multiline,
                minLines = if (multiline) 4 else 1,
                maxLines = if (multiline) 8 else 1,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun MiuixEditorDialog(
    field: EditorField,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by androidx.compose.runtime.remember(field, initialValue) { androidx.compose.runtime.mutableStateOf(initialValue) }
    val multiline = field != EditorField.Append
    OverlayDialog(
        show = true,
        title = editorTitle(field),
        onDismissRequest = onDismiss,
        content = {
            MiuixTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = !multiline,
                maxLines = if (multiline) 8 else 1,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                MiuixTextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                MiuixTextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = { onConfirm(value) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

@Composable
private fun editorTitle(field: EditorField): String = when (field) {
    EditorField.Append -> stringResource(R.string.assistant_append_text)
    EditorField.Rules -> stringResource(R.string.assistant_rules)
    EditorField.Emoticons -> stringResource(R.string.assistant_custom_emoticons)
    EditorField.Texts -> stringResource(R.string.assistant_custom_texts)
}

private fun openOverlaySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
}
