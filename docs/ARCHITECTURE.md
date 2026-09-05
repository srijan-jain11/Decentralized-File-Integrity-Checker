# Architecture & Design Diagrams

All diagrams below are written in [Mermaid](https://mermaid.js.org/) and render
automatically when this file is viewed on GitHub.

## 1. System Architecture

```mermaid
flowchart TB
    subgraph CLI["Presentation Layer"]
        Main["Main.java (CLI)"]
    end

    subgraph Core["Core Domain Layer"]
        Scanner["FileScanner"]
        Hasher["FileHasher"]
        Tree["MerkleTree / MerkleNode"]
        Detector["TamperDetector"]
    end

    subgraph Persistence["Persistence Layer"]
        Baseline["IntegrityBaseline\n(flat-file snapshot)"]
        Audit["AuditLogger\n(hash-chained log)"]
    end

    subgraph Notification["Notification Layer"]
        AlertSvc["AlertService (Subject)"]
        Listener["ConsoleAlertListener (Observer)"]
    end

    Main --> Scanner
    Scanner --> Hasher
    Scanner --> Tree
    Main --> Baseline
    Main --> Detector
    Detector --> Tree
    Detector --> AlertSvc
    AlertSvc --> Listener
    Main --> Audit
```

**Layering rationale:** the CLI never touches hashing or file I/O directly;
it only orchestrates calls to the domain layer. This keeps each module
independently testable (see `src/test`) and satisfies the Maintainability
non-functional requirement.

## 2. Use Case Diagram

```mermaid
flowchart LR
    User(("User / Admin"))
    UC1(["Create Baseline"])
    UC2(["Verify Directory"])
    UC3(["Check Audit Log Integrity"])
    UC4(["Receive Tamper Alerts"])

    User --> UC1
    User --> UC2
    User --> UC3
    UC2 -.includes.-> UC4
```

## 3. Class Diagram (simplified)

```mermaid
classDiagram
    class Main {
        +main(args)
    }
    class FileScanner {
        +scan(Path) List~FileRecord~
    }
    class FileHasher {
        +hashFile(Path) String
        +hashString(String) String
        +combine(String, String) String
    }
    class FileRecord {
        -String relativePath
        -long sizeBytes
        -long lastModifiedEpochMillis
        -String sha256Hash
        +hasSameContent(FileRecord) boolean
    }
    class MerkleTree {
        -MerkleNode root
        +getRootHash() String
    }
    class MerkleNode {
        -String hash
        -MerkleNode left
        -MerkleNode right
        -FileRecord fileRecord
    }
    class IntegrityBaseline {
        -String rootHash
        -Instant createdAt
        -List~FileRecord~ files
        +save(Path)
        +load(Path)$ IntegrityBaseline
    }
    class TamperDetector {
        +detect(IntegrityBaseline, List~FileRecord~) DetectionResult
    }
    class AlertService {
        +subscribe(AlertListener)
        +publish(AlertEvent)
    }
    class AlertListener {
        <<interface>>
        +onAlert(AlertEvent)
    }
    class ConsoleAlertListener {
        +onAlert(AlertEvent)
    }
    class AuditLogger {
        +log(String, String)
        +verifyChain(Path) boolean
    }

    Main --> FileScanner
    Main --> TamperDetector
    Main --> AuditLogger
    Main --> IntegrityBaseline
    FileScanner --> FileHasher
    FileScanner --> FileRecord
    MerkleTree --> MerkleNode
    MerkleNode --> FileRecord
    TamperDetector --> MerkleTree
    TamperDetector --> AlertService
    AlertService --> AlertListener
    ConsoleAlertListener ..|> AlertListener
    IntegrityBaseline --> FileRecord
```

## 4. Sequence Diagram — `verify` command

```mermaid
sequenceDiagram
    actor U as User
    participant M as Main
    participant B as IntegrityBaseline
    participant S as FileScanner
    participant T as TamperDetector
    participant A as AlertService
    participant L as ConsoleAlertListener
    participant AL as AuditLogger

    U->>M: verify <directory>
    M->>B: load(baselineFile)
    B-->>M: baseline
    M->>S: scan(directory)
    S-->>M: List<FileRecord>
    M->>T: detect(baseline, currentScan)
    T->>T: build MerkleTree(currentScan)
    T->>T: compare root hash + per-file diff
    loop for each discrepancy
        T->>A: publish(AlertEvent)
        A->>L: onAlert(event)
        L-->>U: prints alert to console
    end
    T-->>M: DetectionResult
    M->>AL: log(VERIFY_PASSED/FAILED, summary)
    M-->>U: prints final result
```

## 5. Process / Workflow Diagram

```mermaid
flowchart TD
    Start(["Start"]) --> Cmd{"Which command?"}
    Cmd -->|baseline| Scan1["Scan directory"]
    Scan1 --> Hash1["Hash every file (SHA-256)"]
    Hash1 --> Build1["Build Merkle tree"]
    Build1 --> Save["Save baseline + root hash"]
    Save --> LogA["Write audit entry"]
    LogA --> End1(["Done"])

    Cmd -->|verify| Load["Load saved baseline"]
    Load --> Scan2["Re-scan directory"]
    Scan2 --> Build2["Build new Merkle tree"]
    Build2 --> Compare{"Root hash matches?"}
    Compare -->|Yes| Pass["Report: INTACT"]
    Compare -->|No| Diff["Diff file-by-file"]
    Diff --> Alerts["Publish alerts for each change"]
    Alerts --> Fail["Report: TAMPERING DETECTED"]
    Pass --> LogB["Write audit entry"]
    Fail --> LogB
    LogB --> End2(["Done"])

    Cmd -->|audit-check| Walk["Walk audit.log entries"]
    Walk --> Verify{"Chain unbroken?"}
    Verify -->|Yes| OK["Report: Log intact"]
    Verify -->|No| Bad["Report: Log tampered"]
```

## 6. Why a Merkle Tree instead of a flat hash list?

| Approach | Detect *that* something changed | Detect *what* changed | Cost to re-verify a large tree |
|---|---|---|---|
| Flat list of file hashes | O(n) compare | O(n) compare | O(n) |
| Single hash of concatenated files | O(1) compare | Not possible without full re-scan | O(n) but no localization |
| **Merkle tree (this project)** | **O(1)** via root hash | **O(n)** with structural pruning possible | O(n) to build, O(log n) to prove a single file's inclusion |

The Merkle root gives an instant yes/no answer ("has anything in this
directory changed since baseline?"), while still allowing a full,
explainable diff when the answer is "yes" — which is what a real
tamper-detection tool needs to be useful rather than just alarming.
