package versioning

import org.gradle.api.Plugin
import org.gradle.api.Project
import versioning.android.DefaultVersionNamingStrategy
import versioning.android.StubVersionNamingStrategy
import versioning.git.GitRepository
import versioning.git.NoOpRepository
import versioning.git.Repository

class VersioningPlugin : Plugin<Project> {

    // Visible for testing
    var repository: Repository? = null

    override fun apply(project: Project): Unit {

        repository = repository ?: if (isVersioned(project)) {
            GitRepository()
        } else {
            NoOpRepository()
        }

        val versionNamingStrategy = if (isVersioned(project)) {
            DefaultVersionNamingStrategy(repository!!)
        } else {
            StubVersionNamingStrategy()
        }

        project.extensions.create("versioning",
                VersioningExtension::class.java, versionNamingStrategy.versionCode, versionNamingStrategy.versionName)

        project.tasks.create("increaseVersion", IncreaseVersionTask::class.java) {
            it.group = "Versioning"
            it.description = "Increase version in given _scope_ and tag current commit with new version."
            it.repository = repository!!
        }
    }

    fun isVersioned(project: Project): Boolean = project.findProperty("versioned")?.toString().equals("true", ignoreCase = true)
}
