package com.example.labassignment2_cse489

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.navigation.NavigationView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.navigation.ui.R as NavigationUiR

/**
 * The navigation drawer: four items in the brief's order, each opening its screen with the
 * hamburger icon, the right title and the item highlighted, and Back closing an open drawer.
 */
@RunWith(AndroidJUnit4::class)
class DrawerNavigationTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var decorView: View

    @Before
    fun rememberDecorView() {
        scenarioRule.scenario.onActivity { activity -> decorView = activity.window.decorView }
    }

    @Test
    fun drawer_listsFourItemsInOrderWithExactLabels() {
        scenarioRule.scenario.onActivity { activity ->
            val menu = activity.findViewById<NavigationView>(R.id.nav_view).menu
            val titles = menu.children.map { it.title.toString() }.toList()
            assertEquals(listOf("Broadcast Receiver", "Image Scale", "Video", "Audio"), titles)
        }
    }

    @Test
    fun app_startsOnBroadcastReceiver() {
        assertOnScreen(R.id.broadcastFragment, "Broadcast Receiver")
        onMain(withId(R.id.select_label)).check(matches(withText("Select a broadcast type")))
    }

    @Test
    fun drawer_opensImageScale() {
        navigateTo(R.id.imageScaleFragment)
        assertOnScreen(R.id.imageScaleFragment, "Image Scale")
    }

    @Test
    fun drawer_opensVideo() {
        navigateTo(R.id.videoFragment)
        assertOnScreen(R.id.videoFragment, "Video")
    }

    @Test
    fun drawer_opensAudio() {
        navigateTo(R.id.audioFragment)
        assertOnScreen(R.id.audioFragment, "Audio")
    }

    @Test
    fun drawer_visitsEveryScreenAndReturnsToBroadcastReceiver() {
        navigateTo(R.id.imageScaleFragment)
        navigateTo(R.id.videoFragment)
        navigateTo(R.id.audioFragment)
        navigateTo(R.id.broadcastFragment)
        assertOnScreen(R.id.broadcastFragment, "Broadcast Receiver")
    }

    @Test
    fun hamburger_opensDrawer_andBackClosesItWithoutLeavingTheApp() {
        onMain(withContentDescription(NavigationUiR.string.nav_app_bar_open_drawer_description))
            .perform(click())
        waitUntil { onMain(withId(R.id.drawer_layout)).check(matches(isOpen())) }

        pressBack()

        waitUntil { onMain(withId(R.id.drawer_layout)).check(matches(isClosed())) }
        assertEquals(Lifecycle.State.RESUMED, scenarioRule.scenario.state)
        assertOnScreen(R.id.broadcastFragment, "Broadcast Receiver")
    }

    // ---- Helpers ------------------------------------------------------------------------------

    /**
     * Looks for views in the activity's own window. The Video screen's MediaController draws in
     * a separate panel window, which Espresso would otherwise pick as the root to search. While
     * the controls are up the activity window has no focus, so Espresso waits for them to hide.
     */
    private fun onMain(matcher: Matcher<View>): ViewInteraction =
        onView(matcher).inRoot(withDecorView(`is`(decorView)))

    /** Opens the drawer and taps the item with [destinationId], as a user would. */
    private fun navigateTo(@IdRes destinationId: Int) {
        onMain(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onMain(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(destinationId))
        // NavigationUI closes the drawer after the tap. Wait until it is fully closed: while it
        // slides away it still counts as open, so DrawerActions.open() would do nothing.
        waitUntil { onMain(withId(R.id.drawer_layout)).check(matches(isClosed())) }
    }

    /**
     * Checks that [destinationId] is the current screen: the toolbar shows [title] and the
     * hamburger icon (every drawer item is top-level), and the drawer highlights its item.
     */
    private fun assertOnScreen(@IdRes destinationId: Int, title: String) {
        onMain(allOf(isAssignableFrom(TextView::class.java), withParent(withId(R.id.toolbar))))
            .check(matches(withText(title)))
        onMain(withContentDescription(NavigationUiR.string.nav_app_bar_open_drawer_description))
            .check(matches(isDisplayed()))

        scenarioRule.scenario.onActivity { activity ->
            val navHost = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            assertEquals(destinationId, navHost.navController.currentDestination?.id)
            val navView = activity.findViewById<NavigationView>(R.id.nav_view)
            assertEquals(destinationId, navView.checkedItem?.itemId)
        }
    }
}
