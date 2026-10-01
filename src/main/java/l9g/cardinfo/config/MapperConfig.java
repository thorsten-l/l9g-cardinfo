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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the configured LDAP attribute mappers for API v1 and v2.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Configuration
@Slf4j
public class MapperConfig
{

  /**
   * @param className {@code cardinfo.attributes-mapper-class}
   * @return the v1 mapper
   * @throws ReflectiveOperationException if the class cannot be instantiated
   */
  @Bean
  public l9g.cardinfo.mapper.LdapEntryToCardinfoResponse ldapEntryToCardinfoResponse(
    @Value("${cardinfo.attributes-mapper-class}") String className)
    throws ReflectiveOperationException
  {
    log.info("Using {} to map an LDAP entry to a cardinfo response.", className);
    Class<?> clazz = Class.forName(className);
    return (l9g.cardinfo.mapper.LdapEntryToCardinfoResponse)clazz.getDeclaredConstructor().newInstance();
  }

  /**
   * @param className {@code cardinfo.attributes-mapper-v2-class}
   * @return the v2 mapper
   * @throws ReflectiveOperationException if the class cannot be instantiated
   */
  @Bean
  public l9g.cardinfo.v2.mapper.LdapEntryToCardinfoResponse ldapEntryToCardinfoResponseV2(
    @Value("${cardinfo.attributes-mapper-v2-class}") String className)
    throws ReflectiveOperationException
  {
    log.info("Using {} to map an LDAP entry to a cardinfo v2 response.", className);
    Class<?> clazz = Class.forName(className);
    return (l9g.cardinfo.v2.mapper.LdapEntryToCardinfoResponse)clazz.getDeclaredConstructor().newInstance();
  }

}
