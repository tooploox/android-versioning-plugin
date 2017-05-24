package versioning.model

import versioning.Constants

data class RevisionVersionInfo(
        val lastVersion: Version,
        val commitsSinceLastTag: Int, // 0 if we're on tag, -1 if there's no previous tag
        val currentRevisionSha: String?

) {
    companion object {

        @JvmStatic
        fun fromGitDescribe(description: String?): RevisionVersionInfo {
            if (description == null) {
                return RevisionVersionInfo(Version.getDefault(), -1, null)
            }

            val parts = description.replace(Constants.TAG_PREFIX, "").split("-")

            return RevisionVersionInfo(
                    Version.fromString(parts[0]),
                    parts.getOrNull(1)?.toInt() ?: 0,
                    parts.getOrNull(2)?.substring(1)
            )
        }
    }

    fun hasVersionTag(): Boolean = commitsSinceLastTag == 0
}
