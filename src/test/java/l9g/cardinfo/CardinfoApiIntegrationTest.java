/*
 * Copyright 2026 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l9g.cardinfo;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import de.l9g.crypto.core.CryptoHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the cardinfo API against an in-memory LDAP server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardinfoApiIntegrationTest
{
  private static final String API = "/api/v1/cardinfo";

  private static final String BIND_PASSWORD = "bind-secret";

  private static final String ENCRYPTED_TOKEN_VALUE = "encrypted-token-value";

  private static final String UNAUTHORIZED =
    "ERROR: Unauthorized, a valid Bearer token is required.";

  private static final String INTERNAL_ERROR = "ERROR: Internal server error.";

  private static InMemoryDirectoryServer ldapServer;

  @Autowired
  private MockMvc mockMvc;

  static
  {
    try
    {
      startLdapServer();
      writeConfigYaml();
    }
    catch(Exception e)
    {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Writes data/config.yaml into the test working directory (target/test-work)
   * like in production, so the {AES256} values are decrypted by l9g
   * crypto-spring while the environment is prepared.
   */
  private static void writeConfigYaml()
    throws Exception
  {
    CryptoHandler cryptoHandler = CryptoHandler.getInstance();
    Path config = Path.of("data", "config.yaml");
    Files.createDirectories(config.getParent());
    Files.writeString(config, String.join("\n",
      "ldap:",
      "  host:",
      "    port: " + ldapServer.getListenPort(),
      "  bind:",
      "    password: \"" + cryptoHandler.encrypt(BIND_PASSWORD) + "\"",
      "bearer-tokens:",
      "  map:",
      "    encrypted-token:",
      "      token: \"" + cryptoHandler.encrypt(ENCRYPTED_TOKEN_VALUE) + "\"",
      "      owner: encrypted",
      "      enabled: true",
      "      ldap:",
      "        base-dn: dc=sonia,dc=de",
      "        scope: sub",
      ""));
  }

  private static void startLdapServer()
    throws Exception
  {
    InMemoryDirectoryServerConfig config =
      new InMemoryDirectoryServerConfig("dc=sonia,dc=de");
    config.addAdditionalBindCredentials("cn=Directory Manager", BIND_PASSWORD);
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("ldap", 0));
    config.setSchema(null); // allow the sonia* attributes without schema
    config.setAuthenticationRequiredOperationTypes(
      com.unboundid.ldap.sdk.OperationType.SEARCH);

    ldapServer = new InMemoryDirectoryServer(config);
    ldapServer.add("dn: dc=sonia,dc=de", "objectClass: top",
      "objectClass: domain", "dc: sonia");
    ldapServer.add("dn: ou=people,dc=sonia,dc=de", "objectClass: top",
      "objectClass: organizationalUnit", "ou: people");
    ldapServer.add("dn: ou=other,dc=sonia,dc=de", "objectClass: top",
      "objectClass: organizationalUnit", "ou: other");

    person("ou=people", "jdoe", "1001", "givenName: John", "sn: Doe",
      "soniaBirthday: 1970-01-01", "soniaCustomerNumber: 0012345678",
      "soniaChipcardBarcode: 87654321", "soniaHisPersonId: 654321",
      "employeeType: b", "soniaIsValidFrom: 2024-01-01",
      "soniaIsValidUntil: 2028-12-31");
    person("ou=people", "student", "1002", "givenName: Erika",
      "sn: Musterfrau", "employeeType: s",
      "soniaStudentValidityCode: X:01.04.2025:30.09.2026");
    person("ou=people", "broken", "1003", "employeeType: s");
    person("ou=people", "dup1", "3001", "sn: Dup");
    person("ou=people", "dup2", "3001", "sn: Dup");
    person("ou=other", "other", "2001", "givenName: Otto", "sn: Other");

    ldapServer.startListening();
  }

  private static void person(String ou, String uid, String externalUid,
    String... attributes)
    throws Exception
  {
    String[] ldif = new String[attributes.length + 4];
    ldif[0] = "dn: uid=" + uid + "," + ou + ",dc=sonia,dc=de";
    ldif[1] = "objectClass: inetOrgPerson";
    ldif[2] = "uid: " + uid;
    ldif[3] = "soniaExternalUid: " + externalUid;
    System.arraycopy(attributes, 0, ldif, 4, attributes.length);
    ldapServer.add(ldif);
  }

  @AfterAll
  static void stopLdapServer()
  {
    if(ldapServer != null)
    {
      ldapServer.shutDown(true);
    }
  }

  private static String bearer(String token)
  {
    return "Bearer " + token;
  }

  // --- authentication ------------------------------------------------------

  @Test
  void missingTokenIsUnauthorized()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1001"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.status").value(UNAUTHORIZED))
      .andExpect(jsonPath("$.firstName").doesNotExist());
  }

  @Test
  void unknownTokenIsUnauthorized()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, bearer("wrong")))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.status").value(UNAUTHORIZED));
  }

  @Test
  void disabledTokenIsUnauthorized()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, bearer("disabled-token-value")))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void otherAuthorizationSchemesAreUnauthorized()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, "Basic cGVvcGxlLXRva2VuLXZhbHVl"))
      .andExpect(status().isUnauthorized());
    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, "Bearer "))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void encryptedTokenFromConfigurationIsAccepted()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, bearer(ENCRYPTED_TOKEN_VALUE)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.lastName").value("Doe"));
  }

  @Test
  void encryptedFormOfTokenIsNotAccepted()
    throws Exception
  {
    String encrypted = CryptoHandler.getInstance().encrypt(ENCRYPTED_TOKEN_VALUE);

    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, bearer(encrypted)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void bearerSchemeIsCaseInsensitive()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, "bearer people-token-value"))
      .andExpect(status().isOk());
  }

  @Test
  void otherHttpMethodsRequireAuthentication()
    throws Exception
  {
    mockMvc.perform(head(API).param("userId", "1001"))
      .andExpect(status().isUnauthorized());
    mockMvc.perform(post(API).param("userId", "1001"))
      .andExpect(status().isUnauthorized());
    mockMvc.perform(get(API + "/").param("userId", "1001"))
      .andExpect(status().isUnauthorized());
  }

  // --- card info -----------------------------------------------------------

  @Test
  void returnsCardinfoOfEmployee()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OK"))
      .andExpect(jsonPath("$.firstName").value("John"))
      .andExpect(jsonPath("$.lastName").value("Doe"))
      .andExpect(jsonPath("$.birthday").value("1970-01-01"))
      .andExpect(jsonPath("$.customerNumber").value("0012345678"))
      .andExpect(jsonPath("$.barcodeNumber").value("87654321"))
      .andExpect(jsonPath("$.barcodeFormat").value("code39"))
      .andExpect(jsonPath("$.campusManagementId").value("654321"))
      .andExpect(jsonPath("$.employeeType").value("b"))
      .andExpect(jsonPath("$.validFrom").value("2024-01-01"))
      .andExpect(jsonPath("$.validUntil").value("2028-12-31"));
  }

  @Test
  void returnsValidityOfStudent()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1002")
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.validFrom").value("2025-04-01"))
      .andExpect(jsonPath("$.validUntil").value("2026-09-30"))
      // null values are not transferred
      .andExpect(jsonPath("$.birthday").doesNotExist());
  }

  @Test
  void unknownUserIsNotFound()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "9999")
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.status").value("ERROR: User not found."));
  }

  @Test
  void missingUserIdIsBadRequest()
    throws Exception
  {
    mockMvc.perform(get(API)
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.status").value(
        "ERROR: Bad request, please provide a single userId parameter."));
  }

  @Test
  void tokenIsRestrictedToItsBaseDn()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "2001")
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isNotFound());
    mockMvc.perform(get(API).param("userId", "2001")
      .header(HttpHeaders.AUTHORIZATION, bearer("other-token-value")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.firstName").value("Otto"));
    mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, bearer("other-token-value")))
      .andExpect(status().isNotFound());
  }

  // --- LDAP injection ------------------------------------------------------

  @Test
  void wildcardUserIdDoesNotMatchAnyEntry()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "*")
      .header(HttpHeaders.AUTHORIZATION, bearer("other-token-value")))
      .andExpect(status().isNotFound());
    mockMvc.perform(get(API).param("userId", "100*")
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isNotFound());
  }

  @Test
  void filterInjectionIsNotPossible()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "x)(|(soniaExternalUid=2001")
      .header(HttpHeaders.AUTHORIZATION, bearer("other-token-value")))
      .andExpect(status().isNotFound());
    mockMvc.perform(get(API).param("userId", "2001)(objectClass=*")
      .header(HttpHeaders.AUTHORIZATION, bearer("other-token-value")))
      .andExpect(status().isNotFound());
  }

  // --- error responses do not leak internals -------------------------------

  @Test
  void ambiguousUserIdIsInternalErrorWithoutDetails()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "3001")
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.status").value(INTERNAL_ERROR));
  }

  @Test
  void mapperErrorIsInternalErrorWithoutDetails()
    throws Exception
  {
    mockMvc.perform(get(API).param("userId", "1003")
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.status").value(INTERNAL_ERROR));
  }

  // --- HTTP security -------------------------------------------------------

  @Test
  void responsesAreStatelessAndCarrySecurityHeaders()
    throws Exception
  {
    MvcResult result = mockMvc.perform(get(API).param("userId", "1001")
      .header(HttpHeaders.AUTHORIZATION, bearer("people-token-value")))
      .andExpect(status().isOk())
      .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
      .andExpect(header().string("X-Content-Type-Options", "nosniff"))
      .andExpect(header().string("X-Frame-Options", "DENY"))
      .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
        containsString("no-store")))
      .andReturn();

    assertThat(result.getRequest().getSession(false)).isNull();
  }

  @Test
  void openApiDocumentationIsDisabledByDefault()
    throws Exception
  {
    mockMvc.perform(get("/v3/api-docs"))
      .andExpect(status().isNotFound());
  }

  @Test
  void buildInfoIsAvailable()
    throws Exception
  {
    mockMvc.perform(get("/api/v1/buildinfo"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.artifact").value("l9g-cardinfo"))
      .andExpect(jsonPath("$.['java.version']").value(not("21")));
  }

}
