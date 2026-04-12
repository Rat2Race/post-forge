package dev.iamrat.ai.comment.service;

import dev.iamrat.ai.comment.dto.AiCommentReplyResponse;
import dev.iamrat.board.comment.CommentContext;
import dev.iamrat.board.comment.CommentReader;
import dev.iamrat.board.comment.CommentWriter;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.board.post.PostContext;
import dev.iamrat.board.post.PostReader;
import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class PostCommentReplyService {

    private static final String AI_USER_ID = "ai-post-generator";
    private static final String AI_NICKNAME = "AI 분석가";

    private static final String SYSTEM_PROMPT = """
        당신은 이미 게시된 AI 주식 분석 글에 대해 후속 질문에 답변하는 어시스턴트입니다.
        원문 게시글의 내용과 사용자의 질문만을 바탕으로 간결하고 신중하게 답변하세요.
        새로운 투자 추천을 하지 말고, 기존 분석의 근거를 재설명하거나 불확실성을 분명히 밝히세요.
        답변은 댓글 형식으로 짧고 명확하게 작성하세요.
        """;

    private final ChatModel chatModel;
    private final PostReader postReader;
    private final CommentReader commentReader;
    private final CommentWriter commentWriter;

    public PostCommentReplyService(
        ChatModel chatModel,
        PostReader postReader,
        CommentReader commentReader,
        CommentWriter commentWriter
    ) {
        this.chatModel = chatModel;
        this.postReader = postReader;
        this.commentReader = commentReader;
        this.commentWriter = commentWriter;
    }

    public AiCommentReplyResponse reply(Long postId, Long commentId) {
        PostContext post = postReader.read(postId);
        CommentContext comment = commentReader.read(commentId);

        if (!post.id().equals(comment.postId())) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_PARENT);
        }
        if (comment.parentId() != null) {
            throw new CustomException(ErrorCode.MAX_COMMENT_DEPTH_EXCEEDED);
        }
        if (!AI_USER_ID.equals(post.userId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        String answer = generateReply(post, comment);
        Long createdCommentId = commentWriter.write(postId, commentId, answer, AI_USER_ID, AI_NICKNAME);
        return new AiCommentReplyResponse(postId, commentId, createdCommentId, answer);
    }

    private String generateReply(PostContext post, CommentContext comment) {
        String userPrompt = """
            [원문 게시글]
            제목: %s
            요약: %s
            본문:
            %s

            [사용자 질문]
            %s
            """.formatted(
            post.title(),
            post.summary() != null ? post.summary() : "",
            post.content(),
            comment.content()
        );

        return chatModel.call(new Prompt(List.of(
            new SystemMessage(SYSTEM_PROMPT),
            new UserMessage(userPrompt)
        )))
            .getResult()
            .getOutput()
            .getText();
    }
}
