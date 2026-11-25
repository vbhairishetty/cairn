package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

// TM-07: every published Compose port must bind to the loopback address.
class ComposePortsTest {

  private static final Path COMPOSE_FILE = Path.of("docker-compose.yml");

  @Test
  void everyPublishedPortBindsToLoopback() throws IOException {
    List<String> ports = publishedPorts(COMPOSE_FILE);

    assertThat(ports).isNotEmpty().allSatisfy(port -> assertThat(port).startsWith("127.0.0.1:"));
  }

  @Test
  void portWithoutHostAddressIsDetected() throws IOException {
    Path file = Files.createTempFile("compose-bare", ".yml");
    Files.writeString(file, "services:\n  db:\n    image: x\n    ports:\n      - \"5432:5432\"\n");

    List<String> ports = publishedPorts(file);

    assertThat(ports).containsExactly("5432:5432");
    assertThat(ports).noneMatch(port -> port.startsWith("127.0.0.1:"));
  }

  @Test
  void portBoundToAllInterfacesIsDetected() throws IOException {
    Path file = Files.createTempFile("compose-open", ".yml");
    Files.writeString(
        file, "services:\n  db:\n    image: x\n    ports:\n      - \"0.0.0.0:5432:5432\"\n");

    List<String> ports = publishedPorts(file);

    assertThat(ports).containsExactly("0.0.0.0:5432:5432");
    assertThat(ports).noneMatch(port -> port.startsWith("127.0.0.1:"));
  }

  @SuppressWarnings("unchecked")
  static List<String> publishedPorts(Path file) throws IOException {
    List<String> result = new ArrayList<>();
    try (Reader reader = Files.newBufferedReader(file)) {
      Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
      Map<String, Object> services =
          (Map<String, Object>) ((Map<String, Object>) root).get("services");
      for (Object service : services.values()) {
        Object ports = ((Map<String, Object>) service).get("ports");
        if (ports instanceof List<?> list) {
          list.forEach(port -> result.add(String.valueOf(port)));
        }
      }
    }
    return result;
  }
}
