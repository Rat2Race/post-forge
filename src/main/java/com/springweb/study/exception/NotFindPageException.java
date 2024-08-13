package com.springweb.study.exception;

public class NotFindPageException extends RuntimeException {

    public NotFindPageException() {
        super();
    }

    public NotFindPageException(String message) {
        super(message);
    }

    public NotFindPageException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFindPageException(Throwable cause) {
        super(cause);
    }
}
