package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * Compress a file before uploading it to the chat
 */
class CompressFileForChatUseCase @Inject constructor(
    private val isImageFileUseCase: IsImageFileUseCase,
    private val isVideoFileUseCase: IsVideoFileUseCase,
    private val downscaleImageForChatUseCase: DownscaleImageForChatUseCase,
    private val compressVideoForChatUseCase: CompressVideoForChatUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(original: File): File? {
        val path = original.absolutePath
        return when {
            isImageFileUseCase(path) -> {
                downscaleImageForChatUseCase(original)
            }

            isVideoFileUseCase(path) && VIDEO_COMPRESSION_ISSUE_FIXED -> {
                compressVideoForChatUseCase(original)
            }

            else -> null
        }
    }

    companion object {
        /**
         * Video compression has an issue with landscape videos, this flag will be removed once solved
         */
        internal const val VIDEO_COMPRESSION_ISSUE_FIXED = false
    }
}