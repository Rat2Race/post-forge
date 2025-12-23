package com.postforge.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostRequest(
   @NotBlank(message = "제목은 필수입니다")
   @Size(min = 2, max = 100, message = "제목은 2-100자여야 합니다")
   @Pattern(regexp = "^[^<>]*$", message = "제목에 HTML 태그를 사용할 수 없습니다")
   String title,

   @NotBlank(message = "내용은 필수입니다")
   @Size(min = 10, max = 10000, message = "내용은 10-10000자여야 합니다")
   String content
) {
}
