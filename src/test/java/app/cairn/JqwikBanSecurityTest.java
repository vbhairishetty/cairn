package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
  private static final String PARENT = "parent must be spring-boot-starter-parent";
  private static final String TEST_PLUGIN_DEPENDENCIES =
      "surefire or failsafe plugin declares dependencies";
  private static final String INTERPOLATED = "groupId or artifactId contains ${";
  private static final String MAVEN_CONFIG = "maven.config must be absent or blank";
  private static final String JVM_CONFIG = "jvm.config differs from the committed lines";
  private static final String MAVEN_CONFIG_PATH = ".mvn/maven.config";
  private static final String JVM_CONFIG_PATH = ".mvn/jvm.config";
  private static final String MVNW_PATH = "mvnw";
  private static final String WRAPPER_PATH = ".mvn/wrapper/maven-wrapper.properties";
  private static final String EXTENSIONS_PATH = ".mvn/extensions.xml";
  private static final List<String> LIVE_FILES =
      List.of(
          "pom.xml", MAVEN_CONFIG_PATH, JVM_CONFIG_PATH, MVNW_PATH, WRAPPER_PATH, EXTENSIONS_PATH);
  private static final String WRAPPER_FILES = "differs from the pinned SHA-256";
  private static final String EXTENSIONS = ".mvn/extensions.xml must be absent";
  private static final String JQWIK_DEPENDENCY =
      "<dependency><groupId>net.jqwik</groupId><artifactId>jqwik</artifactId>"
          + "<version>1.10.2</version></dependency>";
  private static final String PROFILE_ENFORCER_SKIP =
      "<profiles><profile><id>p</id><activation><activeByDefault>true</activeByDefault>"
          + "</activation><build><plugins><plugin><artifactId>maven-enforcer-plugin</artifactId>"
          + "<configuration><skip>true</skip></configuration></plugin></plugins></build>"
          + "</profile></profiles>";

  // Edits map a path under the repo root to a change of that file's live text. A file that is
  // absent live starts as empty text, so an edit can create it.
  record Case(String expected, Map<String, UnaryOperator<String>> edits) {}

  @TempDir Path dir;

  @ParameterizedTest(name = "{argumentSetName} is reported")
  @MethodSource("weakenedBuilds")
  void weakenedBuildIsReported(Case weakened) throws Exception {
    copyLiveFiles(dir, weakened.edits());

    assertThat(BuildGuard.violations(BuildGuard.BuildFiles.under(dir)))
        .anySatisfy(violation -> assertThat(violation).contains(weakened.expected()));
  }

  @Test
  void unchangedLiveBuildFilesAreNotReported() throws Exception {
    copyLiveFiles(dir, Map.of());

    assertThat(BuildGuard.violations(BuildGuard.BuildFiles.under(dir))).isEmpty();
  }

  @Test
  void blankMavenConfigIsNotReported() throws Exception {
    copyLiveFiles(dir, Map.of(MAVEN_CONFIG_PATH, live -> "\n  \n"));

    assertThat(BuildGuard.violations(BuildGuard.BuildFiles.under(dir))).isEmpty();
  }

  private static void copyLiveFiles(Path target, Map<String, UnaryOperator<String>> edits)
      throws IOException {
    for (String path : LIVE_FILES) {
      Path source = Path.of(path);
      String text = Files.exists(source) ? Files.readString(source) : "";
      String written = edits.getOrDefault(path, UnaryOperator.identity()).apply(text);
      if (Files.exists(source) || !written.isEmpty()) {
        Path copy = target.resolve(path);
        Files.createDirectories(copy.getParent());
        Files.writeString(copy, written);
      }
    }
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
            "surefire-plugin-dependency-interpolated",
            TEST_PLUGIN_DEPENDENCIES,
            both(
                edit(
                    "<artifactId>maven-surefire-plugin</artifactId>",
                    "<artifactId>maven-surefire-plugin</artifactId><dependencies><dependency>"
                        + "<groupId>net.${a}${b}</groupId><artifactId>${a}${b}</artifactId>"
                        + "<version>1.10.2</version></dependency></dependencies>"),
                edit("<java.version>", "<a>jq</a><b>wik</b><java.version>"))),
        pomCase(
            "failsafe-plugin-dependency",
            TEST_PLUGIN_DEPENDENCIES,
            edit(
                "<artifactId>maven-failsafe-plugin</artifactId>",
                "<artifactId>maven-failsafe-plugin</artifactId><dependencies>"
                    + "<dependency><groupId>com.example</groupId><artifactId>x</artifactId>"
                    + "<version>1</version></dependency></dependencies>")),
        pomCase(
            "project-dependency-interpolated",
            INTERPOLATED,
            edit(
                "<dependencies>\n    <dependency>",
                "<dependencies>\n    <dependency><groupId>net.${g}</groupId>"
                    + "<artifactId>x</artifactId></dependency>\n    <dependency>")),
        pomCase(
            "plugin-management-interpolated-enforcer-skip",
            INTERPOLATED,
            both(
                edit("<java.version>", "<e>enforcer</e><java.version>"),
                edit(
                    "<build>",
                    "<build><pluginManagement><plugins><plugin><groupId>org.apache.maven.plugins"
                        + "</groupId><artifactId>maven-${e}-plugin</artifactId><configuration>"
                        + "<skip>true</skip></configuration></plugin></plugins>"
                        + "</pluginManagement>"))),
        pomCase(
            "dependency-comment-split-jqwik",
            OUTSIDE,
            edit(
                "<dependencies>\n    <dependency>",
                "<dependencies>\n    <dependency><groupId>net.jq<!-- -->wik</groupId>"
                    + "<artifactId>jq<!-- -->wik</artifactId><version>1.10.2</version>"
                    + "</dependency>\n    <dependency>")),
        pomCase(
            "dependency-cdata-split-jqwik",
            OUTSIDE,
            edit(
                "<dependencies>\n    <dependency>",
                "<dependencies>\n    <dependency><groupId>net.jq<![CDATA[wik]]></groupId>"
                    + "<artifactId>jq<![CDATA[wik]]></artifactId><version>1.10.2</version>"
                    + "</dependency>\n    <dependency>")),
        pomCase(
            "surefire-second-block-interpolated-artifact-id",
            INTERPOLATED,
            both(
                edit("<java.version>", "<s>surefire</s><java.version>"),
                edit(
                    "<artifactId>maven-failsafe-plugin</artifactId>",
                    "<artifactId>maven-${s}-plugin</artifactId><configuration><skip>true"
                        + "</skip></configuration></plugin><plugin><groupId>"
                        + "org.apache.maven.plugins</groupId>"
                        + "<artifactId>maven-failsafe-plugin</artifactId>"))),
        pomCase("relative-path-removed", PARENT, edit("<relativePath/>", "")),
        pomCase(
            "parent-artifact-changed",
            PARENT,
            edit(
                "<artifactId>spring-boot-starter-parent</artifactId>",
                "<artifactId>other-parent</artifactId>")),
        pomCase(
            "local-parent",
            PARENT,
            edit("<relativePath/>", "<relativePath>../parent/pom.xml</relativePath>")),
        appendCase("maven-config-skip", MAVEN_CONFIG, MAVEN_CONFIG_PATH, "-Denforcer.skip=true\n"),
        appendCase("maven-config-pom-path", MAVEN_CONFIG, MAVEN_CONFIG_PATH, "-Dcairn.pomPath=x\n"),
        appendCase("maven-config-jqwik", MAVEN_CONFIG, MAVEN_CONFIG_PATH, "-Djqwik.x=1\n"),
        appendCase("maven-config-file-redirect", MAVEN_CONFIG, MAVEN_CONFIG_PATH, "-fbuild.xml\n"),
        appendCase("maven-config-settings", MAVEN_CONFIG, MAVEN_CONFIG_PATH, "-ssettings.xml\n"),
        appendCase("mvnw-edited", WRAPPER_FILES, MVNW_PATH, "# extra\n"),
        appendCase("wrapper-properties-edited", WRAPPER_FILES, WRAPPER_PATH, "# extra\n"),
        appendCase("extensions-xml-present", EXTENSIONS, EXTENSIONS_PATH, "<extensions/>"),
        appendCase("jvm-config-skip", JVM_CONFIG, JVM_CONFIG_PATH, "-Denforcer.skip=true\n"),
        appendCase("jvm-config-jqwik", JVM_CONFIG, JVM_CONFIG_PATH, "-Djqwik.x=1\n"),
        appendCase("jvm-config-extra-line", JVM_CONFIG, JVM_CONFIG_PATH, "-Dfoo=bar\n"));
  }

  private static Arguments pomCase(String name, String expected, UnaryOperator<String> pomEdit) {
    return fileCase(name, expected, "pom.xml", pomEdit);
  }

  private static Arguments fileCase(
      String name, String expected, String path, UnaryOperator<String> edit) {
    return Arguments.argumentSet(name, new Case(expected, Map.of(path, edit)));
  }

  private static Arguments appendCase(String name, String expected, String path, String appended) {
    return fileCase(name, expected, path, live -> live + appended);
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
