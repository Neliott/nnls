# DAI - Practical Work 2: CDSP (Collaborative Diff Synchronization Protocol)

## Description

CDSP is a client-server network application providing real-time collaborative text file synchronization across multiple users organized into virtual rooms. Instead of retransmitting entire documents on every edit, the client calculates and sends incremental line diffs (`INSERT`, `DELETE`, `CHANGE`), while the central server handles concurrent updates and ensures document consistency via SHA-256 checksum validation.

The full protocol specification is documented in [PROTOCOL.md](PROTOCOL.md).

### Authors
* Eliott JAQUIER
* Shanshe GUNDISHVILI
* Aurélien DEVAUD

---

## Build Instructions

```sh
# Clone repository
git clone https://github.com/Neliott/nnls.git
cd nnls

# Build the executable JAR and run tests
./mvnw clean package
```

The executable JAR is generated under `target/nnls-1.0-SNAPSHOT.jar`.

---

## Usage

```sh
java -jar target/nnls-1.0-SNAPSHOT.jar --help
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
