package dev.iamrat.ingest.pipeline.application;

import dev.iamrat.ingest.pipeline.domain.DocumentChunk;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestPipelineService {

    private final DocumentChunker documentChunker;
    private final DocumentChunkStore documentChunkStore;

    @Transactional
    public DocumentIngestResult store(List<DocumentIngestCommand> commands) {
        List<DocumentChunk> chunks = documentChunker.toChunks(commands);
        DocumentStoreResult storeResult = documentChunkStore.store(chunks);
        if (storeResult.embeddingsStored()) {
            log.info("{}건의 문서를 벡터 스토어에 저장했습니다.", chunks.size());
        } else {
            log.warn("{}건의 문서를 임베딩 없이 접수했습니다. reason={}", chunks.size(), storeResult.degradationReason());
        }
        return DocumentIngestResult.from(commands.size(), storeResult);
    }
}
