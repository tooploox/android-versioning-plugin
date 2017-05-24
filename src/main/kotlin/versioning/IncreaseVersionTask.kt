package versioning

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import versioning.git.Repository
import versioning.model.RevisionVersionInfo
import versioning.model.Scope

open class IncreaseVersionTask : DefaultTask() {

    lateinit var repository: Repository

    @TaskAction
    fun increaseVersion(): Unit {

        try {
            logger.lifecycle("Checking current branch and repository status")
            checkBranch()

            val scopeString = project.findProperty("scope") as String?
                    ?: throw IllegalArgumentException("Scope should be specified with -Pscope=[major|minor|patch]")

            val scope = Scope.fromString(scopeString)
            logger.lifecycle("Increasing version in scope: $scope")

            val revisionVersionInfo = RevisionVersionInfo.fromGitDescribe(repository.describe())

            logger.lifecycle("Last version: ${revisionVersionInfo.lastVersion}")
            logger.lifecycle("Commits since last release: ${revisionVersionInfo.commitsSinceLastTag}")
            logger.lifecycle("Current revision SHA (null if on tag): ${revisionVersionInfo.currentRevisionSha}")

            if (revisionVersionInfo.hasVersionTag()) {
                throw IllegalStateException("Can't change version twice for the same revision.")
            }

            val newVersion = revisionVersionInfo.lastVersion.withScopeIncreased(scope)
            logger.lifecycle("New version: $newVersion")

            val tagName = "${Constants.TAG_PREFIX}$newVersion"
            logger.lifecycle("Creating and pushing tag: $tagName")

            repository.addAndPushTag(tagName)
        } finally {
            repository.close()
        }
    }

    fun checkBranch(): Unit {
        if (repository.currentBranchName() != Constants.RELEASE_ALLOWED_BRANCH) {
            throw IllegalStateException("Release can only be created on develop branch")
        }

        if (!repository.isClean()) {
            throw IllegalStateException("Repository must be clean to create a release")
        }
    }
}
