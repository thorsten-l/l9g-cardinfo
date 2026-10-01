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
package l9g.cardinfo.config;

import l9g.cardinfo.token.BearerTokenConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import de.l9g.crypto.core.CryptoHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 *
 * Spring Security configuration for the application.
 * <p>
 * This class sets up the security filter chain, which handles authentication
 * and authorization for the application's endpoints. It disables stateful
 * security features like CSRF and sessions, and configures a custom filter for
 * stateless Bearer Token authentication.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig
{
  private final BearerTokenConfig bearerTokenConfig;


  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint(
    @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
  )
  {
    return (request, response, authException) -> {
      resolver.resolveException(request, response, null, authException);
    };
  }

  /**
   * Configures the main security filter chain for the application.
   * <p>
   * This bean defines the security rules:
   * <ul>
   *     <li>Disables CSRF, HTTP Basic, form login and logout; no HTTP session
   *         is created (stateless).</li>
   *     <li>Sets up a custom entry point to delegate auth exceptions.</li>
   *     <li>Adds the {@link StaticBearerTokenFilter} to process Bearer tokens.</li>
   *     <li>Requires authentication for {@code /api/v1/cardinfo} (any HTTP
   *         method).</li>
   *     <li>Permits all other requests.</li>
   * </ul>
   *
   * @param http The {@link HttpSecurity} to configure.
   * @param authenticationEntryPoint The entry point for handling auth exceptions.
   * @return The configured {@link SecurityFilterChain}.
   * @throws Exception if an error occurs during configuration.
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint)
    throws Exception
  {
    http
      .csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .httpBasic(hb -> hb.disable())
      .formLogin(fl -> fl.disable())
      .logout(lo -> lo.disable());

    http.exceptionHandling(eh -> eh
      .authenticationEntryPoint(authenticationEntryPoint)
    );

    http.addFilterBefore(new StaticBearerTokenFilter(bearerTokenConfig),
      AbstractPreAuthenticatedProcessingFilter.class);

    http.authorizeHttpRequests(auth -> auth
      // all HTTP methods (HEAD is served by the GET handler)
      .requestMatchers("/api/v1/cardinfo/**").authenticated()
      .anyRequest().permitAll()
    );

    return http.build();
  }

  /**
   * A filter that authenticates requests based on a static Bearer Token.
   * <p>
   * This filter extracts a token from the {@code Authorization: Bearer} header,
   * looks it up in a pre-configured map of known tokens, and if found and
   * valid, creates an {@link Authentication} object and places it in the
   * {@link SecurityContextHolder}.
   */
  static class StaticBearerTokenFilter extends OncePerRequestFilter
  {
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();

    /**
     * SHA-256 hash of the token value -> token name.
     * Only enabled tokens are indexed and no clear text token is kept.
     */
    private final Map<String, String> tokenIndex;

    private final Map<String, BearerTokenConfig.BearerToken> tokensByName;

    StaticBearerTokenFilter(BearerTokenConfig config)
    {
      this.tokensByName = (config.getMap() != null)
        ? Map.copyOf(config.getMap()) : Map.of();

      Map<String, String> index = new HashMap<>();
      tokensByName.forEach((name, bearerToken) ->
      {
        // {AES256} values are already decrypted by l9g crypto-spring
        String token = bearerToken.getToken();
        if( ! bearerToken.isEnabled())
        {
          log.info("bearer token '{}' is disabled", name);
        }
        else if(token == null || token.isBlank())
        {
          log.warn("bearer token '{}' has no value and is ignored", name);
        }
        else if(token.startsWith(CryptoHandler.AES256_PREFIX))
        {
          throw new IllegalStateException("bearer token '" + name
            + "' has not been decrypted");
        }
        else if(index.putIfAbsent(sha256(token), name) != null)
        {
          throw new IllegalStateException("bearer token '" + name
            + "' uses the same value as another token");
        }
      });
      this.tokenIndex = Map.copyOf(index);
    }

    @Override
    protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException
    {
      String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
      if(auth == null ||  ! auth.regionMatches(true, 0, BEARER_PREFIX, 0,
        BEARER_PREFIX.length()))
      {
        chain.doFilter(request, response);
        return;
      }

      String token = auth.substring(BEARER_PREFIX.length()).trim();
      String name = token.isEmpty() ? null : tokenIndex.get(sha256(token));
      BearerTokenConfig.BearerToken bt = (name != null)
        ? tokensByName.get(name) : null;

      if(bt == null ||  ! bt.isEnabled())
      {
        chain.doFilter(request, response);
        return;
      }

      SecurityContext context = securityContextHolderStrategy.createEmptyContext();
      context.setAuthentication(new StaticBearerAuthenticationToken(
        name, bt.getOwner(), AuthorityUtils.NO_AUTHORITIES));
      securityContextHolderStrategy.setContext(context);

      try
      {
        chain.doFilter(request, response);
      }
      finally
      {
        securityContextHolderStrategy.clearContext();
      }
    }

    static String sha256(String value)
    {
      try
      {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
      }
      catch(NoSuchAlgorithmException e)
      {
        throw new IllegalStateException(e);
      }
    }

  }

  /**
   * A custom {@link Authentication} token representing a successfully
   * authenticated client via a static Bearer token.
   * <p>
   * It holds the principal (the token's name) and details (the token's owner).
   */
  static class StaticBearerAuthenticationToken extends AbstractAuthenticationToken
  {
    private final String principalName;

    private final String owner;

    StaticBearerAuthenticationToken(String principalName, String owner,
      Collection<? extends GrantedAuthority> authorities)
    {
      super(authorities);
      this.principalName = principalName;
      this.owner = owner;
      setAuthenticated(true);
    }

    @Override
    public Object getCredentials()
    {
      return ""; // kein Geheimnis mehr speichern
    }

    @Override
    public Object getPrincipal()
    {
      return principalName;
    }

    @Override
    public Object getDetails()
    {
      return owner;
    }

  }

}
