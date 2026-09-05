package com.integrity;

import com.integrity.alert.AlertService;
import com.integrity.alert.ConsoleAlertListener;
import com.integrity.audit.AuditLogger;
import com.integrity.detector.TamperDetector;
import com.integrity.exception.IntegrityException;
import com.integrity.merkle.MerkleTree;
import com.integrity.model.FileRecord;
import com.integrity.model.IntegrityBaseline;
import com.integrity.scanner.FileScanner;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Command-line entry point for the Decentralized File Integrity &amp;
 * Tamper Detection System.
 *
 * <p>Usage:</p>
 * <pre>
 *   java -jar file-integrity.jar baseline &lt;directory&gt;
 *   java -jar file-integrity.jar verify   &lt;directory&gt;
 *   java -jar file-integrity.jar audit-check
 * </pre>
 *
 * <ul>
 *   <li><b>baseline</b> - scans the directory, builds a Merkle tree, and
 *       saves a snapshot ({@code integrity-baseline.dat}) plus an audit entry.</li>
 *   <li><b>verify</b> - rescans the directory, compares it against the saved
 *       baseline, prints any discrepancies via {@link ConsoleAlertListener},
 *       and records the outcome in the audit log.</li>
 *   <li><b>audit-check</b> - independently verifies that the audit log's
 *       hash chain has not been tampered with.</li>
 * </ul>
 */
public class Main {

    private static final Path BASELINE_FILE = Path.of("integrity-baseline.dat");
    private static final Path AUDIT_LOG_FILE = Path.of("audit.log");

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                printUsage();
                return;
            }

            String command = args[0];
            switch (command) {
                case "baseline" -> runBaseline(requireDirectoryArg(args));
                case "verify" -> runVerify(requireDirectoryArg(args));
                case "audit-check" -> runAuditCheck();
                default -> {
                    System.out.println("Unknown command: " + command);
                    printUsage();
                }
            }
        } catch (IntegrityException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Path requireDirectoryArg(String[] args) throws IntegrityException {
        if (args.length < 2) {
            throw new IntegrityException("Missing directory argument. Usage: " + args[0] + " <directory>");
        }
        return Path.of(args[1]);
    }

    private static void runBaseline(Path directory) throws IntegrityException {
        System.out.println("Scanning " + directory + " ...");
        FileScanner scanner = new FileScanner();
        List<FileRecord> records = scanner.scan(directory);

        MerkleTree tree = new MerkleTree(records);
        IntegrityBaseline baseline = new IntegrityBaseline(tree.getRootHash(), Instant.now(), records);
        baseline.save(BASELINE_FILE);

        AuditLogger auditLogger = new AuditLogger(AUDIT_LOG_FILE);
        auditLogger.log("BASELINE_CREATED",
                "Baseline created for " + directory + " with " + records.size()
                        + " files, root hash " + tree.getRootHash());

        System.out.println("Baseline saved: " + BASELINE_FILE.toAbsolutePath());
        System.out.println("Files scanned : " + records.size());
        System.out.println("Merkle root   : " + tree.getRootHash());
    }

    private static void runVerify(Path directory) throws IntegrityException {
        IntegrityBaseline baseline = IntegrityBaseline.load(BASELINE_FILE);

        System.out.println("Re-scanning " + directory + " ...");
        FileScanner scanner = new FileScanner();
        List<FileRecord> currentScan = scanner.scan(directory);

        AlertService alertService = new AlertService();
        alertService.subscribe(new ConsoleAlertListener());

        TamperDetector detector = new TamperDetector(alertService);
        TamperDetector.DetectionResult result = detector.detect(baseline, currentScan);

        AuditLogger auditLogger = new AuditLogger(AUDIT_LOG_FILE);
        String summary = result.isIntact()
                ? "Verification PASSED - no tampering detected (" + currentScan.size() + " files)"
                : "Verification FAILED - " + result.getTotalDiscrepancies() + " discrepancies found "
                        + "(modified=" + result.getModifiedCount()
                        + ", deleted=" + result.getDeletedCount()
                        + ", added=" + result.getAddedCount() + ")";
        auditLogger.log(result.isIntact() ? "VERIFY_PASSED" : "VERIFY_FAILED", summary);

        System.out.println();
        System.out.println(result.isIntact() ? "RESULT: INTACT ✔" : "RESULT: TAMPERING DETECTED ✘");
        System.out.println(summary);
    }

    private static void runAuditCheck() throws IntegrityException {
        AuditLogger auditLogger = new AuditLogger(AUDIT_LOG_FILE);
        boolean valid = auditLogger.verifyChain(AUDIT_LOG_FILE);
        System.out.println(valid
                ? "Audit log chain is INTACT - no entries have been altered."
                : "WARNING: Audit log chain is BROKEN - log tampering suspected!");
    }

    private static void printUsage() {
        System.out.println("""
                Decentralized File Integrity & Tamper Detection System

                Usage:
                  baseline <directory>   Create a trusted snapshot of a directory
                  verify   <directory>   Check a directory against its saved baseline
                  audit-check            Verify the audit log itself hasn't been tampered with
                """);
    }
}
