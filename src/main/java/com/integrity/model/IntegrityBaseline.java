package com.integrity.model;

import com.integrity.exception.IntegrityException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A saved "known-good" snapshot of a directory: the Merkle root hash
 * at the time of the snapshot, the timestamp it was taken, and the
 * per-file records that produced it.
 *
 * <p>Baselines are persisted in a simple, human-readable flat-file
 * format (rather than Java's binary serialization) so that a student
 * or reviewer can open the snapshot file directly and understand it -
 * this matters for the project's Maintainability and Usability
 * non-functional requirements. The format is:</p>
 *
 * <pre>
 * ROOT_HASH=&lt;sha256&gt;
 * TIMESTAMP=&lt;iso-8601&gt;
 * FILE_COUNT=&lt;n&gt;
 * ---
 * relativePath|size|lastModifiedMillis|sha256
 * relativePath|size|lastModifiedMillis|sha256
 * ...
 * </pre>
 */
public class IntegrityBaseline {

    private final String rootHash;
    private final Instant createdAt;
    private final List<FileRecord> files;

    public IntegrityBaseline(String rootHash, Instant createdAt, List<FileRecord> files) {
        this.rootHash = rootHash;
        this.createdAt = createdAt;
        this.files = files;
    }

    public String getRootHash() {
        return rootHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<FileRecord> getFiles() {
        return files;
    }

    /** Writes this baseline to disk in the flat-file format described above. */
    public void save(Path destination) throws IntegrityException {
        try (BufferedWriter writer = Files.newBufferedWriter(destination)) {
            writer.write("ROOT_HASH=" + rootHash);
            writer.newLine();
            writer.write("TIMESTAMP=" + createdAt.toString());
            writer.newLine();
            writer.write("FILE_COUNT=" + files.size());
            writer.newLine();
            writer.write("---");
            writer.newLine();
            for (FileRecord record : files) {
                writer.write(record.toBaselineLine());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IntegrityException("Failed to save baseline to " + destination, e);
        }
    }

    /** Loads a baseline previously written by {@link #save(Path)}. */
    public static IntegrityBaseline load(Path source) throws IntegrityException {
        if (!Files.exists(source)) {
            throw new IntegrityException("Baseline file not found: " + source
                    + ". Run the 'baseline' command first.");
        }
        try (BufferedReader reader = Files.newBufferedReader(source)) {
            String rootHash = readValue(reader.readLine(), "ROOT_HASH");
            String timestamp = readValue(reader.readLine(), "TIMESTAMP");
            readValue(reader.readLine(), "FILE_COUNT"); // consumed for format validation
            reader.readLine(); // "---" separator

            List<FileRecord> files = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    files.add(FileRecord.fromBaselineLine(line));
                }
            }
            return new IntegrityBaseline(rootHash, Instant.parse(timestamp), files);
        } catch (IOException e) {
            throw new IntegrityException("Failed to load baseline from " + source, e);
        }
    }

    private static String readValue(String line, String expectedKey) throws IntegrityException {
        if (line == null || !line.startsWith(expectedKey + "=")) {
            throw new IntegrityException("Corrupted baseline file: expected key '" + expectedKey + "'");
        }
        return line.substring(expectedKey.length() + 1);
    }
}
