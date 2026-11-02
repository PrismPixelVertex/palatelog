# Project Guidelines & Development Workflows

When working on this repository, please adhere strictly to the following guidelines:

1. **Git Commits & Pushes**: Always confirm with the user before committing code to Git or pushing to remote repositories.
2. **Issue Resolution Workflow**: When an issue is reported, first analyze and debug the root cause, explain the RCA (Root Cause Analysis), and suggest potential solutions. Always get explicit user confirmation before proceeding with code fixes.
3. **New Feature Workflow**: For new features or major enhancements, conduct a detailed analysis, present solution options along with any potential impact on existing code/models, and obtain user confirmation before starting implementation.
4. **Changelog & Version Synchronization**: Always update and maintain `CHANGELOG.md` with every notable fix, enhancement, or new feature added to the codebase. Whenever `CHANGELOG.md` is updated with a new version release header, always ensure `app/build.gradle.kts` (`versionCode` and `versionName`) is updated simultaneously in lockstep.
5. Follow the Clean Architecture, MVVM, and Room patterns established in @doc/architecture_and_design.md.
6. **Git Branching & PR Strategy**:
   - Before cutting a new feature/fix branch, always check for open/pending Pull Requests (PRs) or unmerged branches on remote (`git fetch origin`, `gh pr list`). If any pending PRs exist, notify the user first so they can review and merge them into `main` before starting a new feature implementation to prevent rebase conflicts.
   - Always create a dedicated feature/fix branch off the default branch (`main`), apply changes on that branch, and create a Pull Request (PR) to merge back into `main`.
7. **Repository File Links & Path Sanitization**: When writing or updating documentation, changelogs, architecture records, PR descriptions, or any markdown/code files committed to version control, **NEVER** include local machine absolute file paths or file URLs (such as `file:///...`, `D:/...`, or `C:\...`). Additionally, **NEVER** include personal device hardware identifiers, ADB serial numbers, or device UUIDs (e.g., `RZCT...`); always refer to test devices generically (e.g., "physical Android test device" or "Samsung Galaxy A52s test device") to protect developer hardware privacy. Always use clean repository-relative paths (e.g., `app/src/main/...` or `../app/src/main/...`) or code backticks (e.g., `ClassName`) to avoid leaking developer workstation filesystem structures into public repositories.
