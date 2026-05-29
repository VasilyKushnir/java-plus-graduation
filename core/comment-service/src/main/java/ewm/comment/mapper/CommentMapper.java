package ewm.comment.mapper;

import ewm.comment.model.Comment;
import ewm.interaction.dto.comment.CommentDto;
import ewm.interaction.dto.comment.NewCommentDto;
import ewm.interaction.dto.comment.UpdateCommentRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class CommentMapper {

    public static Comment mapToComment(NewCommentDto commentDto) {
        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        return comment;
    }

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(comment.getAuthorId())
                .event(comment.getEventId())
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
