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
    fun startsAtLibraryAndNavigatesAcrossTopLevelDestinations() {
        composeRule.onNodeWithTag("screen-library").assertIsDisplayed()

        composeRule.onNodeWithText("Playlists").performClick()
        composeRule.onNodeWithTag("screen-playlists").assertIsDisplayed()

        composeRule.onNodeWithText("Em reprodução").performClick()
        composeRule.onNodeWithTag("screen-now-playing").assertIsDisplayed()
    }
}
