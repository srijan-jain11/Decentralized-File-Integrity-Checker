package com.integrity.merkle;

import com.integrity.crypto.FileHasher;
import com.integrity.exception.IntegrityException;
import com.integrity.model.FileRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Binary Merkle (hash) tree built over a sorted list of {@link FileRecord}s.
 *
 * <p>Every leaf holds one file's SHA-256 hash; every internal node
 * holds SHA256(left.hash + right.hash). The single {@link #getRootHash()}
 * value therefore acts as a tamper-evident fingerprint of the entire
 * scanned file set: changing, adding, or removing a single file
 * changes the root, and the tree structure lets a verifier localize
 * *where* a mismatch occurred in O(log n) comparisons instead of
 * re-hashing everything from scratch (Scalability / Performance
 * non-functional requirements).</p>
 *
 * <p>Files are sorted by relative path before the tree is built so
 * that two scans of an unmodified directory always produce an
 * identical tree shape and root hash, regardless of filesystem
 * traversal order.</p>
 */
public class MerkleTree {

    private final MerkleNode root;
    private final int leafCount;

    public MerkleTree(List<FileRecord> files) throws IntegrityException {
        if (files == null || files.isEmpty()) {
            throw new IntegrityException("Cannot build a Merkle tree over an empty file set");
        }
        List<FileRecord> sorted = new ArrayList<>(files);
        sorted.sort(FileRecord::compareTo);

        List<MerkleNode> level = new ArrayList<>();
        for (FileRecord record : sorted) {
            level.add(new MerkleNode(record));
        }
        this.leafCount = level.size();
        this.root = buildUp(level);
    }

    /** Recursively combines a level of nodes into the next level up until one root remains. */
    private MerkleNode buildUp(List<MerkleNode> level) throws IntegrityException {
        if (level.size() == 1) {
            return level.get(0);
        }
        List<MerkleNode> nextLevel = new ArrayList<>();
        for (int i = 0; i < level.size(); i += 2) {
            MerkleNode left = level.get(i);
            // If this level has an odd number of nodes, duplicate the last one
            // (standard Merkle tree convention) so every level pairs cleanly.
            MerkleNode right = (i + 1 < level.size()) ? level.get(i + 1) : level.get(i);
            String combined = FileHasher.combine(left.getHash(), right.getHash());
            nextLevel.add(new MerkleNode(left, right, combined));
        }
        return buildUp(nextLevel);
    }

    public String getRootHash() {
        return root.getHash();
    }

    public MerkleNode getRoot() {
        return root;
    }

    public int getLeafCount() {
        return leafCount;
    }
}
