package dev.iamrat.core.board.post;

public interface PostWriter {
    Long write(PostWriteCommand command);
}
