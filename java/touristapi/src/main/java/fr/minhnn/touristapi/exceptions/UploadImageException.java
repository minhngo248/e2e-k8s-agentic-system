package fr.minhnn.touristapi.exceptions;

public class UploadImageException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public UploadImageException(String message) {
        super(message);
    }
}
