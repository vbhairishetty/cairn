package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// TM-07: every published Compose port must bind to the loopback address, and nothing may
// change that from another file. The guard accepts only short-form 127.0.0.1 ports, so a
// long-form entry with host_ip 127.0.0.1 fails closed.
class ComposePortsTest {

  private static final String GOOD =
      "services:\n  db:\n    image: x\n    ports:\n      - \"127.0.0.1:5432:5432\"\n";

  @Test
  void everyPublishedPortBindsToLoopback() {
    assertThat(ComposeGuard.violations(Path.of("docker-compose.yml"), Path.of("."))).isEmpty();
  }

  @Test
  void composeFilePublishesAtLeastOnePort() {
    assertThat(ComposeGuard.publishedPorts(Path.of("docker-compose.yml"))).isNotEmpty();
  }

  @Test
  void loopbackOnlyFileHasNoViolations(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("docker-compose.yml");
    Files.writeString(file, GOOD);

    assertThat(ComposeGuard.violations(file, dir)).isEmpty();
  }

  static Stream<Arguments> bypasses() {
    return Stream.of(
        Arguments.of(
            "bare port",
            "services:\n  db:\n    image: x\n    ports:\n      - \"5432:5432\"\n",
            "unrelated.txt"),
        Arguments.of(
            "all interfaces",
            "services:\n  db:\n    image: x\n    ports:\n      - \"0.0.0.0:5432:5432\"\n",
            "unrelated.txt"),
        Arguments.of(
            "long form without host_ip",
            "services:\n  db:\n    image: x\n    ports:\n      - target: 5432\n        published: 5432\n",
            "unrelated.txt"),
        Arguments.of("override file", GOOD, "docker-compose.override.yml"),
        Arguments.of("override yaml file", GOOD, "docker-compose.override.yaml"),
        Arguments.of("compose override file", GOOD, "compose.override.yml"),
        Arguments.of("compose override yaml file", GOOD, "compose.override.yaml"),
        Arguments.of("sibling compose.yaml", GOOD, "compose.yaml"),
        Arguments.of("sibling compose.yml", GOOD, "compose.yml"),
        Arguments.of("include", "include:\n  - other.yml\n" + GOOD, "unrelated.txt"),
        Arguments.of(
            "extends",
            "services:\n  db:\n    extends:\n      file: other.yml\n      service: base\n",
            "unrelated.txt"),
        Arguments.of(
            "host network",
            "services:\n  db:\n    image: x\n    network_mode: host\n",
            "unrelated.txt"),
        Arguments.of(
            "interpolated host network",
            "services:\n  db:\n    image: x\n    network_mode: \"${NET:-host}\"\n",
            "unrelated.txt"),
        Arguments.of(
            "bridge network_mode key",
            "services:\n  db:\n    image: x\n    network_mode: bridge\n",
            "unrelated.txt"),
        Arguments.of(
            "interpolated port",
            "services:\n  db:\n    image: x\n    ports:\n      - \"127.0.0.1:${PORT}:5432\"\n",
            "unrelated.txt"),
        Arguments.of(
            "long form with loopback host_ip",
            "services:\n  db:\n    image: x\n    ports:\n      - target: 5432\n        published: 5432\n        host_ip: 127.0.0.1\n",
            "unrelated.txt"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("bypasses")
  void bypassIsReported(String name, String composeYaml, String siblingFile, @TempDir Path dir)
      throws IOException {
    Path file = dir.resolve("docker-compose.yml");
    Files.writeString(file, composeYaml);
    Files.writeString(dir.resolve(siblingFile), "x: 1\n");

    assertThat(ComposeGuard.violations(file, dir)).isNotEmpty();
  }
}
