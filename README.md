# L9G Card Info

## Project Overview

L9G Card Info is a Spring Boot service that provides campus card information
(name, birthday, customer and barcode number, validity, …) for a user. The data
is read from an LDAP directory; clients authenticate with static Bearer tokens.

Each token is bound to its own LDAP base DN and search scope, so different
clients can be restricted to different parts of the directory.

**Key Technologies:**

*   **Framework:** Spring Boot 4.1
*   **Language:** Java 25
*   **Security:** Spring Security with static Bearer token authentication
*   **Directory Service:** LDAP (via UnboundID LDAP SDK)
*   **Encrypted configuration:** `de.l9g:crypto-core` / `crypto-spring` (AES-256/GCM)
*   **API Documentation:** springdoc-openapi

See [CHANGELOG.md](CHANGELOG.md) for the changes per version and the upgrade
notes (1.2.0: Java 25 and new encryption; 2.0.0: API v2).

## Building and Running

### Prerequisites

*   Java 25
*   Apache Maven

### Build

```bash
mvn clean package
```

This builds `target/l9g-cardinfo.jar` and runs the tests.

### Test

```bash
mvn test
mvn test -Dtest=CardinfoApiIntegrationTest     # a single test class
```

The tests run against an in-memory LDAP server in the working directory
`target/test-work`; the real `data/` directory is never touched.

### Run

Start the application from the directory that contains the `data` directory:

```bash
java -jar target/l9g-cardinfo.jar
```

*   API: port `8080`
*   Health check: `http://localhost:9000/actuator/health` (management port)

## Configuration

The application reads two files from the `data` directory (relative to the
working directory):

**`data/secret.bin`:** the 32-byte AES-256 key used to encrypt and decrypt
configuration values. It is created automatically on first use with file
permissions `r--------`. Keep it secret and back it up — without it the
encrypted values in `config.yaml` cannot be decrypted. A different location can
be set with the system property `secret.path` or the environment variable
`SECRET_PATH`.

**`data/config.yaml`:** the actual configuration. Values written as
`{AES256}...` are decrypted at startup. See `data/config-sample.yaml`.

```yaml
ldap:
  host:
    name: ldap.example.org
    port: 636
    ssl: true
    # only for testing: accept any LDAPS server certificate (default: false)
    # trust-all-certificates: false
  bind:
    dn: cn=Directory Manager
    password: "{AES256}..."
  filter: (soniaExternalUid=%s)        # %s is replaced by the escaped userId
  user:
    attributes: eduPersonEntitlement,givenName,sn,soniaBirthday,soniaCustomerNumber,soniaChipcardBarcode,soniaHisPersonId,employeeType,soniaIsValidFrom,soniaIsValidUntil,soniaStudentValidityCode

bearer-tokens:
  map:
    library:                           # token name, used in the logs
      token: "{AES256}..."
      owner: Library
      description: Card printing in the library
      enabled: true
      ldap:                            # required for every token
        base-dn: ou=people,dc=example,dc=org
        scope: sub                     # sub | one | base
```

LDAPS server certificates are validated against the JVM truststore, including
the hostname. Add a self-signed LDAP certificate to the truststore rather than
enabling `trust-all-certificates`.

The class that maps an LDAP entry to the response is configurable with
`cardinfo.attributes-mapper-class` (default:
`l9g.cardinfo.mapper.SoniaAttributeMapper`), for v2 with
`cardinfo.attributes-mapper-v2-class` (default:
`l9g.cardinfo.v2.mapper.SoniaAttributeMapper`).

### Command-line options

| Option | Description |
|---|---|
| `-i` | Initialize `data/secret.bin` |
| `-e "<text>"` | Encrypt a value for `config.yaml` |
| `-g` | Generate a new random bearer token and print it in clear text and encrypted |
| `-h` | Show help |

```bash
java -jar target/l9g-cardinfo.jar -g
java -jar target/l9g-cardinfo.jar -e "my-secret-value"
```

The clear-text token from `-g` is given to the client; the encrypted value goes
into `config.yaml`.

## API

### `GET /api/v1/cardinfo?userId=<id>`

