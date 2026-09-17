# Decentralized File Integrity & Tamper Detection System

A lightweight Java command-line tool that monitors directories for unauthorized file changes, deletions, or new additions. It computes SHA-256 hashes and builds an in-memory **Merkle tree** over your directory structure, allowing instant $O(1)$ checks for whether anything changed, coupled with a **hash-chained audit log** to prevent log tampering.

---

## Why this project?

Standard file integrity tools usually take one of two approaches:
1. **Re-hashing every file sequentially:** Slow and resource-heavy on larger directory trees.
2. **Flat lists of hashes:** Easy to check individual files, but there is no simple way to verify the integrity of the *entire collection* at once or prove that files weren't quietly added or removed.

This tool solves both issues by building a Merkle tree over the directory:
- **Instant $O(1)$ check:** Comparing a single root hash tells you immediately whether the directory is untouched.
- **Granular diagnosis:** If the root hash changes, the tool diffs the file metadata against the baseline to tell you exactly which files were modified, deleted, or added.
- **Tamper-evident audit trail:** Every scan writes to an append-only audit log where each entry contains the cryptographic hash of the previous record. If someone edits the log to hide an unauthorized file change, the chain breaks and `audit-check` flags it.

---

## Features

- **Baseline Snapshots:** Recursively scans a folder, hashes every file, builds a Merkle tree, and saves the root hash and metadata into `integrity-baseline.dat`.
- **Integrity Verification:** Scans the folder again, compares live hashes against the saved baseline, and reports:
  - Overall status (`INTACT` vs `TAMPERING DETECTED`)
  - Categorized discrepancies (`modified`, `deleted`, `added`)
- **Observer-based Alerts:** Uses the Observer pattern (`AlertService` and `AlertListener`) to publish real-time alerts. A console listener is included, and additional listeners (e.g., email or webhooks) can be plugged in without changing detection code.
- **Hash-Chained Audit Logging:** Tracks all baseline and verification events in `audit.log`. Each record links to the previous one via SHA-256.
- **Audit Verification:** An independent `audit-check` command verifies the integrity of the audit trail from the genesis record forward.
- **Unit Tested:** Includes unit test suites covering cryptographic hashing, Merkle tree construction, and tamper detection logic.

---

## Tech Stack

- **Language:** Java 17
- **Build Tool:** Apache Maven
- **Testing:** JUnit 5
- **Cryptography:** `java.security.MessageDigest` (SHA-256)

---

## Project Layout

```text
decentralized-file-integrity/
├── pom.xml
├── README.md
├── statement.md
├── docs/
│   ├── ARCHITECTURE.md                  # UML diagrams and system workflows
│   └── Project_Report_Decentralized_File_Integrity.pdf
├── sample-data/                         # Test directory for manual verification
│   ├── file1.txt
│   ├── config.properties
│   ├── script.py
│   └── subfolder/
│       ├── nested.txt
│       └── data.json
└── src/
    ├── main/java/com/integrity/
    │   ├── Main.java                    # CLI entry point
    │   ├── crypto/FileHasher.java       # Buffered SHA-256 hashing utility
    │   ├── model/FileRecord.java        # Per-file metadata and hash representation
    │   ├── model/IntegrityBaseline.java # Baseline persistence and loading
    │   ├── merkle/MerkleNode.java       # Binary tree node
    │   ├── merkle/MerkleTree.java       # Bottom-up tree construction and root hash
    │   ├── scanner/FileScanner.java     # Recursive directory scanner
    │   ├── detector/TamperDetector.java # Baseline vs live scan comparison
    │   ├── audit/AuditLogger.java       # Hash-chained audit logger and validator
    │   ├── alert/AlertEvent.java        # Alert event model
    │   ├── alert/AlertListener.java     # Observer interface
    │   ├── alert/ConsoleAlertListener.java # Console alert output
    │   ├── alert/AlertService.java      # Observer event dispatcher
    │   └── exception/IntegrityException.java
    └── test/java/com/integrity/
        ├── crypto/FileHasherTest.java
        ├── merkle/MerkleTreeTest.java
        └── detector/TamperDetectorTest.java
```

---

## Setup & Usage

### Prerequisites
- JDK 17 or higher
- Maven 3.6+ (or use the included `mvn.cmd` on Windows)

### Build
Clone the repository and package the runnable JAR:

```bash
mvn clean package
```
*(On Windows systems where Maven isn't in your PATH, you can run `.\mvn.cmd clean package`)*

This compiles the code, runs the test suite, and outputs an executable JAR at `target/file-integrity.jar`.

---

### Commands

#### 1. Create a Baseline
Creates a trusted snapshot of the target directory:

```bash
java -jar target/file-integrity.jar baseline sample-data
```

Example Output:
```text
Scanning sample-data ...
Baseline saved: E:\...\integrity-baseline.dat
Files scanned : 5
Merkle root   : 273f0fbc4a71c533d18a1a247376ac21d5cad6fa61f26ed6164628fd0883130e
```

#### 2. Verify Directory Integrity
Verifies the current directory contents against the saved baseline:

```bash
java -jar target/file-integrity.jar verify sample-data
```

Example Output (Intact):
```text
Re-scanning sample-data ...

RESULT: INTACT ✔
Verification PASSED - no tampering detected (5 files)
```

#### 3. Tamper Detection in Action
If a file's content is modified:

```bash
echo "tampered content" >> sample-data/file1.txt
java -jar target/file-integrity.jar verify sample-data
```

Example Output:
```text
Re-scanning sample-data ...
[MODIFIED] Content changed: file1.txt (expected 8fbe5f78c8..., found e9b12a84ef...)
[ALERT]    Merkle root hash mismatch! Expected 273f0fbc... but computed 94a1b023...

RESULT: TAMPERING DETECTED ✘
Verification FAILED - 1 discrepancies found (modified=1, deleted=0, added=0)
```

#### 4. Verify Audit Log Integrity
Validates that the `audit.log` file itself has not been altered:

```bash
java -jar target/file-integrity.jar audit-check
```

Example Output:
```text
Audit log chain is INTACT - no entries have been altered.
```

---

## Running Unit Tests

Run the full test suite via Maven:

```bash
mvn test
```
*(or `.\mvn.cmd test`)*

The test suites validate:
- **`FileHasherTest`:** Hash determinism, sensitivity to single-byte modifications, and empty file handling.
- **`MerkleTreeTest`:** Tree construction, root stability under different file orders, and odd-leaf balancing.
- **`TamperDetectorTest`:** Identification of modified, deleted, and newly added files, as well as alert dispatching.

---

## Key Design Considerations

- **Memory Efficiency:** File hashing runs through an 8 KB buffer stream (`InputStream`) instead of reading whole files into memory, keeping memory usage constant regardless of file size.
- **Deterministic Hashing:** Paths are normalized across operating systems (Unix `/` separators) and sorted alphabetically before tree construction so results remain consistent across different machines.
- **Graceful Error Handling:** Custom `IntegrityException` wrappers capture I/O and algorithm failures cleanly without dumping raw JVM stack traces into the terminal.
- **Decoupled Architecture:** Using the Observer pattern for alerts keeps detection logic independent of where alerts are displayed or sent.

---

## Future Improvements

- Add a real-time `--watch` mode using Java's `WatchService` for continuous directory monitoring.
- Support generating Merkle proofs to verify single files independently without needing the full tree.
- Add external alert notification channels (SMTP email and webhook notifications).
