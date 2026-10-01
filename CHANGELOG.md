# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.2.0 - unreleased

### ⚠️ Upgrade notes

- **Runtime requires Java 25.** The Docker base image is now
  `bellsoft/liberica-openjdk-alpine:25`.
- **`data/secret.bin` and all `{AES256}` values in `data/config.yaml` must be
  recreated.** The new crypto library uses AES-256/GCM with a 32-byte key; the
  old 48-byte secret (AES/CBC key + fixed IV) and its encrypted values are no
  longer readable, and the application refuses to start with them.
  1. Back up `data/secret.bin` and `data/config.yaml`, then move the old
     `secret.bin` out of the way.
  2. `java -jar l9g-cardinfo.jar -i` creates a new secret.
  3. Re-encrypt the LDAP bind password with `-e "<password>"`, and the bearer
     tokens either with `-e "<token>"` (clients keep their tokens) or create new
     ones with `-g`.
- **LDAPS server certificates are now validated** against the JVM truststore,
  including the hostname. Self-signed LDAP certificates must be added to the
  truststore. The previous trust-all behaviour is available only via
  `ldap.host.trust-all-certificates: true` (testing only).
- **No fully executable jar anymore.** Spring Boot 4 removed the launch script;
  start the application with `java -jar l9g-cardinfo.jar` instead of running the
  jar directly or as an init.d service.
- The application fails to start if two bearer tokens share the same value or a
  token value is still encrypted.

### Changed

- Spring Boot 3.5.14 → 4.1.1 (Spring Framework 7.0, Spring Security 7.1).
- Java 21 → 25.
- springdoc-openapi 2.8.17 → 3.1.1.
- Lombok 1.18.48.
- `spring-boot-starter-web` replaced by `spring-boot-starter-webmvc`.
- Encrypted configuration is now provided by the libraries
  `de.l9g:crypto-core` and `de.l9g:crypto-spring` 1.0.7; `{AES256}` properties
  are decrypted while the environment is prepared. The secret location can be
  changed with `secret.path` / `SECRET_PATH`.
- `/api/v1/cardinfo` requires authentication for every HTTP method (previously
  only `GET`).
- Error responses (HTTP 400, 401, 500) contain fixed messages only, no
  exception details. A missing `userId` now returns the documented HTTP 400
  JSON response.
- Sessions are explicitly stateless.
- Bearer tokens are kept in memory only as SHA-256 hashes; disabled tokens are
  not indexed at all. The `Bearer` scheme is matched case-insensitively.
- Replaced deprecated APIs (`MemberCategory.DECLARED_FIELDS`, relocated
  `UserDetailsServiceAutoConfiguration`, raw `ResponseEntity`).
- Docker image is based on the JRE instead of the JDK
  (`bellsoft/liberica-openjre-alpine:25`) and has a `HEALTHCHECK`.
- Docker helper scripts work from any directory, stop on errors and accept
  `IMAGE=...` to use another image (e.g. the locally built one).
- Build scripts and Dockerfile use JDK 25; `NATIVE_COMPILE.sh` expects GraalVM
  at `/opt/graalvm/25`.

### Removed

- In-project crypto package `l9g.cardinfo.crypto` including the
  `@EncryptedValue` annotation; use `@Value` / `@ConfigurationProperties`.

### Fixed

- Application crashed at startup with `ArrayIndexOutOfBoundsException` as soon
  as `data/secret.bin` already existed (48-byte file read into a 32-byte buffer).
- `-h` output showed the wrong program name and did not list `-i`.
- `-h` and a normal server start created `data/secret.bin` as a side effect
  before evaluating the arguments; only `-e`, `-g` and `-i` load or create the
  secret now.
- Dockerfile started the non-existent `/l9g-uidgen.jar`; Docker scripts and
  `docker-compose.yaml` built, tagged and ran `l9g-uidgen` images instead of
  `l9g-cardinfo`.

### Security

- **LDAP filter injection:** the `userId` request parameter was inserted into
  the LDAP filter unescaped; e.g. `userId=*` returned card data of arbitrary
  entries. The value is now escaped with `Filter.encodeValue`.
- **Unauthenticated `HEAD` requests** to `/api/v1/cardinfo` passed the security
  filter chain.
- **Predictable bearer tokens:** `-g` generated tokens with `java.util.Random`
  seeded with the current time; tokens are now generated with `SecureRandom`.
- **Weak encryption of configuration values:** AES/CBC with a fixed IV and no
  integrity protection replaced by AES-256/GCM with a random IV per value.
- **LDAPS man-in-the-middle:** server certificates were not validated (see
  upgrade notes).
- **Information disclosure:** HTTP 500 responses included internal exception
  messages.
- **Secrets in logs:** the LDAP bind password was logged in clear text at trace
  level, and bearer token values appeared in `toString()` output.

### Added

- Test suite (JUnit 6, MockMvc, UnboundID in-memory LDAP server) covering
  authentication, per-token base DN restriction, LDAP injection, error
  responses, security headers and attribute mapping. Tests run in
  `target/test-work` and never touch the real `data/` directory.
- Configuration option `ldap.host.trust-all-certificates` (default `false`).

## 1.1.1 - 2026-02-21

Initial version in this repository.
