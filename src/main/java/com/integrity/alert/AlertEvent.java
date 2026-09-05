package com.integrity.alert;

/**
 * A single tamper-detection finding, e.g. "this file's content
 * changed" or "the overall root hash no longer matches the baseline".
 */
public class AlertEvent {

    /** The category of integrity finding this event represents. */
    public enum Type {
        FILE_MODIFIED,
        FILE_DELETED,
        FILE_ADDED,
        ROOT_HASH_MISMATCH
    }

    private final Type type;
    private final String filePath; // null for ROOT_HASH_MISMATCH
    private final String message;

    public AlertEvent(Type type, String filePath, String message) {
        this.type = type;
        this.filePath = filePath;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMessage() {
        return message;
    }
}
