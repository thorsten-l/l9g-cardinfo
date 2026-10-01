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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SoniaAttributeMapperTest
{
  private final SoniaAttributeMapper mapper = new SoniaAttributeMapper();

  @Test
  void mapsEmployee()
  {
    Entry entry = new Entry("uid=jdoe,dc=sonia,dc=de");
    entry.addAttribute("givenName", "John");
    entry.addAttribute("sn", "Doe");
    entry.addAttribute("soniaBirthday", "1970-01-01");
    entry.addAttribute("soniaCustomerNumber", "0012345678");
    entry.addAttribute("soniaChipcardBarcode", "87654321");
    entry.addAttribute("soniaHisPersonId", "654321");
    entry.addAttribute("employeeType", "b");
    entry.addAttribute("soniaIsValidFrom", "2024-01-01");

    CardinfoResponse response = mapper.mapAttributes(entry);

    assertThat(response).isEqualTo(new CardinfoResponse("John", "Doe",
      "1970-01-01", "0012345678", "87654321", "code39", "654321", "b",
      "2024-01-01", null, "OK"));
  }

  @Test
  void studentValidityIsTakenFromValidityCode()
  {
    Entry entry = new Entry("uid=student,dc=sonia,dc=de");
    entry.addAttribute("employeeType", "S");
    entry.addAttribute("soniaIsValidFrom", "1999-01-01");
    entry.addAttribute("soniaStudentValidityCode", "X:01.04.2025:30.09.2026");

    CardinfoResponse response = mapper.mapAttributes(entry);

    assertThat(response.validFrom()).isEqualTo("2025-04-01");
    assertThat(response.validUntil()).isEqualTo("2026-09-30");
  }

  private static Entry student(String validityCode)
  {
    Entry entry = new Entry("uid=student,dc=sonia,dc=de");
    entry.addAttribute("employeeType", "s");
    entry.addAttribute("soniaIsValidFrom", "1999-01-01");
    entry.addAttribute("soniaIsValidUntil", "2099-12-31");
    if(validityCode != null)
    {
      entry.addAttribute("soniaStudentValidityCode", validityCode);
    }
    return entry;
  }

  @Test
  void studentWithoutValidityCodeHasNoValidity()
  {
    CardinfoResponse response = mapper.mapAttributes(student(null));

    assertThat(response.validFrom()).isNull();
    assertThat(response.validUntil()).isNull();
    assertThat(response.status()).isEqualTo("OK");
  }

  @Test
  void studentWithEmptyValidityCodeHasNoValidity()
  {
    for(String code : new String[]{ "", "   " })
    {
      CardinfoResponse response = mapper.mapAttributes(student(code));

      assertThat(response.validFrom()).as("'%s'", code).isNull();
      assertThat(response.validUntil()).as("'%s'", code).isNull();
    }
  }

  @Test
  void studentWithNaValidityCodeHasNoValidity()
  {
    CardinfoResponse response = mapper.mapAttributes(
      student(SoniaAttributeMapper.NO_VALIDITY_CODE));

    assertThat(response.validFrom()).isNull();
    assertThat(response.validUntil()).isNull();
    assertThat(response.status()).isEqualTo("OK");
  }

  @Test
  void otherNaValidityCodesStillFail()
  {
    assertThatThrownBy(() -> mapper.mapAttributes(
      student("01:na:na:na:na:na:na")))
      .isInstanceOf(MapperException.class);
    assertThatThrownBy(() -> mapper.mapAttributes(student("00:na")))
      .isInstanceOf(MapperException.class);
  }

  @Test
  void studentWithMalformedDatesFails()
  {
    Entry entry = new Entry("uid=student,dc=sonia,dc=de");
    entry.addAttribute("employeeType", "s");
    entry.addAttribute("soniaStudentValidityCode", "X:2025-04-01:30.9.2026");

    assertThatThrownBy(() -> mapper.mapAttributes(entry))
      .isInstanceOf(MapperException.class);
  }

}
