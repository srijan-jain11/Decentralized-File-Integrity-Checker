# Decentralized File Integrity & Tamper Detection System

A Java command-line tool that detects unauthorized modification, deletion,
or addition of files in a directory using **SHA-256 hashing**, a
**Merkle (hash) tree**, and a **hash-chained, tamper-evident audit log**.

Built for the VITyarthi "Build Your Own Project" flipped-course assignment.

## Overview

Traditional file-integrity checkers either re-hash every file every time
(slow at scale) or keep a flat list of hashes with no way to prove the
*set as a whole* hasn't changed. This project builds a **Merkle tree**
over a directory's file hashes so that:

- A single root-hash comparison answers "has anything changed?" in O(1).
- If something *has* changed, the tool still walks the per-file records
  to report exactly which files were modified, deleted, or added.
- The tool's own audit trail is hash-chained (each log entry embeds the
  hash of the previous one), so someone tampering with the *log itself*
  to hide evidence of tampering is also detectable.

## Features

- **Baseline creation** — recursively scans a directory, computes a
  SHA-256 hash per file, and builds a Merkle tree whose root hash is
  saved as the trusted snapshot.
- **Verification** — re-scans the same directory later and reports:
  - overall integrity (root hash match/mismatch)
  - which specific files were **modified**, **deleted**, or **added**
- **Real-time-style alerts** via the Observer pattern — pluggable
  `AlertListener`s react to each discrepancy (a console listener is
  included; email/webhook listeners can be added without touching
  detection logic).
- **Tamper-evident audit logging** — every baseline/verify run is
  recorded in a hash-chained log; `audit-check` independently verifies
  that log has not itself been altered.
- **Unit tested** core logic (hashing, Merkle tree construction, tamper
  detection) using JUnit 5.

## Technologies / Tools Used

- Java 17
- Maven (build & dependency management)
- JUnit 5 (unit testing)
- `java.security.MessageDigest` (SHA-256)
- Mermaid diagrams (architecture & UML docs, render on GitHub)

## Project Structure

```
decentralized-file-integrity/
├── pom.xml
├── README.md
├── statement.md
├── docs/
│   └── ARCHITECTURE.md         # System architecture, use case, class, sequence diagrams
├── sample-data/                # Sample directory to try the tool on
│   ├── file1.txt
│   ├── config.properties
│   ├── script.py
│   └── subfolder/
│       ├── nested.txt
│       └── data.json
└── src/
    ├── main/java/com/integrity/
    │   ├── Main.java                       # CLI entry point
    │   ├── crypto/FileHasher.java          # SHA-256 hashing utility
    │   ├── model/FileRecord.java           # Per-file metadata + hash
    │   ├── model/IntegrityBaseline.java    # Snapshot persistence
    │   ├── merkle/MerkleNode.java          # Tree node
    │   ├── merkle/MerkleTree.java          # Tree construction & root hash
    │   ├── scanner/FileScanner.java        # Directory walking
    │   ├── detector/TamperDetector.java    # Baseline vs. current comparison
    │   ├── audit/AuditLogger.java          # Hash-chained audit log
    │   ├── alert/AlertEvent.java           # Alert data model
    │   ├── alert/AlertListener.java        # Observer interface
    │   ├── alert/ConsoleAlertListener.java # Console observer
    │   ├── alert/AlertService.java         # Observer subject/dispatcher
    │   └── exception/IntegrityException.java
    └── test/java/com/integrity/
        ├── crypto/FileHasherTest.java
        ├── merkle/MerkleTreeTest.java
        └── detector/TamperDetectorTest.java
```

That's **13 main classes + 3 test classes across 8 packages** — comfortably
within the assignment's 5–10 meaningful modules/files requirement, organized
by responsibility rather than dumped in one package.

## Steps to Install & Run

### Prerequisites
- JDK 17 or later
- Maven 3.6+

### Build
```bash
git clone <your-repo-url>
cd decentralized-file-integrity
mvn clean package
```
This produces a runnable JAR at `target/file-integrity.jar`.

### Run

**1. Create a baseline (trusted snapshot) of a directory:**
```bash
java -jar target/file-integrity.jar baseline sample-data
```
Output:
```
Scanning sample-data ...
Baseline saved: /path/to/integrity-baseline.dat
Files scanned : 5
Merkle root   : 7a1f3c...
```

**2. Verify the directory later (no changes made):**
```bash
java -jar target/file-integrity.jar verify sample-data
```
```
RESULT: INTACT ✔
Verification PASSED - no tampering detected (5 files)
```

**3. Simulate tampering and verify again:**
```bash
echo "malicious edit" >> sample-data/file1.txt
java -jar target/file-integrity.jar verify sample-data
```
```
[MODIFIED] Content changed: file1.txt (expected 9f2ab1c3d4..., found e0771a9f22...)
[ALERT]    Merkle root hash mismatch! Expected 7a1f3c... but computed 4b8e91...

RESULT: TAMPERING DETECTED ✘
Verification FAILED - 1 discrepancies found (modified=1, deleted=0, added=0)
```

**4. Check that the audit log itself hasn't been tampered with:**
```bash
java -jar target/file-integrity.jar audit-check
```
```
Audit log chain is INTACT - no entries have been altered.
```

## Instructions for Testing

Run the full unit test suite (hashing, Merkle tree, tamper detection):
```bash
mvn test
```

Test coverage includes:
- Hash determinism and sensitivity to content changes (`FileHasherTest`)
- Merkle root stability under reordering, and change-detection on
  modification (`MerkleTreeTest`)
- Correct classification of modified/deleted/added files and alert
  publishing (`TamperDetectorTest`)

## Non-Functional Requirements Addressed

| Requirement | How it's addressed |
|---|---|
| **Security** | SHA-256 content hashing; hash-chained audit log resists silent log tampering |
| **Reliability** | Domain-specific `IntegrityException` wraps all I/O/hashing failures; graceful CLI error messages instead of stack traces |
| **Performance** | Streamed (buffered) file hashing avoids loading whole files into memory; Merkle root gives O(1) "did anything change" checks |
| **Scalability** | Works over arbitrarily nested directories; Merkle structure scales to large file sets without re-hashing everything on every check |
| **Maintainability** | Clear package-per-responsibility layout; Observer pattern (`AlertService`/`AlertListener`) allows new alert channels without touching detection logic |
| **Usability** | Simple three-command CLI; human-readable baseline file format |
| **Logging/Monitoring** | Every baseline/verify run is permanently recorded in `audit.log` |

## Screenshots

_(Add terminal screenshots of `baseline`, `verify`, and `audit-check` runs here before submission.)_

## Future Enhancements

- Add a `--watch` mode that polls a directory continuously instead of one-shot verification
- Merkle proof generation so a single file's integrity can be verified without holding the whole tree
- Pluggable alert channels: email (SMTP) and webhook listeners
- Web dashboard (Spring Boot) visualizing the Merkle tree and audit history
