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
package l9g.cardinfo.mapper;

import com.unboundid.ldap.sdk.Entry;
import l9g.cardinfo.controller.CardinfoResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps the SONIA LDAP attributes to a {@link CardinfoResponse}.
 * <p>
 * {@code validFrom} / {@code validUntil} are taken from
 * {@code soniaIsValidFrom} / {@code soniaIsValidUntil}. For students
 * ({@code employeeType=s}) they are taken from {@code soniaStudentValidityCode}
 * ({@code x:DD.MM.YYYY:DD.MM.YYYY...}) instead; if the code is missing, empty or
 * {@value #NO_VALIDITY_CODE}, both are {@code null}.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
public class SoniaAttributeMapper implements LdapEntryToCardinfoResponse
{
  /**
   * Student validity code without validity dates.
   */
  public static final String NO_VALIDITY_CODE = "00:na:na:na:na:na:na";

  /**
   * {@inheritDoc}
   *
   * @throws MapperException if the student validity code is malformed
   */
  @Override
  public CardinfoResponse mapAttributes(Entry entry)
  {
    log.debug("mapAttributes for {}", entry);

    String firstName = entry.getAttributeValue("givenName");
    String lastName = entry.getAttributeValue("sn");
    String birthday = entry.getAttributeValue("soniaBirthday");
    String customerNumber = entry.getAttributeValue("soniaCustomerNumber");
    String barcodeNumber = entry.getAttributeValue("soniaChipcardBarcode");
    String campusManagementId = entry.getAttributeValue("soniaHisPersonId");
    String employeeType = entry.getAttributeValue("employeeType");
    String validFrom = entry.getAttributeValue("soniaIsValidFrom");
    String validUntil = entry.getAttributeValue("soniaIsValidUntil");
    
    if ("s".equalsIgnoreCase(employeeType))
    {
      String validityCode = entry.getAttributeValue("soniaStudentValidityCode");
      log.debug("Student validity code = {}", validityCode);
      if ( validityCode == null || validityCode.isBlank()
        || NO_VALIDITY_CODE.equalsIgnoreCase(validityCode.trim()))
      {
        // no validity dates available for this student
        validFrom = null;
        validUntil = null;
      }
      else if ( validityCode.split(":").length > 2)
      {
        String[] tokens = validityCode.split(":");
        
        log.debug( "tokens[1] = {}, tokens[2] = {}", tokens[1], tokens[2]);
        
        if ( tokens[1].length() == 10 && tokens[2].length() == 10 )
        {
          validFrom = tokens[1].substring(6, 10) + "-" 
            + tokens[1].substring(3, 5) + "-" 
            + tokens[1].substring(0, 2);
          validUntil = tokens[2].substring(6, 10) + "-" 
            + tokens[2].substring(3, 5) + "-" 
            + tokens[2].substring(0, 2);
        }
        else
        {
          throw new MapperException("malformed valid from/until tokens");
        }
        
        log.debug( "valid from = {}, until = {}", validFrom, validUntil);
      }
      else
      {
        throw new MapperException("malformed student validity code");
      }
    }

    return new CardinfoResponse(firstName, lastName, birthday, customerNumber,
      barcodeNumber, "code39", campusManagementId, employeeType, validFrom, 
      validUntil, "OK");
  }

}
