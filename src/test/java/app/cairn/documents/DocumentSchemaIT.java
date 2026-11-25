package app.cairn.documents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.cairn.CairnApplication;
import app.cairn.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = CairnApplication.class)
@Import(TestcontainersConfiguration.class)
@Transactional
class DocumentSchemaIT {

  private static final String SHA_A = "a".repeat(64);
  private static final String SHA_B = "b".repeat(64);

  @Autowired private JdbcClient jdbc;

  @Test
  void migrationCreatesDocumentTable() {
    List<String> columns =
        jdbc.sql(
                """
                select column_name from information_schema.columns
                where table_schema = 'documents' and table_name = 'document'
                order by ordinal_position
                """)
            .query(String.class)
            .list();

    assertThat(columns)
        .containsExactly(
            "id", "kind", "sha256", "bytes", "page_count", "storage_name", "uploaded_at");
  }

  @Test
  void duplicateSha256IsRejected() {
    insert("sbc", SHA_A, "one");

    assertThatThrownBy(() -> insert("sbc", SHA_A, "two"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("document_sha256_key");
  }

  @Test
  void duplicateStorageNameIsRejected() {
    insert("sbc", SHA_A, "one");

    assertThatThrownBy(() -> insert("sbc", SHA_B, "one"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("document_storage_name_key");
  }

  @Test
  void unknownKindIsRejected() {
    assertThatThrownBy(() -> insert("invoice", SHA_A, "one"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("document_kind_check");
  }

  @Test
  void nonPositiveBytesAreRejected() {
    assertThatThrownBy(() -> insert("sbc", SHA_A, "one", 0, 1))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("document_bytes_check");
  }

  @Test
  void nonPositivePageCountIsRejected() {
    assertThatThrownBy(() -> insert("sbc", SHA_A, "one", 10, 0))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("document_page_count_check");
  }

  private void insert(String kind, String sha256, String storageName) {
    insert(kind, sha256, storageName, 10, 1);
  }

  private void insert(String kind, String sha256, String storageName, long bytes, int pages) {
    jdbc.sql(
            """
            insert into documents.document
              (id, kind, sha256, bytes, page_count, storage_name, uploaded_at)
            values (:id, :kind, :sha256, :bytes, :pages, :storage, now())
            """)
        .param("id", UUID.randomUUID())
        .param("kind", kind)
        .param("sha256", sha256)
        .param("bytes", bytes)
        .param("pages", pages)
        .param("storage", storageName)
        .update();
  }
}
