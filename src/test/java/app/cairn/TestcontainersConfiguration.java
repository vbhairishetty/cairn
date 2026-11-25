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
        DockerImageName.parse(composeImage()).asCompatibleSubstituteFor("postgres");
    return new PostgreSQLContainer(image);
  }

  @SuppressWarnings("unchecked")
  static String composeImage() {
    try (Reader reader = Files.newBufferedReader(Path.of("docker-compose.yml"))) {
      Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
      Map<String, Object> services =
          (Map<String, Object>) ((Map<String, Object>) root).get("services");
      Map<String, Object> postgres = (Map<String, Object>) services.get("postgres");
      return (String) postgres.get("image");
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read docker-compose.yml", e);
    }
  }
}
