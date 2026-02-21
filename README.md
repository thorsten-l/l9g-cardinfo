# L9G Card Info

## Project Overview

This project is a Spring Boot application that provides a service for retrieving card information. It's designed to be used in a microservices architecture where a central service is needed to manage and distribute unique identifiers.

The application exposes a RESTful API for retrieving card information and is secured using Bearer Token authentication. It leverages an LDAP directory to retrieve user data.

**Key Technologies:**

*   **Framework:** Spring Boot
*   **Language:** Java 21
*   **Security:** Spring Security with Bearer Token authentication
*   **Directory Service:** LDAP (via UnboundID LDAP SDK)
*   **API Documentation:** SpringDoc OpenAPI

## Building and Running

### Prerequisites

*   Java 21
*   Apache Maven

### Build

To build the project, run the following command from the project root directory:

```bash
mvn clean package
```

This will generate a JAR file in the `target` directory.

### Run

To run the application, use the following command:

```bash
java -jar target/l9g-cardinfo.jar
```

The application will start on the port configured in `src/main/resources/application.yaml`.

### Configuration

The application is configured using the `config.yaml` and `secret.bin` files. These files should be placed in the `data` directory.

**`config.yaml`:**

This file contains the main configuration for the application, including the LDAP connection details.

**`secret.bin`:**

This file contains the encrypted secrets used by the application, such as the LDAP password and the bearer tokens.

To generate a new bearer token, you can use the `-g` command-line option:

```bash
java -jar target/l9g-cardinfo.jar -g
```

To encrypt a value for use in the configuration, you can use the `-e` command-line option:

```bash
java -jar target/l9g-cardinfo.jar -e "my-secret-value"
```

## Development Conventions

*   **Code Style:** The project follows the standard Java coding conventions.
*   **Dependencies:** Project dependencies are managed using Maven.
*   **API Documentation:** The RESTful API is documented using SpringDoc OpenAPI. You can access the OpenAPI UI at `/swagger-ui.html` and the OpenAPI specification at `/v3/api-docs`.
