package dev.iamrat.ingest.document.service;

import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.ingest.document.dto.DocumentRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private DocumentService documentService;

    @Test
    @DisplayName("문서 요청을 VectorStore 문서로 변환해 저장한다")
    void store_convertsRequestsToVectorDocuments() {
        DocumentRequest request = new DocumentRequest(
            "news content",
            NewsDocumentMetadata.SOURCE_NAVER_NEWS,
            NewsDocumentMetadata.autoPostEligible(
                "AI",
                "AI 반도체 수요 증가",
                "https://news.example/1",
                ""
            ).toMap()
        );

        documentService.store(List.of(request));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentsCaptor.capture());

        List<Document> documents = documentsCaptor.getValue();
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getText()).isEqualTo("news content");
        assertThat(documents.getFirst().getMetadata())
            .containsEntry("source", NewsDocumentMetadata.SOURCE_NAVER_NEWS)
            .containsEntry(NewsDocumentMetadata.KEYWORD, "AI")
            .containsEntry(NewsDocumentMetadata.ORIGINAL_LINK, "https://news.example/1");
    }
}
