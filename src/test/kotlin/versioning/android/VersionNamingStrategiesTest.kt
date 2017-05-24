package versioning.android

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import versioning.createGitDescribeString
import versioning.git.Repository

@RunWith(Parameterized::class)
class VersionNamingStrategiesTest(
        private val gitDescribe: String?,
        private val currentBranchName: String,
        private val expectedVersionCode: Int,
        private val expectedVersionName: String
) {

    companion object {

        @Parameterized.Parameters(name = """Git describe "{0}" on branch {1}""")
        @JvmStatic
        fun data(): Collection<Array<out Any?>> {
            return listOf(
                    arrayOf(null, "develop", -1, "0.0.0"),
                    arrayOf(null, "master", -1, "0.0.0"),
                    arrayOf(createGitDescribeString("1.2.2"), "develop", 12200, "1.2.2"),
                    arrayOf(createGitDescribeString("1.2.2", 4, "abcdef"), "develop", 12204, "1.2.2-4-develop-abcdef"),
                    arrayOf(createGitDescribeString("0.0.0"), "master", 0, "0.0.0"),
                    arrayOf(createGitDescribeString("5.2.1", 80, "12bcd2"), "master", 52180, "5.2.1-80-master-12bcd2"),
                    arrayOf(createGitDescribeString("500.21.12"), "develop", 5022200, "500.21.12")
            )
        }
    }

    lateinit var repository: Repository

    @Before
    fun setUp() {
        repository = mock<Repository> {
            on { describe() } doReturn gitDescribe
            on { currentBranchName() } doReturn currentBranchName
        }
    }

    @After
    fun tearDown() {
        repository.close()
    }

    @Test
    fun defaultVersionNamingStrategyTest() {
        DefaultVersionNamingStrategy(repository).let {
            assertThat(it.versionCode).isEqualTo(expectedVersionCode)
            assertThat(it.versionName).isEqualTo(expectedVersionName)
        }
    }

    @Test
    fun stubVersionNamingStrategyTest() {
        StubVersionNamingStrategy().let {
            assertThat(it.versionCode).isEqualTo(1)
            assertThat(it.versionName).isEqualTo("non-versioned")
        }
    }
}
