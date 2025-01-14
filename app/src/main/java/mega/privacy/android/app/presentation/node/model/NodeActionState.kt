package mega.privacy.android.app.presentation.node.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.node.FileNodeContent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * State for NodeActionsViewModel
 *
 * @property selectedNodes Selected nodes
 * @property error Error
 * @property nodeNameCollisionResult Node name collision result
 * @property showForeignNodeDialog Show foreign node dialog
 * @property showQuotaDialog Show quota dialog
 * @property accessPermissionIcon Access permission icon
 * @property shareInfo Share info
 * @property outgoingShares Outgoing shares
 * @property contactsData Contacts data
 * @property downloadEvent Download event
 * @property selectAll Select All event
 * @property clearAll clear All Event
 */
data class NodeActionState(
    val selectedNodes: List<TypedNode> = emptyList(),
    val error: StateEventWithContent<Throwable> = consumed(),
    val nodeNameCollisionResult: StateEventWithContent<NodeNameCollisionResult> = consumed(),
    val showForeignNodeDialog: StateEvent = consumed,
    val showQuotaDialog: StateEventWithContent<Boolean> = consumed(),
    val accessPermissionIcon: Int? = null,
    val shareInfo: String? = null,
    val outgoingShares: List<ShareData> = emptyList(),
    val contactsData: StateEventWithContent<Triple<List<String>, Boolean, String>> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val selectAll: StateEvent = consumed,
    val clearAll: StateEvent = consumed,
)
