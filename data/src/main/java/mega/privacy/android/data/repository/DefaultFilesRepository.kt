package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.APP_DATA_BACKGROUND_TRANSFER
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getFileName
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.MegaShareMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.ViewerNode
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NullFileException
import mega.privacy.android.domain.exception.SynchronisationException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequest
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [FilesRepository]
 *
 * @property context
 * @property megaApiGateway
 * @property megaApiFolderGateway
 * @property megaChatApiGateway
 * @property ioDispatcher
 * @property megaLocalStorageGateway
 * @property megaShareMapper
 * @property megaExceptionMapper
 * @property sortOrderIntMapper
 * @property cacheFolderGateway
 * @property nodeMapper
 * @property fileTypeInfoMapper
 * @property offlineNodeInformationMapper
 * @property fileGateway
 * @property chatFilesFolderUserAttributeMapper
 */
internal class DefaultFilesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val megaShareMapper: MegaShareMapper,
    private val megaExceptionMapper: MegaExceptionMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val cacheFolderGateway: CacheFolderGateway,
    private val nodeMapper: NodeMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper,
    private val fileGateway: FileGateway,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
) : FilesRepository, FileRepository {

    override suspend fun copyNode(
        nodeToCopy: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String,
    ): NodeId = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.copyNode(
                nodeToCopy = nodeToCopy,
                newNodeParent = newNodeParent,
                newNodeName = newNodeName,
                listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK && request.type == MegaRequest.TYPE_COPY) {
                            continuation.resumeWith(Result.success(NodeId(request.nodeHandle)))
                        } else {
                            continuation.failWithError(error)
                        }
                    }
                )
            )
        }
    }

    @Throws(MegaException::class)
    override suspend fun getRootFolderVersionInfo(): FolderVersionInfo =
        withContext(ioDispatcher) {
            val rootNode = megaApiGateway.getRootNode()
            suspendCoroutine { continuation ->
                megaApiGateway.getFolderInfo(
                    rootNode,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestFolderInfoCompleted(continuation)
                    )
                )
            }
        }

    private fun onRequestFolderInfoCompleted(continuation: Continuation<FolderVersionInfo>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(
                    Result.success(
                        with(request.megaFolderInfo) {
                            FolderVersionInfo(
                                numVersions,
                                versionsSize
                            )
                        }
                    )
                )
            } else {
                continuation.failWithError(error)
            }
        }

    override suspend fun getRootNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getRootNode()
    }

    override suspend fun getInboxNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getInboxNode()
    }

    override suspend fun getRubbishBinNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getRubbishBinNode()
    }

    override suspend fun isInRubbish(node: MegaNode): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isInRubbish(node)
    }

    override suspend fun getParentNode(node: MegaNode): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getParentNode(node)
    }

    override suspend fun getChildNode(parentNode: MegaNode?, name: String?): MegaNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getChildNode(parentNode, name)
        }

    override suspend fun getChildrenNode(parentNode: MegaNode, order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getChildrenByNode(parentNode, sortOrderIntMapper(order))
        }

    override suspend fun getNodeByPath(path: String?, megaNode: MegaNode?): MegaNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getNodeByPath(path, megaNode)
        }

    override suspend fun getNodeByHandle(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)
    }

    override suspend fun getFingerprint(filePath: String): String? = withContext(ioDispatcher) {
        megaApiGateway.getFingerprint(filePath)
    }

    override suspend fun getNodesByOriginalFingerprint(
        originalFingerprint: String,
        parentNode: MegaNode?,
    ): MegaNodeList? = withContext(ioDispatcher) {
        megaApiGateway.getNodesByOriginalFingerprint(originalFingerprint, parentNode)
    }

    override suspend fun getNodeByFingerprintAndParentNode(
        fingerprint: String,
        parentNode: MegaNode?,
    ): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getNodeByFingerprintAndParentNode(fingerprint, parentNode)
    }

    override suspend fun getNodeByFingerprint(fingerprint: String): MegaNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getNodeByFingerprint(fingerprint)
        }

    override suspend fun setOriginalFingerprint(node: MegaNode, originalFingerprint: String) {
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.setOriginalFingerprint(
                    node = node,
                    originalFingerprint = originalFingerprint,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK && request.type == MegaRequest.TYPE_SET_ATTR_NODE) {
                                continuation.resumeWith(Result.success(Unit))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    )
                )
            }
        }
    }

    override suspend fun getIncomingSharesNode(order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getIncomingSharesNode(sortOrderIntMapper(order))
        }

    override suspend fun getOutgoingSharesNode(order: SortOrder): List<ShareData> =
        withContext(ioDispatcher) {
            megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(order))
                .map { megaShareMapper(it) }
        }

    override suspend fun isNodeInRubbish(handle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let { megaApiGateway.isInRubbish(it) } ?: false
    }

    override suspend fun authorizeNode(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiFolderGateway.authorizeNode(handle)
    }

    override suspend fun getPublicLinks(order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getPublicLinks(sortOrderIntMapper(order))
        }

    override suspend fun isPendingShare(node: MegaNode): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isPendingShare(node)
    }

    override suspend fun hasInboxChildren(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.getInboxNode()?.let { megaApiGateway.hasChildren(it) } ?: false
    }

    override suspend fun downloadBackgroundFile(viewerNode: ViewerNode): String =
        withContext(ioDispatcher) {
            getMegaNode(viewerNode)?.let { node ->
                suspendCoroutine { continuation ->
                    val file = cacheFolderGateway.getCacheFile(CacheFolderConstant.TEMPORARY_FOLDER,
                        node.getFileName())
                    if (file == null) {
                        continuation.resumeWith(Result.failure(NullFileException()))
                        return@suspendCoroutine
                    }

                    megaApiGateway.startDownload(
                        node = node,
                        localPath = file.absolutePath,
                        fileName = file.name,
                        appData = APP_DATA_BACKGROUND_TRANSFER,
                        startFirst = true,
                        cancelToken = null,
                        listener = OptionalMegaTransferListenerInterface(
                            onTransferTemporaryError = { _, error ->
                                continuation.failWithError(error)
                            },
                            onTransferFinish = { _, error ->
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(file.absolutePath))
                                } else {
                                    continuation.failWithError(error)
                                }
                            }
                        )
                    )
                }
            } ?: throw NullPointerException()
        }

    suspend fun getMegaNode(viewerNode: ViewerNode): MegaNode? = withContext(ioDispatcher) {
        when (viewerNode) {
            is ViewerNode.ChatNode -> getMegaNodeFromChat(viewerNode)
            is ViewerNode.FileLinkNode -> MegaNode.unserialize(viewerNode.serializedNode)
            is ViewerNode.FolderLinkNode -> getMegaNodeFromFolderLink(viewerNode)
            is ViewerNode.GeneralNode -> megaApiGateway.getMegaNodeByHandle(viewerNode.id)
        }
    }

    private suspend fun getMegaNodeFromChat(chatNode: ViewerNode.ChatNode) =
        withContext(ioDispatcher) {
            with(chatNode) {
                val messageChat = megaChatApiGateway.getMessage(chatId, messageId)
                    ?: megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId)

                if (messageChat != null) {
                    val node = messageChat.megaNodeList.get(0)
                    val chat = megaChatApiGateway.getChatRoom(chatId)

                    if (chat?.isPreview == true) {
                        megaApiGateway.authorizeChatNode(node, chat.authorizationToken)
                    } else {
                        node
                    }
                } else null
            }
        }

    private suspend fun getMegaNodeFromFolderLink(folderLinkNode: ViewerNode.FolderLinkNode) =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(folderLinkNode.id)?.let {
                megaApiFolderGateway.authorizeNode(it)
            }
        }

    override suspend fun checkAccessErrorExtended(node: MegaNode, level: Int): MegaException =
        withContext(ioDispatcher) {
            megaExceptionMapper(megaApiGateway.checkAccessErrorExtended(node, level))
        }

    override suspend fun getBackupFolderId(): NodeId =
        withContext(ioDispatcher) {
            val backupsFolderAttributeIdentifier = MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER
            suspendCancellableCoroutine { continuation ->
                megaApiGateway.getUserAttribute(backupsFolderAttributeIdentifier,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (request.paramType == backupsFolderAttributeIdentifier) {
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(NodeId(request.nodeHandle)))
                                } else {
                                    continuation.failWithError(error)
                                }
                            }
                        }
                    ))
            }
        }

    override suspend fun getNodeById(nodeId: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.id)?.let {
            nodeMapper(
                it,
                cacheFolderGateway::getThumbnailCacheFilePath,
                megaApiGateway::hasVersion,
                megaApiGateway::getNumChildFolders,
                megaApiGateway::getNumChildFiles,
                fileTypeInfoMapper,
                megaApiGateway::isPendingShare,
                megaApiGateway::isInRubbish,
            )
        }
    }

    override suspend fun getNodeChildren(folderNode: FolderNode): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(folderNode.id.id)?.let { parent ->
                megaApiGateway.getChildrenByNode(parent)
                    .map {
                        nodeMapper(
                            it,
                            cacheFolderGateway::getThumbnailCacheFilePath,
                            megaApiGateway::hasVersion,
                            megaApiGateway::getNumChildFolders,
                            megaApiGateway::getNumChildFiles,
                            fileTypeInfoMapper,
                            megaApiGateway::isPendingShare,
                            megaApiGateway::isInRubbish,
                        )
                    }
            } ?: throw SynchronisationException("Non null node found be null when fetched from api")
        }
    }

    override fun monitorNodeUpdates(): Flow<List<Node>> {
        return megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnNodesUpdate>()
            .mapNotNull {
                it.nodeList?.map { megaNode ->
                    nodeMapper(
                        megaNode,
                        cacheFolderGateway::getThumbnailCacheFilePath,
                        megaApiGateway::hasVersion,
                        megaApiGateway::getNumChildFolders,
                        megaApiGateway::getNumChildFiles,
                        fileTypeInfoMapper,
                        megaApiGateway::isPendingShare,
                        megaApiGateway::isInRubbish,
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun isNodeInRubbishOrDeleted(nodeHandle: Long): Boolean =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeHandle)?.let { megaApiGateway.isInRubbish(it) }
                ?: true
        }

    override suspend fun getOfflineNodeInformation(nodeId: NodeId) =
        withContext(ioDispatcher) {
            megaLocalStorageGateway.getOfflineInformation(nodeId.id)
                ?.let { offlineNodeInformationMapper(it) }
        }

    override suspend fun getOfflinePath() =
        withContext(ioDispatcher) { fileGateway.getOfflineFilesRootPath() }

    override suspend fun getOfflineInboxPath() =
        withContext(ioDispatcher) { fileGateway.getOfflineFilesInboxRootPath() }

    override suspend fun createFolder(name: String) = withContext(ioDispatcher) {
        val megaNode = megaApiGateway.getRootNode()
        megaNode?.let { parentMegaNode ->
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener { it.nodeHandle }
                megaApiGateway.createFolder(name, parentMegaNode, listener)
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
    }

    override suspend fun setMyChatFilesFolder(nodeHandle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener {
                chatFilesFolderUserAttributeMapper(it.megaStringMap)?.let { value ->
                    megaApiGateway.base64ToHandle(value)
                        .takeIf { handle -> handle != megaApiGateway.getInvalidHandle() }
                }
            }
            megaApiGateway.setMyChatFilesFolder(nodeHandle, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun getUnverifiedIncomingShares(order: SortOrder): List<ShareData> =
        withContext(ioDispatcher) {
            megaApiGateway.getUnverifiedIncomingShares(sortOrderIntMapper(order)).map {
                megaShareMapper(it)
            }
        }

    override suspend fun getUnverifiedOutgoingShares(order: SortOrder): List<ShareData> =
        withContext(ioDispatcher) {
            megaApiGateway.getUnverifiedOutgoingShares(sortOrderIntMapper(order)).map {
                megaShareMapper(it)
            }
        }

    override suspend fun openShareDialog(megaNode: MegaNode) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener { return@getRequestListener }
            megaApiGateway.openShareDialog(megaNode, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun upgradeSecurity() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener { return@getRequestListener }
            megaApiGateway.upgradeSecurity(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun setSecureFlag(enable: Boolean) = withContext(ioDispatcher) {
        megaApiGateway.setSecureFlag(enable)
    }
}
