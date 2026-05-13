package ewm.comment.repository;

import ewm.comment.model.Comment;
import ewm.comment.model.CommentStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);

    Optional<Comment> findById(Long commentId);

    List<Comment> findByEventId(Long eventId, Pageable page);

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable page);

    List<Comment> findByAuthorId(Long authorId, Pageable page);

    List<Comment> findAll(Integer from, Integer size);

    List<Comment> findAllByStatus(CommentStatus status, Pageable page);

    Optional<Comment> findByIdAndAuthorId(Long commentId, Long authorId);

    void delete(Comment comment);
}
