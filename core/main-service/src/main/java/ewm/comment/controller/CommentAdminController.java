package ewm.comment.controller;

import ewm.comment.dto.CommentDto;
import ewm.comment.service.CommentService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class CommentAdminController {
    private final CommentService commentService;

    @PatchMapping("/{commentId}/approve")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto approve(@PathVariable @Positive Long commentId) {
        return commentService.approve(commentId);
    }

    @PatchMapping("/{commentId}/reject")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto reject(@PathVariable @Positive Long commentId) {
        return commentService.reject(commentId);
    }
}
