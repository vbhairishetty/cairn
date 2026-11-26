package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnvExampleTest {

  private static final Pattern PASSWORD_LINE =
      Pattern.compile("^\\s*(export\\s+)?CAIRN_DB_PASSWORD\\s*=(.*)$");

  // Compose lets the last assignment win, so exactly one line must exist and it must be empty.
  private static boolean passwordIsEmpty(String text) {
    List<String> values =
        text.lines()
            .map(PASSWORD_LINE::matcher)
            .filter(Matcher::matches)
            .map(m -> m.group(2).trim())
            .toList();
    return values.size() == 1 && values.get(0).isEmpty();
  }

  @Test
  void examplePasswordIsEmptySoComposeStopsUntilOneIsSet() throws IOException {
    String text = Files.readString(Path.of(".env.example"));

    assertThat(passwordIsEmpty(text)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "CAIRN_DB_PASSWORD=change-me\n",
        "CAIRN_DB_PASSWORD=\nCAIRN_DB_PASSWORD=change-me\n",
        "CAIRN_DB_PASSWORD=\nexport CAIRN_DB_PASSWORD=x\n",
        "  CAIRN_DB_PASSWORD = x\n",
        "CAIRN_DB_USER=cairn\n"
      })
  void unsafeOrMissingPasswordLineIsRejected(String text) {
    assertThat(passwordIsEmpty(text)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"CAIRN_DB_PASSWORD=\n", "# note\nCAIRN_DB_USER=a\nCAIRN_DB_PASSWORD=\n"})
  void singleEmptyPasswordLineIsAccepted(String text) {
    assertThat(passwordIsEmpty(text)).isTrue();
  }
}
