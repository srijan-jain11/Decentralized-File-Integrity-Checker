package com.integrity.merkle;

import com.integrity.model.FileRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MerkleTreeTest {

    private FileRecord record(String path, String hash) {
        return new FileRecord(path, 100, 1_700_000_000_000L, hash);
    }

    @Test
    void rootHash_isSameRegardlessOfInputOrder() throws Exception {
        List<FileRecord> filesInOrderA = List.of(
                record("a.txt", "hash-a"),
                record("b.txt", "hash-b"),
                record("c.txt", "hash-c"));

        List<FileRecord> filesInOrderB = new ArrayList<>(filesInOrderA);
        Collections.reverse(filesInOrderB);

        MerkleTree treeA = new MerkleTree(filesInOrderA);
        MerkleTree treeB = new MerkleTree(filesInOrderB);

        assertEquals(treeA.getRootHash(), treeB.getRootHash(),
                "Sorting by path before building means scan order must not affect the root hash");
    }

    @Test
    void rootHash_changesWhenAnyFileHashChanges() throws Exception {
        List<FileRecord> original = List.of(
                record("a.txt", "hash-a"),
                record("b.txt", "hash-b"));
        List<FileRecord> tampered = List.of(
                record("a.txt", "hash-a"),
                record("b.txt", "TAMPERED-HASH"));

        MerkleTree originalTree = new MerkleTree(original);
        MerkleTree tamperedTree = new MerkleTree(tampered);

        assertNotEquals(originalTree.getRootHash(), tamperedTree.getRootHash(),
                "A single modified file must change the Merkle root");
    }

    @Test
    void handlesOddNumberOfFiles_byDuplicatingLastNode() throws Exception {
        List<FileRecord> threeFiles = List.of(
                record("a.txt", "hash-a"),
                record("b.txt", "hash-b"),
                record("c.txt", "hash-c"));

        MerkleTree tree = new MerkleTree(threeFiles);

        assertEquals(3, tree.getLeafCount());
        assertNotNull(tree.getRootHash());
        assertEquals(64, tree.getRootHash().length());
    }

    @Test
    void singleFile_rootHashEqualsThatFilesHash() throws Exception {
        FileRecord onlyFile = record("solo.txt", "abc123");
        MerkleTree tree = new MerkleTree(List.of(onlyFile));

        assertEquals("abc123", tree.getRootHash(),
                "A tree with a single leaf has that leaf as its root");
    }

    @Test
    void emptyFileList_throwsException() {
        assertThrows(Exception.class, () -> new MerkleTree(List.of()));
    }
}
