package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ComposeImageTest {

  private static final Pattern PINNED = Pattern.compile(".+:[^@]+@sha256:[0-9a-f]{64}$");

  @Test
  void composeImageIsPinnedByTagAndDigest() {
    String image = TestcontainersConfiguration.composeImage(Path.of("docker-compose.yml"));

    assertThat(image).matches(PINNED);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "services:\n  other:\n    image: x\n",
        "services:\n  postgres:\n    ports: []\n",
        "services:\n  postgres:\n    image: ${IMG}\n",
        "other: 1\n"
      })
  void unusableComposeFileGivesClearError(String yaml, @TempDir Path dir) throws IOException {
    Path file = dir.resolve("docker-compose.yml");
    Files.writeString(file, yaml);

    assertThatThrownBy(() -> TestcontainersConfiguration.composeImage(file))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("postgres");
  }
}
