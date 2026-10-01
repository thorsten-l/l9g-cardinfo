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
package l9g.cardinfo.config;

import java.util.LinkedHashMap;
import java.util.Map;
import l9g.cardinfo.token.BearerTokenConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticBearerTokenFilterTest
{
  private static BearerTokenConfig config(String... nameTokenEnabled)
  {
    Map<String, BearerTokenConfig.BearerToken> map = new LinkedHashMap<>();
    for(int i = 0; i < nameTokenEnabled.length; i += 3)
    {
      BearerTokenConfig.BearerToken token = new BearerTokenConfig.BearerToken();
      token.setToken(nameTokenEnabled[i + 1]);
      token.setEnabled(Boolean.parseBoolean(nameTokenEnabled[i + 2]));
      map.put(nameTokenEnabled[i], token);
    }
    BearerTokenConfig config = new BearerTokenConfig();
    config.setMap(map);
    return config;
  }

  @Test
  void undecryptedTokenFailsStartup()
  {
    assertThatThrownBy(() -> new SecurityConfig.StaticBearerTokenFilter(
      config("t1", "{AES256}abcdef", "true")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("t1");
  }

  @Test
  void duplicateTokenValuesFailStartup()
  {
    assertThatThrownBy(() -> new SecurityConfig.StaticBearerTokenFilter(
      config("t1", "same", "true", "t2", "same", "true")))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void disabledAndEmptyTokensAreIgnored()
  {
    assertThatCode(() -> new SecurityConfig.StaticBearerTokenFilter(
      config("t1", "same", "true", "t2", "same", "false", "t3", "", "true")))
      .doesNotThrowAnyException();
    assertThatCode(() -> new SecurityConfig.StaticBearerTokenFilter(
      new BearerTokenConfig()))
      .doesNotThrowAnyException();
  }

}
