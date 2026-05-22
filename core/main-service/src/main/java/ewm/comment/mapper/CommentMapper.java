package ewm.comment.mapper;

import ewm.comment.dto.CommentDto;
import ewm.comment.dto.NewCommentDto;
import ewm.comment.dto.UpdateCommentRequest;
import ewm.comment.model.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static Comment mapToComment(NewCommentDto commentDto) {
        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        return comment;
    }

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(comment.getAuthor().getUserId())
                .event(comment.getEvent().getId())
                .status(comment.getStatus().name())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .build();
    }

    public static Comment updateComment(Comment comment, UpdateCommentRequest updateCommentRequest) {
        if (updateCommentRequest.hasText()) {
            comment.setText(updateCommentRequest.getText());
        }
        return comment;
    }
}
