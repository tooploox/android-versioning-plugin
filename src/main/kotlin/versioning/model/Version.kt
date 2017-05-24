package versioning.model

data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int
) {

    companion object {

        fun fromString(version: String): Version =
                version.split(".").let {
                    Version(it[0].toInt(), it[1].toInt(), it[2].toInt())
                }

        fun getDefault(): Version = Version(0, 0, 0)
    }

    fun withScopeIncreased(scope: Scope): Version =
            when (scope) {
                Scope.MAJOR -> Version(major + 1, 0, 0)
                Scope.MINOR -> Version(major, minor + 1, 0)
                Scope.PATCH -> Version(major, minor, patch + 1)
            }

    override fun toString(): String = "$major.$minor.$patch"
}
