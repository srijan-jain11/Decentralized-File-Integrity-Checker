package com.integrity.scanner;

import com.integrity.crypto.FileHasher;
import com.integrity.exception.IntegrityException;
import com.integrity.model.FileRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Walks a directory tree and produces a {@link FileRecord} for every
 * regular file found, relative to the scanned root.
 *
 * <p>This is the only module that touches the filesystem for reading
 * directory structure, keeping the I/O boundary of the application in
 * one place (Maintainability / Modularity).</p>
 */
public class FileScanner {

    /**
     * Recursively scans {@code rootDirectory} and returns one
     * {@link FileRecord} per regular file encountered.
     *
     * @param rootDirectory an existing, readable directory
     * @throws IntegrityException if the directory is invalid or a
     *                            file cannot be hashed
     */
    public List<FileRecord> scan(Path rootDirectory) throws IntegrityException {
        if (!Files.isDirectory(rootDirectory)) {
            throw new IntegrityException("Not a directory: " + rootDirectory);
        }

        List<FileRecord> records = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rootDirectory)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                records.add(toRecord(rootDirectory, file));
            }
        } catch (IOException e) {
            throw new IntegrityException("Failed to walk directory: " + rootDirectory, e);
        }
        return records;
    }

    private FileRecord toRecord(Path root, Path file) throws IntegrityException {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            String relativePath = root.relativize(file).toString().replace('\\', '/');
            String hash = FileHasher.hashFile(file);
            return new FileRecord(relativePath, attrs.size(), attrs.lastModifiedTime().toMillis(), hash);
        } catch (IOException e) {
            throw new IntegrityException("Failed to read attributes for file: " + file, e);
        }
    }
}
