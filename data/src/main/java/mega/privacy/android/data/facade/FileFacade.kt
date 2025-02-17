package mega.privacy.android.data.facade

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.DATA
import android.provider.MediaStore.MediaColumns.DATE_MODIFIED
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import android.provider.MediaStore.MediaColumns.SIZE
import android.provider.MediaStore.VOLUME_EXTERNAL
import android.provider.MediaStore.VOLUME_INTERNAL
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.exception.FileNotCreatedException
import mega.privacy.android.domain.exception.NotEnoughStorageException
import nz.mega.sdk.AndroidGfxProcessor
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Intent extra data for node handle
 */
const val INTENT_EXTRA_NODE_HANDLE = "NODE_HANDLE"

/**
 * File facade
 *
 * Refer to [mega.privacy.android.app.utils.FileUtil]
 */
class FileFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : FileGateway {

    override val localDCIMFolderPath: String
        get() = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        )?.absolutePath ?: ""

    override suspend fun getTotalSize(file: File?): Long {
        file ?: return -1L
        return file.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    override fun deleteFolderAndSubFolders(folder: File?): Boolean {
        folder ?: return false
        Timber.d("deleteFolderAndSubFolders: ${folder.absolutePath}")
        return folder.deleteRecursively().also {
            Timber.d("Folder delete success $it for ${folder.absolutePath}")
        }
    }

    @Deprecated("File already exposes exists() function", ReplaceWith("file?.exists() == true"))
    override suspend fun isFileAvailable(file: File?): Boolean = file?.exists() == true
    override suspend fun isFileAvailable(fileString: String): Boolean = File(fileString).exists()
    override suspend fun isDocumentFileAvailable(documentFile: DocumentFile?) =
        documentFile?.exists() == true

    override suspend fun doesExternalStorageDirectoryExists(): Boolean =
        Environment.getExternalStorageDirectory() != null

    override suspend fun buildDefaultDownloadDir(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?.let { downloadsDir ->
                File(downloadsDir, DOWNLOAD_DIR)
            } ?: context.filesDir

    override suspend fun getLocalFile(
        fileName: String,
        fileSize: Long,
        lastModifiedDate: Long,
    ) = kotlin.runCatching {
        val selectionArgs = arrayOf(
            fileName,
            fileSize.toString(),
            lastModifiedDate.toString()
        )
        getExternalFile(selectionArgs) ?: getInternalFile(selectionArgs)
    }.getOrNull()

    override suspend fun getFileByPath(path: String): File? {
        val file = File(path)
        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    private fun getExternalFile(
        selectionArgs: Array<String>,
    ) = getExternalCursor(selectionArgs)?.let { cursor ->
        getFileFromCursor(cursor).also { cursor.close() }
    }

    private fun getInternalFile(
        selectionArgs: Array<String>,
    ) = getInternalCursor(selectionArgs)?.let { cursor ->
        getFileFromCursor(cursor).also { cursor.close() }
    }

    private fun getExternalCursor(
        selectionArgs: Array<String>,
    ) = getNonEmptyCursor(
        MediaStore.Files.getContentUri(VOLUME_EXTERNAL),
        selectionArgs,
    )

    private fun getInternalCursor(
        selectionArgs: Array<String>,
    ) = getNonEmptyCursor(
        MediaStore.Files.getContentUri(VOLUME_INTERNAL),
        selectionArgs,
    )

    private fun getNonEmptyCursor(
        uri: Uri,
        selectionArgs: Array<String>,
    ): Cursor? {
        val cursor = context.contentResolver.query(
            /* uri = */
            uri,
            /* projection = */
            arrayOf(DATA),
            /* selection = */
            "$DISPLAY_NAME = ? AND $SIZE = ? AND $DATE_MODIFIED = ?",
            /* selectionArgs = */
            selectionArgs,
            /* sortOrder = */
            null,
        ) ?: return null

        return if (cursor.moveToFirst()) {
            cursor
        } else {
            cursor.close()
            null
        }
    }

    private fun getFileFromCursor(cursor: Cursor): File? {
        val path = cursor.getString(cursor.getColumnIndexOrThrow(DATA))
        return File(path).takeIf { it.exists() }
    }

    override suspend fun getOfflineFilesRootPath() =
        context.filesDir.absolutePath + File.separator + OFFLINE_DIR

    override suspend fun getOfflineFilesBackupsRootPath() =
        context.filesDir.absolutePath + File.separator + OFFLINE_DIR + File.separator + "in"

    override suspend fun removeGPSCoordinates(filePath: String) {
        try {
            val exif = ExifInterface(filePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, REF_LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, REF_LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, REF_LAT_LNG)
            exif.saveAttributes()
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    override suspend fun copyFile(source: File, destination: File) {
        if (source.absolutePath != destination.absolutePath) {
            withContext(Dispatchers.IO) {
                val inputStream = FileInputStream(source)
                val outputStream = FileOutputStream(destination)
                val inputChannel = inputStream.channel
                val outputChannel = outputStream.channel
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size())
                inputChannel.close()
                outputChannel.close()
                inputStream.close()
                outputStream.close()
            }
        }
    }

    override suspend fun createTempFile(rootPath: String, localPath: String, newPath: String) {
        val srcFile = File(localPath)
        if (!srcFile.exists()) {
            Timber.e("Source File doesn't exist")
            throw FileNotFoundException()
        }
        val hasEnoughSpace = hasEnoughStorage(rootPath, srcFile)
        if (!hasEnoughSpace) {
            Timber.e("Not Enough Storage")
            throw NotEnoughStorageException()
        }
        val destinationFile = File(newPath)
        try {
            copyFile(srcFile, destinationFile)
        } catch (e: IOException) {
            Timber.e(e)
            throw FileNotCreatedException()
        }
    }

    override suspend fun hasEnoughStorage(rootPath: String, file: File) =
        runCatching { StatFs(rootPath).availableBytes >= file.length() }.onFailure { Timber.e(it) }
            .getOrDefault(false)

    override suspend fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    override suspend fun createDirectory(path: String) = run {
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        directory
    }

    override suspend fun deleteDirectory(path: String) = run {
        val directory = File(path)
        directory.deleteRecursively()
    }

    override fun scanMediaFile(paths: Array<String>, mimeTypes: Array<String>) {
        MediaScannerConnection.scanFile(
            context,
            paths,
            mimeTypes,
            null
        )
    }

    override suspend fun getExternalPathByContentUri(contentUri: String): String? = run {
        contentUri
            .toUri()
            .lastPathSegment
            ?.split(":")
            ?.lastOrNull()
            ?.let {
                Environment.getExternalStorageDirectory().path +
                        "/" + it
            }
    }

    override suspend fun deleteFilesInDirectory(directory: File) {
        directory.listFiles()?.forEach {
            it.delete()
        }
    }

    override suspend fun buildExternalStorageFile(filePath: String): File =
        File(Environment.getExternalStorageDirectory().absolutePath + filePath)

    override suspend fun renameFile(oldFile: File, newName: String): Boolean {
        if (oldFile.exists()) {
            val newRKFile = File(oldFile.parentFile, newName)
            return oldFile.renameTo(newRKFile)
        }
        return false
    }

    override suspend fun getAbsolutePath(path: String) =
        File(path).takeIf { it.exists() }?.absolutePath

    override suspend fun setLastModified(path: String, timestamp: Long) =
        File(path).takeIf { it.exists() }?.setLastModified(timestamp)

    override suspend fun saveTextOnContentUri(uri: String, text: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = context.contentResolver.openFileDescriptor(uri.toUri(), "w")
                file?.use {
                    FileOutputStream(file.fileDescriptor).use {
                        it.write(text.toByteArray())
                    }
                } ?: return@runCatching false
            }.fold(
                onSuccess = { true },
                onFailure = { false }
            )
        }

    override suspend fun getUriForFile(file: File, authority: String): Uri =
        withContext(Dispatchers.IO) {
            FileProvider.getUriForFile(context, authority, file)
        }

    override suspend fun getOfflineFolder() =
        createDirectory(context.filesDir.toString() + File.separator + OFFLINE_DIR)

    override suspend fun createNewImageUri(fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // use default location for below Android Q
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$PHOTO_DIR"
                )
            }
        }

        val contentResolver = context.contentResolver
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override suspend fun createNewVideoUri(fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/$PHOTO_DIR"
                )
            }
        }

        val contentResolver = context.contentResolver
        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override suspend fun isFileUri(uriString: String) = uriString.toUri().scheme == "file"

    override suspend fun isFilePath(path: String) = File(path).isFile

    override suspend fun getFileFromUriFile(uriString: String): File =
        uriString.toUri().toFile()

    override suspend fun isContentUri(uriString: String) = uriString.toUri().scheme == "content"

    override suspend fun isExternalStorageContentUri(uriString: String) =
        with(Uri.parse(uriString)) {
            scheme == "content" && authority?.startsWith("com.android.externalstorage") == true
        }

    override suspend fun getFileNameFromUri(uriString: String): String? {
        val cursor = context.contentResolver.query(uriString.toUri(), null, null, null, null)
        return cursor?.use {
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                    ?.let { cursor.getString(it) }
            } else null
        }
    }

    override suspend fun copyContentUriToFile(uriString: String, file: File) {
        val uri = uriString.toUri()
        require(uri.scheme == "content")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
        }
    }

    override fun downscaleImage(file: File, destination: File, maxPixels: Long) {
        val orientation = AndroidGfxProcessor.getExifOrientation(file.absolutePath)
        val fileRect = AndroidGfxProcessor.getImageDimensions(file.absolutePath, orientation)
        val fileBitmap = AndroidGfxProcessor.getBitmap(
            file.absolutePath,
            fileRect,
            orientation,
            fileRect.right,
            fileRect.bottom
        )
        if (fileBitmap == null) {
            Timber.e("Bitmap NULL when decoding image file for upload it to chat.")
            return
        }

        var width = fileBitmap.width
        var height = fileBitmap.height
        val totalPixels = width * height
        if (totalPixels == 0) {
            Timber.e("Bitmap is not valid, it has 0 pixels")
            return
        }
        val division: Float = maxPixels.toFloat() / totalPixels.toFloat()
        val factor = sqrt(division.toDouble()).coerceAtMost(1.0).toFloat()
        if (factor < 1) {
            width = (width * factor).toInt()
            height = (height * factor).toInt()
            Timber.d(
                "DATA connection factor<1\n" +
                        "totalPixels: $totalPixels\n" +
                        "width: $width\n" +
                        "height: $height\n" +
                        "DOWNSCALE_IMAGES_PX/totalPixels: $division\n" +
                        "Math.sqrt(DOWNSCALE_IMAGES_PX/totalPixels): ${sqrt(division)}"
            )
            val scaleBitmap =
                Bitmap.createScaledBitmap(fileBitmap, width, height, true)

            val fOut: FileOutputStream
            try {
                fOut = FileOutputStream(destination)
                scaleBitmap.compress(file.getCompressFormat(), 100, fOut)
                fOut.flush()
                fOut.close()
            } catch (e: java.lang.Exception) {
                Timber.e(e, "Exception compressing image file for upload it to chat.")
            } finally {
                scaleBitmap.recycle()
            }

        } else {
            Timber.d("No need to scale the image as it is smaller than the target")
        }
        fileBitmap.recycle()
    }

    override suspend fun deleteFileByUri(uri: Uri): Boolean =
        context.contentResolver.delete(uri, null, null) > 0

    private fun File.getCompressFormat(): CompressFormat = when (extension) {
        "jpeg", "jpg" -> CompressFormat.JPEG
        "png" -> CompressFormat.PNG
        "webp" -> CompressFormat.WEBP
        else -> CompressFormat.JPEG
    }

    private companion object {
        const val DOWNLOAD_DIR = "MEGA Downloads"
        const val PHOTO_DIR = "MEGA Photos"
        const val OFFLINE_DIR = "MEGA Offline"
        const val LAT_LNG = "0/1,0/1,0/1000"
        const val REF_LAT_LNG = "0"
    }
}
