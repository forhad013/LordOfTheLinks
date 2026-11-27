# Sliding tab animation — Navigation 3.0 compatible

**Branch:** `claude/weekly-reminder-list-integration-4snu37`
**Date:** 2026-11-27

---

## Goal

Swipe-to-switch between bottom tabs with a smooth horizontal slide animation, like the
Claude mobile app. Tabs slide left/right based on direction; swiping the content area
switches to adjacent tabs.

---

## Current architecture

```
RootScreen.kt
├── Scaffold
│   ├── bottomBar: NavigationBar (3 tabs: Feed, Library, Settings)
│   ├── floatingActionButton: FAB (Feed tab only)
│   └── content: when (currentTab) {
│       ├── FeedTab      → LibraryTabNavigation (own NavDisplay)
│       ├── LibraryTab   → OrganizeTabNavigation (own NavDisplay)
│       └── SettingsTab  → SettingsTabNavigation (own NavDisplay)
│   }
```

Tab switching is a bare `when` statement — instant swap, no animation. Each tab has its
own `NavDisplay` for within-tab navigation (shared elements, list-detail on wide screens).

**Nav 3 version:** `navigation3-ui = 1.1.1` (JetBrains multiplatform fork)

---

## NavDisplay transition API (confirmed from 1.1.1 jar)

`NavDisplay` accepts these animation parameters:

```kotlin
NavDisplay(
    backStack = ...,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform,
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform,
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.(progress: Int) -> ContentTransform,
    sizeTransform: SizeTransform?,
    sharedTransitionScope: SharedTransitionScope?,
    ...
)
```

These control **within-tab** push/pop transitions (enter a detail screen, back to list).
They are NOT suitable for the **tab switch** because:
- Tabs don't form a stack — `NavDisplay` expects push/pop semantics
- Each tab has its own `NavDisplay` instance already

---

## Chosen approach: `AnimatedContent` keyed on current tab

Wrap the existing `when` in `AnimatedContent(targetState = currentTab)`. This is the
same mechanism `NavDisplay` uses internally for its own transitions.

### Why not a root-level NavDisplay for tabs?

- Tabs are peers, not a stack. Switching from tab 1→3 would "push"; 3→1 would "pop".
  This fights the mental model.
- Each tab already has its own `NavDisplay` with shared transitions. Nesting tab switching
  inside another `NavDisplay` adds complexity for no benefit.
- `AnimatedContent` gives exact control over slide direction without stack semantics.

### Why not HorizontalPager?

- `HorizontalPager` is a Compose Foundation component, not Navigation 3.
- It composes all pages simultaneously (even with `beyondViewportPageCount = 0`, it
  still pre-composes neighbors during swipe).
- Conflicts with each tab's own `NavDisplay` lifecycle and state management.
- `AnimatedContent` is lighter and more Nav 3-idiomatic.

---

## Implementation plan

### Step 1: Track tab index for slide direction

**File:** `app/src/commonMain/kotlin/.../screens/root/RootNavigationState.kt`

```kotlin
class RootNavigationState(
    startTab: RootScreens,
    currentTab: MutableState<RootScreens>
) {
    var currentTab: RootScreens by currentTab

    val tabs: List<RootScreens>
        get() = listOf(
            RootScreens.FeedTab,
            RootScreens.LibraryTab,
            RootScreens.RemindersTab,  // NEW
            RootScreens.SettingsTab
        )

    fun indexOf(tab: RootScreens): Int = tabs.indexOf(tab)
}
```

### Step 2: AnimatedContent wrapper

**File:** `app/src/commonMain/kotlin/.../screens/root/RootScreen.kt`

Replace the bare `when` block:

```kotlin
// BEFORE
Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
    when (rootNavigator.getCurrentTab()) {
        RootScreens.FeedTab -> LibraryTabNavigation(...)
        RootScreens.LibraryTab -> OrganizeTabNavigation()
        RootScreens.SettingsTab -> SettingsTabNavigation()
    }
}

// AFTER
Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
    AnimatedContent(
        targetState = rootNavigator.getCurrentTab(),
        transitionSpec = {
            val fromIndex = rootNavigationState.indexOf(initialState)
            val toIndex = rootNavigationState.indexOf(targetState)
            if (toIndex > fromIndex) {
                // Slide left (going right in tab order)
                slideInHorizontally { fullWidth -> fullWidth } + fadeIn(
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut(
                    animationSpec = tween(300)
                )
            } else {
                // Slide right (going left in tab order)
                slideInHorizontally { fullWidth -> -fullWidth } + fadeIn(
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally { fullWidth -> fullWidth } + fadeOut(
                    animationSpec = tween(300)
                )
            } using SizeTransform(clip = false)
        },
        label = "TabSwitch",
    ) { tab ->
        when (tab) {
            RootScreens.FeedTab -> LibraryTabNavigation(
                tabNavigator = feedTabNavigator,
                onAddLink = openAddLinkSheet,
            )
            RootScreens.LibraryTab -> OrganizeTabNavigation()
            RootScreens.RemindersTab -> RemindersTabNavigation()
            RootScreens.SettingsTab -> SettingsTabNavigation()
        }
    }
}
```

