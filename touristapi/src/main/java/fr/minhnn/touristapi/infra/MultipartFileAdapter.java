package fr.minhnn.touristapi.infra;

import fr.minhnn.touristapi.destination.Destination;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MultipartFileAdapter {
    private MultipartFileAdapter() {
    }

    /**
     * Convert Spring's MultipartFile to domain ImageFile
     */
    public static Destination.ImageFile toDomain(MultipartFile multipartFile) throws IOException {
        return new Destination.ImageFile(
                multipartFile.getOriginalFilename(),
                multipartFile.getBytes(),
                multipartFile.getContentType()
        );
    }

    /**
     * Convert domain ImageFile to Spring's MultipartFile (if needed)
     */
    public static MultipartFile toMultipartFile(Destination.ImageFile imageFile) {
        return new InMemoryMultipartFile(
                imageFile.fileName(),
                imageFile.content(),
                imageFile.contentType()
        );
    }
}

class InMemoryMultipartFile implements MultipartFile {
    private final String fileName;
    private final byte[] content;
    private final String contentType;

    public InMemoryMultipartFile(String fileName, byte[] content, String contentType) {
        this.fileName = fileName;
        this.content = content;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String getOriginalFilename() {
        return fileName;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) {
        throw new UnsupportedOperationException("transferTo not supported for in-memory file");
    }
}
