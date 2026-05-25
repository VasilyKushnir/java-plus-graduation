package ewm.comment.repository;

import ewm.comment.model.Comment;
import ewm.comment.model.CommentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DatabaseCommentRepository extends CommentRepository, JpaRepository<Comment, Long> {
    @Override
    @Query("SELECT c FROM Comment c WHERE c.eventId = :eventId ORDER BY c.createdOn DESC")
    List<Comment> findByEventId(Long eventId, Pageable page);

    @Override
    @Query("SELECT c FROM Comment c WHERE c.eventId = :eventId AND c.status = :status ORDER BY c.createdOn DESC")
    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable page);

    @Override
    @Query("SELECT c FROM Comment c WHERE c.authorId = :authorId ORDER BY c.createdOn DESC")
    List<Comment> findByAuthorId(Long authorId, Pageable page);

    @Override
    @Query("SELECT c FROM Comment c ORDER BY c.createdOn DESC")
    List<Comment> findAll(Integer from, Integer size);

    @Override
    @Query("SELECT c FROM Comment c WHERE c.status = :status ORDER BY c.createdOn DESC")
    List<Comment> findAllByStatus(CommentStatus status, Pageable page);

    @Override
    @Query("SELECT c FROM Comment c WHERE c.id = :commentId AND c.authorId = :authorId")
    Optional<Comment> findByIdAndAuthorId(Long commentId, Long authorId);
}
