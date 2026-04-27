package dev.iamrat.ai.comment.service;

import dev.iamrat.ai.comment.dto.AiCommentReplyResponse;
import dev.iamrat.board.comment.CommentContext;
import dev.iamrat.board.comment.CommentReader;
import dev.iamrat.board.comment.CommentWriter;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.board.post.PostContext;
import dev.iamrat.board.post.PostReader;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCommentReplyServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatModel chatModel;

    @Mock
    private PostReader postReader;

    @Mock
    private CommentReader commentReader;

    @Mock
    private CommentWriter commentWriter;

    @InjectMocks
    private PostCommentReplyService postCommentReplyService;

    @Test
    @DisplayName("AI 게시글의 최상위 댓글에 답변 댓글을 생성한다")
    void reply_validTopLevelComment_createsAiReply() {
        given(postReader.read(1L)).willReturn(new PostContext(
            1L, "제목", "본문", "요약", List.of("삼성전자"), "ai-post-generator", "AI 분석가"
        ));
        given(commentReader.read(10L)).willReturn(new CommentContext(
            10L, 1L, null, "이 공시가 왜 단기 악재예요?", "user-1", "민식"
        ));
        given(chatModel.call(any(Prompt.class)).getResult().getOutput().getText()).willReturn("희석 우려 때문에 단기 부담으로 본 것입니다.");
        given(commentWriter.write(1L, 10L, "희석 우려 때문에 단기 부담으로 본 것입니다.", "ai-post-generator", "AI 분석가"))
            .willReturn(11L);

        AiCommentReplyResponse response = postCommentReplyService.reply(1L, 10L);

        assertThat(response.commentId()).isEqualTo(11L);
        assertThat(response.parentCommentId()).isEqualTo(10L);
        assertThat(response.content()).contains("단기 부담");
        verify(commentWriter).write(1L, 10L, "희석 우려 때문에 단기 부담으로 본 것입니다.", "ai-post-generator", "AI 분석가");
    }

    @Test
    @DisplayName("AI 게시글이 아니면 댓글 답변을 거부한다")
    void reply_nonAiPost_throwsAccessDenied() {
        given(postReader.read(1L)).willReturn(new PostContext(
            1L, "제목", "본문", "요약", List.of(), "user-1", "민식"
        ));
        given(commentReader.read(10L)).willReturn(new CommentContext(
            10L, 1L, null, "질문", "user-2", "질문자"
        ));

        assertThatThrownBy(() -> postCommentReplyService.reply(1L, 10L))
            .isInstanceOf(CustomException.class);
    }
}
