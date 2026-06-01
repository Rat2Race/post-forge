package dev.iamrat.board.comment.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommentRequest(
    Long parentId,

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(min = 2, max = 500, message = "댓글은 2-500자여야 합니다")
    @Pattern(regexp = "^(?!.*<\\s*(?:script|iframe|object|embed|form|input|button|link|style|img\\s+[^>]*on)[\\s>]).*$",
             flags = {Pattern.Flag.CASE_INSENSITIVE, Pattern.Flag.DOTALL},
             message = "허용되지 않는 HTML 태그가 포함되어 있습니다")
    String content
) {
}
