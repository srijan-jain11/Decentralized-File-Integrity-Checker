package com.integrity.audit;

import com.integrity.crypto.FileHasher;
import com.integrity.exception.IntegrityException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only, hash-chained audit log: every entry embeds the hash of
 * the previous entry, so retroactively editing or deleting a past log
 * line breaks the chain and is immediately detectable by
 * {@link #verifyChain(Path)}. This mirrors the "blockchain-lite"
 * concept referenced in the project's problem statement, applied to
 * the tool's own operational log rather than to the scanned files.
 *
 * <p>Log line format:</p>
 * <pre>timestamp|event|message|previousHash|entryHash</pre>
 * where {@code entryHash = SHA256(timestamp + event + message + previousHash)}.
 */
public class AuditLogger {

    private static final String GENESIS_HASH = "0".repeat(64);

    private final Path logFile;

    public AuditLogger(Path logFile) {
        this.logFile = logFile;
    }

    /** Appends one tamper-evident entry to the log. */
    public void log(String event, String message) throws IntegrityException {
        try {
            String previousHash = getLastEntryHash();
            String timestamp = Instant.now().toString();
            String entryHash = FileHasher.hashString(timestamp + event + message + previousHash);
            String line = String.join("|", timestamp, event, message, previousHash, entryHash);

            try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IntegrityException("Failed to write audit log entry", e);
        }
    }

    /**
     * Walks the entire log and confirms every entry's stored hash both
     * (a) matches a fresh recomputation from its own fields, and
     * (b) correctly references the previous entry's hash.
     *
     * @return true if the chain is unbroken, false if tampering is detected
     */
    public boolean verifyChain(Path logFileToVerify) throws IntegrityException {
        if (!Files.exists(logFileToVerify)) {
            return true; // no log yet = nothing to verify = trivially valid
        }
        try (BufferedReader reader = Files.newBufferedReader(logFileToVerify)) {
            String expectedPrevious = GENESIS_HASH;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", 5);
                String timestamp = parts[0], event = parts[1], message = parts[2];
                String storedPrevious = parts[3], storedEntryHash = parts[4];

                if (!storedPrevious.equals(expectedPrevious)) {
                    return false; // chain link broken
                }
                String recomputed = FileHasher.hashString(timestamp + event + message + storedPrevious);
                if (!recomputed.equals(storedEntryHash)) {
                    return false; // entry content was altered after the fact
                }
                expectedPrevious = storedEntryHash;
            }
            return true;
        } catch (IOException e) {
            throw new IntegrityException("Failed to verify audit log chain", e);
        }
    }

    /** Returns every parsed entry, oldest first (used for reporting/CLI display). */
    public List<String> readAll() throws IntegrityException {
        List<String> entries = new ArrayList<>();
        if (!Files.exists(logFile)) {
            return entries;
        }
        try {
            entries.addAll(Files.readAllLines(logFile));
        } catch (IOException e) {
            throw new IntegrityException("Failed to read audit log", e);
        }
        return entries;
    }

    private String getLastEntryHash() throws IntegrityException {
        if (!Files.exists(logFile)) {
            return GENESIS_HASH;
        }
        try {
            List<String> lines = Files.readAllLines(logFile);
            if (lines.isEmpty()) {
                return GENESIS_HASH;
            }
            String lastLine = lines.get(lines.size() - 1);
            String[] parts = lastLine.split("\\|", 5);
            return parts[4]; // entryHash of the last line
        } catch (IOException e) {
            throw new IntegrityException("Failed to read audit log for chaining", e);
        }
    }
}
