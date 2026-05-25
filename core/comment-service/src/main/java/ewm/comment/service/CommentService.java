package ewm.comment.service;

import ewm.interaction.dto.comment.CommentDto;
import ewm.interaction.dto.comment.NewCommentDto;
import ewm.interaction.dto.comment.UpdateCommentRequest;

import java.util.List;

public interface CommentService {
    CommentDto create(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto update(Long userId, Long commentId, UpdateCommentRequest updateCommentRequest);

    List<CommentDto> getEventComments(Long eventId, int from, int size);

    List<CommentDto> getComments(int from, int size);

    void delete(Long userId, Long commentId);

    CommentDto approve(Long commentId);

    CommentDto reject(Long commentId);
}
