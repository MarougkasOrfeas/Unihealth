package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.FileEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Every lookup here takes the owner.
 *
 * <p>That is the point, not a convention: these rows are medical documents, so "find by id" is a
 * question nothing should be able to ask. Loading by (id, owner) rather than by id and then
 * comparing also means a row that is not yours is simply not found — the safe answer, and one
 * branch fewer for a caller to forget.
 */
@Repository
public interface FileRepository extends BaseRepository<FileEntity> {

  Optional<FileEntity> findByIdAndUserId(String id, String userId);

  boolean existsByIdAndUserId(String id, String userId);

  /** Backs the advisory "have I already uploaded something called this" check. */
  boolean existsByUserIdAndNameIgnoreCase(String userId, String name);
}
