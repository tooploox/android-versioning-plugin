package versioning

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import versioning.git.Repository

class VersioningPluginTest {

    lateinit var plugin: VersioningPlugin
    lateinit var project: Project
    lateinit var repository: Repository

    private val Project.versioningExtension: VersioningExtension
        get() = this.extensions.findByName("versioning") as VersioningExtension

    @Before
    fun setUp() {
        repository = mock<Repository>()
        plugin = VersioningPlugin()
        plugin.repository = this.repository
        project = ProjectBuilder.builder().build()
    }

    @Test
    fun createsVersioningTask() {
        plugin.apply(project)

        assertThat(project.tasks.findByName("increaseVersion")).isNotNull()
    }

    @Test
    fun addsVersioningExtension() {
        plugin.apply(project)

        val extension = project.extensions.findByName("versioning")
        assertThat(extension).isNotNull()
        assertThat(extension).isInstanceOf(VersioningExtension::class.java)
    }

    @Test
    fun versionIsStubbedWhenNotVersioned() {
        plugin.apply(project)

        assertThat(project.versioningExtension.currentVersionCode).isEqualTo(1)
        assertThat(project.versioningExtension.currentVersionName).isEqualTo("non-versioned")

        verifyZeroInteractions(repository)
    }

    @Test
    fun versionIsInferredProperlyWhenOnTag() {
        project.addVersionedProperty(true)

        whenever(repository.describe()).thenReturn("releases/2.4.0")

        plugin.apply(project)

        assertThat(project.versioningExtension.currentVersionCode).isEqualTo(24000)
        assertThat(project.versioningExtension.currentVersionName).isEqualTo("2.4.0")
    }

    @Test
    fun versionIsInferredProperlyWhenNotOnTag() {
        project.addVersionedProperty(true)

        whenever(repository.describe()).thenReturn("releases/2.4.0-15-gabcdef")
        whenever(repository.currentBranchName()).thenReturn("develop")

        plugin.apply(project)

        assertThat(project.versioningExtension.currentVersionCode).isEqualTo(24015)
        assertThat(project.versioningExtension.currentVersionName).isEqualTo("2.4.0-15-develop-abcdef")
    }

    @Test
    fun versionIsInferredProperlyWhenNoTag() {
        project.addVersionedProperty(true)

        whenever(repository.describe()).thenReturn(null)
        whenever(repository.currentBranchName()).thenReturn("develop")

        plugin.apply(project)

        assertThat(project.versioningExtension.currentVersionCode).isEqualTo(-1)
        assertThat(project.versioningExtension.currentVersionName).isEqualTo("0.0.0")
    }
}