**New imports needed:**
```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
```

### Step 3: Swipe gesture on content area

Add swipe detection to the `Box` wrapping `AnimatedContent`:

```kotlin
val currentTabIndex = rootNavigationState.indexOf(rootNavigator.getCurrentTab())
val tabCount = rootNavigationState.tabs.size

Box(
    modifier = Modifier
        .padding(padding)
        .consumeWindowInsets(padding)
        .pointerInput(currentTabIndex) {
            detectHorizontalDragGestures { _, dragAmount ->
                // Only register on significant drag
            }
        }
) { ... }
```

**Better approach — fling detection:**

```kotlin
.pointerInput(currentTabIndex) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragEnd = {
            if (totalDrag > 100.dp.toPx() && currentTabIndex > 0) {
                rootNavigator.switchTab(rootNavigationState.tabs[currentTabIndex - 1])
            } else if (totalDrag < -100.dp.toPx() && currentTabIndex < tabCount - 1) {
                rootNavigator.switchTab(rootNavigationState.tabs[currentTabIndex + 1])
            }
            totalDrag = 0f
        },
        onDragCancel = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
    )
}
```

### Step 4: Handle gesture conflicts

Inner screens (Feed list, Library) may have horizontal scrolling (e.g. tag chip rows,
`ChipRow` uses `AppLazyRow`). The swipe gesture should only trigger when:
- The inner content is at a scroll boundary (can't scroll further in that direction)
- Or the gesture starts from the edge of the screen

**Simple approach:** Use a velocity threshold instead of drag distance. A fast horizontal
fling (> 500 dp/s) switches tabs; slower drags are consumed by inner content.

```kotlin
detectHorizontalDragGestures(
    onDragEnd = {
        val velocity = totalDrag / dragDurationMs * 1000
        if (velocity > 500.dp.toPx()) { /* switch left */ }
        if (velocity < -500.dp.toPx()) { /* switch right */ }
    }
)
```

**Alternative:** Only detect edge swipes (first touch within 20dp of screen edge). This
avoids all conflicts with inner horizontal scroll but feels less fluid.

---

## Animation tuning

| Parameter | Value | Rationale |
|---|---|---|
| Duration | 300ms | Matches Material 3 motion spec for lateral transitions |
| Easing | `FastOutSlowIn` | Standard deceleration curve |
| Fade alpha range | 0.0 → 1.0 | Prevents content overlap during slide |
| Slide offset | Full width | Tab content slides fully out/in (like Claude app) |
| Clip | `false` | Content slides over the edge without clipping artifacts |

For a more polished feel, reduce the slide offset to 30% and increase fade:
```kotlin
slideInHorizontally { fullWidth -> (fullWidth * 0.3).toInt() } + fadeIn(tween(250))
togetherWith
slideOutHorizontally { fullWidth -> -(fullWidth * 0.3).toInt() } + fadeOut(tween(200))
```

This gives a subtle parallax effect similar to iOS tab transitions.

---

## Files touched (summary)

| File | Change |
|---|---|
| `app/.../screens/root/RootScreen.kt` | `AnimatedContent` wrapper, swipe gesture, Reminders tab branch |
| `app/.../screens/root/RootNavigationState.kt` | `indexOf()` helper, add `RemindersTab` to `tabs` list |
| `app/.../screens/root/RootScreens.kt` | Add `RemindersTab` (also needed for Task 2) |

---

## Testing

1. **Visual:** tap each tab — content slides in from the correct direction
2. **Swipe:** fling left/right on tab content — switches to adjacent tab
3. **Edge cases:** swipe at first tab (no-op), swipe at last tab (no-op)
4. **Conflict:** horizontal chip rows in Library still scroll without triggering tab switch
5. **State preservation:** switch away from a tab and back — scroll position and form state survive (each tab's `NavDisplay` and `rememberSaveable` handle this)
6. **Performance:** no jank during animation — `AnimatedContent` only composes the entering and exiting tab, not all 4
