package dev.iamrat.board.post.presentation.dto;

import dev.iamrat.core.board.post.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostRequest(
   @NotBlank(message = "제목은 필수입니다")
   @Size(min = 2, max = 100, message = "제목은 2-100자여야 합니다")
   @Pattern(regexp = "^[^<>]*$", message = "제목에 HTML 태그를 사용할 수 없습니다")
   String title,

   @NotBlank(message = "내용은 필수입니다")
   @Size(min = 10, max = 10000, message = "내용은 10-10000자여야 합니다")
   String content,

   @Size(max = 500, message = "요약은 500자 이하여야 합니다")
   String summary,

   @Size(max = 20, message = "태그는 최대 20개까지 등록할 수 있습니다")
   List<@Size(max = 50, message = "태그는 50자 이하여야 합니다") String> tags,

   PostCategory category,

   List<Long> fileIds
) {
}
