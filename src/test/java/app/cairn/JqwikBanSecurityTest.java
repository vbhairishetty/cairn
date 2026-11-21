package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * TM-14: every known way to weaken or skip the jqwik ban is reported (ADR 0006). Each case edits
 * the live pom.xml in memory, so the cases follow the pom as it changes. An edit fails loudly if
 * its anchor text is missing or not unique.
 */
class JqwikBanSecurityTest {

  private static final String DIFFERS = "differs from the approved configuration";
  private static final String ONCE = "must be declared exactly once";
  private static final String ENFORCER_PROPERTY = "a pom property starts with enforcer.";
  private static final String OUTSIDE = "jqwik appears outside the approved enforcer plugin";
  private static final String JQWIK_DEPENDENCY =
      "<dependency><groupId>net.jqwik</groupId><artifactId>jqwik</artifactId>"
          + "<version>1.10.2</version></dependency>";
  private static final String PROFILE_ENFORCER_SKIP =
      "<profiles><profile><id>p</id><activation><activeByDefault>true</activeByDefault>"
          + "</activation><build><plugins><plugin><artifactId>maven-enforcer-plugin</artifactId>"
          + "<configuration><skip>true</skip></configuration></plugin></plugins></build>"
          + "</profile></profiles>";

  record Case(
      String expected, UnaryOperator<String> pomEdit, String mavenConfig, String jvmConfig) {}

  @TempDir Path dir;

  @ParameterizedTest(name = "{argumentSetName} is reported")
  @MethodSource("weakenedBuilds")
  void weakenedBuildIsReported(Case weakened) throws Exception {
    Path pom = dir.resolve("pom.xml");
    Files.writeString(pom, weakened.pomEdit().apply(Files.readString(Path.of("pom.xml"))));
    Files.writeString(dir.resolve("maven.config"), weakened.mavenConfig());
    Files.writeString(dir.resolve("jvm.config"), weakened.jvmConfig());

    assertThat(BuildGuard.violations(pom, dir.resolve("maven.config"), dir.resolve("jvm.config")))
        .anySatisfy(violation -> assertThat(violation).contains(weakened.expected()));
  }

  @Test
  void unchangedLiveBuildFilesAreNotReported() throws Exception {
    Path pom = dir.resolve("pom.xml");
    Files.writeString(pom, Files.readString(Path.of("pom.xml")));

    assertThat(
            BuildGuard.violations(pom, dir.resolve("absent.config"), dir.resolve("absent2.config")))
        .isEmpty();
  }

