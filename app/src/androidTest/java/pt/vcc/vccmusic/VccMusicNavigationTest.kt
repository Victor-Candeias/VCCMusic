package pt.vcc.vccmusic

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class VccMusicNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startsAtMainMenuAndNavigatesAcrossTopLevelDestinations() {
        composeRule.onNodeWithTag("screen-main-menu").assertIsDisplayed()

        composeRule.onNodeWithTag("action-music").performClick()
        composeRule.onNodeWithTag("screen-library").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom-playlists").performClick()
        composeRule.onNodeWithTag("screen-playlists").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom-folders").performClick()
        composeRule.onNodeWithTag("screen-library").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom-now-playing").performClick()
        composeRule.onNodeWithTag("screen-now-playing").assertIsDisplayed()
    }

}
