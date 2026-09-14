-- vector_store.embedding 차원을 1536(OpenAI text-embedding-ada-002)에서 1024(로컬 bge-m3)로 변경한다.
-- 외부 임베딩 API 장애가 문서 적재를 막고, 적재 실패가 자동 게시를 하드 블록하던 의존을 끊기 위한 변경이다.
--
-- 기존 행은 ada-002 기준이라 bge-m3 질의 벡터와 의미가 호환되지 않고,
-- 차원이 다르면 컬럼 타입 변경 자체가 불가능하므로 비운다.
-- 원문은 외부 소스에 남아 있으므로 재적재로 복구한다.
DELETE FROM public.vector_store;

-- HNSW 인덱스는 컬럼 차원에 묶여 있어 타입 변경 전에 내리고 뒤에 같은 정의로 다시 만든다.
DROP INDEX IF EXISTS public.spring_ai_vector_index;

ALTER TABLE public.vector_store
    ALTER COLUMN embedding TYPE public.vector(1024);

CREATE INDEX spring_ai_vector_index ON public.vector_store USING hnsw (embedding public.vector_cosine_ops);
