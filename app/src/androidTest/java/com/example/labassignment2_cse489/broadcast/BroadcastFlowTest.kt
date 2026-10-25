package com.example.labassignment2_cse489.broadcast

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.labassignment2_cse489.MainActivity
import com.example.labassignment2_cse489.R
import com.example.labassignment2_cse489.finishActivitiesAboveMain
import com.example.labassignment2_cse489.hasTextInputError
import com.example.labassignment2_cse489.waitUntil
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Broadcast Receiver flow end to end: Screen 1 -> Activity 2 -> Activity 3, for both
 * spinner options.
 */
@RunWith(AndroidJUnit4::class)
class BroadcastFlowTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @After
    fun closeOpenedActivities() = finishActivitiesAboveMain()

    @Test
    fun spinner_hasTheTwoBroadcastTypes() {
        selectBroadcastType(CUSTOM)
        onView(withId(R.id.broadcast_type_spinner)).check(matches(withSpinnerText(CUSTOM)))
        selectBroadcastType(BATTERY)
        onView(withId(R.id.broadcast_type_spinner)).check(matches(withSpinnerText(BATTERY)))
    }

    @Test
    fun customFlow_textTypedInActivity2_isReceivedByReceiverInActivity3() {
        selectBroadcastType(CUSTOM)
        onView(withId(R.id.proceed_button)).perform(click())

        // Activity 2: type the message and send it.
        onView(withId(R.id.message_input)).perform(typeText(MESSAGE), closeSoftKeyboard())
        onView(withId(R.id.send_button)).perform(click())

        // Activity 3: CustomMessageReceiver is registered and shows the broadcast text.
        onView(withId(R.id.receiver_status))
            .check(matches(withText("CustomMessageReceiver is registered")))
        onView(withId(R.id.receiver_action))
            .check(matches(withText("Action: ${CustomMessageReceiver.ACTION_CUSTOM_MESSAGE}")))
        waitUntil {
            onView(withId(R.id.received_message))
                .check(matches(withText("Received via broadcast: $MESSAGE")))
        }
    }

    @Test
    fun customFlow_keyboardSendAction_opensActivity3() {
        selectBroadcastType(CUSTOM)
        onView(withId(R.id.proceed_button)).perform(click())

        onView(withId(R.id.message_input)).perform(typeText(MESSAGE), pressImeActionButton())

        waitUntil {
            onView(withId(R.id.received_message))
                .check(matches(withText("Received via broadcast: $MESSAGE")))
        }
    }

    @Test
    fun customFlow_blankMessage_showsInlineErrorAndStays() {
        selectBroadcastType(CUSTOM)
        onView(withId(R.id.proceed_button)).perform(click())

        onView(withId(R.id.message_input)).perform(typeText("   "), closeSoftKeyboard())
        onView(withId(R.id.send_button)).perform(click())

        onView(withId(R.id.message_layout))
            .check(matches(hasTextInputError("Please type a message first")))
        onView(withId(R.id.send_button)).check(matches(isDisplayed()))
    }

    @Test
    fun batteryFlow_showsPercentage_andActivity3DoesNothing() {
        selectBroadcastType(BATTERY)
        onView(withId(R.id.proceed_button)).perform(click())

        // ACTION_BATTERY_CHANGED is sticky, so a value is shown straight away.
        waitUntil { onView(withId(R.id.battery_percent)).check(matches(withText(endsWith("%")))) }
        onView(withId(R.id.next_button)).perform(click())

        onView(withId(R.id.nothing_heading)).check(matches(withText("Nothing to do here")))
    }

    /** Opens the spinner and picks the item with the given [label]. */
    private fun selectBroadcastType(label: String) {
        onView(withId(R.id.broadcast_type_spinner)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`(label))).perform(click())
    }

    private companion object {
        const val CUSTOM = "Custom broadcast receiver"
        const val BATTERY = "System battery notification receiver"
        const val MESSAGE = "Hello CSE489"
    }
}
