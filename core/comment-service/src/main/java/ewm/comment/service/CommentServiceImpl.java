package ewm.comment.service;

import ewm.comment.mapper.CommentMapper;
import ewm.comment.model.Comment;
import ewm.comment.model.CommentStatus;
import ewm.comment.repository.CommentRepository;
import ewm.interaction.client.event.EventClient;
import ewm.interaction.client.user.UserClient;
import ewm.interaction.dto.comment.CommentDto;
import ewm.interaction.dto.comment.NewCommentDto;
import ewm.interaction.dto.comment.UpdateCommentRequest;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.enums.EventState;
import ewm.interaction.exception.ConflictException;
import ewm.interaction.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;

    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    @Transactional
    public CommentDto create(Long userId, Long eventId, NewCommentDto newCommentDto) {
        UserDto user;
        EventFullDto event;
        try {
            user = userClient.getUser(userId);
        } catch (NotFoundException e) {
            throw new ConflictException("User not found");
        }

        try {
            event = eventClient.getEvent(eventId);
        } catch (NotFoundException e) {
            throw new ConflictException("Event not found");
        }

        if (!event.getState().equals(EventState.PUBLISHED.name())) {
            throw new ConflictException("Comments can only be added to published events");
        }

        Comment comment = CommentMapper.mapToComment(newCommentDto);
        comment.setAuthorId(userId);
        comment.setEventId(eventId);
        comment.setStatus(CommentStatus.NEW);

        Comment saved = commentRepository.save(comment);
        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CommentDto update(Long userId, Long commentId, UpdateCommentRequest updateCommentRequest) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));

        if (comment.getStatus() == CommentStatus.REJECTED) {
            throw new ConflictException("Cannot update rejected comment");
        }

        CommentMapper.updateComment(comment, updateCommentRequest);
        comment.setStatus(CommentStatus.NEW); // Reset to NEW when updated

        Comment saved = commentRepository.save(comment);
        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        if (eventClient.getEvent(eventId) == null) {
            throw new NotFoundException("Event not found: " + eventId);
        }

        Pageable page = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.APPROVED, page);

        return comments.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getComments(int from, int size) {
        Pageable page = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findAllByStatus(CommentStatus.APPROVED, page);

        return comments.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));

        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public CommentDto approve(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));

        comment.setStatus(CommentStatus.APPROVED);
        Comment saved = commentRepository.save(comment);
        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CommentDto reject(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));

        comment.setStatus(CommentStatus.REJECTED);
        Comment saved = commentRepository.save(comment);
        return CommentMapper.toDto(saved);
    }
}
