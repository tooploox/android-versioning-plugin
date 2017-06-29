package versioning.git

import com.jcraft.jsch.Session
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.gradle.api.logging.Logger

class GitRepository(
        private val logger: Logger,
        repository: org.eclipse.jgit.lib.Repository? = null
) : Repository {

    private val git = Git.wrap(
            repository ?:
                    FileRepositoryBuilder()
                            .findGitDir()
                            .build()
    )

    private val sshSessionFactory = object : JschConfigSessionFactory() {
        override fun configure(host: OpenSshConfig.Host, session: Session) {
            // No-op
        }
    }

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

        try {
            git.push().applyAndCall {
                setTransportConfigCallback { transport -> (transport as? SshTransport)?.sshSessionFactory = sshSessionFactory }
                add(tag)
            }
        } catch (_: TransportException) {
            logger.error("Tag push failed. Push the tag by calling `git push origin $tagName` manually.")
        }
    }

    override fun close() {
        git.close()
    }
}
