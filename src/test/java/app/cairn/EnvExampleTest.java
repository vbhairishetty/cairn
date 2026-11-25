package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EnvExampleTest {

  @Test
  void examplePasswordIsEmptySoComposeStopsUntilOneIsSet() throws IOException {
    String value =
        Files.readAllLines(Path.of(".env.example")).stream()
            .filter(line -> line.startsWith("CAIRN_DB_PASSWORD="))
            .map(line -> line.substring("CAIRN_DB_PASSWORD=".length()))
            .findFirst()
            .orElseThrow();

    assertThat(value).isEmpty();
  }
}
