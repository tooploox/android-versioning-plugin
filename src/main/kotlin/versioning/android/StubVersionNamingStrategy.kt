package versioning.android

class StubVersionNamingStrategy : VersionNamingStrategy {

    override val versionCode: Int = 1

    override val versionName: String = "non-versioned"
}
