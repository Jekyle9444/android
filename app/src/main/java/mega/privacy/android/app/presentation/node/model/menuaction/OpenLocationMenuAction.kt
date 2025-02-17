package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Open location menu action
 */
class OpenLocationMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = painterResource(id = iconPackR.drawable.ic_folder_open_medium_regular_outline)

    @Composable
    override fun getDescription() = stringResource(id = R.string.search_open_location)

    override val orderInCategory: Int
        get() = 130
    override val testTag: String
        get() = "menu_action:open_location"
}