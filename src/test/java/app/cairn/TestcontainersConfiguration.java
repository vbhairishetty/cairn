package app.cairn;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Starts the same pgvector image that docker-compose.yml pins, so there is one tag to bump. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    DockerImageName image =
        DockerImageName.parse(composeImage(Path.of("docker-compose.yml")))
            .asCompatibleSubstituteFor("postgres");
    return new PostgreSQLContainer(image);
  }

  static String composeImage(Path composeFile) {
    Object root;
    try (Reader reader = Files.newBufferedReader(composeFile)) {
      root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + composeFile, e);
    }
    Object image = child(child(child(root, "services"), "postgres"), "image");
    if (!(image instanceof String text) || text.isBlank() || text.contains("${")) {
      throw new IllegalStateException(
          "services.postgres.image in " + composeFile + " must be a literal image reference");
    }
    return text;
  }

  private static Object child(Object parent, String key) {
    return parent instanceof Map<?, ?> map ? map.get(key) : null;
  }
}
