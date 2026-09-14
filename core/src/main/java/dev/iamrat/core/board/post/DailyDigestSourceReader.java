package dev.iamrat.core.board.post;

import java.time.LocalDate;
import java.util.List;

public interface DailyDigestSourceReader {

    List<DailyDigestSourceItem> findLaunchNews(BoardCategory category, LocalDate newsDate);

    boolean digestExists(BoardCategory category, String title);
}
