package versioning

import org.gradle.api.Project
import versioning.model.Scope

const val TAG_PREFIX = "releases/"

private const val PROPERTY_SCOPE = "scope"
private const val PROPERTY_VERSIONED = "versioned"

fun createGitDescribeString(lastVersion: String, commitsSinceLastTag: Int = 0, currentSha: String? = null) =
        if (commitsSinceLastTag > 0) "$TAG_PREFIX$lastVersion-$commitsSinceLastTag-g${currentSha!!}" else lastVersion

fun Project.addScopeProperty(scope: Scope) = extensions.add(PROPERTY_SCOPE, scope.name)

fun Project.addVersionedProperty(versioned: Boolean) = extensions.add(PROPERTY_VERSIONED, versioned)
