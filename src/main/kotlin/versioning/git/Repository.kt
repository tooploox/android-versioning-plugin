package versioning.git

import java.io.Closeable

interface Repository : Closeable {

    fun describe(): String?

    fun currentBranchName(): String

    fun isClean(): Boolean

    fun addAndPushTag(tagName: String)
}
