package com.integrity.crypto;

import com.integrity.exception.IntegrityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileHasherTest {

    @TempDir
    Path tempDir;

    @Test
    void hashFile_isDeterministic_forSameContent() throws Exception {
        Path file1 = tempDir.resolve("a.txt");
        Path file2 = tempDir.resolve("b.txt");
        Files.writeString(file1, "hello integrity");
        Files.writeString(file2, "hello integrity");

        assertEquals(FileHasher.hashFile(file1), FileHasher.hashFile(file2),
                "Identical content must produce identical hashes");
    }

    @Test
    void hashFile_changesWhenContentChanges() throws Exception {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "version one");
        String hashBefore = FileHasher.hashFile(file);

        Files.writeString(file, "version two");
        String hashAfter = FileHasher.hashFile(file);

        assertNotEquals(hashBefore, hashAfter, "Changing content must change the hash");
    }

    @Test
    void hashFile_producesA64CharacterHexString() throws Exception {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "any content");
        String hash = FileHasher.hashFile(file);

        assertEquals(64, hash.length(), "SHA-256 hex digest must be 64 characters");
        assertTrue(hash.matches("[0-9a-f]+"), "Hash must be lowercase hexadecimal");
    }

    @Test
    void hashFile_throwsIntegrityException_whenFileMissing() {
        Path missing = tempDir.resolve("does-not-exist.txt");
        assertThrows(IntegrityException.class, () -> FileHasher.hashFile(missing));
    }

    @Test
    void combine_isOrderSensitive() throws Exception {
        String h1 = FileHasher.hashString("left");
        String h2 = FileHasher.hashString("right");

        assertNotEquals(FileHasher.combine(h1, h2), FileHasher.combine(h2, h1),
                "combine(a,b) must differ from combine(b,a) - order carries meaning in a Merkle tree");
    }
}
