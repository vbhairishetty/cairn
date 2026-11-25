package app.cairn;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * TM-07 rule for Compose files. Only short-form ports that start with 127.0.0.1: are accepted, so a
 * long-form entry fails closed. Files that can change the published ports from elsewhere are
 * reported too.
 */
final class ComposeGuard {

  private static final String LOOPBACK = "127.0.0.1:";

  private static final List<String> FORBIDDEN_SIBLINGS =
      List.of(
          "compose.yaml",
          "compose.yml",
          "docker-compose.override.yml",
          "docker-compose.override.yaml",
          "compose.override.yml",
          "compose.override.yaml");

  private ComposeGuard() {}

  static List<String> violations(Path composeFile, Path repoRoot) {
    List<String> result = new ArrayList<>();
    for (String name : FORBIDDEN_SIBLINGS) {
      if (Files.exists(repoRoot.resolve(name))) {
        result.add("file must not exist: " + name);
      }
    }
    Map<?, ?> root = load(composeFile);
    if (root.containsKey("include")) {
      result.add("top-level include is not allowed");
    }
    services(root)
        .forEach((name, service) -> {
          if (service.containsKey("extends")) {
            result.add("service " + name + " uses extends");
          }
          if ("host".equals(service.get("network_mode"))) {
            result.add("service " + name + " uses network_mode host");
          }
          for (String port : ports(service)) {
            if (!port.startsWith(LOOPBACK)) {
              result.add("service " + name + " port is not loopback short form: " + port);
            }
          }
        });
    return result;
  }

  static List<String> publishedPorts(Path composeFile) {
    List<String> result = new ArrayList<>();
    services(load(composeFile)).values().forEach(service -> result.addAll(ports(service)));
    return result;
  }

  private static List<String> ports(Map<?, ?> service) {
    List<String> result = new ArrayList<>();
    if (service.get("ports") instanceof List<?> list) {
      list.forEach(port -> result.add(String.valueOf(port)));
    }
    return result;
  }

  private static Map<String, Map<?, ?>> services(Map<?, ?> root) {
    Map<String, Map<?, ?>> result = new java.util.LinkedHashMap<>();
    if (root.get("services") instanceof Map<?, ?> services) {
      services.forEach((name, service) -> {
        if (service instanceof Map<?, ?> map) {
          result.put(String.valueOf(name), map);
        }
      });
    }
    return result;
  }

  private static Map<?, ?> load(Path composeFile) {
    try (Reader reader = Files.newBufferedReader(composeFile)) {
      Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
      return root instanceof Map<?, ?> map ? map : Map.of();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + composeFile, e);
    }
  }
}
