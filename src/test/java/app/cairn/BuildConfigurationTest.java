package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class BuildConfigurationTest {

  // Test hook: point at a copy of the pom to check that these tests catch a weakened ban.
  private static final Path POM = Path.of(System.getProperty("cairn.pomPath", "pom.xml"));

  private static final Path MAVEN_CONFIG =
      Path.of(System.getProperty("cairn.mavenConfigPath", ".mvn/maven.config"));

  private static final Path JVM_CONFIG =
      Path.of(System.getProperty("cairn.jvmConfigPath", ".mvn/jvm.config"));

  private static final String ENFORCER =
      "/project/build/plugins/plugin[artifactId='maven-enforcer-plugin']";
  private static final String EXECUTION =
      ENFORCER + "/executions/execution[phase='validate' and goals/goal='enforce']";
  private static final String RULE = EXECUTION + "/configuration/rules/bannedDependencies";
  private static final String PROFILES = "/project/profiles/profile";

  @Test
  void errorProneTreatsReferenceAndBigDecimalEqualityAsErrors() throws IOException {
    String pom = Files.readString(POM);

    assertThat(pom)
        .contains("-Xep:ReferenceEquality:ERROR")
        .contains("-Xep:BigDecimalEquals:ERROR");
  }

  @Test
  void enforcerRunsEnforceGoalInValidatePhase() throws Exception {
    assertThat(pomHas("boolean(" + EXECUTION + ")")).isTrue();
  }

  @Test
  void buildBansJqwik() throws Exception {
    assertThat(pomHas("boolean(" + RULE + "/excludes/exclude[normalize-space()='net.jqwik:*'])"))
        .isTrue();
  }

  @Test
  void jqwikBanSearchesTransitiveDependencies() throws Exception {
    assertThat(pomHas("not(" + RULE + "/searchTransitive[normalize-space()!='true'])")).isTrue();
  }

  @Test
  void jqwikBanFailsTheBuildAtErrorLevel() throws Exception {
    assertThat(pomHas("not(" + RULE + "/level[normalize-space()!='ERROR'])")).isTrue();
  }

  @Test
  void jqwikBanHasNoIncludesThatAllowIt() throws Exception {
    assertThat(pomHas("not(" + RULE + "/includes)")).isTrue();
  }

  @Test
  void enforcerHasNoSkipSetting() throws Exception {
    assertThat(pomHas("not(" + ENFORCER + "//skip)")).isTrue();
  }

  @Test
  void enforcerSkipPropertyIsNotSet() throws Exception {
    assertThat(pomHas("not(/project/properties/enforcer.skip)")).isTrue();
  }

  @Test
  void enforcerFailSettingIsAbsentOrTrue() throws Exception {
    assertThat(pomHas("not(" + ENFORCER + "//fail[normalize-space()!='true'])")).isTrue();
  }

  @Test
  void noProfileConfiguresTheEnforcer() throws Exception {
    assertThat(pomHas("not(" + PROFILES + "//plugin[artifactId='maven-enforcer-plugin'])"))
        .isTrue();
  }

  @Test
  void noProfileSetsTheEnforcerSkipProperty() throws Exception {
    assertThat(pomHas("not(" + PROFILES + "/properties/enforcer.skip)")).isTrue();
  }

  @Test
  void mavenConfigDoesNotSkipTheEnforcer() throws IOException {
    String config = Files.exists(MAVEN_CONFIG) ? Files.readString(MAVEN_CONFIG) : "";

    assertThat(config).doesNotContain("enforcer.skip");
  }

  @Test
  void jvmConfigDoesNotSkipTheEnforcer() throws IOException {
    String config = Files.exists(JVM_CONFIG) ? Files.readString(JVM_CONFIG) : "";

    assertThat(config).doesNotContain("enforcer.skip");
  }

  @Test
  void enforcerPluginIsDeclaredOnce() throws Exception {
    assertThat(pomHas("count(/project/build/plugins/plugin[artifactId='maven-enforcer-plugin'])=1"))
        .isTrue();
  }

  private static boolean pomHas(String xpath) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    Document pom = factory.newDocumentBuilder().parse(POM.toFile());
    return (Boolean)
        XPathFactory.newInstance().newXPath().evaluate(xpath, pom, XPathConstants.BOOLEAN);
  }
}
