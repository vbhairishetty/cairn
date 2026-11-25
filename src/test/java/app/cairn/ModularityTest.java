package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

  private static final List<String> EXPECTED_MODULES =
      List.of(
          "common",
          "documents",
          "jobs",
          "extract",
          "normalize",
          "validate",
          "engine",
          "diff",
          "rules",
          "profile",
          "timeline",
          "reminders",
          "ask",
          "web",
          "cli");

  private final ApplicationModules modules = ApplicationModules.of(CairnApplication.class);

  @Test
  void modulesFollowTheAllowedDependencies() {
    modules.verify();
  }

  @Test
  void detectsExactlyTheFifteenModules() {
    List<String> detected = modules.stream().map(m -> m.getIdentifier().toString()).toList();

    assertThat(detected).containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
  }
}
