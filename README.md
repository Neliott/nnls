# DAI - Practical Work 1: File Processing CLI

## Description

A short description of what your CLI application does.

### Authors
* Author 1
* Author 2
* Author 3

---

## Build Instructions

```sh
# Clone repository
git clone <repository-url>
cd <repository-directory>

# Build the executable JAR and run tests
./mvnw clean package
```

The executable JAR is generated under `target/dai-pw1-1.0-SNAPSHOT.jar`.

---

## Usage

```sh
java -jar target/dai-pw1-1.0-SNAPSHOT.jar --help
```

### Subcommands

* `command1`: Description of command 1.
* `command2`: Description of command 2.

---

## Running Tests

```sh
./mvnw test
```

---

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on code style, commit conventions, and our workflow.

### Commit Message Guidelines

We strictly follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification:

```text
<type>(<scope>): <description>
```

Common types:
* `feat`: A new feature
* `fix`: A bug fix
* `docs`: Documentation changes
* `style`: Formatting, missing semicolons, etc.
* `refactor`: Code refactoring without behavioral changes
* `test`: Adding or updating tests
* `chore`: Build tasks, dependency updates, configuration
