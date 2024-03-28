package mega.privacy.android.app.presentation.recentactions.model

import com.brandongogetap.stickyheaders.exposed.StickyHeader
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import nz.mega.sdk.MegaRecentActionBucket

/**
 *  Define the different recent action item type hold by [mega.privacy.android.app.presentation.recentactions.RecentActionsAdapter]
 *
 *  @property timestamp the timestamp of the item
 */
@Deprecated("Should be removed when Compose implementation is released")
sealed class RecentActionItemType(val timestamp: Long) {

    /**
     *  Define a generic item hold by [mega.privacy.android.app.presentation.recentactions.RecentActionsAdapter]
     *
     *  @property bucket a [MegaRecentActionBucket]
     *  @property userName the name of the user associated to the bucket
     *  @property parentFolderName the name of the parent folder containing the nodes
     *  @property parentFolderSharesType the share type of the parent folder
     *  @property currentUserIsOwner true if the current user is the owner of the recent actions
     *  @property isKeyVerified true if node.isNodeKeyDecrypted & areCredentialsVerified returns true
     */
    class Item(
        val bucket: RecentActionBucket,
        val userName: String = "",
        val parentFolderName: String = "",
        val parentFolderSharesType: RecentActionsSharesType = RecentActionsSharesType.NONE,
        val currentUserIsOwner: Boolean = false,
        val isKeyVerified: Boolean = false,
    ) : RecentActionItemType(timestamp = bucket.timestamp)

    /**
     * Define a header hold by [mega.privacy.android.app.presentation.recentactions.RecentActionsAdapter]
     */
    class Header(_timestamp: Long) :
        RecentActionItemType(timestamp = _timestamp), StickyHeader
}

