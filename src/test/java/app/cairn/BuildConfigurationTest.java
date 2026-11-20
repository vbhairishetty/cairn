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
import org.w3c.dom.NodeList;

class BuildConfigurationTest {

  @Test
  void errorProneTreatsReferenceAndBigDecimalEqualityAsErrors() throws IOException {
    String pom = Files.readString(Path.of("pom.xml"));

    assertThat(pom)
        .contains("-Xep:ReferenceEquality:ERROR")
        .contains("-Xep:BigDecimalEquals:ERROR");
  }

  @Test
  void buildBansJqwik() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    Document pom = factory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());

    NodeList excludes =
        (NodeList)
            XPathFactory.newInstance()
                .newXPath()
                .evaluate(
                    "//*[local-name()='plugin'][*[local-name()='artifactId']"
                        + "='maven-enforcer-plugin']//*[local-name()='bannedDependencies']"
                        + "//*[local-name()='excludes']/*[local-name()='exclude']",
                    pom,
                    XPathConstants.NODESET);
    java.util.List<String> values = new java.util.ArrayList<>();
    for (int i = 0; i < excludes.getLength(); i++) {
      values.add(excludes.item(i).getTextContent().trim());
    }

    assertThat(values).contains("net.jqwik:*");
  }
}
