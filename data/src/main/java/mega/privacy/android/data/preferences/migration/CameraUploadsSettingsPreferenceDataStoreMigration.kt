package mega.privacy.android.data.preferences.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.data.preferences.CameraUploadsSettingsPreferenceDataStore
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import javax.inject.Inject

internal class CameraUploadsSettingsPreferenceDataStoreMigration @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    private val cameraUploadsSettingsPreferenceDataStoreFactory: CameraUploadsSettingsPreferenceDataStoreFactory,
) : DataMigration<Preferences> {
    override suspend fun cleanUp() {
        // No-op
    }

    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData.asMap().keys.isEmpty()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val newPreferences = currentData.toMutablePreferences()
        val store = cameraUploadsSettingsPreferenceDataStoreFactory(newPreferences)
        val oldPreferences = databaseHandler.preferences
        if (oldPreferences == null) {
            setDefaults(store)
        } else {
            setExistingValues(store, oldPreferences)
        }
        return newPreferences
    }

    private suspend fun setDefaults(store: CameraUploadsSettingsPreferenceDataStore) {
        store.setValues(
            isCameraUploadsEnabled = null,
            isMediaUploadsEnabled = null,
            cameraUploadsHandle = null,
            mediaUploadsHandle = null,
            cameraUploadsLocalPath = null,
            mediaUploadsLocalPath = null,
            areLocationTagsEnabled = false,
            uploadVideoQuality = VideoQuality.ORIGINAL.value,
            areUploadFileNamesKept = false,
            isChargingRequiredForVideoCompression = true,
            videoCompressionSizeLimit = GetVideoCompressionSizeLimitUseCase.DEFAULT_SIZE,
            fileUploadOption = 1003,
            isUploadsByWifi = true,
        )
    }

    private suspend fun setExistingValues(
        store: CameraUploadsSettingsPreferenceDataStore,
        oldPreferences: MegaPreferences,
    ) {
        store.setValues(
            isCameraUploadsEnabled = oldPreferences.camSyncEnabled?.toBooleanStrictOrNull()
                ?: false,
            isMediaUploadsEnabled = oldPreferences.secondaryMediaFolderEnabled?.toBooleanStrictOrNull()
                ?: false,
            cameraUploadsHandle = oldPreferences.camSyncHandle?.toLongOrNull(),
            mediaUploadsHandle = oldPreferences.megaHandleSecondaryFolder?.toLongOrNull(),
            cameraUploadsLocalPath = if (oldPreferences.cameraFolderExternalSDCard?.toBooleanStrictOrNull() == true) oldPreferences.uriExternalSDCard else oldPreferences.camSyncLocalPath,
            mediaUploadsLocalPath = oldPreferences.localPathSecondaryFolder,
            areLocationTagsEnabled = oldPreferences.removeGPS?.toBooleanStrictOrNull()?.not()
                ?: false,
            uploadVideoQuality = oldPreferences.uploadVideoQuality?.toIntOrNull()
                ?: VideoQuality.ORIGINAL.value,
            areUploadFileNamesKept = oldPreferences.keepFileNames?.toBooleanStrictOrNull() ?: false,
            isChargingRequiredForVideoCompression = oldPreferences.conversionOnCharging?.toBooleanStrictOrNull()
                ?: true,
            videoCompressionSizeLimit = oldPreferences.chargingOnSize?.toIntOrNull()
                ?: GetVideoCompressionSizeLimitUseCase.DEFAULT_SIZE,
            fileUploadOption = oldPreferences.camSyncFileUpload?.toIntOrNull()
                ?: 1003,
            isUploadsByWifi = oldPreferences.camSyncWifi?.toBooleanStrictOrNull() ?: true,
        )
    }

    private suspend fun CameraUploadsSettingsPreferenceDataStore.setValues(
        isCameraUploadsEnabled: Boolean?,
        isMediaUploadsEnabled: Boolean?,
        cameraUploadsHandle: Long?,
        mediaUploadsHandle: Long?,
        cameraUploadsLocalPath: String?,
        mediaUploadsLocalPath: String?,
        areLocationTagsEnabled: Boolean,
        uploadVideoQuality: Int,
        areUploadFileNamesKept: Boolean,
        isChargingRequiredForVideoCompression: Boolean,
        videoCompressionSizeLimit: Int,
        fileUploadOption: Int,
        isUploadsByWifi: Boolean,
    ) {
        with(this) {
            isCameraUploadsEnabled?.let { setCameraUploadsEnabled(it) }
            isMediaUploadsEnabled?.let { setMediaUploadsEnabled(it) }
            setCameraUploadsHandle(cameraUploadsHandle)
            setMediaUploadsHandle(mediaUploadsHandle)
            setCameraUploadsLocalPath(cameraUploadsLocalPath)
            setMediaUploadsLocalPath(mediaUploadsLocalPath)
            setLocationTagsEnabled(areLocationTagsEnabled)
            setUploadVideoQuality(uploadVideoQuality)
            setUploadFileNamesKept(areUploadFileNamesKept)
            setChargingRequiredForVideoCompression(isChargingRequiredForVideoCompression)
            setVideoCompressionSizeLimit(videoCompressionSizeLimit)
            setFileUploadOption(fileUploadOption)
            setUploadsByWifi(isUploadsByWifi)
        }
    }

}
