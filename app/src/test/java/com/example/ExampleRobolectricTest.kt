package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppLaunchAndPlay() {
        // Wait for the "Play Local Multiplayer" node
        rule.onNodeWithText("Play Local Multiplayer").performClick()
        rule.waitForIdle()
        // verify something on ChessScreen?
        rule.onNodeWithText("Resign").assertExists()
    }
}
