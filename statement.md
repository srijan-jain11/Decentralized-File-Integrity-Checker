# Problem Statement

## Problem Statement

Organizations and individuals frequently need to know whether a set of
critical files — configuration files, legal documents, source code,
compliance records — has been altered, deleted, or tampered with,
whether by malicious actors, accidental edits, or software bugs.
Simple checksum tools tell you *that* a single file changed, but they
don't scale efficiently to large directories, and they offer no
protection against someone editing the *log* that recorded past
integrity checks to cover their tracks.

This project builds a lightweight, dependency-free Java tool that:

1. Fingerprints an entire directory tree using SHA-256 hashes combined
   into a **Merkle tree**, producing a single root hash that changes if
   *any* file in the tree changes.
2. Detects and classifies exactly what changed (modified / deleted /
   added files) when the root hash no longer matches a saved baseline.
3. Records every check in a **hash-chained audit log**, so tampering
   with the audit trail itself is also detectable.

## Scope of the Project

**In scope:**
- Recursive directory scanning and SHA-256 file hashing
- Merkle tree construction and root-hash comparison
- Per-file diff (modified / deleted / added) against a saved baseline
- Observer-pattern alerting to the console
- Hash-chained, append-only audit logging with independent chain
  verification
- Command-line interface (`baseline`, `verify`, `audit-check`)
- Unit tests for the hashing, Merkle tree, and detection logic

**Out of scope (see Future Enhancements in README):**
- Continuous/real-time filesystem watching (this is a point-in-time,
  on-demand tool)
- Distributed/multi-node deployment (despite the "decentralized" name
  referring to the tamper-evident *data structure*, not a networked
  system)
- GUI or web dashboard
- Encryption/backup of the files themselves (this tool detects
  tampering; it does not prevent or reverse it)

## Target Users

- Students/developers who want to verify a project folder or codebase
  hasn't been altered between milestones
- System administrators monitoring configuration directories for
  unauthorized changes
- Anyone needing a simple, auditable "has this folder changed since
  I last checked?" tool without standing up external infrastructure

## High-Level Features

1. **Baseline creation** — snapshot a directory's Merkle root hash and
   per-file metadata as the trusted reference point.
2. **Verification** — re-scan and compare against the baseline,
   reporting overall integrity plus a detailed per-file diff.
3. **Audit trail** — every baseline/verify operation is permanently,
   verifiably logged via a hash chain.
4. **Extensible alerting** — new alert delivery channels (email,
   webhook, file) can be added by implementing one interface, without
   modifying detection logic.
