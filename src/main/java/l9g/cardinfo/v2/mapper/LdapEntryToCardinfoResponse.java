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
package l9g.cardinfo.v2.mapper;

import com.unboundid.ldap.sdk.Entry;
import l9g.cardinfo.v2.controller.CardinfoResponse;

/**
 * Maps an LDAP entry to a {@link CardinfoResponse} (API v2).
 * <p>
 * The implementation is selected with
 * {@code cardinfo.attributes-mapper-v2-class} and needs a public no-arg
 * constructor.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public interface LdapEntryToCardinfoResponse
{
  /**
   * Maps the attributes of an LDAP entry.
   *
   * @param entry the LDAP entry of the user
   * @return the card information including the Deutschlandticket information
   */
  public CardinfoResponse mapAttributes( Entry entry );
}
