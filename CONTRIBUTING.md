# Contributing to this Repository

## Java Style Guide & Coding Standards

### 1. Language & Naming Conventions

* **Language:** All comments, variable names, class names, documentation, and commit messages must be in **English**.
* **Variables & Methods:** Use **lowerCamelCase** (e.g., `inputFile`, `processData()`).
* **Classes & Interfaces:** Use **PascalCase** (e.g., `TextProcessor`, `CommandOne`).
* **Constants:** Use **UPPER_SNAKE_CASE** (e.g., `DEFAULT_BUFFER_SIZE`, `DEFAULT_CHARSET`).
* **Packages:** Use **lowercase** reverse domain notation (e.g., `ch.heigvd`, `ch.heigvd.commands`).

### 2. Formatting & Structure

* **Indentation:** Consistent use of **2 or 4 spaces** per level (no tabs).
* **Line Length:** Avoid excessively long lines (> 100-120 characters).
* **Resource Management:** Always use `try-with-resources` when dealing with I/O streams, readers, and writers to prevent resource leaks.
* **Separation of Concerns:** Keep core data processing strictly separate from CLI parsing and file system operations to allow seamless unit testing with in-memory streams (`Reader`/`Writer`, `InputStream`/`OutputStream`).

### 3. Documentation

* **Classes & Public Methods:** Must be documented using standard **Javadoc** format (`/** ... */`).
* **Comments:** Code must be appropriately commented to explain non-obvious logic or architectural decisions without restating trivial code.

---

## Workflow

### Branches

* Main branch: `main` (always working and buildable).
* Feature branches: `feat/<feature-name>` (e.g., `feat/input-reader`, `feat/uppercase-command`).
* Bugfix branches: `fix/<bug-name>` (e.g., `fix/stream-leak`, `fix/charset-encoding`).

### Commits

Follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), inspired by the [Angular Conventional Config](https://github.com/conventional-changelog/commitlint/tree/master/%40commitlint/config-conventional) and [Angular Contributing Guidelines](https://github.com/angular/angular/blob/22b96b9/CONTRIBUTING.md#-commit-message-guidelines).

**Scheme:**
```text
<type>(<scope>): <description>
```

**Types can be:**
* **`feat:`** A new feature for the user of the software (a *MINOR* change in [semantic versioning](https://semver.org/#summary))
* **`fix:`** A bug fix for the user of the software (a *PATCH* change in [semantic versioning](https://semver.org/#summary))
* **`refactor:`** A code change that neither fixes a bug nor adds a feature
* **`perf:`** A code change that improves performance
* **`test:`** Adding missing tests, correcting existing tests, adding test utils or refactoring tests
* **`style:`** Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc.)
* **`docs:`** Documentation only changes
* **`chore:`** Changes to the build process, auxiliary tools, and dependencies
* **`ci:`** Changes to CI configuration files and scripts

To indicate a *MAJOR* change in [semantic versioning](https://semver.org/#summary), you can use the `BREAKING CHANGE:` keyword in the commit body and draw attention to this change with a `!` after the type (e.g., `feat!: ...`).

### Commit Signing

All commits should be cryptographically signed using GPG or SSH (`git commit -S`) to ensure authenticity (course criterion 9).

---

## Pull Requests & Code Review Checklist

When opening or reviewing a Pull Request:

1. **Build & Tests:** The branch must compile cleanly (`./mvnw clean package`) and all JUnit tests must pass (`./mvnw test`).
2. **Commit History:** Ensure commit messages follow Conventional Commits and are signed.
3. **I/O Standards:**
   - Are streams buffered?
   - Is character encoding explicitly specified where applicable?
   - Are resources properly enclosed in `try-with-resources`?
   - Are error messages user-friendly and explicit?
4. **Testing:** Does the PR include automated unit tests covering typical and edge cases?
