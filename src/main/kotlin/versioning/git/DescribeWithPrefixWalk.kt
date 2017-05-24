package versioning.git

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevFlag
import org.eclipse.jgit.revwalk.RevFlagSet
import org.eclipse.jgit.revwalk.RevWalk

/**
 * Allows searching for the closes tag with given prefix.
 * Just like `git describe --tags --matches 'TAG_PREFIX*'
 *
 * Based on org.eclipse.jgit.api.DescribeCommand
 *
 * This file is available under Eclipse Distribution Licence v1.0
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 * names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
class DescribeWithPrefixWalk(
        private val repository: Repository
) {

    companion object {
        private const val MAX_CANDIDATES = 10
    }

    private val walk: RevWalk = RevWalk(repository).apply {
        isRetainBody = false
    }

    private val target: RevCommit = walk.parseCommit(repository.resolve(Constants.HEAD))

    private fun Ref.shortName(): String = this.name.substring(Constants.R_TAGS.length)

    private fun Ref.isEligible(): Boolean = this.shortName().startsWith(versioning.Constants.TAG_PREFIX)

    fun describe(): String? {
        @Suppress("ConvertTryFinallyToUseCall") // Can't inline local class
        try {
            val shaToTagMap = repository.refDatabase
                    .getRefs(Constants.R_TAGS)
                    .values
                    .filter { it.isEligible() }
                    .associateBy { repository.peel(it).peeledObjectId ?: it.objectId }

            // combined flags of all the candidate instances
            val allFlags = RevFlagSet()

            /**
             * Tracks the depth of each tag as we find them.
             */
            class Candidate(
                    commit: RevCommit,
                    val tag: Ref
            ) {
                val flag: RevFlag = walk.newFlag(tag.name).also {
                    // we'll mark all the nodes reachable from this tag accordingly
                    allFlags.add(it)
                    walk.carry(it)
                    commit.add(it)

                    // As of this writing, JGit carries a flag from a child to its parents
                    // right before RevWalk.next() returns, so all the flags that are added
                    // must be manually carried to its parents. If that gets fixed,
                    // this will be unnecessary.
                    commit.carry(it)
                }

                /**
                 * Number of commits that are reachable from the tip but not reachable from the tag.
                 */
                var depth: Int = 0

                /**
                 * Does this tag contain the given commit?
                 */
                fun reaches(commit: RevCommit): Boolean = commit.has(flag)

                fun describe(tip: ObjectId): String = "${tag.shortName()}-$depth-g${walk.objectReader.abbreviate(tip).name()}"
            }

            val candidates = mutableListOf<Candidate>() // all the candidates we find

            // Is the target already pointing to a tag? if so, we are done!
            val lucky = shaToTagMap[target]
            if (lucky != null) {
                return lucky.shortName()
            }

            walk.markStart(target)

            var seen = 0   // commit seen thus far
            for (commit in walk) {

                if (!commit.hasAny(allFlags)) {
                    // if a tag already dominates this commit,
                    // then there's no point in picking a tag on this commit
                    // since the one that dominates it is always more preferable
                    shaToTagMap[commit]?.let {
                        candidates.add(Candidate(commit, it).apply {
                            depth = seen
                        })
                    }
                }

                // if the newly discovered commit isn't reachable from a tag that we've seen
                // it counts toward the total depth.
                candidates
                        .filterNot { it.reaches(commit) }
                        .forEach { it.depth++ }

                // if we have search going for enough tags, we will start
                // closing down. JGit can only give us a finite number of bits,
                // so we can't track all tags even if we wanted to.
                if (candidates.size >= MAX_CANDIDATES)
                    break

                // TODO: if all the commits in the queue of RevWalk has allFlags
                // there's no point in continuing search as we'll not discover any more
                // tags. But RevWalk doesn't expose this.
                seen++
            }

            // at this point we aren't adding any more tags to our search,
            // but we still need to count all the depths correctly.
            for (commit in walk) {
                if (commit.hasAll(allFlags)) {
                    // no point in visiting further from here, so cut the search here
                    commit.parents.forEach { it.add(RevFlag.SEEN) }
                } else {
                    candidates
                            .filterNot { it.reaches(commit) }
                            .forEach { it.depth++ }
                }
            }

            // if all the nodes are dominated by all the tags, the walk stops

            return candidates
                    .minBy { it.depth }
                    ?.describe(target)
        } finally {
            walk.close()
        }
    }
}
