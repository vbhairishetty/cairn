package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** TM-14: every known way to weaken or skip the jqwik ban is reported (ADR 0006). */
class JqwikBanSecurityTest {

  private static final Path FIXTURES = Path.of("src/test/resources/build-guard");

  @ParameterizedTest(name = "{0} is reported")
  @ValueSource(
      strings = {
        "skip-in-execution",
        "fail-false",
        "search-transitive-false",
        "exclude-removed",
        "skip-property",
        "wrong-phase",
        "plugin-management-only",
        "level-warn",
        "includes",
        "profile-skip",
        "profile-skip-property",
        "skip-expression",
        "fail-expression",
        "phase-none-split",
        "duplicate-plugin",
        "plugin-management-skip",
        "plugin-management-fail-false",
        "plugin-level-level-warn",
        "plugin-level-includes",
        "maven-config-skip",
        "jvm-config-skip",
        "maven-config-pom-path",
        "system-property-variables"
      })
  void weakenedBuildIsReported(String fixture) throws Exception {
    Path dir = FIXTURES.resolve(fixture);

    assertThat(
            BuildGuard.violations(
                dir.resolve("pom.xml"), dir.resolve("maven.config"), dir.resolve("jvm.config")))
        .isNotEmpty();
  }

  @Test
  void realBuildFilesAreNotReported() throws Exception {
    assertThat(
            BuildGuard.violations(
                Path.of("pom.xml"), Path.of(".mvn/maven.config"), Path.of(".mvn/jvm.config")))
        .isEmpty();
  }
}
