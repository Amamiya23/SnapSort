package com.snapsort.app.core

private val supportedRawExtensions = setOf(
    "raw",
    "cr2",
    "cr3",
    "nef",
    "arw",
    "raf",
    "rw2",
    "dng",
    "orf",
    "pef"
)

fun isJpgFile(fileName: String): Boolean {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension == "jpg" || extension == "jpeg"
}

fun isSupportedRawFile(fileName: String): Boolean {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in supportedRawExtensions
}

fun matchRawFiles(files: List<ScannedFile>): Map<String, RawMatch> {
    val rawByBaseName = files
        .asSequence()
        .filter { isSupportedRawFile(it.fileName) }
        .groupBy { it.baseName.lowercase() }
        .mapValues { (_, matches) ->
            matches
                .sortedWith(compareBy<ScannedFile> { it.extension }.thenBy { it.fileName.lowercase() })
                .first()
                .let { file ->
                    RawMatch(
                        uri = file.uri,
                        fileName = file.fileName,
                        extension = file.extension
                    )
                }
        }

    return files
        .asSequence()
        .filter { isJpgFile(it.fileName) }
        .mapNotNull { jpg ->
            rawByBaseName[jpg.baseName.lowercase()]?.let { raw -> jpg.uri to raw }
        }
        .toMap()
}
