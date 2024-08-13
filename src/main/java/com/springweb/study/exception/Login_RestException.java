package com.springweb.study.exception;

public class Login_RestException extends RuntimeException {

    public Login_RestException() {
        super();
    }

    public Login_RestException(String message) {
        super(message);
    }

    public Login_RestException(String message, Throwable cause) {
        super(message, cause);
    }

    public Login_RestException(Throwable cause) {
        super(cause);
    }
}
