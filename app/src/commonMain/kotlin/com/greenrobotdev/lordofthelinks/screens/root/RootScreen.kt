package com.greenrobotdev.lordofthelinks.screens.root

import androidx.compose.runtime.saveable.rememberSaveable
import com.greenrobotdev.lordofthelinks.utils.preferences.WelcomeTourPreference
import com.greenrobotdev.lordofthelinks.screens.welcome.WelcomeTourScreen
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.savedstate.serialization.SavedStateConfiguration
import com.greenrobotdev.lordofthelinks.model.SharedContent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.add.sheet.AddLinkSheet
import com.greenrobotdev.lordofthelinks.bookmarks.screens.reminders.RemindersScreen
import com.greenrobotdev.lordofthelinks.screens.library.navigation.LibraryNavRoutes
import com.greenrobotdev.lordofthelinks.screens.library.navigation.LibraryTabNavigation
import com.greenrobotdev.lordofthelinks.screens.organize.navigation.OrganizeTabNavigation
import com.greenrobotdev.lordofthelinks.screens.settings.navigation.SettingsTabNavigation
import com.greenrobotdev.lordofthelinks.design.motion.rememberReducedMotion
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer

private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(RootScreens::class) {
            subclass(RootScreens.FeedTab::class, serializer<RootScreens.FeedTab>())
            subclass(RootScreens.LibraryTab::class, serializer<RootScreens.LibraryTab>())
            subclass(RootScreens.RemindersTab::class, serializer<RootScreens.RemindersTab>())
            subclass(RootScreens.SettingsTab::class, serializer<RootScreens.SettingsTab>())
        }
    }
}

data class TabDefinition(
    val tab: RootScreens,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val title: String
)

