package com.snapsort.app.data.scanner

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import com.snapsort.app.core.CaptureTimeSource
import com.snapsort.app.core.ScannedFile
import com.snapsort.app.core.ScannedPhoto
import com.snapsort.app.core.SortDirection
import com.snapsort.app.core.groupPhotos
import com.snapsort.app.core.isJpgFile
import com.snapsort.app.core.isSupportedRawFile
import com.snapsort.app.core.matchRawFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Locale

data class ScanSettings(
    val burstThresholdMillis: Long,
    val looseGroupThresholdMillis: Long,
    val sortDirection: SortDirection
)

sealed class ScanEvent {
    data class Progress(
        val stage: String,
        val processed: Int,
        val total: Int,
        val currentFileName: String?
    ) : ScanEvent()

    data class Complete(
        val folderName: String,
        val groups: List<com.snapsort.app.core.PhotoGroup>
    ) : ScanEvent()

    data class Failed(val message: String) : ScanEvent()
}

class FolderScanner(
    private val context: Context
) {
    fun scan(folderUri: Uri, settings: ScanSettings): Flow<ScanEvent> = flow {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
        if (folder == null || !folder.isDirectory) {
            emit(ScanEvent.Failed("无法读取所选文件夹"))
            return@flow
        }

        emit(ScanEvent.Progress("读取文件夹", 0, 0, null))
        val files = queryFolderFiles(folderUri, folder)
            .filter { isJpgFile(it.fileName) || isSupportedRawFile(it.fileName) }

        val jpgFiles = files.filter { isJpgFile(it.fileName) }
        if (jpgFiles.isEmpty()) {
            emit(ScanEvent.Complete(folder.name ?: "所选文件夹", emptyList()))
            return@flow
        }

        emit(ScanEvent.Progress("匹配同名 RAW", 0, jpgFiles.size, null))
        val rawMatches = matchRawFiles(files)
        val photos = mutableListOf<ScannedPhoto>()
        var lastProgressAtMillis = 0L
        var lastProgressIndex = 0

        jpgFiles.forEachIndexed { index, jpg ->
            val processed = index + 1
            val now = SystemClock.elapsedRealtime()
            if (
                processed == 1 ||
                processed == jpgFiles.size ||
                processed - lastProgressIndex >= PROGRESS_BATCH_SIZE ||
                now - lastProgressAtMillis >= PROGRESS_MIN_INTERVAL_MILLIS
            ) {
                emit(ScanEvent.Progress("解析拍摄时间", processed, jpgFiles.size, jpg.fileName))
                lastProgressAtMillis = now
                lastProgressIndex = processed
            }
            val captureTime = readCaptureTime(Uri.parse(jpg.uri), jpg.modifiedAtMillis)
            photos.add(
                ScannedPhoto(
                    jpgUri = jpg.uri,
                    fileName = jpg.fileName,
                    baseName = jpg.baseName,
                    extension = jpg.extension,
                    capturedAtMillis = captureTime.millis,
                    captureTimeSource = captureTime.source,
                    modifiedAtMillis = jpg.modifiedAtMillis,
                    rawMatch = rawMatches[jpg.uri]
                )
            )
        }

        emit(ScanEvent.Progress("生成分组", jpgFiles.size, jpgFiles.size, null))
        val groups = groupPhotos(
            photos = photos,
            burstThresholdMillis = settings.burstThresholdMillis,
            looseGroupThresholdMillis = settings.looseGroupThresholdMillis,
            sortDirection = settings.sortDirection
        )
        emit(ScanEvent.Complete(folder.name ?: "所选文件夹", groups))
    }.flowOn(Dispatchers.IO)

    private fun queryFolderFiles(folderUri: Uri, folder: DocumentFile): List<ScannedFile> {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        return try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val documentIdIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val files = ArrayList<ScannedFile>(cursor.count.coerceAtLeast(0))

                while (cursor.moveToNext()) {
                    val fileName = cursor.getString(nameIndex) ?: continue
                    val mimeType = cursor.getString(mimeTypeIndex)
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) continue

                    val documentId = cursor.getString(documentIdIndex) ?: continue
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId)
                    files.add(
                        ScannedFile(
                            uri = childUri.toString(),
                            fileName = fileName,
                            modifiedAtMillis = cursor.getLong(modifiedIndex)
                        )
                    )
                }

                files
            } ?: queryFolderFilesWithDocumentFile(folder)
        } catch (_: Exception) {
            queryFolderFilesWithDocumentFile(folder)
        }
    }

    private fun queryFolderFilesWithDocumentFile(folder: DocumentFile): List<ScannedFile> {
        return folder.listFiles()
            .filter { it.isFile && it.name != null }
            .map { document ->
                ScannedFile(
                    uri = document.uri.toString(),
                    fileName = document.name.orEmpty(),
                    modifiedAtMillis = document.lastModified()
                )
            }
    }

    private fun readCaptureTime(uri: Uri, modifiedAtMillis: Long): CaptureTime {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                val parsed = dateTimeOriginal?.let { exifDateFormat.parse(it)?.time }
                if (parsed != null) {
                    CaptureTime(parsed, CaptureTimeSource.EXIF)
                } else {
                    CaptureTime(modifiedAtMillis, CaptureTimeSource.MODIFIED_TIME)
                }
            } ?: CaptureTime(modifiedAtMillis, CaptureTimeSource.MODIFIED_TIME)
        } catch (_: Exception) {
            CaptureTime(modifiedAtMillis, CaptureTimeSource.MODIFIED_TIME)
        }
    }

    private data class CaptureTime(
        val millis: Long,
        val source: CaptureTimeSource
    )

    companion object {
        private const val PROGRESS_BATCH_SIZE = 32
        private const val PROGRESS_MIN_INTERVAL_MILLIS = 120L
        private val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
    }
}
