package com.integrity.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable value object describing one file at the moment it was
 * scanned: its relative path, size, last-modified timestamp, and
 * SHA-256 content hash.
 *
 * <p>Two {@code FileRecord}s are considered "equal" for path-matching
 * purposes when {@link #getRelativePath()} matches; use
 * {@link #hasSameContent(FileRecord)} to compare content instead.</p>
 */
public final class FileRecord implements Serializable, Comparable<FileRecord> {

    private static final long serialVersionUID = 1L;

    private final String relativePath;
    private final long sizeBytes;
    private final long lastModifiedEpochMillis;
    private final String sha256Hash;

    public FileRecord(String relativePath, long sizeBytes, long lastModifiedEpochMillis, String sha256Hash) {
        this.relativePath = relativePath;
        this.sizeBytes = sizeBytes;
        this.lastModifiedEpochMillis = lastModifiedEpochMillis;
        this.sha256Hash = sha256Hash;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getLastModifiedEpochMillis() {
        return lastModifiedEpochMillis;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    /** True when both records point at the same relative path AND the same content hash. */
    public boolean hasSameContent(FileRecord other) {
        return other != null
                && relativePath.equals(other.relativePath)
                && sha256Hash.equals(other.sha256Hash);
    }

    /** Serializes this record to a single pipe-delimited line for the flat-file baseline format. */
    public String toBaselineLine() {
        return relativePath + "|" + sizeBytes + "|" + lastModifiedEpochMillis + "|" + sha256Hash;
    }

    /** Parses a line produced by {@link #toBaselineLine()}. */
    public static FileRecord fromBaselineLine(String line) {
        String[] parts = line.split("\\|", 4);
        return new FileRecord(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]), parts[3]);
    }

    @Override
    public int compareTo(FileRecord other) {
        return this.relativePath.compareTo(other.relativePath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileRecord)) return false;
        FileRecord that = (FileRecord) o;
        return relativePath.equals(that.relativePath) && sha256Hash.equals(that.sha256Hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath, sha256Hash);
    }

    @Override
    public String toString() {
        return relativePath + " (" + sizeBytes + " bytes, hash=" + sha256Hash.substring(0, 10) + "...)";
    }
}
