package dev.iamrat.board.comment.application;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import static org.assertj.core.api.Assertions.assertThat;

class CommentInteractionServiceTest {

    @Test
    @DisplayName("댓글 좋아요 요청은 read-only가 아닌 쓰기 트랜잭션을 사용한다")
    void likeComment_usesReadWriteTransaction() throws NoSuchMethodException {
        TransactionAttribute transactionAttribute = getTransactionAttribute("likeComment");

        assertThat(transactionAttribute).isNotNull();
        assertThat(transactionAttribute.isReadOnly()).isFalse();
    }

    @Test
    @DisplayName("댓글 좋아요 취소 요청은 read-only가 아닌 쓰기 트랜잭션을 사용한다")
    void unlikeComment_usesReadWriteTransaction() throws NoSuchMethodException {
        TransactionAttribute transactionAttribute = getTransactionAttribute("unlikeComment");

        assertThat(transactionAttribute).isNotNull();
        assertThat(transactionAttribute.isReadOnly()).isFalse();
    }

    private TransactionAttribute getTransactionAttribute(String methodName) throws NoSuchMethodException {
        Method method = CommentInteractionService.class.getMethod(methodName, Long.class, Long.class);
        return new AnnotationTransactionAttributeSource()
            .getTransactionAttribute(method, CommentInteractionService.class);
    }
}
