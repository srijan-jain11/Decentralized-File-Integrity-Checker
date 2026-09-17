# Project Statement

## Problem Statement

Developers and system administrators frequently need a dependable way to verify that a set of critical files—such as application configs, source code, deployment scripts, or audit records—has not been tampered with or modified.

Traditional checksum utilities typically check individual files one by one. This approach does not scale well over large directories, does not provide a single proof of integrity for the entire directory tree, and provides no protection if an intruder modifies the log file itself to erase evidence of changes.

This project implements a lightweight Java CLI utility that:
1. Generates a cryptographic fingerprint of an entire directory structure using SHA-256 and a **Merkle tree**, yielding a single root hash that changes if any file in the tree is altered.
2. Identifies and categorizes discrepancies (modified, added, or deleted files) when the root hash diverges from a saved baseline.
3. Records all verification activity into a **hash-chained audit log**, making unauthorized modifications to the audit log itself detectable.

---

## Scope of the Project

**In Scope:**
- Recursive directory scanning with streaming SHA-256 hashing
- In-memory Merkle tree construction and root hash comparison
- Detailed file diffing (identifying modified, deleted, and newly added files)
- Decoupled real-time alert notifications using the Observer pattern
- Append-only, hash-chained audit logging with independent integrity checking
- Clean command-line interface (`baseline`, `verify`, `audit-check`)
- Unit test coverage for core hashing, tree, and detection components

**Out of Scope:**
- Real-time continuous filesystem polling (designed for point-in-time, on-demand verification)
- Multi-node network synchronization (focuses on local directory integrity and cryptographic verification)
- Graphical user interface (GUI) or web dashboard
- File backup and encryption (focuses on detection and auditing rather than file recovery)

---

## Target Users

- **Developers:** Verifying code repositories and builds between release milestones without external infrastructure.
- **System Administrators:** Checking critical configuration directories and sensitive server paths for unauthorized changes.
- **Auditors / Security Enthusiasts:** Ensuring logs and files remain provably unchanged over time.

---

## High-Level Features

1. **Baseline Generation:** Creates a trusted snapshot containing per-file metadata, hashes, and the Merkle root hash.
2. **Verification & Localization:** Compares current directory state against the baseline, validating overall integrity in $O(1)$ time and reporting exact file discrepancies when changes exist.
3. **Tamper-Evident Audit Trail:** Chained SHA-256 log entries ensure historical verification records cannot be modified unnoticed.
4. **Pluggable Alerts:** Uses an Observer architecture so new alert destinations can be added easily without touching core detection code.
