# Java Diff Tool

A Java-based utility built to solve a specific migration-drift problem: **comparing a monolithic codebase against its microservice extraction, while both were still under active development in parallel.**

During a monolith → microservice migration, it's common for the same business logic to keep getting bug-fixed or enhanced in the monolith even after part of it has already been carved out into a new microservice. Over time the two implementations quietly drift apart, and there's no easy way to tell which methods in the microservice are now stale/behind the monolith (or vice versa) — file paths, package structure, and even class names (e.g. `RestAPI` → `Controller`) change during the extraction, so a plain `git diff` or folder-level diff is useless.

This tool solves that by parsing both codebases into ASTs (via JavaParser), matching up the equivalent class on each side even when it has moved or been renamed, and diffing method-by-method / constructor-by-constructor to report exactly which methods have logic differences — so you know what needs to be back-ported or re-synced between the two.

## What it does

- Walks the monolith ("left") directory tree of `.java` files and matches each class against its counterpart in the microservice ("right") directory tree, even if it has moved to a different package/folder.
- Handles the common rename pattern from this migration automatically: `XxxRestAPI.java` ↔ `XxxController.java` are treated as the same logical class.
- Extracts every method and constructor body from both sides and compares bodies (ignoring formatting/whitespace) to find real logic differences vs. cosmetic ones.
- Flags classes that couldn't be uniquely matched (`NO_FILE`, `MORE_FILES`) so renames/splits can be resolved manually.
- Supports an exclude list for classes that are known to have intentionally diverged (e.g. already fully migrated, or monolith-only code) so they don't clutter the report.

## Modules / classes

Single Maven module (`core`); each class below is its own standalone entry point (`main()`), no shared CLI yet:

| Class | Purpose |
|---|---|
| `JavaClassDiffGenerator` | Core diff logic. Outputs a plain text/CSV-style report listing changed methods (`DIFF_FILE`) and skipped classes (`NO_FILE`, `MORE_FILES`, `SKIPPING`). |
| `JavaClassDiffGeneratorHTML` | Same diff logic, rendered as a styled HTML table with side-by-side monolith/microservice method bodies and inline highlighted differences — much easier to review a large drift report in. |
| `AdvanceJavaClassDiffHTML` | Extended HTML generator that can ingest a previously-reviewed output HTML file and suppress rows already confirmed as false positives — built for repeated/iterative drift checks as both codebases keep moving. |
| `JavaFileNameFilter` | `FileFilter` used to match classes by suffix or exact name while recursing directories. |
| `FindDuplicateFiles` | Standalone utility to scan a directory tree and flag filenames that appear more than once (currently hardcoded to `dao`-named files) — useful for spotting duplicated DAOs left behind mid-migration. |

## Requirements

- Java 8
- Maven

## Setup

\`\`\`bash
git clone https://github.com/prasadjd/java-diff-tool.git
cd java-diff-tool
mvn clean install
\`\`\`

## Usage

Each entry-point class takes its inputs as hardcoded constants at the top of `main()` rather than CLI args — edit these before running:

\`\`\`java
String leftDirPath  = "{targetProjectPath}";   // monolith codebase path
String rightDirPath = "{sourceProjectPath}";   // microservice codebase path
File finalFile = new File("{Outputfile path}"); // where the drift report gets written
\`\`\`

There's also a path-substring filter inside `process()` used to make sure only relevant source folders get walked (e.g. skipping test folders, or unrelated modules) — update the substring check to match a distinguishing folder name in your two project paths.

Run via Maven:

\`\`\`bash
mvn -pl core exec:java -Dexec.mainClass="org.jd.diff.JavaClassDiffGeneratorHTML"
\`\`\`

(swap in `JavaClassDiffGenerator` for the plain-text report, or `AdvanceJavaClassDiffHTML` when re-running against a codebase you've already reviewed once, to filter out confirmed non-issues.)

### Output

- **Text mode**: flat file, one line per finding — `DIFF_FILE,ClassName,methodSignature,leftDiff,rightDiff`, plus `NO_FILE` / `MORE_FILES` / `SKIPPING` markers for classes that couldn't be matched between monolith and microservice.
- **HTML mode**: a browsable table (`S.No | Type | Class Name | Method Name | Left Changes | Right Changes | Left Method | Right Method`) with color-highlighted diffs and Google-Java-Format-normalized bodies — meant for handing to a reviewer to confirm which drifts are real and need back-porting.

## Known limitations

- No CLI args yet — paths and exclude lists are hardcoded per run (see `TODO`/placeholder strings in source).
- Only one rename convention (`RestAPI` ↔ `Controller`) is auto-matched; other renames during extraction need manual handling.
- `FindDuplicateFiles` is currently hardcoded to a specific filename pattern (`dao`).

## License

_Add a license if you intend this to be used outside your own projects._
