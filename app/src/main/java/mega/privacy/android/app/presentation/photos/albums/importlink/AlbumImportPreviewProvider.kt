package mega.privacy.android.app.presentation.photos.albums.importlink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.MediaDiscoveryImageNodeFetcher.Companion.IS_RECURSIVE
import mega.privacy.android.app.presentation.imagepreview.fetcher.MediaDiscoveryImageNodeFetcher.Companion.PARENT_ID
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetAlbumPhotoFileUrlByNodeIdUseCase
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import java.io.File
import javax.inject.Inject

/**
 * AlbumImport Preview help class
 */
class AlbumImportPreviewProvider @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getAlbumPhotoFileUrlByNodeIdUseCase: GetAlbumPhotoFileUrlByNodeIdUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
) {

    /**
     * onPreviewPhoto
     */
    fun onPreviewPhoto(
        activity: Activity,
        photo: Photo,
    ) {
        if (photo is Photo.Video) {
            (activity as LifecycleOwner).lifecycleScope.launch {
                launchVideoScreenFromAlbumSharing(activity = activity, photo = photo)
            }
        } else {
            startImagePreviewFromAlbumSharing(activity = activity, photo = photo)
        }
    }

    fun onPreviewPhotoFromMD(
        activity: Activity,
        photo: Photo,
        photoIds: List<Long>,
        currentSort: Sort,
        isFolderLink: Boolean = false,
        folderNodeId: Long? = null,
    ) {
        if (photo is Photo.Video) {
            (activity as LifecycleOwner).lifecycleScope.launch {
                launchVideoScreenFromMD(
                    activity = activity,
                    photo = photo,
                    currentSort = currentSort,
                    isFolderLink = isFolderLink,
                    folderNodeId = folderNodeId,
                )
            }
        } else {
            startImagePreviewFromMD(
                activity = activity,
                photoIds = photoIds,
                photo = photo,
                folderNodeId = folderNodeId,
            )
        }
    }

    private fun startImagePreviewFromMD(
        activity: Activity,
        photoIds: List<Long>,
        photo: Photo,
        folderNodeId: Long?,
    ) {
        (activity as LifecycleOwner).lifecycleScope.launch {
            folderNodeId?.let { parentID ->
                monitorSubFolderMediaDiscoverySettingsUseCase().collectLatest { recursive ->
                    ImagePreviewActivity.createIntent(
                        context = activity,
                        imageSource = ImagePreviewFetcherSource.MEDIA_DISCOVERY,
                        menuOptionsSource = ImagePreviewMenuSource.MEDIA_DISCOVERY,
                        anchorImageNodeId = NodeId(photo.id),
                        params = mapOf(PARENT_ID to parentID, IS_RECURSIVE to recursive),
                        showScreenLabel = false,
                    ).run {
                        activity.startActivity(this)
                    }
                }
            }
        }
    }

    private fun startImagePreviewFromAlbumSharing(
        activity: Activity,
        photo: Photo,
    ) = (activity as LifecycleOwner).lifecycleScope.launch {
        val intent = ImagePreviewActivity.createIntent(
            context = activity,
            imageSource = ImagePreviewFetcherSource.ALBUM_SHARING,
            menuOptionsSource = ImagePreviewMenuSource.ALBUM_SHARING,
            anchorImageNodeId = NodeId(photo.id),
            showScreenLabel = false,
        )
        activity.startActivity(intent)
    }

    /**
     * Launch video player
     *
     * @param activity
     * @param photo Photo item
     */
    private suspend fun launchVideoScreenFromAlbumSharing(activity: Activity, photo: Photo) {
        val nodeHandle = photo.id
        val nodeName = photo.name
        val intent = Util.getMediaIntent(activity, nodeName).apply {
            putExtra(Constants.INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(
                Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                Constants.FROM_ALBUM_SHARING
            )
            putExtra(
                Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                getNodeParentHandle(nodeHandle)
            )
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        activity.startActivity(
            isLocalFile(nodeHandle)?.let { localPath ->
                File(localPath).let { mediaFile ->
                    kotlin.runCatching {
                        FileProvider.getUriForFile(
                            activity,
                            Constants.AUTHORITY_STRING_FILE_PROVIDER,
                            mediaFile
                        )
                    }.onFailure {
                        Uri.fromFile(mediaFile)
                    }.map { mediaFileUri ->
                        intent.setDataAndType(
                            mediaFileUri,
                            MimeTypeList.typeForName(nodeName).type
                        )
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                }
                intent
            } ?: updateIntent(
                handle = nodeHandle,
                name = nodeName,
                intent = intent,
                isAlbumSharing = true
            )
        )
    }

    private suspend fun launchVideoScreenFromMD(
        activity: Activity,
        photo: Photo,
        currentSort: Sort,
        isFolderLink: Boolean = false,
        folderNodeId: Long? = null,
    ) {
        val nodeHandle = photo.id
        val nodeName = photo.name
        val intent = Util.getMediaIntent(activity, nodeName).apply {
            putExtra(Constants.INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FROM_MEDIA_DISCOVERY)

            putExtra(
                Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                folderNodeId ?: getNodeParentHandle(nodeHandle)
            )
            putExtra(
                Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                if (currentSort == Sort.NEWEST) {
                    SortOrder.ORDER_MODIFICATION_DESC
                } else {
                    SortOrder.ORDER_MODIFICATION_ASC
                }
            )
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        activity.startActivity(
            isLocalFile(nodeHandle)?.let { localPath ->
                File(localPath).let { mediaFile ->
                    kotlin.runCatching {
                        FileProvider.getUriForFile(
                            activity,
                            Constants.AUTHORITY_STRING_FILE_PROVIDER,
                            mediaFile
                        )
                    }.onFailure {
                        Uri.fromFile(mediaFile)
                    }.map { mediaFileUri ->
                        intent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(nodeName).type)
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                }
                intent
            } ?: updateIntent(
                handle = nodeHandle,
                name = nodeName,
                intent = intent,
                isFolderLink = isFolderLink,
            )
        )
    }

    /**
     * Get node parent handle
     *
     * @param handle node handle
     * @return parent handle
     */
    private suspend fun getNodeParentHandle(handle: Long): Long? =
        getNodeByHandle(handle)?.parentHandle

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return true is local file, otherwise is false
     */
    private suspend fun isLocalFile(
        handle: Long,
    ): String? =
        getNodeByHandle(handle)?.let { node ->
            val localPath = FileUtil.getLocalFile(node)
            File(FileUtil.getDownloadLocation(), node.name).let { file ->
                if (localPath != null && ((FileUtil.isFileAvailable(file) && file.length() == node.size)
                            || (node.fingerprint == getFingerprintUseCase(localPath)))
                ) {
                    localPath
                } else {
                    null
                }
            }
        }

    /**
     * Update intent
     *
     * @param handle node handle
     * @param name node name
     * @param intent Intent
     * @return updated intent
     */
    private suspend fun updateIntent(
        handle: Long,
        name: String,
        intent: Intent,
        isFolderLink: Boolean = false,
        isAlbumSharing: Boolean = false,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        when {
            isAlbumSharing -> {
                getAlbumPhotoFileUrlByNodeIdUseCase(NodeId(handle))
            }

            isFolderLink -> {
                getLocalFolderLinkFromMegaApiUseCase(handle)
            }

            else -> {
                getFileUrlByNodeHandleUseCase(handle)
            }
        }?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, MimeTypeList.typeForName(name).type)
            }
        }

        return intent
    }
}