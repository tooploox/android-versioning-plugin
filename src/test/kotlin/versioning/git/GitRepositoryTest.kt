package versioning.git

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.junit.JGitTestUtil
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase
import org.eclipse.jgit.transport.URIish
import org.junit.Before
import org.junit.Test

class GitRepositoryTest : LocalDiskRepositoryTestCase() {

    lateinit var repository: Repository

    lateinit var actualRepo: Git
    lateinit var repositoryFolder: FileRepository

    @Before
    override fun setUp() {
        super.setUp()

        repositoryFolder = createWorkRepository()
        repository = GitRepository(repositoryFolder)
        actualRepo = Git.wrap(repositoryFolder)

        JGitTestUtil.writeTrashFile(repositoryFolder, "readme", "[empty]")
        actualRepo.add().addFilepattern(".").call()
        actualRepo.commit().setMessage("Initial commit").call()
    }

    @Test
    fun readsBranchNameProperly() {
        actualRepo.checkout().setCreateBranch(true).setName("develop").call()
        assertThat(repository.currentBranchName()).isEqualTo("develop")

        actualRepo.checkout().setName("master").call()
        assertThat(repository.currentBranchName()).isEqualTo("master")

        actualRepo.checkout().setCreateBranch(true).setName("feature/release").call()
        assertThat(repository.currentBranchName()).isEqualTo("feature/release")
    }

    @Test
    fun readsCleanStateProperly() {
        // Clean repository
        assertThat(repository.isClean()).isTrue()

        // Unstaged file
        JGitTestUtil.writeTrashFile(repositoryFolder, "someFile", "[empty]")
        assertThat(repository.isClean()).isFalse()

        // Staged file
        actualRepo.add().addFilepattern(".").call()
        assertThat(repository.isClean()).isFalse()

        // Commited file - clean repository
        actualRepo.commit().setMessage("commit").setAll(true).call()
        assertThat(repository.isClean()).isTrue()
    }

    @Test
    fun describesProperly() {
        // No tag
        assertThat(repository.describe()).isNull()

        // Non-release tak
        actualRepo.tag().setName("notReleases/1.0.0").call()
        assertThat(repository.describe()).isNull()

        // Directly on tag
        actualRepo.tag().setName("releases/1.0.0").call()
        assertThat(repository.describe()).isEqualTo("releases/1.0.0")

        // n commits away from latest tag
        repeat(5) {
            val lastCommit = actualRepo.commit().setMessage("msg").call()
            assertThat(repository.describe()).isEqualTo("releases/1.0.0-${it + 1}-g${lastCommit.abbreviate(7).name()}")
        }

        // Directly on tag again
        actualRepo.tag().setName("releases/1.5.0").call()
        assertThat(repository.describe()).isEqualTo("releases/1.5.0")
    }

    @Test
    fun addsTag() {
        val remoteRepository = createWorkRepository()
        actualRepo.remoteAdd().apply {
            setName("origin")
            setUri(URIish(remoteRepository.workTree.path))
        }.call()

        val newTagName = "tagName"

        repository.addAndPushTag(newTagName)
        assertThat(actualRepo.describe().call()).isEqualTo(newTagName)

        val remoteTagsList = Git.wrap(remoteRepository).tagList().call()
        assertThat(remoteTagsList
                .map { ref -> ref.name })
                .contains("refs/tags/$newTagName")
    }
}
