package com.meow.assistant.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import com.meow.assistant.data.repository.SettingsRepositoryImpl
import com.meow.assistant.ui.component.bottombar.BottomBar
import com.meow.assistant.ui.component.UpdatePrompt
import com.meow.assistant.ui.component.bottombar.MainPagerState
import com.meow.assistant.ui.component.bottombar.NavigationBadgeState
import com.meow.assistant.ui.component.bottombar.SideRail
import com.meow.assistant.ui.component.bottombar.rememberMainPagerState
import com.meow.assistant.ui.component.bottombar.useNavigationRail
import com.meow.assistant.ui.navigation3.LocalNavigator
import com.meow.assistant.ui.navigation3.Navigator
import com.meow.assistant.ui.navigation3.Route
import com.meow.assistant.ui.navigation3.rememberNavigator
import com.meow.assistant.ui.screen.colorpalette.ColorPaletteScreen
import com.meow.assistant.ui.screen.home.HomePager
import com.meow.assistant.ui.screen.settings.SettingPager
import com.meow.assistant.ui.screen.function.FunctionPager
import com.meow.assistant.ui.theme.MeowAssistantTheme
import com.meow.assistant.ui.theme.LocalColorMode
import com.meow.assistant.ui.theme.LocalEnableBlur
import com.meow.assistant.ui.theme.LocalEnableFloatingBottomBar
import com.meow.assistant.ui.theme.LocalEnableFloatingBottomBarBlur
import com.meow.assistant.ui.theme.LocalEnableNavigationBadge
import com.meow.assistant.ui.util.rememberBlurBackdrop
import com.meow.assistant.ui.util.rememberContentReady
import com.meow.assistant.ui.viewmodel.MainActivityViewModel
import com.meow.assistant.ui.viewmodel.MainPagerConfig
import com.meow.assistant.update.ReleaseInfo
import com.meow.assistant.update.UpdateManager
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {

    private val intentChannel = Channel<Intent>(capacity = Channel.BUFFERED)
    private var availableUpdate by mutableStateOf<ReleaseInfo?>(null)
    private var updateDownloading by mutableStateOf(false)
    private var updateError by mutableStateOf<String?>(null)
    private var updateCheckInFlight = false
    private var lastUpdateCheckAt = 0L

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPackageVisibilityPermission()

        if (savedInstanceState == null) intent?.let { intentChannel.trySend(it) }

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedMainPage by viewModel.selectedMainPage.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            val uiMode = uiState.uiMode
            val darkMode = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && isSystemInDarkTheme())

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            val navigator = rememberNavigator(Route.Main)
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
            }

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalEnableBlur provides uiState.enableBlur,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
                LocalEnableNavigationBadge provides uiState.enableNavigationBadge,
                LocalUiMode provides uiMode,
            ) {
                MeowAssistantTheme(appSettings = appSettings, uiMode = uiMode) {
                    val mainScreenEntry = @Composable {
                        MainScreen(
                            initialPage = selectedMainPage,
                            onPageChanged = viewModel::setSelectedMainPage,
                        )
                    }

                    val navDisplay = @Composable {
                        NavDisplay(
                            backStack = navigator.backStack,
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()
                            ),
                            onBack = {
                                when (val top = navigator.current()) {
                                    else -> navigator.pop()
                                }
                            },
                            entryProvider = entryProvider {
                                entry<Route.Main> { mainScreenEntry() }
                                entry<Route.ColorPalette> { ColorPaletteScreen() }
                                entry<Route.Home> { mainScreenEntry() }
                                entry<Route.Settings> { mainScreenEntry() }
                            }
                        )
                    }

                    when (uiMode) {
                        UiMode.Material -> androidx.compose.material3.Scaffold(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) { navDisplay() }

                        UiMode.Miuix -> Scaffold { navDisplay() }
                    }

                    availableUpdate?.let { release ->
                        UpdatePrompt(
                            release = release,
                            downloading = updateDownloading,
                            error = updateError,
                            onDownload = { downloadUpdate(release) },
                            onDismiss = {
                                if (!updateDownloading) {
                                    availableUpdate = null
                                    updateError = null
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkForUpdates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentChannel.trySend(intent)
    }

    private fun checkForUpdates() {
        val now = System.currentTimeMillis()
        if (updateCheckInFlight || now - lastUpdateCheckAt < UPDATE_CHECK_INTERVAL) return
        if (!runCatching { SettingsRepositoryImpl().checkUpdate }.getOrDefault(true)) return
        updateCheckInFlight = true
        lifecycleScope.launch {
            val release = UpdateManager.checkForUpdate()
            lastUpdateCheckAt = System.currentTimeMillis()
            updateCheckInFlight = false
            if (!isFinishing && release != null) availableUpdate = release
        }
    }

    private fun downloadUpdate(release: ReleaseInfo) {
        if (updateDownloading) return
        updateDownloading = true
        updateError = null
        lifecycleScope.launch {
            val result = UpdateManager.downloadAndInstall(this@MainActivity, release)
            updateDownloading = false
            result.exceptionOrNull()?.let { updateError = it.message ?: getString(com.meow.assistant.R.string.update_failed) }
            if (result.isSuccess) availableUpdate = null
        }
    }

    private fun requestPackageVisibilityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.QUERY_ALL_PACKAGES),
                REQUEST_QUERY_ALL_PACKAGES,
            )
        }
    }

    companion object {
        private const val UPDATE_CHECK_INTERVAL = 30 * 60 * 1000L
        private const val REQUEST_QUERY_ALL_PACKAGES = 1001
    }
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { MainPagerConfig.PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)
    val userScrollEnabled = true
    val navigationBadge = NavigationBadgeState()
    val uiMode = LocalUiMode.current
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface // Blur is not used in Material, this is just a placeholder
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    val blurBackdrop = rememberBlurBackdrop(enableBlur)

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onPageChanged(settledPage)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(mainPagerState, navController)

    val useNavigationRail = useNavigationRail(enableFloatingBottomBar)

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState
    ) {
        val contentReady = rememberContentReady()
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier) {
                HorizontalPager(
                    modifier = Modifier
                        .then(if (enableFloatingBottomBar && enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 3 else 0,
                    overscrollEffect = null,
                    userScrollEnabled = userScrollEnabled,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (page) {
                        0 -> if (isCurrentPage || contentReady) HomePager(navController, bottomInnerPadding, isCurrentPage)
                        1 -> if (isCurrentPage || contentReady) FunctionPager(bottomInnerPadding)
                        2 -> if (isCurrentPage || contentReady) SettingPager(navController, bottomInnerPadding)
                    }
                }
            }
        }

        if (useNavigationRail) {
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row {
                        SideRail(navigationBadge)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }

                UiMode.Miuix -> Scaffold { _ ->
                    Row {
                        SideRail(navigationBadge)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }
            }
        } else {
            val bottomBar = @Composable {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BottomBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                        navigationBadge = navigationBadge,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    bottomBar = bottomBar,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> Scaffold(bottomBar = bottomBar) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}


@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 && mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}
