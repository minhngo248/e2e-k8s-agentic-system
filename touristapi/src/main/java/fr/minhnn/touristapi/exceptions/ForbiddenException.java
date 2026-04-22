package fr.minhnn.touristapi.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends RuntimeException{
    private HttpStatus status;

    public ForbiddenException(String message) {
        super(message);
        this.status = HttpStatus.FORBIDDEN;
    }
}
