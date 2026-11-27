# DAI - Practical Work 2: NNLS (Not Live Share)

## Description

NNLS is a client-server network application providing collaborative text file synchronization across multiple users organized into virtual rooms. Instead of retransmitting entire documents on every edit, the client calculates and sends incremental line diffs (`INSERT`, `DELETE`, `CHANGE`), while the central server handles concurrent updates and ensures document consistency via SHA-256 checksum validation.

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

## Docker

### Build the Image

Since the `Dockerfile` packages the compiled JAR directly, build the project with Maven first:

```sh
./mvnw clean package
docker build -t nnls .
```

To target a specific processor architecture (for example, `linux/amd64` from an Apple Silicon Mac):

```sh
docker build --platform linux/amd64 -t nnls .
```

### Publish to GitHub Container Registry (`ghcr.io`)

1. **Create a token** on GitHub: **Settings** > **Developer settings** > **Personal access tokens** > **Tokens (classic)**, with the `write:packages` scope.
2. **Log in, tag (in lowercase), and push:**

```sh
docker login ghcr.io -u <username>
docker tag nnls ghcr.io/<username>/nnls:1.0
docker push ghcr.io/<username>/nnls:1.0
```

Or build and push a multi-architecture image directly with `buildx`:

```sh
docker buildx build --platform linux/amd64,linux/arm64 -t ghcr.io/<username>/nnls:1.0 --push .
```

3. **Make the package public** on GitHub (`https://github.com/<username>?tab=packages` > **Package settings** > **Danger Zone** > **Change visibility** > **Public**) so anyone can pull and run it.

### Alternative: Pull Latest CI Images (`main` / `develop`)

Every push to `main` or `develop` runs the [CI workflow](.github/workflows/ci.yml), which fails if any JUnit test fails and otherwise publishes the latest multi-architecture Docker image to GHCR (as well as the `nnls-jar` build artifact in GitHub Actions):

```sh
# Latest image from main (also tagged :latest)
docker pull ghcr.io/<username>/nnls:main
docker run --rm ghcr.io/<username>/nnls:main

# Latest image from develop
docker pull ghcr.io/<username>/nnls:develop
docker run --rm ghcr.io/<username>/nnls:develop
```

You can also use `ghcr.io/<username>/nnls:main` or `ghcr.io/<username>/nnls:develop` in [compose.yaml](compose.yaml) instead of `ghcr.io/<username>/nnls:1.0`.

### Run Server and Clients with Docker Compose

An example Compose configuration is provided in [compose.yaml](compose.yaml) (starting a `server` and a `client` connected on Compose's default network). From the repository root:

```sh
docker compose up -d
docker compose ps
docker compose logs -f
docker compose down
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
