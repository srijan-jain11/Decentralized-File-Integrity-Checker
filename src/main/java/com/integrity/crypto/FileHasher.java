package com.integrity.crypto;

import com.integrity.exception.IntegrityException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stateless cryptographic utility responsible for producing SHA-256
 * fingerprints, both for individual files on disk and for arbitrary
 * byte content (used internally by the Merkle tree to hash pairs of
 * child nodes).
 *
 * <p><b>Design notes:</b> files are hashed via a buffered stream
 * rather than being loaded fully into memory, so the tool scales to
 * large files without a proportional memory cost (Resource
 * Efficiency non-functional requirement).</p>
 */
public final class FileHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    private FileHasher() {
        // utility class - no instances
    }

    /**
     * Computes the SHA-256 hash of a file's contents.
     *
     * @param file path to an existing, readable file
     * @return lowercase hexadecimal representation of the hash
     * @throws IntegrityException if the file cannot be read or the
     *                            hashing algorithm is unavailable
     */
    public static String hashFile(Path file) throws IntegrityException {
        MessageDigest digest = newDigest();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new IntegrityException("Unable to read file for hashing: " + file, e);
        }
        return toHex(digest.digest());
    }

    /**
     * Computes the SHA-256 hash of a plain string (used for combining
     * Merkle child hashes and for audit-log chain links).
     */
    public static String hashString(String content) throws IntegrityException {
        MessageDigest digest = newDigest();
        digest.update(content.getBytes());
        return toHex(digest.digest());
    }

    /**
     * Combines two child hashes into a parent hash for the Merkle
     * tree: parent = SHA256(left || right).
     */
    public static String combine(String leftHash, String rightHash) throws IntegrityException {
        return hashString(leftHash + rightHash);
    }

    private static MessageDigest newDigest() throws IntegrityException {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen on a standard JVM - SHA-256 is mandatory.
            throw new IntegrityException("SHA-256 algorithm unavailable on this JVM", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
