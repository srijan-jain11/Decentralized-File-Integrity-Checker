package com.integrity.merkle;

import com.integrity.model.FileRecord;

/**
 * A single node in the {@link MerkleTree}. Leaf nodes wrap a
 * {@link FileRecord}; internal nodes only hold the combined hash of
 * their two children.
 */
public class MerkleNode {

    private final String hash;
    private final MerkleNode left;
    private final MerkleNode right;
    private final FileRecord fileRecord; // non-null only for leaves

    /** Constructs a leaf node directly from a file's record. */
    public MerkleNode(FileRecord fileRecord) {
        this.fileRecord = fileRecord;
        this.hash = fileRecord.getSha256Hash();
        this.left = null;
        this.right = null;
    }

    /** Constructs an internal node from two children and their pre-combined hash. */
    public MerkleNode(MerkleNode left, MerkleNode right, String combinedHash) {
        this.left = left;
        this.right = right;
        this.hash = combinedHash;
        this.fileRecord = null;
    }

    public String getHash() {
        return hash;
    }

    public MerkleNode getLeft() {
        return left;
    }

    public MerkleNode getRight() {
        return right;
    }

    public boolean isLeaf() {
        return fileRecord != null;
    }

    public FileRecord getFileRecord() {
        return fileRecord;
    }
}
