package versioning

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import versioning.git.Repository
import versioning.model.Scope

class IncreaseVersionTaskTest {

    lateinit var repository: Repository
    lateinit var task: IncreaseVersionTask
    lateinit var project: Project

    @Before
    fun setUp() {
        project = ProjectBuilder.builder().build()
        repository = mock<Repository>()

        task = project.tasks.create("increaseVersion", IncreaseVersionTask::class.java) {
            it.repository = repository
        }
    }

    @Test
    fun failsIfNotOnDevelop() {
        project.addScopeProperty(Scope.MAJOR)
        whenever(repository.currentBranchName()).thenReturn("feature/whatever")
        whenever(repository.isClean()).thenReturn(true)
        assertThatThrownBy { task.increaseVersion() }
                .hasMessageContaining("Release can only be created on develop branch")
    }

    @Test
    fun failsIfRepositoryNotClean() {
        project.addScopeProperty(Scope.MAJOR)
        whenever(repository.currentBranchName()).thenReturn("develop")
        whenever(repository.isClean()).thenReturn(false)
        assertThatThrownBy { task.increaseVersion() }
                .hasMessageContaining("Repository must be clean to create a release")
    }

    @Test
    fun failsIfNoScopeProvided() {
        setupProperRepositoryState()
        assertThatThrownBy { task.increaseVersion() }
                .hasMessageContaining("Scope should be specified")
    }

    @Test
    fun failsIfAlreadyOnTag() {
        setupProperRepositoryState()
        project.addScopeProperty(Scope.MAJOR)
        setupDescribeResult("1.0.0")

        assertThatThrownBy { task.increaseVersion() }
                .hasMessageContaining("Can't change version twice for the same revision.")
    }

    @Test
    fun increasesMajorScope() {
        testScopeIncrease("1.3.5", Scope.MAJOR, "2.0.0")
    }

    @Test
    fun increasesMinorScope() {
        testScopeIncrease("1.3.5", Scope.MINOR, "1.4.0")
    }

    @Test
    fun increasesPatchScope() {
        testScopeIncrease("1.3.5", Scope.PATCH, "1.3.6")
    }

    fun testScopeIncrease(initialVersion: String, scope: Scope, expectedVersion: String) {
        setupProperRepositoryState()
        setupDescribeResult(initialVersion, 5, "abcdef")
        project.addScopeProperty(scope)

        task.increaseVersion()

        verify(repository).addAndPushTag("$TAG_PREFIX$expectedVersion")
    }

    @Test
    fun createsInitialMajorVersionProperly() {
        testCreatingInitialVersion(Scope.MAJOR, "1.0.0")
    }

    @Test
    fun createsInitialMinorVersionProperly() {
        testCreatingInitialVersion(Scope.MINOR, "0.1.0")
    }

    @Test
    fun createsInitialPatchVersionProperly() {
        testCreatingInitialVersion(Scope.PATCH, "0.0.1")
    }

    fun testCreatingInitialVersion(scope: Scope, expectedVersion: String) {
        setupProperRepositoryState()
        whenever(repository.describe()).thenReturn(null)
        project.addScopeProperty(scope)

        task.increaseVersion()

        verify(repository).addAndPushTag("$TAG_PREFIX$expectedVersion")
    }

    @Test
    fun closesRepository() {
        setupProperRepositoryState()
        whenever(repository.describe()).thenReturn(null)
        project.addScopeProperty(Scope.MAJOR)

        task.increaseVersion()

        verify(repository).close()
    }

    private fun setupProperRepositoryState() {
        whenever(repository.currentBranchName()).thenReturn("develop")
        whenever(repository.isClean()).thenReturn(true)
    }

    private fun setupDescribeResult(lastVersion: String, commitsSinceLastTag: Int = 0, currentSha: String? = null) {
        whenever(repository.describe()).thenReturn(createGitDescribeString(lastVersion, commitsSinceLastTag, currentSha))
    }
}
