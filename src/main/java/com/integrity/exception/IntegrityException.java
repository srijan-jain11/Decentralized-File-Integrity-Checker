package com.integrity.exception;

/**
 * Checked exception thrown whenever a file-integrity operation fails
 * (hashing errors, unreadable baseline snapshots, corrupted audit
 * chains, malformed scan input, etc.).
 *
 * <p>Wrapping all low-level failures (IOException, NoSuchAlgorithmException, ...)
 * in a single domain exception keeps the public API of every module
 * clean and lets callers handle "something went wrong with integrity
 * checking" as one category, per the project's error-handling strategy.</p>
 */
public class IntegrityException extends Exception {

    public IntegrityException(String message) {
        super(message);
    }

    public IntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
