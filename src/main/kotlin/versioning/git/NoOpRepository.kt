package versioning.git

class NoOpRepository : Repository {

    override fun describe(): String? = null

    override fun currentBranchName(): String = ""

    override fun isClean(): Boolean = false

    override fun addAndPushTag(tagName: String) {
        // no-op
    }

    override fun close() {
        // no-op
    }
}