Requires the header `Authorization: Bearer <token>`.

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/cardinfo?userId=1001"
```

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "birthday": "1970-01-01",
  "customerNumber": "0012345678",
  "barcodeNumber": "87654321",
  "barcodeFormat": "code39",
  "campusManagementId": "654321",
  "employeeType": "b",
  "validFrom": "2024-01-01",
  "validUntil": "2028-12-31",
  "status": "OK"
}
```

Attributes with a `null` value are omitted.

`validFrom` / `validUntil` come from `soniaIsValidFrom` / `soniaIsValidUntil`.
For students (`employeeType=s`) they are taken from `soniaStudentValidityCode`
(`x:DD.MM.YYYY:DD.MM.YYYY…`) instead. If that code is missing, empty or
`00:na:na:na:na:na:na`, both fields are omitted; any other malformed code
results in HTTP 500.

Errors are returned as JSON with a `status` field only:

| Status | Meaning |
|---|---|
| 400 | `userId` parameter missing |
| 401 | Bearer token missing, unknown or disabled |
| 404 | No entry for `userId` below the token's base DN |
| 500 | Internal error, e.g. `userId` not unique (details only in the server log) |

### `GET /api/v2/cardinfo?userId=<id>`

Same as v1 (authentication, parameters, error codes), the response
additionally contains the Deutschlandticket information:

```json
{
  "firstName": "John",
  "...": "...",
  "validTicket": true,
  "eduPersonEntitlement": "urn:mace:ride-ticketing.de:entitlement:dticket:timeframe:20260901-20270228",
  "status": "OK"
}
```

*   `validTicket` is `true` if today (Europe/Berlin) lies within the timeframe
    `yyyyMMdd-yyyyMMdd` of at least one entitlement
    `urn:mace:ride-ticketing.de:entitlement:dticket:timeframe:…` (both days
    inclusive). It is `false` if `eduPersonEntitlement` is missing, no
    timeframe matches or the value is malformed. `validTicket` is always
    present, also in error responses.
*   `eduPersonEntitlement` contains only the Deutschlandticket entitlements
    (comma separated); other entitlements are not returned. It is omitted if
    there are none.

`eduPersonEntitlement` must be listed in `ldap.user.attributes`.

### `GET /api/v1/buildinfo`

Returns the build properties (version, build time, Java version, …). No
authentication required.

### OpenAPI

The OpenAPI specification (`/v3/api-docs`) and Swagger UI (`/swagger-ui.html`,
redirect from `/api/docs`) are disabled by default. Enable them in
`config.yaml`:

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

## Docker

The image (`bellsoft/liberica-openjre-alpine:25`) runs the jar with working
directory `/`, so the configuration is expected in the volume `/data`
(`/data/config.yaml`, `/data/secret.bin`). A `HEALTHCHECK` queries
`/actuator/health` on the management port.

```bash
docker/BUILD_IMAGE.sh                  # build the jar and the local image l9g-cardinfo:latest
docker/BUILDX2.sh 2.0.0 2.0 latest     # multi-arch build, push to ghcr.io and Docker Hub
```

Helper scripts that run the image against the project's `data` directory:

```bash
docker/INITIALIZE_SECRET.sh            # -i
docker/ENCRYPT_TEXT.sh "my-secret"     # -e
docker/GENERATE_TOKEN.sh               # -g
```

They use `ghcr.io/thorsten-l/l9g-cardinfo:latest`; set `IMAGE=l9g-cardinfo:latest`
to use the locally built image. `docker/docker-compose.yaml` starts the service
with `../data` mounted read-only.

`docker/compose.yaml` runs the service without building an image: it mounts
`docker/l9g-cardinfo.jar` and `../data` read-only into
`bellsoft/liberica-openjre-debian:25`. Copy the current jar first:

```bash
mvn clean package && cp target/l9g-cardinfo.jar docker/
docker compose -f docker/compose.yaml up -d
```

## Native Image

```bash
./NATIVE_COMPILE.sh
```

Builds a GraalVM native image (`mvn -Pnative native:compile`); the script
expects GraalVM 25 in `/opt/graalvm/25`.

## Development Conventions

*   **Code Style:** The project follows the standard Java coding conventions.
*   **Dependencies:** Project dependencies are managed using Maven.
*   **Tests:** New functionality is covered by tests in `src/test`; security
    relevant behaviour (authentication, LDAP filter escaping, error responses)
    is tested end-to-end in `CardinfoApiIntegrationTest`.
