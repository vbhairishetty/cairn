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

  private static final String ENFORCER =
      "/project/build/plugins/plugin[artifactId='maven-enforcer-plugin']";
  private static final String EXECUTION = ENFORCER + "/executions/execution";
  private static final String RULE = EXECUTION + "/configuration/rules/bannedDependencies";

  @Test
  void errorProneTreatsReferenceAndBigDecimalEqualityAsErrors() throws IOException {
    String pom = Files.readString(POM);

    assertThat(pom)
        .contains("-Xep:ReferenceEquality:ERROR")
        .contains("-Xep:BigDecimalEquals:ERROR");
  }

  @Test
  void enforcerRunsEnforceGoalInValidatePhase() throws Exception {
    assertThat(pomHas("boolean(" + EXECUTION + "[phase='validate' and goals/goal='enforce'])"))
        .isTrue();
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
  void enforcerIsNotSkipped() throws Exception {
    assertThat(pomHas("not(" + ENFORCER + "//skip[normalize-space()='true'])")).isTrue();
    assertThat(pomHas("not(/project/properties/enforcer.skip)")).isTrue();
  }

  @Test
  void enforcerFailsTheBuildOnViolation() throws Exception {
    assertThat(pomHas("not(" + ENFORCER + "//fail[normalize-space()='false'])")).isTrue();
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
