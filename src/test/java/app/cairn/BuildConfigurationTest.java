package app.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BuildConfigurationTest {

  @Test
  void errorProneTreatsReferenceAndBigDecimalEqualityAsErrors() throws IOException {
    String pom = Files.readString(Path.of("pom.xml"));

    assertThat(pom)
        .contains("-Xep:ReferenceEquality:ERROR")
        .contains("-Xep:BigDecimalEquals:ERROR");
  }

  @Test
  void buildGuardFindsNoViolationsInTheRealBuildFiles() throws Exception {
    assertThat(BuildGuard.violations(BuildGuard.BuildFiles.under(Path.of(".")))).isEmpty();
  }
}
