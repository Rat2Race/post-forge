package com.springweb.study.exception.handler.advice;

import com.springweb.study.exception.LoginException;
import com.springweb.study.exception.NotFindPageException;
import com.springweb.study.exception.handler.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice(basePackages = "study.controller")
public class MVCControllerAdvice {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResult loginHandler(LoginException e) {
        log.error("[exception] ex", e);
        return new ErrorResult("login-EX", e.getMessage());
    }

    @ExceptionHandler(NotFindPageException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResult NotFindException(NotFindPageException e) {
        log.error("[exception] ex", e);
        return new ErrorResult("NotFound-EX", e.getMessage());
    }
}
