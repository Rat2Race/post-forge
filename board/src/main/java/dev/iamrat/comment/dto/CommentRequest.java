package dev.iamrat.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommentRequest(
    Long parentId,

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(min = 2, max = 500, message = "댓글은 2-500자여야 합니다")
    @Pattern(regexp = "^[^<>]*$", message = "댓글에 HTML 태그를 사용할 수 없습니다")
    String content
) {

}
