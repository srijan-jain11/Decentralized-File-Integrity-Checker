package com.integrity.detector;

import com.integrity.alert.AlertEvent;
import com.integrity.alert.AlertService;
import com.integrity.merkle.MerkleTree;
import com.integrity.model.FileRecord;
import com.integrity.model.IntegrityBaseline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TamperDetectorTest {

    private List<AlertEvent> capturedEvents;
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        capturedEvents = new ArrayList<>();
        alertService = new AlertService();
        alertService.subscribe(capturedEvents::add);
    }

    private FileRecord record(String path, String hash) {
        return new FileRecord(path, 50, 1_700_000_000_000L, hash);
    }

    @Test
    void detect_reportsIntact_whenNothingChanged() throws Exception {
        List<FileRecord> files = List.of(record("a.txt", "h1"), record("b.txt", "h2"));
        IntegrityBaseline baseline = new IntegrityBaseline(
                new MerkleTree(files).getRootHash(), Instant.now(), files);

        TamperDetector detector = new TamperDetector(alertService);
        TamperDetector.DetectionResult result = detector.detect(baseline, files);

        assertTrue(result.isIntact());
        assertEquals(0, result.getTotalDiscrepancies());
        assertTrue(capturedEvents.isEmpty());
    }

    @Test
    void detect_flagsModifiedFile() throws Exception {
        List<FileRecord> original = List.of(record("a.txt", "h1"), record("b.txt", "h2"));
        List<FileRecord> modified = List.of(record("a.txt", "h1"), record("b.txt", "TAMPERED"));

        IntegrityBaseline baseline = new IntegrityBaseline(
                new MerkleTree(original).getRootHash(), Instant.now(), original);

        TamperDetector detector = new TamperDetector(alertService);
        TamperDetector.DetectionResult result = detector.detect(baseline, modified);

        assertFalse(result.isIntact());
        assertEquals(1, result.getModifiedCount());
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getType() == AlertEvent.Type.FILE_MODIFIED));
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getType() == AlertEvent.Type.ROOT_HASH_MISMATCH));
    }

    @Test
    void detect_flagsDeletedFile() throws Exception {
        List<FileRecord> original = List.of(record("a.txt", "h1"), record("b.txt", "h2"));
        List<FileRecord> afterDeletion = List.of(record("a.txt", "h1"));

        IntegrityBaseline baseline = new IntegrityBaseline(
                new MerkleTree(original).getRootHash(), Instant.now(), original);

        TamperDetector detector = new TamperDetector(alertService);
        TamperDetector.DetectionResult result = detector.detect(baseline, afterDeletion);

        assertEquals(1, result.getDeletedCount());
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getType() == AlertEvent.Type.FILE_DELETED));
    }

    @Test
    void detect_flagsAddedFile() throws Exception {
        List<FileRecord> original = List.of(record("a.txt", "h1"));
        List<FileRecord> afterAddition = List.of(record("a.txt", "h1"), record("new.txt", "h3"));

        IntegrityBaseline baseline = new IntegrityBaseline(
                new MerkleTree(original).getRootHash(), Instant.now(), original);

        TamperDetector detector = new TamperDetector(alertService);
        TamperDetector.DetectionResult result = detector.detect(baseline, afterAddition);

        assertEquals(1, result.getAddedCount());
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getType() == AlertEvent.Type.FILE_ADDED));
    }
}
