package fr.minhnn.touristapi.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends RuntimeException{
    private HttpStatus status;

    public NotFoundException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;
    }
}
