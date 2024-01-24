package mega.privacy.android.data.mapper.camerauploads

import androidx.work.Data
import androidx.work.WorkInfo
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.ARE_UPLOADS_PAUSED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CHECK_FILE_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_ERROR
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_FILE_INDEX
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_TYPE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_UNAVAILABLE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.NOT_ENOUGH_STORAGE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.OUT_OF_SPACE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.START
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STATUS_INFO
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STORAGE_OVER_QUOTA
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_COUNT
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_TO_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED_BYTES
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOAD_BYTES
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import timber.log.Timber
import javax.inject.Inject

/**
 * Mapper that converts a [HeartbeatStatus] into an [Integer]
 */
class CameraUploadsStatusInfoMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param progress [Data]
     * @return [CameraUploadsStatusInfo]
     */
    operator fun invoke(progress: Data, state: WorkInfo.State): CameraUploadsStatusInfo? {
        return when {
            state.isFinished -> return CameraUploadsStatusInfo.Finished

            else -> when (progress.keyValueMap[STATUS_INFO]) {
                START -> {
                    CameraUploadsStatusInfo.Started
                }

                PROGRESS -> {
                    runCatching {
                        with(progress) {
                            CameraUploadsStatusInfo.UploadProgress(
                                totalUploaded = getInt(TOTAL_UPLOADED, 0),
                                totalToUpload = getInt(TOTAL_TO_UPLOAD, 0),
                                totalUploadedBytes = getLong(TOTAL_UPLOADED_BYTES, 0L),
                                totalUploadBytes = getLong(TOTAL_UPLOAD_BYTES, 0L),
                                progress = Progress(getFloat(CURRENT_PROGRESS, 0f)),
                                areUploadsPaused = getBoolean(ARE_UPLOADS_PAUSED, false)
                            )
                        }
                    }.onFailure {
                        Timber.e(it)
                    }.getOrNull()
                }

                COMPRESSION_PROGRESS -> {
                    runCatching {
                        with(progress) {
                            CameraUploadsStatusInfo.VideoCompressionProgress(
                                currentFileIndex = getInt(CURRENT_FILE_INDEX, 0),
                                totalCount = getInt(TOTAL_COUNT, 0),
                                progress = Progress(getFloat(CURRENT_PROGRESS, 0f)),
                            )
                        }
                    }.onFailure {
                        Timber.e(it)
                    }.getOrNull()
                }

                COMPRESSION_ERROR -> {
                    CameraUploadsStatusInfo.VideoCompressionError
                }

                OUT_OF_SPACE -> {
                    CameraUploadsStatusInfo.VideoCompressionOutOfSpace
                }

                CHECK_FILE_UPLOAD -> {
                    CameraUploadsStatusInfo.CheckFilesForUpload
                }

                STORAGE_OVER_QUOTA -> {
                    CameraUploadsStatusInfo.StorageOverQuota
                }

                NOT_ENOUGH_STORAGE -> {
                    CameraUploadsStatusInfo.NotEnoughStorage
                }

                FOLDER_UNAVAILABLE -> {
                    val folderType = CameraUploadFolderType.entries[progress.getInt(
                        FOLDER_TYPE,
                        CameraUploadFolderType.Primary.ordinal
                    )]
                    CameraUploadsStatusInfo.FolderUnavailable(folderType)
                }

                else -> {
                    null
                }
            }
        }
    }
}
