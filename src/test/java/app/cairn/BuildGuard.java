package app.cairn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Checks that the build still bans jqwik (ADR 0006). The Enforcer plugin must match one approved
 * shape exactly, so any extra setting that could weaken or skip it is reported. Files are read from
 * the paths given, never from system properties.
 */
final class BuildGuard {

  private static final String ENFORCER_ID = "normalize-space(artifactId)='maven-enforcer-plugin'";

  // A plugin version bump must update this approved shape and the pom together.
  private static final String APPROVED_PLUGIN =
      """
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
        <version>3.7.0</version>
        <executions>
          <execution>
            <id>enforce-banned-dependencies</id>
            <phase>validate</phase>
            <goals>
              <goal>enforce</goal>
            </goals>
            <configuration>
              <rules>
                <bannedDependencies>
                  <searchTransitive>true</searchTransitive>
                  <excludes>
                    <exclude>net.jqwik:*</exclude>
                  </excludes>
                  <message>net.jqwik is banned, see docs/adr/0006-remove-jqwik.md</message>
                </bannedDependencies>
              </rules>
              <fail>true</fail>
            </configuration>
          </execution>
        </executions>
      </plugin>
      """;

  private BuildGuard() {}

  static List<String> violations(Path pom, Path mavenConfig, Path jvmConfig) throws Exception {
    List<String> found = new ArrayList<>();
    Document document = parse(Files.readString(pom));

    int everywhere = count(document, "//plugin[" + ENFORCER_ID + "]");
    int inBuild = count(document, "/project/build/plugins/plugin[" + ENFORCER_ID + "]");
    if (everywhere != 1 || inBuild != 1) {
      found.add(
          "maven-enforcer-plugin must be declared exactly once, under build/plugins (found "
              + everywhere
              + " in the pom, "
              + inBuild
              + " in build/plugins)");
    } else if (!shape(document).equals(canonical(parse(APPROVED_PLUGIN).getDocumentElement()))) {
      found.add("maven-enforcer-plugin differs from the approved configuration in BuildGuard");
    }

    if (count(document, "//properties/*[starts-with(name(),'enforcer.')]") > 0) {
      found.add("a pom property starts with enforcer.");
    }
    if (count(
            document,
            "//systemPropertyVariables/*[contains(name(),'enforcer') or contains(name(),'cairn.')"
                + " or contains(.,'enforcer') or contains(.,'cairn.')]")
        > 0) {
      found.add("systemPropertyVariables mentions enforcer or cairn.");
    }
    if (count(document, "//argLine[contains(.,'enforcer') or contains(.,'cairn.')]") > 0) {
      found.add("argLine mentions enforcer or cairn.");
    }

    checkConfig(mavenConfig, found);
    checkConfig(jvmConfig, found);
    return found;
  }

  private static void checkConfig(Path file, List<String> found) throws IOException {
    String text = Files.exists(file) ? Files.readString(file) : "";
    if (text.contains("enforcer") || text.contains("cairn.")) {
      found.add(file.getFileName() + " mentions enforcer or cairn.");
    }
  }

  private static String shape(Document document) throws XPathExpressionException {
    Node plugin =
        (Node)
            XPathFactory.newInstance()
                .newXPath()
                .evaluate(
                    "/project/build/plugins/plugin[" + ENFORCER_ID + "]",
                    document,
                    XPathConstants.NODE);
    return canonical(plugin);
  }

  private static int count(Document document, String xpath) throws XPathExpressionException {
    Double result =
        (Double)
            XPathFactory.newInstance()
                .newXPath()
                .evaluate("count(" + xpath + ")", document, XPathConstants.NUMBER);
    return result.intValue();
  }

  // Element names, attributes and trimmed text only; comments and whitespace are ignored.
  private static String canonical(Node node) {
    if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
      return node.getTextContent().trim();
    }
    StringBuilder out = new StringBuilder(node.getNodeName());
    NamedNodeMap attributes = node.getAttributes();
    TreeMap<String, String> sorted = new TreeMap<>();
    for (int i = 0; i < attributes.getLength(); i++) {
      sorted.put(attributes.item(i).getNodeName(), attributes.item(i).getNodeValue());
    }
    out.append(sorted).append('[');
    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      String part = childPart(child);
      if (!part.isEmpty()) {
        out.append(part).append(';');
      }
    }
    return out.append(']').toString();
  }

  private static String childPart(Node child) {
    boolean element = child.getNodeType() == Node.ELEMENT_NODE;
    boolean text =
        child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE;
    return element || text ? canonical(child) : "";
  }

  private static Document parse(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(new InputSource(new java.io.StringReader(xml)));
  }
}
