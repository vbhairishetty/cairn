package app.cairn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

  // A plugin version bump must update this approved shape and the pom together. The comparison is
  // order-sensitive on purpose: a reordered plugin is reported, which fails safe.
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

  // Must equal the non-blank lines of .mvn/jvm.config. A change to that file must update both.
  private static final List<String> COMMITTED_JVM_CONFIG =
      List.of(
          "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
          "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
          "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED");

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

    if (count(
            document,
            "/project/parent[groupId='org.springframework.boot'"
                + " and artifactId='spring-boot-starter-parent'"
                + " and relativePath and normalize-space(relativePath)='']")
        != 1) {
      found.add("parent must be spring-boot-starter-parent with an empty relativePath");
    }
    // Enforcer sees project dependencies only, so jqwik elsewhere (for example as a Surefire
    // plugin dependency) would reach the test classpath unnoticed.
    if (mentionsJqwik(document, enforcerPlugin(document))) {
      found.add("jqwik appears outside the approved enforcer plugin");
    }
    if (count(document, "//properties/*[starts-with(name(),'enforcer.')]") > 0) {
      found.add("a pom property starts with enforcer.");
    }
    if (count(
            document,
            "//plugin[normalize-space(artifactId)='maven-surefire-plugin'"
                + " or normalize-space(artifactId)='maven-failsafe-plugin']"
                + "//*[self::dependencies or self::additionalClasspathDependencies"
                + " or self::additionalClasspathElements or self::classpathDependencyExcludes]")
        > 0) {
      found.add(
          "surefire or failsafe plugin declares dependencies or classpath settings;"
              + " they bypass the Enforcer");
    }
    // Interpolation could assemble a banned coordinate from properties.
    if (count(document, "//dependency/*[self::groupId or self::artifactId][contains(.,'${')]")
        > 0) {
      found.add("a dependency groupId or artifactId contains ${");
    }

    checkMavenConfig(mavenConfig, found);
    checkJvmConfig(jvmConfig, found);
    return found;
  }

  // Maven reads .mvn/maven.config as command-line arguments (-f, -s, -D), so any content could
  // redirect the build away from the guarded pom.
  private static void checkMavenConfig(Path file, List<String> found) throws IOException {
    if (Files.exists(file) && !Files.readString(file).isBlank()) {
      found.add(file.getFileName() + " must be absent or blank");
    }
  }

  private static void checkJvmConfig(Path file, List<String> found) throws IOException {
    if (Files.exists(file) && !lines(Files.readString(file)).equals(COMMITTED_JVM_CONFIG)) {
      found.add(file.getFileName() + " differs from the committed lines in BuildGuard");
    }
  }

  private static List<String> lines(String text) {
    return text.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
  }

  private static String shape(Document document) throws XPathExpressionException {
    return canonical(enforcerPlugin(document));
  }

  private static Node enforcerPlugin(Document document) throws XPathExpressionException {
    return (Node)
        XPathFactory.newInstance()
            .newXPath()
            .evaluate(
                "/project/build/plugins/plugin[" + ENFORCER_ID + "]",
                document,
                XPathConstants.NODE);
  }

  // Checks names, values, attributes and comments of every node except the skipped subtree.
  private static boolean mentionsJqwik(Node node, Node skipped) {
    if (node.isSameNode(skipped)) {
      return false;
    }
    boolean here =
        node.getNodeName().toLowerCase(Locale.ROOT).contains("jqwik")
            || (node.getNodeValue() != null
                && node.getNodeValue().toLowerCase(Locale.ROOT).contains("jqwik"));
    return here || attributesMention(node) || childrenMention(node, skipped);
  }

  private static boolean attributesMention(Node node) {
    NamedNodeMap attributes = node.getAttributes();
    for (int i = 0; attributes != null && i < attributes.getLength(); i++) {
      if (mentionsJqwik(attributes.item(i), null)) {
        return true;
      }
    }
    return false;
  }

  private static boolean childrenMention(Node node, Node skipped) {
    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (mentionsJqwik(child, skipped)) {
        return true;
      }
    }
    return false;
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
