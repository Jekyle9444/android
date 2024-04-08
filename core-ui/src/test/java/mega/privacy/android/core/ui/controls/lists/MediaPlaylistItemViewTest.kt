package mega.privacy.android.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MediaPlaylistItemViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testIcon = R.drawable.ic_audio_medium_solid
    private val testName = "Media Name"
    private val testCurrentPlaylistPosition = "00:00"
    private val testDuration = "10:00"

    private fun setComposeContent(
        @DrawableRes icon: Int = testIcon,
        name: String = testName,
        currentPlaylistPosition: String = testCurrentPlaylistPosition,
        duration: String = testDuration,
        thumbnailData: Any? = null,
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier,
        isPaused: Boolean = false,
        isItemPlaying: Boolean = false,
        isSelected: Boolean = false,
        isReorderEnabled: Boolean = true,
    ) {
        composeTestRule.setContent {
            MediaPlaylistItemView(
                icon = icon,
                name = name,
                currentPlaylistPosition = currentPlaylistPosition,
                duration = duration,
                thumbnailData = thumbnailData,
                onClick = onClick,
                modifier = modifier,
                isPaused = isPaused,
                isItemPlaying = isItemPlaying,
                isSelected = isSelected,
                isReorderEnabled = isReorderEnabled
            )
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when default parameters are set`() {
        setComposeContent()

        listOf(
            MEDIA_PLAYLIST_ITEM_VIEW_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_REORDER_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG.textEquals(testName)
        MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG.textEquals(testDuration)

        listOf(
            MEDIA_PLAYLIST_ITEM_SELECT_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.doesNotExist()
        }
    }

    private fun String.isDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.textEquals(expectedText: String) =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true)
            .assertTextEquals(expectedText)

    private fun String.doesNotExist() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertDoesNotExist()

    @Test
    fun `test that the UIs are displayed correctly when isSelected is true`() {
        setComposeContent(isSelected = true)

        listOf(
            MEDIA_PLAYLIST_ITEM_VIEW_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_SELECT_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_REORDER_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG.textEquals(testName)
        MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG.textEquals(testDuration)

        listOf(
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.doesNotExist()
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when isReorderEnabled is false`() {
        setComposeContent(isReorderEnabled = false)

        listOf(
            MEDIA_PLAYLIST_ITEM_VIEW_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG.textEquals(testName)
        MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG.textEquals(testDuration)

        listOf(
            MEDIA_PLAYLIST_ITEM_SELECT_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_REORDER_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.doesNotExist()
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when the current item is playing`() {
        setComposeContent(isItemPlaying = true)

        listOf(
            MEDIA_PLAYLIST_ITEM_VIEW_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_ICON_TEST_TAG,
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG.textEquals(testName)
        val expectedDuration = "$testCurrentPlaylistPosition / $testDuration"
        MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG.textEquals(expectedDuration)

        listOf(
            MEDIA_PLAYLIST_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_SELECT_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_REORDER_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.doesNotExist()
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when playing item is paused`() {
        setComposeContent(isPaused = true, isItemPlaying = true)

        listOf(
            MEDIA_PLAYLIST_ITEM_VIEW_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_THUMBNAIL_ICON_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_PLAYLIST_ITEM_PAUSED_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG.textEquals(testName)
        val expectedDuration = "$testCurrentPlaylistPosition / $testDuration"
        MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG.textEquals(expectedDuration)

        MEDIA_PLAYLIST_ITEM_SELECT_ICON_TEST_TAG.doesNotExist()
        MEDIA_PLAYLIST_ITEM_REORDER_ICON_TEST_TAG.doesNotExist()
    }

    @Test
    fun `test that onClick is invoked when the item is clicked`() {
        val onClick = Mockito.mock<() -> Unit>()
        setComposeContent(onClick = onClick)

        composeTestRule.onNodeWithTag(MEDIA_PLAYLIST_ITEM_VIEW_TEST_TAG, true).performClick()
        verify(onClick).invoke()
    }
}