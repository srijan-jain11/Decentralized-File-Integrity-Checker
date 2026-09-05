package com.integrity.detector;

import com.integrity.alert.AlertEvent;
import com.integrity.alert.AlertService;
import com.integrity.exception.IntegrityException;
import com.integrity.merkle.MerkleTree;
import com.integrity.model.FileRecord;
import com.integrity.model.IntegrityBaseline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core detection engine: given a saved {@link IntegrityBaseline} and a
 * fresh scan of the same directory, determines whether anything was
 * modified, deleted, or added, and whether the overall Merkle root
 * hash still matches.
 *
 * <p>The root-hash comparison alone answers "has anything changed?"
 * in a single hash comparison (O(1)) - the per-file diff below is only
 * computed to explain *what* changed, which is what makes this a
 * genuinely Merkle-tree-based design rather than a flat hash list.</p>
 */
public class TamperDetector {

    private final AlertService alertService;

    public TamperDetector(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Compares a baseline against a current scan, publishing one
     * {@link AlertEvent} per discrepancy found.
     *
     * @return a {@link DetectionResult} summarizing the outcome
     */
    public DetectionResult detect(IntegrityBaseline baseline, List<FileRecord> currentScan)
            throws IntegrityException {

        MerkleTree currentTree = new MerkleTree(currentScan);
        boolean rootMatches = currentTree.getRootHash().equals(baseline.getRootHash());

        Map<String, FileRecord> baselineByPath = indexByPath(baseline.getFiles());
        Map<String, FileRecord> currentByPath = indexByPath(currentScan);

        int modified = 0, deleted = 0, added = 0;

        // Files present in the baseline: either unchanged, modified, or deleted.
        for (FileRecord baselineRecord : baseline.getFiles()) {
            FileRecord currentRecord = currentByPath.get(baselineRecord.getRelativePath());
            if (currentRecord == null) {
                deleted++;
                alertService.publish(new AlertEvent(
                        AlertEvent.Type.FILE_DELETED,
                        baselineRecord.getRelativePath(),
                        "File missing since baseline: " + baselineRecord.getRelativePath()));
            } else if (!currentRecord.getSha256Hash().equals(baselineRecord.getSha256Hash())) {
                modified++;
                alertService.publish(new AlertEvent(
                        AlertEvent.Type.FILE_MODIFIED,
                        baselineRecord.getRelativePath(),
                        "Content changed: " + baselineRecord.getRelativePath()
                                + " (expected " + baselineRecord.getSha256Hash().substring(0, 10)
                                + "..., found " + currentRecord.getSha256Hash().substring(0, 10) + "...)"));
            }
        }

        // Files present now but absent from the baseline are new additions.
        for (FileRecord currentRecord : currentScan) {
            if (!baselineByPath.containsKey(currentRecord.getRelativePath())) {
                added++;
                alertService.publish(new AlertEvent(
                        AlertEvent.Type.FILE_ADDED,
                        currentRecord.getRelativePath(),
                        "New file not present in baseline: " + currentRecord.getRelativePath()));
            }
        }

        if (!rootMatches) {
            alertService.publish(new AlertEvent(
                    AlertEvent.Type.ROOT_HASH_MISMATCH,
                    null,
                    "Merkle root hash mismatch! Expected " + baseline.getRootHash()
                            + " but computed " + currentTree.getRootHash()));
        }

        return new DetectionResult(rootMatches, modified, deleted, added, currentTree.getRootHash());
    }

    private Map<String, FileRecord> indexByPath(List<FileRecord> records) {
        Map<String, FileRecord> map = new HashMap<>();
        for (FileRecord r : records) {
            map.put(r.getRelativePath(), r);
        }
        return map;
    }

    /** Immutable summary of one detection run. */
    public static class DetectionResult {
        private final boolean intact;
        private final int modifiedCount;
        private final int deletedCount;
        private final int addedCount;
        private final String currentRootHash;

        public DetectionResult(boolean intact, int modifiedCount, int deletedCount,
                                int addedCount, String currentRootHash) {
            this.intact = intact;
            this.modifiedCount = modifiedCount;
            this.deletedCount = deletedCount;
            this.addedCount = addedCount;
            this.currentRootHash = currentRootHash;
        }

        public boolean isIntact() {
            return intact;
        }

        public int getModifiedCount() {
            return modifiedCount;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public int getAddedCount() {
            return addedCount;
        }

        public String getCurrentRootHash() {
            return currentRootHash;
        }

        public int getTotalDiscrepancies() {
            return modifiedCount + deletedCount + addedCount;
        }
    }
}