  static Stream<Arguments> weakenedBuilds() {
    return Stream.of(
        pomCase(
            "skip-in-execution",
            DIFFERS,
            edit("<fail>true</fail>", "<skip>true</skip><fail>true</fail>")),
        pomCase("fail-false", DIFFERS, edit("<fail>true</fail>", "<fail>false</fail>")),
        pomCase(
            "search-transitive-false",
            DIFFERS,
            edit(
                "<searchTransitive>true</searchTransitive>",
                "<searchTransitive>false</searchTransitive>")),
        pomCase(
            "exclude-removed",
            DIFFERS,
            edit("<exclude>net.jqwik:*</exclude>", "<exclude>com.example:*</exclude>")),
        pomCase(
            "skip-property",
            ENFORCER_PROPERTY,
            edit("<java.version>", "<enforcer.skip>true</enforcer.skip><java.version>")),
        pomCase(
            "enforcer-version-property",
            ENFORCER_PROPERTY,
            edit("<java.version>", "<enforcer.version>3.7.0</enforcer.version><java.version>")),
        pomCase("wrong-phase", DIFFERS, edit("<phase>validate</phase>", "<phase>verify</phase>")),
        pomCase(
            "level-warn",
            DIFFERS,
            edit("<searchTransitive>", "<level>WARN</level><searchTransitive>")),
        pomCase(
            "includes",
            DIFFERS,
            edit("<message>", "<includes><include>net.jqwik:*</include></includes><message>")),
        pomCase(
            "skip-expression",
            DIFFERS,
            both(
                edit("<fail>true</fail>", "<skip>${x}</skip><fail>true</fail>"),
                edit("<java.version>", "<x>true</x><java.version>"))),
        pomCase(
            "fail-expression",
            DIFFERS,
            both(
                edit("<fail>true</fail>", "<fail>${y}</fail>"),
                edit("<java.version>", "<y>false</y><java.version>"))),
        pomCase(
            "phase-none-split",
            DIFFERS,
            both(
                edit("<phase>validate</phase>", "<phase>none</phase>"),
                JqwikBanSecurityTest::addNoOpEnforceExecution)),
        pomCase(
            "plugin-level-level-warn",
            DIFFERS,
            edit(
                "<version>3.7.0</version>",
                "<version>3.7.0</version><configuration><rules><bannedDependencies>"
                    + "<level>WARN</level></bannedDependencies></rules></configuration>")),
        pomCase(
            "plugin-level-includes",
            DIFFERS,
            edit(
                "<version>3.7.0</version>",
                "<version>3.7.0</version><configuration><rules><bannedDependencies><includes>"
                    + "<include>net.jqwik:*</include></includes></bannedDependencies></rules>"
                    + "</configuration>")),
        pomCase(
            "plugin-management-only",
            ONCE,
            pom -> {
              String block = enforcerBlock(pom);
              return pom.replace(block, "")
                  .replace(
                      "<build>",
                      "<build><pluginManagement><plugins>"
                          + block
                          + "</plugins></pluginManagement>");
            }),
        pomCase(
            "plugin-management-skip",
            ONCE,
            edit(
                "<build>",
                "<build><pluginManagement><plugins><plugin><groupId>org.apache.maven.plugins"
                    + "</groupId><artifactId>maven-enforcer-plugin</artifactId><configuration>"
                    + "<skip>true</skip></configuration></plugin></plugins></pluginManagement>")),
        pomCase(
            "plugin-management-fail-false",
            ONCE,
            edit(
                "<build>",
                "<build><pluginManagement><plugins><plugin><groupId>org.apache.maven.plugins"
                    + "</groupId><artifactId>maven-enforcer-plugin</artifactId><configuration>"
                    + "<fail>false</fail></configuration></plugin></plugins></pluginManagement>")),
        pomCase("profile-skip", ONCE, edit("</project>", PROFILE_ENFORCER_SKIP + "</project>")),
        pomCase(
            "profile-skip-property",
            ENFORCER_PROPERTY,
            edit(
                "</project>",
                "<profiles><profile><id>p</id><properties><enforcer.skip>true</enforcer.skip>"
                    + "</properties></profile></profiles></project>")),
        pomCase(
            "duplicate-plugin",
            ONCE,
            pom -> {
              String block = enforcerBlock(pom);
              return pom.replace(block, block + block.replace("validate", "none"));
            }),
        pomCase(
            "system-property-variables",
            "systemPropertyVariables mentions",
            edit(
                "<argLine>@{argLine}</argLine>",
                "<argLine>@{argLine}</argLine><systemPropertyVariables><cairn.pomPath>decoy.xml"
                    + "</cairn.pomPath></systemPropertyVariables>")),
        pomCase(
            "surefire-plugin-dependency",
            OUTSIDE,
            edit(
                "<artifactId>maven-surefire-plugin</artifactId>",
                "<artifactId>maven-surefire-plugin</artifactId><dependencies>"
                    + JQWIK_DEPENDENCY
                    + "</dependencies>")),
        pomCase(
            "surefire-additional-classpath-dependency",
            OUTSIDE,
            edit(
                "<argLine>@{argLine}</argLine>",
                "<argLine>@{argLine}</argLine><additionalClasspathDependencies>"
                    + JQWIK_DEPENDENCY
                    + "</additionalClasspathDependencies>")),
        pomCase(
            "local-parent",
            "parent must be spring-boot-starter-parent",
            edit("<relativePath/>", "<relativePath>../parent/pom.xml</relativePath>")),
        configCase("maven-config-skip", "maven.config mentions", "-Denforcer.skip=true\n", ""),
        configCase("jvm-config-skip", "jvm.config mentions", "", "-Denforcer.skip=true\n"),
        configCase("maven-config-pom-path", "maven.config mentions", "-Dcairn.pomPath=x\n", ""),
        configCase("maven-config-jqwik", "maven.config mentions", "-Djqwik.x=1\n", ""),
        configCase("jvm-config-jqwik", "jvm.config mentions", "", "-Djqwik.x=1\n"));
  }

  private static Arguments pomCase(String name, String expected, UnaryOperator<String> pomEdit) {
    return Arguments.argumentSet(name, new Case(expected, pomEdit, "", ""));
  }

  private static Arguments configCase(
      String name, String expected, String mavenConfig, String jvmConfig) {
    return Arguments.argumentSet(
        name, new Case(expected, UnaryOperator.identity(), mavenConfig, jvmConfig));
  }

  private static UnaryOperator<String> both(
      UnaryOperator<String> first, UnaryOperator<String> second) {
    return pom -> second.apply(first.apply(pom));
  }

  // Replaces the anchor once. Fails if the anchor is missing or appears more than once.
  private static UnaryOperator<String> edit(String anchor, String replacement) {
    return pom -> {
      assertThat(pom).as("anchor %s", anchor).contains(anchor);
      assertThat(pom.indexOf(anchor))
          .as("anchor %s is unique", anchor)
          .isEqualTo(pom.lastIndexOf(anchor));
      return pom.replace(anchor, replacement);
    };
  }

  private static String addNoOpEnforceExecution(String pom) {
    String marker = "<executions>";
    int at = pom.indexOf("maven-enforcer-plugin");
    int insert = pom.indexOf(marker, at) + marker.length();
    return pom.substring(0, insert)
        + "<execution><id>noop</id><phase>validate</phase><goals><goal>enforce</goal></goals>"
        + "<configuration><rules><alwaysPass/></rules></configuration></execution>"
        + pom.substring(insert);
  }

  private static String enforcerBlock(String pom) {
    int at = pom.indexOf("<artifactId>maven-enforcer-plugin</artifactId>");
    assertThat(at).as("enforcer plugin in pom").isPositive();
    int start = pom.lastIndexOf("<plugin>", at);
    int end = pom.indexOf("</plugin>", at) + "</plugin>".length();
    return pom.substring(start, end);
  }
}