val APP_TABS = listOf(
    TabDefinition(RootScreens.FeedTab, Icons.Filled.RssFeed, Icons.Outlined.RssFeed, "Feed"),
    TabDefinition(RootScreens.LibraryTab, Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "Library"),
    TabDefinition(RootScreens.RemindersTab, Icons.Filled.Notifications, Icons.Outlined.Notifications, "Reminders"),
    TabDefinition(RootScreens.SettingsTab, Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
)

@Composable
fun RootScreen(
    sharedContent: SharedContent? = null,
    onSharedContentHandled: () -> Unit = {}
) {
    val rootNavigationState = rememberRootNavigationState()
    val rootNavigator = remember { RootNavigator(rootNavigationState) }

    // First launch after install: the welcome tour, once. Opening the app by sharing a link saves
    // that first; the tour then waits for the next launch.
    val tourPreference = remember { WelcomeTourPreference() }
    var showTour by rememberSaveable { mutableStateOf(!tourPreference.seen && sharedContent == null) }
    if (showTour) {
        WelcomeTourScreen(onDone = {
            tourPreference.markSeen()
            showTour = false
        })
        return
    }

    // Feed tab reuses the Library tab's navigation (Add link, details, notes)
    val feedTabNavState = rememberTabNavigationState(
        startRoute = LibraryNavRoutes.LibraryBase
    )
    val feedTabNavigator = remember { TabNavigator(feedTabNavState) }

    // Step one of Add link is a modal sheet; Continue opens the existing full add-link screen
    var addLinkSheetUrl by remember { mutableStateOf<String?>(null) }
    val openAddLinkSheet = { url: String? -> addLinkSheetUrl = url ?: "" }

    // Back at the root of any tab goes to the previously visited tab (Feed → Library → Reminders,
    // Back lands on Library), and only leaves the app at the bottom of the history. Screens deeper
    // in a tab (Navigation 3 uses this same back-event system) register later, so they pop first.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = rootNavigationState.canGoBack(),
        onBackCompleted = { rootNavigationState.goBackTab() },
    )

    // Share sheet: open Add link on the Feed tab
    LaunchedEffect(sharedContent) {
        if (sharedContent?.url != null) {
            rootNavigator.switchTab(RootScreens.FeedTab)
            openAddLinkSheet(sharedContent.url)
            onSharedContentHandled()
        }
    }
    addLinkSheetUrl?.let { url ->
        AddLinkSheet(
            initialUrl = url.takeIf { it.isNotEmpty() },
            onContinue = { continued ->
                addLinkSheetUrl = null
                feedTabNavigator.navigateToNew(LibraryNavRoutes.AddLink(continued))
            },
            onDismiss = { addLinkSheetUrl = null },
        )
    }

    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            // systemBars only (not IME) so the keyboard doesn't push the background content up
            .systemBarsPadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                APP_TABS.forEach { tabDef ->
                    val selected = rootNavigator.isTabActive(tabDef.tab)
                    // M3: selected icon scales up with a spring pop as the pill indicator expands
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabIconScale",
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { rootNavigator.switchTab(tabDef.tab) },
                        icon = {
                            Icon(
                                if (selected) tabDef.selectedIcon else tabDef.unselectedIcon,
                                contentDescription = tabDef.title,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                            )
                        },
                        label = { Text(tabDef.title) }
                    )
                }
            }
        },
        floatingActionButton = {
            // FAB only on the Feed list itself (hidden on Add link, link details, …); opens Add link
            if (rootNavigator.getCurrentTab() == RootScreens.FeedTab &&
                feedTabNavState.backStack.lastOrNull() is LibraryNavRoutes.LibraryBase
            ) {
                FloatingActionButton(
                    onClick = { openAddLinkSheet(null) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add link")
                }
            }
        }
    ) { padding ->
        // consume so inner imePadding doesn't re-add the bottom nav bar's height
        val currentTab = rootNavigator.getCurrentTab()
        val currentTabIndex = rootNavigationState.indexOf(currentTab)
        val reducedMotion = rememberReducedMotion()
        val density = LocalDensity.current
        val flingDistance = with(density) { 80.dp.toPx() }

        // Motion specs read here: transitionSpec isn't composable, so pass them in as values
        val slideSpec = tween<IntOffset>(300, easing = FastOutSlowInEasing)
        val fadeSpec = tween<Float>(200)

        Box(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                // Fling left/right on the content switches to the adjacent tab. Velocity-style
                // threshold (one firm swipe over ~80dp) keeps inner horizontal scrolling (chip
                // rows, carousels) working — only a deliberate long swipe changes tabs.
                .pointerInput(currentTabIndex) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                        onDragEnd = {
                            if (totalDrag < -flingDistance && currentTabIndex < APP_TABS.lastIndex) {
                                rootNavigator.switchTab(APP_TABS[currentTabIndex + 1].tab)
                            } else if (totalDrag > flingDistance && currentTabIndex > 0) {
                                rootNavigator.switchTab(APP_TABS[currentTabIndex - 1].tab)
                            }
                            totalDrag = 0f
                        },
                        onDragCancel = { totalDrag = 0f },
                    )
                }
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (reducedMotion) {
                        // No motion: straight crossfade of the two tabs
                        fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
                    } else {
                        val from = rootNavigationState.indexOf(initialState)
                        val to = rootNavigationState.indexOf(targetState)
                        if (to > from) {
                            // Moving right in tab order: new tab slides in from the right
                            (slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec)) togetherWith
                                (slideOutHorizontally(slideSpec) { -it } + fadeOut(fadeSpec))
                        } else {
                            // Moving left in tab order: new tab slides in from the left
                            (slideInHorizontally(slideSpec) { -it } + fadeIn(fadeSpec)) togetherWith
                                (slideOutHorizontally(slideSpec) { it } + fadeOut(fadeSpec))
                        }
                    }
                },
                label = "TabSwitch",
            ) { tab ->
                when (tab) {
                    RootScreens.FeedTab -> LibraryTabNavigation(
                        tabNavigator = feedTabNavigator,
                        onAddLink = openAddLinkSheet,
                    )
                    RootScreens.LibraryTab -> OrganizeTabNavigation(
                        onOpenReminders = { rootNavigator.switchTab(RootScreens.RemindersTab) }
                    )
                    RootScreens.RemindersTab -> RemindersScreen()
                    RootScreens.SettingsTab -> SettingsTabNavigation()
                }
            }
        }
    }
}
