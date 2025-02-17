package mega.privacy.android.app.presentation.recentactions.recentactionbucket

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRecentActionNodes
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.presentation.recentactions.model.RecentActionBucketUIState
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.UpdateRecentAction
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionBucketFragment]
 */
@HiltViewModel
class RecentActionBucketViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val updateRecentAction: UpdateRecentAction,
    private val getRecentActionNodes: GetRecentActionNodes,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
) : ViewModel() {
    private val _actionMode = MutableLiveData<Boolean>()

    /**
     * True if the actionMode should to be visible
     */
    val actionMode: LiveData<Boolean> = _actionMode

    private val _nodesToAnimate = MutableLiveData<Set<Int>>()

    /**
     * Set of node positions to animate
     */
    val nodesToAnimate: LiveData<Set<Int>> = _nodesToAnimate

    private val selectedNodes: MutableSet<NodeItem> = mutableSetOf()

    /**
     * Current bucket
     */
    private val _bucket: MutableStateFlow<RecentActionBucket?> = MutableStateFlow(null)
    val bucket = _bucket.asStateFlow()

    private var cachedActionList: List<RecentActionBucket>? = null

    private val _shouldCloseFragment: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * True if the fragment needs to be closed
     */
    val shouldCloseFragment: LiveData<Boolean> = _shouldCloseFragment

    /**
     * True if the parent of the bucket is an incoming shares
     */
    var isInShare = false

    /**
     *  List of node items in the current bucket
     */
    val items = _bucket
        .map { it?.let { getRecentActionNodes(it.nodes) } ?: emptyList() }
        .onEach {
            isInShare = it.firstOrNull()?.node?.let { node ->
                getParentNodeUseCase(NodeId(node.handle))?.isIncomingShare
            } ?: false
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _state = MutableStateFlow(RecentActionBucketUIState())

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorNodeUpdatesUseCase().collectLatest {
                Timber.d("Received node update")
                updateCurrentBucket()
                clearSelection()
            }
        }

        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .collect { accountDetail ->
                    _state.update {
                        it.copy(accountDetail = accountDetail)
                    }
                }
        }

        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            _state.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }
    }

    /**
     * Check if the current bucket is set
     *
     * @return true if the current bucket is set
     */
    fun isCurrentBucketSet(): Boolean = _bucket.value != null

    /**
     * Set bucket value
     *
     * @param selectedBucket
     */
    fun setBucket(selectedBucket: RecentActionBucket?) = viewModelScope.launch {
        _bucket.emit(selectedBucket)
    }

    /**
     * Set cached action list
     *
     * @param cachedActions
     */
    fun setCachedActionList(cachedActions: List<RecentActionBucket>?) {
        cachedActionList = cachedActions
    }

    /**
     * Get the selected nodes
     *
     * @return the selected nodes
     */
    fun getSelectedNodes(): List<NodeItem> = selectedNodes.toList()

    /**
     * Retrieves the list of non-null [MegaNode] objects from [selectedNodes]
     *
     * @return A list of nun-null [MegaNode] objects
     */
    fun getSelectedMegaNodes(): List<MegaNode> =
        selectedNodes.toList().mapNotNull { it.node }

    /**
     * Checks whether any [MegaNode] in [getSelectedMegaNodes] belongs in Backups
     *
     * @return True if at least one [MegaNode] belongs in Backups, and False if otherwise
     */
    fun isAnyNodeInBackups(): Boolean =
        getSelectedMegaNodes().any { node -> megaApi.isInInbox(node) }

    /**
     * Get the count of selected nodes
     *
     * @return the count of selected nodes
     */
    fun getSelectedNodesCount(): Int = selectedNodes.size

    /**
     * Get the count of nodes
     *
     * @return the count of nodes
     */
    fun getNodesCount(): Int = items.value.size

    /**
     * Clear selected nodes
     */
    fun clearSelection() {
        _actionMode.value = false
        selectedNodes.clear()

        val animNodeIndices = mutableSetOf<Int>()
        val nodeList = items.value

        for ((position, node) in nodeList.withIndex()) {
            if (node in selectedNodes) {
                animNodeIndices.add(position)
            }
            node.selected = false
            node.uiDirty = true
        }

        _nodesToAnimate.value = animNodeIndices
    }

    /**
     * Receive on node long click
     *
     * @param position the position of the item in the adapter
     * @param node the node item
     */
    fun onNodeLongClicked(position: Int, node: NodeItem) {
        val nodeList = items.value

        if (position < 0 || position >= nodeList.size || nodeList[position].hashCode() != node.hashCode()
        ) {
            return
        }

        nodeList[position].selected = !nodeList[position].selected

        if (nodeList[position] !in selectedNodes) {
            selectedNodes.add(node)
        } else {
            selectedNodes.remove(node)
        }

        nodeList[position].uiDirty = true
        _actionMode.value = selectedNodes.isNotEmpty()

        _nodesToAnimate.value = hashSetOf(position)
    }

    /**
     * Select all nodes
     */
    fun selectAll() {
        val nodeList = items.value

        val animNodeIndices = mutableSetOf<Int>()

        for ((position, node) in nodeList.withIndex()) {
            if (!node.selected) {
                animNodeIndices.add(position)
            }
            node.selected = true
            node.uiDirty = true
            selectedNodes.add(node)
        }

        _nodesToAnimate.value = animNodeIndices
        _actionMode.value = true
    }

    /**
     * Update the current bucket
     */
    private suspend fun updateCurrentBucket() {
        _bucket.value
            ?.let { updateRecentAction(it, cachedActionList) }
            ?.let { _bucket.emit(it) }
            ?: run {
                // No nodes contained in the bucket or the action bucket is no loner exists.
                _shouldCloseFragment.postValue(true)
            }
    }

    fun hideOrUnhideNodes(hide: Boolean) = viewModelScope.launch {
        getSelectedNodes().forEach {
            it.node?.let { node ->
                async {
                    runCatching {
                        updateNodeSensitiveUseCase(nodeId = NodeId(node.handle), isSensitive = hide)
                    }.onFailure { throwable -> Timber.e("Update sensitivity failed: $throwable") }
                }
            }
        }
    }

    /**
     * Mark hidden nodes onboarding has shown
     */
    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }
}
