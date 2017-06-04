package versioning.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class GitRepository(
        repository: org.eclipse.jgit.lib.Repository? = null
) : Repository {

    private val git = Git.wrap(
            repository ?:
                    FileRepositoryBuilder()
                            .findGitDir()
                            .build()
    )

    fun <T : GitCommand<R>, R> T.applyAndCall(config: T.() -> Unit): R {
        this.config()
        return this.call()
    }

    override fun describe(): String? = DescribeWithPrefixWalk(git.repository).describe()

    override fun currentBranchName(): String = git.repository.exactRef("HEAD").target.name.let {
        org.eclipse.jgit.lib.Repository.shortenRefName(it)
    }

    override fun isClean(): Boolean = git.status().call().isClean

    override fun addAndPushTag(tagName: String) {
        val tag = git.tag().applyAndCall {
            name = tagName
        }

        git.push().applyAndCall {
            add(tag)
        }
    }

    override fun close() {
        git.close()
    }
}
