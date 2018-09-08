package app.vyh.services.exception;

import app.vyh.services.model.response.LibraryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * @author Vic on 9/1/2018
 **/
@ControllerAdvice
@RestController
public class LibraryExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    public final ResponseEntity<Object> handleAllExceptions(Exception exception, WebRequest request) {
        LOGGER.error("Major exception processing request", exception);
        LibraryResponse errorResponse = new LibraryResponse()
                .withError(exception.getMessage())
                .withHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return handleExceptionInternal(exception, errorResponse, null, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value = LibraryOperationException.class)
    public final ResponseEntity<Object> handleStatusThrowable(LibraryOperationException exception, WebRequest request) {
        LOGGER.error("Error processing request", exception);
        LibraryResponse errorResponse = new LibraryResponse()
                .withError(exception.getMessage())
                .withHttpStatusCode(exception.getResponseStatus().value());
        return handleExceptionInternal(exception, errorResponse, null, exception.getResponseStatus(), request);
    }

}
