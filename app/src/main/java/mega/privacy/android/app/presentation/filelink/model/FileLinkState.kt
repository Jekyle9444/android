package mega.privacy.android.app.presentation.filelink.model

import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.PublicNodeException

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.filelink.FileLinkActivity]
 *
 * @property shouldLogin            Whether to show login screen
 * @property url                    Url of the file
 * @property fileNode               Current node
 * @property previewPath            Path of the preview image
 * @property iconResource           the icon resource that represents this node
 * @property askForDecryptionDialog Whether to show AskForDecryptionDialog
 * @property collision              Node with existing names
 * @property copyThrowable          Throwable error on copy
 * @property collisionCheckThrowable Throwable error on collision check
 * @property copySuccess            Whether copy was success or not
 * @property fetchPublicNodeError   Exception while fetching current public node
 */
data class FileLinkState(
    val shouldLogin: Boolean? = null,
    val url: String? = null,
    val fileNode: TypedFileNode? = null,
    val previewPath: String? = null,
    val iconResource: Int? = null,
    val askForDecryptionDialog: Boolean = false,
    val collision: NameCollision? = null,
    val collisionCheckThrowable: Throwable? = null,
    val copyThrowable: Throwable? = null,
    val copySuccess: Boolean = false,
    val fetchPublicNodeError: PublicNodeException? = null,
)
