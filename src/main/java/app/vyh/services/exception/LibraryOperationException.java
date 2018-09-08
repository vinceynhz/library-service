package app.vyh.services.exception;

import org.springframework.http.HttpStatus;

/**
 * @author Vic on 9/2/2018
 **/
public class LibraryOperationException extends Exception {
    private HttpStatus responseStatus;

    public LibraryOperationException(String message, HttpStatus responseStatus) {
        super(message);
        this.responseStatus = responseStatus;
    }

    HttpStatus getResponseStatus() {
        return responseStatus;
    }
}
