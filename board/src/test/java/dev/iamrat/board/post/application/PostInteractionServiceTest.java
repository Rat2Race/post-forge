package dev.iamrat.board.post.application;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import static org.assertj.core.api.Assertions.assertThat;

class PostInteractionServiceTest {

    @Test
    @DisplayName("게시글 좋아요 요청은 read-only가 아닌 쓰기 트랜잭션을 사용한다")
    void likePost_usesReadWriteTransaction() throws NoSuchMethodException {
        TransactionAttribute transactionAttribute = getTransactionAttribute("likePost");

        assertThat(transactionAttribute).isNotNull();
        assertThat(transactionAttribute.isReadOnly()).isFalse();
    }

    @Test
    @DisplayName("게시글 좋아요 취소 요청은 read-only가 아닌 쓰기 트랜잭션을 사용한다")
    void unlikePost_usesReadWriteTransaction() throws NoSuchMethodException {
        TransactionAttribute transactionAttribute = getTransactionAttribute("unlikePost");

        assertThat(transactionAttribute).isNotNull();
        assertThat(transactionAttribute.isReadOnly()).isFalse();
    }

    private TransactionAttribute getTransactionAttribute(String methodName) throws NoSuchMethodException {
        Method method = PostInteractionService.class.getMethod(methodName, Long.class, Long.class);
        return new AnnotationTransactionAttributeSource()
            .getTransactionAttribute(method, PostInteractionService.class);
    }
}
