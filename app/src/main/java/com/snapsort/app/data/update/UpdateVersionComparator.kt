package com.snapsort.app.data.update

internal object UpdateVersionComparator {
    fun isUpdateAvailable(
        currentVersionName: String,
        currentVersionCode: Int,
        latestTagName: String
    ): Boolean {
        val currentTag = currentVersionName.trim()
        val latestTag = latestTagName.trim()
        if (latestTag.isBlank()) {
            return false
        }
        if (currentTag.equals(latestTag, ignoreCase = true)) {
            return false
        }

        val currentTimestamp = parseSnapSortTimestampTag(currentTag)
        val latestTimestamp = parseSnapSortTimestampTag(latestTag)
        if (currentTimestamp != null && latestTimestamp != null) {
            return latestTimestamp > currentTimestamp
        }

        val currentSemVer = parseSemVer(currentTag)
        val latestSemVer = parseSemVer(latestTag)
        if (currentSemVer != null && latestSemVer != null) {
            return latestSemVer > currentSemVer
        }

        val latestNumericCode = parsePlainNumericTag(latestTag)
        if (latestNumericCode != null) {
            return latestNumericCode > currentVersionCode
        }

        return true
    }

    private fun parseSnapSortTimestampTag(value: String): Long? {
        val match = TimestampTagRegex.matchEntire(value) ?: return null
        val year = match.groupValues[1]
        val month = match.groupValues[2]
        val day = match.groupValues[3]
        val time = match.groupValues[4]
        return "$year$month$day$time".toLongOrNull()
    }

    private fun parseSemVer(value: String): SemVer? {
        val match = SemVerRegex.matchEntire(value) ?: return null
        return SemVer(
            major = match.groupValues[1].toIntOrNull() ?: return null,
            minor = match.groupValues[2].toIntOrNull() ?: return null,
            patch = match.groupValues[3].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
        )
    }

    private fun parsePlainNumericTag(value: String): Long? {
        val normalized = value.removePrefix("v").removePrefix("V")
        return normalized.takeIf { it.all(Char::isDigit) }?.toLongOrNull()
    }

    private data class SemVer(
        val major: Int,
        val minor: Int,
        val patch: Int
    ) : Comparable<SemVer> {
        override fun compareTo(other: SemVer): Int {
            return compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)
        }
    }

    private val TimestampTagRegex = Regex("""^v?(\d{4})\.(\d{2})\.(\d{2})-(\d{4})$""")
    private val SemVerRegex = Regex("""^v?(\d+)\.(\d+)(?:\.(\d+))?$""")
}
