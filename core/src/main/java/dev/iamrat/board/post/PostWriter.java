package dev.iamrat.board.post;

public interface PostWriter {
    Long write(PostWriteCommand command);
}
