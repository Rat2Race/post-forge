package dev.iamrat.core.board.post;

import java.time.LocalDate;
import java.util.List;

/** ingest가 뉴스 글을 board에 묻고 게시하는 계약. 글 종류와 봇 작성자는 board가 정한다. */
public interface NewsPostPort {

    boolean isPublished(String canonicalUrl);

    long countPublished(String keyword, LocalDate publishedDate);

    /** 글과 출처 링크를 한 트랜잭션으로 저장한다. */
    Long publishLaunchNews(LaunchNewsPost post);

    List<DailyDigestSourceItem> findLaunchNews(NewsSection section, LocalDate newsDate);

    boolean digestExists(NewsSection section, LocalDate newsDate);

    Long publishDailyDigest(NewsSection section, LocalDate newsDate, DailyDigestDraft draft);
}
