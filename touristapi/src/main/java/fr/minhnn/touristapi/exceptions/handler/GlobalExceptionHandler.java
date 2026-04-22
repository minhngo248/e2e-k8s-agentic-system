package fr.minhnn.touristapi.exceptions.handler;

import fr.minhnn.touristapi.exceptions.BadRequestException;
import fr.minhnn.touristapi.exceptions.ForbiddenException;
import fr.minhnn.touristapi.exceptions.NotFoundException;
import fr.minhnn.touristapi.exceptions.UploadImageException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = {NotFoundException.class})
    protected ResponseEntity<?> handleNotFoundException(NotFoundException ex) {
        String message = messageSource.getMessage("exception.not_found", new Object[]{ex.getMessage()}, LocaleContextHolder.getLocale());
        List<String> messages = new ArrayList<>();
        messages.add(message);
        return ResponseEntity.notFound().build();
    }


    @ExceptionHandler(value = {ForbiddenException.class})
    protected ResponseEntity<?> handleForbiddenException(ForbiddenException ex) {
        String message = ex.getMessage();
        List<String> messages = new ArrayList<>();
        messages.add(message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }


    @ExceptionHandler(value = {BadRequestException.class})
    protected ResponseEntity<?> handleBadRequestException(BadRequestException ex) {
        List<String> messages = new ArrayList<>();
        String message = ex.getMessage();
        messages.add(message);
        return ResponseEntity.badRequest().build();
    }


    @ExceptionHandler(value = {UploadImageException.class})
    protected ResponseEntity<?> handleUploadImageException(UploadImageException ex) {
        List<String> messages = new ArrayList<>();
        String message = ex.getMessage();
        messages.add(message);
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(value = {BindException.class})
    protected ResponseEntity<?> handleBindException(BindException ex) {

        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getFieldErrors()) {
            try {
                errors.add(fieldError.getField() + " " +fieldError.getDefaultMessage());
            } catch (Exception e) {
                errors.add(messageSource.getMessage("exception.general", null, LocaleContextHolder.getLocale()));
            }
        }
        return ResponseEntity.badRequest().build();
    }
}
