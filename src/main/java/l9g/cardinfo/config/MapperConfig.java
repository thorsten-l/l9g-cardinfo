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

import l9g.cardinfo.mapper.LdapEntryToCardinfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MapperConfig
{

  @Bean
  public LdapEntryToCardinfoResponse ldapEntryToCardinfoResponse(
    @Value("${cardinfo.attributes-mapper-class}") String className)
    throws ReflectiveOperationException
  {
    log.info("Using {} to map an LDAP entry to a cardinfo response.", className);
    Class<?> clazz = Class.forName(className);
    return (LdapEntryToCardinfoResponse)clazz.getDeclaredConstructor().newInstance();
  }

}
