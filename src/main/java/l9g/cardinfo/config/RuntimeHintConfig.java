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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import l9g.cardinfo.mapper.SoniaAttributeMapper;
import l9g.cardinfo.crypto.EncryptedValue;
import l9g.cardinfo.handler.LdapHandler;
import org.springframework.aot.hint.MemberCategory;

@Configuration
@ImportRuntimeHints(RuntimeHintConfig.RuntimeHint.class)
public class RuntimeHintConfig
{

  static class RuntimeHint implements RuntimeHintsRegistrar
  {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader)
    {
      hints.reflection().registerType(SoniaAttributeMapper.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      
      hints.reflection().registerType(EncryptedValue.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.INVOKE_PUBLIC_METHODS);
      
      hints.reflection().registerType(LdapHandler.class,
        MemberCategory.DECLARED_FIELDS);
    }

  }

}
