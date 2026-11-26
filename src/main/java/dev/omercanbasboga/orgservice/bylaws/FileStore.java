package dev.omercanbasboga.orgservice.bylaws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Uploaded files on local disk, under one directory, with names we generate. */
@Component
public class FileStore {

    private final Path root;

    public FileStore(@Value("${app.uploads.dir:uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    public void save(String storedName, InputStream in) throws IOException {
        Files.createDirectories(root);
        Files.copy(in, resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException ignored) {
            // an orphan file is harmless; the row that would point at it was never written
        }
    }

    public Path resolve(String storedName) {
        Path p = root.resolve(storedName).normalize();
        // stored names are generated, but check anyway: nothing outside the upload root
        if (!p.startsWith(root)) throw new IllegalArgumentException("Bad file name.");
        return p;
    }
}
