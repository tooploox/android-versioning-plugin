package versioning.android

import versioning.git.Repository
import versioning.model.RevisionVersionInfo

class DefaultVersionNamingStrategy(
        repository: Repository
) : VersionNamingStrategy {

    override val versionCode: Int

    override val versionName: String

    init {

        try {
            val revisionVersionInfo = RevisionVersionInfo.fromGitDescribe(repository.describe())

            val version = revisionVersionInfo.lastVersion
            versionCode = version.major * 10000 + version.minor * 1000 + version.patch * 100 + revisionVersionInfo.commitsSinceLastTag

            val versionNameBuilder = StringBuilder()
            versionNameBuilder.append("${version.major}.${version.minor}.${version.patch}")

            if (revisionVersionInfo.commitsSinceLastTag > 0) {
                versionNameBuilder.append("-${revisionVersionInfo.commitsSinceLastTag}")
                versionNameBuilder.append("-${repository.currentBranchName()}")
                versionNameBuilder.append("-${revisionVersionInfo.currentRevisionSha!!}")
            }

            versionName = versionNameBuilder.toString()
        } finally {
            repository.close()
        }
    }
}
