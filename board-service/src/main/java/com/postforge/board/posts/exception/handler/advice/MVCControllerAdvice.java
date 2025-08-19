package rat.boardservice.posts.exception.handler.advice;

import rat.boardservice.posts.exception.NotFindPageException;
import rat.boardservice.posts.exception.handler.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice(basePackages = "study.controller")
public class MVCControllerAdvice {

    @ExceptionHandler(NotFindPageException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResult NotFindException(NotFindPageException e) {
        log.error("[exception] ex", e);
        return new ErrorResult("NotFound-EX", e.getMessage());
    }
}