/*
 * Copyright 2025 Thorsten Ludewig (t.ludewig@gmail.com).
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
package l9g.cardinfo.handler;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.ssl.HostNameSSLSocketVerifier;
import com.unboundid.util.ssl.JVMDefaultTrustManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLSocketFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * Component for handling all interactions with an LDAP directory.
 * <p>
 * This class encapsulates the logic for connecting to an LDAP server,
 * searching for entries, and retrieving user data. Configuration for the LDAP
 * connection and queries is injected from application properties. It is
 * - * designed to be used by services that need to query user information from
 * - * LDAP.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Component
@RequiredArgsConstructor
public class LdapHandler
{
  private final static Logger LOGGER =
    LoggerFactory.getLogger(LdapHandler.class);

  @Value("${ldap.host.name}")
  private String ldapHostname;

  @Value("${ldap.host.port}")
  private int ldapPort;

  @Value("${ldap.host.ssl}")
  private boolean ldapSslEnabled;

  /**
   * Accept any server certificate. Only for testing - this disables protection
   * against man-in-the-middle attacks.
   */
  @Value("${ldap.host.trust-all-certificates:false}")
  private boolean ldapTrustAllCertificates;

  @Value("${ldap.bind.dn}")
  private String ldapBindDn;

  // {AES256} values are decrypted by l9g crypto-spring
  @Value("${ldap.bind.password}")
  private String ldapBindPassword;

  @Value("${ldap.filter}")
  private String ldapFilter;

  @Value("${ldap.user.attributes}")
  private String[] ldapUserAttributeNames;

  private LDAPConnection getConnection()
    throws Exception
  {
    LOGGER.debug("host={}", ldapHostname);
    LOGGER.debug("port={}", ldapPort);
    LOGGER.debug("ssl={}", ldapSslEnabled);
    LOGGER.debug("bind dn={}", ldapBindDn);

    LDAPConnection ldapConnection;

    LDAPConnectionOptions options = new LDAPConnectionOptions();
    if(ldapSslEnabled)
    {
      if( ! ldapTrustAllCertificates)
      {
        options.setSSLSocketVerifier(new HostNameSSLSocketVerifier(false));
      }
      ldapConnection = new LDAPConnection(createSSLSocketFactory(), options,
        ldapHostname, ldapPort,
        ldapBindDn,
        ldapBindPassword);
    }
    else
    {
      ldapConnection = new LDAPConnection(options,
        ldapHostname, ldapPort,
        ldapBindDn,
        ldapBindPassword);
    }
    ldapConnection.setConnectionName(ldapHostname);
    return ldapConnection;
  }

  private SSLSocketFactory createSSLSocketFactory()
    throws
    GeneralSecurityException
  {
    SSLUtil sslUtil;
    if(ldapTrustAllCertificates)
    {
      LOGGER.warn("LDAP server certificate validation is DISABLED "
        + "(ldap.host.trust-all-certificates=true)");
      sslUtil = new SSLUtil(new TrustAllTrustManager());
    }
    else
    {
      sslUtil = new SSLUtil(JVMDefaultTrustManager.getInstance());
    }
    return sslUtil.createSSLSocketFactory();
  }

  /**
   * Searches the entry of a user.
   * <p>
   * The userId is escaped before it is inserted into {@code ldap.filter}.
   *
   * @param ldapBaseDn the search base DN (of the bearer token)
   * @param ldapScope the search scope: {@code sub} (default), {@code one} or
   * {@code base}
   * @param userId the user id
   * @return the entry, or {@code null} if no entry was found
   * @throws Exception if the search fails or the userId is not unique
   */
  public Entry getEntry( String ldapBaseDn, String ldapScope, String userId)
    throws Exception
  {
    // escape the user supplied value to prevent LDAP filter injection
    String filter = String.format(ldapFilter, Filter.encodeValue(userId));
    LOGGER.debug("LDAP filter: {}", filter);

    SearchScope scope = SearchScope.SUB;
    if("ONE".equalsIgnoreCase(ldapScope))
    {
      scope = SearchScope.ONE;
    }
    else if("BASE".equalsIgnoreCase(ldapScope))
    {
      scope = SearchScope.BASE;
    }

    try(LDAPConnection connection = getConnection())
    {
      SearchRequest searchRequest = new SearchRequest(
        ldapBaseDn, scope, filter, ldapUserAttributeNames
      );

      SearchResult searchResult = connection.search(searchRequest);

      switch(searchResult.getEntryCount())
      {
        case 0 ->
        {
          return null;
        }
        case 1 ->
        {
          return searchResult.getSearchEntries().get(0);
        }
        default ->
          throw new LDAPException(ResultCode.LOCAL_ERROR,
            "UserId is not unique!");
      }
    }
    catch(Exception e)
    {
      LOGGER.error("Error during LDAP search for userId {}: {}",
        userId, e.getMessage());
      throw e;
    }
  }
}
