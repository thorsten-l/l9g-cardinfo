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
package l9g.cardinfo.token;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Configuration properties for loading bearer tokens from the application
 * configuration (e.g., YAML file). This class maps properties under the
 * {@code bearer-tokens} prefix.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Configuration
@ConfigurationProperties(prefix = "bearer-tokens")
@Data
@Getter
@ToString
public class BearerTokenConfig
{
  /**
   * A map of bearer tokens, where the key is a logical name for the token and
   * the value is the {@link BearerToken} object containing the token details.
   */
  private Map<String,BearerToken> map;
  
  /**
   * Represents a single bearer token and its associated metadata.
   */
  @Data
  @ToString
  @Schema(description = "Represents a single bearer token and its metadata.")
  public static class BearerToken
  {
    @Schema(description = "The actual secret token string.",
            example = "abc-123-def-456")
    @ToString.Exclude
    private String token;

    @Schema(description = "The owner or client associated with the token.",
            example = "ExampleClient")
    private String owner;
    
    @Schema(description = "A description of the token's purpose.",
            example = "Token for the primary frontend application")
    private String description;
    
    private Ldap ldap;
    
    @Schema(description = "Whether the token is currently active.",
            example = "true")
    private boolean enabled = false;

    /**
     * Represents LDAP configuration for the bearer token.
     */
    @Data
    @ToString
    public static class Ldap
    {
      private String baseDn;
      private String scope;
    }
  }
}
