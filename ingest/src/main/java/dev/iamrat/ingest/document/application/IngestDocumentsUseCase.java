package dev.iamrat.ingest.document.application;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestDocumentsUseCase {

    private final DocumentChunker documentChunker;
    private final DocumentChunkStore documentChunkStore;

    @Transactional
    public DocumentIngestResult ingest(List<SourceDocumentCommand> commands) {
        List<Document> chunks = documentChunker.toChunks(commands);
        documentChunkStore.store(chunks);
        log.info("{}건의 문서를 {}개 청크로 벡터 스토어에 저장했습니다.", commands.size(), chunks.size());
        return new DocumentIngestResult(commands.size(), chunks.size());
    }
}
