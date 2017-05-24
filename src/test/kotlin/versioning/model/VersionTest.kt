package versioning.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class VersionTest {

    @Test
    fun increasesScopeCorrectly() {
        assertThat(Version(1, 5, 8).withScopeIncreased(Scope.MAJOR)).isEqualTo(Version(2, 0, 0))
        assertThat(Version(1, 5, 8).withScopeIncreased(Scope.MINOR)).isEqualTo(Version(1, 6, 0))
        assertThat(Version(1, 5, 8).withScopeIncreased(Scope.PATCH)).isEqualTo(Version(1, 5, 9))
    }
}
