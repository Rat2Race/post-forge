package dev.iamrat.document.service;

import dev.iamrat.document.dto.DocumentRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VectorStore vectorStore;

    @Transactional
    public void store(List<DocumentRequest> requests) {
        List<Document> documents = requests.stream()
                .map(this::toDocument)
                .toList();

        vectorStore.add(documents);
        log.info("{}건의 문서를 벡터 스토어에 저장했습니다.", documents.size());
    }

    private Document toDocument(DocumentRequest request) {
        Map<String, Object> metadata = new HashMap<>();

        if (request.source() != null) {
            metadata.put("source", request.source());
        }

        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }

        return new Document(request.content(), metadata);
    }
}
