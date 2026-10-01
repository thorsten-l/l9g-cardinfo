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
import l9g.cardinfo.handler.LdapHandler;
import org.springframework.aot.hint.MemberCategory;

/**
 * GraalVM native image hints for classes that are used via reflection.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Configuration
@ImportRuntimeHints(RuntimeHintConfig.RuntimeHint.class)
public class RuntimeHintConfig
{

  static class RuntimeHint implements RuntimeHintsRegistrar
  {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader)
    {
      hints.reflection().registerType(
        l9g.cardinfo.mapper.SoniaAttributeMapper.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

      hints.reflection().registerType(
        l9g.cardinfo.v2.mapper.SoniaAttributeMapper.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

      hints.reflection().registerType(LdapHandler.class,
        MemberCategory.ACCESS_DECLARED_FIELDS);
    }

  }

}
