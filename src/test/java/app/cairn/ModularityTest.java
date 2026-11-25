package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.Modulithic;
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

  @ParameterizedTest(name = "{0}")
  @MethodSource("architectureTable")
  void declaredDependenciesMatchTheArchitectureTable(String module, List<String> expected)
      throws ClassNotFoundException {
    ApplicationModule annotation =
        Class.forName("app.cairn." + module + ".package-info")
            .getPackage()
            .getAnnotation(ApplicationModule.class);

    assertThat(annotation).isNotNull();
    assertThat(annotation.type()).isEqualTo(ApplicationModule.Type.CLOSED);
    assertThat(annotation.allowedDependencies()).containsExactlyInAnyOrderElementsOf(expected);
  }

  static Stream<Arguments> architectureTable() {
    List<String> everyOther =
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
            "ask");
    return Stream.of(
        Arguments.of("common", List.of()),
        Arguments.of("documents", List.of("common")),
        Arguments.of("jobs", List.of("common")),
        Arguments.of("extract", List.of("common", "documents", "jobs")),
        Arguments.of("normalize", List.of("common", "extract")),
        Arguments.of("validate", List.of("common", "normalize")),
        Arguments.of("engine", List.of("common", "validate", "rules")),
        Arguments.of("diff", List.of("common", "validate")),
        Arguments.of("rules", List.of("common")),
        Arguments.of("profile", List.of("common")),
        Arguments.of("timeline", List.of("common", "rules")),
        Arguments.of("reminders", List.of("common", "validate", "rules", "profile")),
        Arguments.of("ask", List.of("common", "engine", "validate", "rules")),
        Arguments.of("web", everyOther),
        Arguments.of("cli", everyOther));
  }

  @Test
  void applicationDeclaresNoSharedModulesOrModulithicSettings() {
    assertThat(modules.getSharedModules()).isEmpty();
    assertThat(AnnotatedElementUtils.hasAnnotation(CairnApplication.class, Modulithic.class))
        .isFalse();
  }

  @Test
  void detectsExactlyTheFifteenModules() {
    List<String> detected = modules.stream().map(m -> m.getIdentifier().toString()).toList();

    assertThat(detected).containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
  }
}
