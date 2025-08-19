package rat.boardservice.posts.exception;

public class NotFindPage_RestException extends RuntimeException {

    public NotFindPage_RestException() {
        super();
    }

    public NotFindPage_RestException(String message) {
        super(message);
    }

    public NotFindPage_RestException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFindPage_RestException(Throwable cause) {
        super(cause);
    }
}