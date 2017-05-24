package versioning.model

enum class Scope {
    MAJOR,
    MINOR,
    PATCH;

    companion object {

        @JvmStatic
        fun fromString(value: String): Scope =
                when (value.toLowerCase()) {
                    "major" -> MAJOR
                    "minor" -> MINOR
                    "patch" -> PATCH
                    else -> throw IllegalArgumentException("Scope should be one of [major, minor, patch]")
                }
    }
}
