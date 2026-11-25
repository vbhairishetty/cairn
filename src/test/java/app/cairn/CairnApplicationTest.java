package app.cairn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = CairnApplication.class)
@Import(TestcontainersConfiguration.class)
class CairnApplicationTest {

  @Test
  void contextLoads() {}
}
