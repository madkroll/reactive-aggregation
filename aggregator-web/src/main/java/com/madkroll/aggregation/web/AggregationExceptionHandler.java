package com.madkroll.aggregation.web;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Log4j2
@ResponseBody
@ControllerAdvice
public class AggregationExceptionHandler {

    /**
     * Handles all other error cases when no any handler matched to prevent leaking internal specifics to clients.
     * */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void sessionNotFoundHandler(final Exception anyOtherException) {
        log.error(anyOtherException.getMessage(), anyOtherException);
    }
}
