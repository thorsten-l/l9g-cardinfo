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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import l9g.cardinfo.mapper.MapperException;
import l9g.cardinfo.v2.controller.CardinfoResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SoniaAttributeMapperTest
{
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  private static final String TICKET =
    "urn:mace:ride-ticketing.de:entitlement:dticket:timeframe:";

  private static final String WINTER = TICKET + "20260901-20270228";

  private static SoniaAttributeMapper mapperOn(String isoDate)
  {
    return new SoniaAttributeMapper(Clock.fixed(
      LocalDate.parse(isoDate).atStartOfDay(ZONE).plusHours(12).toInstant(),
      ZONE));
  }

  private static Entry entry(String... entitlements)
  {
    Entry entry = new Entry("uid=jdoe,dc=sonia,dc=de");
    entry.addAttribute("givenName", "John");
    entry.addAttribute("sn", "Doe");
    entry.addAttribute("employeeType", "b");
    entry.addAttribute("soniaIsValidFrom", "2024-01-01");
    if(entitlements.length > 0)
    {
      entry.addAttribute("eduPersonEntitlement", entitlements);
    }
    return entry;
  }

  @Test
  void ticketWithinTimeframeIsValid()
  {
    CardinfoResponse response = mapperOn("2026-11-15").mapAttributes(entry(WINTER));

    assertThat(response.validTicket()).isTrue();
    assertThat(response.eduPersonEntitlement()).isEqualTo(WINTER);
  }

  @Test
  void firstAndLastDayAreIncluded()
  {
    assertThat(mapperOn("2026-09-01").mapAttributes(entry(WINTER)).validTicket()).isTrue();
    assertThat(mapperOn("2027-02-28").mapAttributes(entry(WINTER)).validTicket()).isTrue();
  }

  @Test
  void ticketOutsideTimeframeIsInvalid()
  {
    assertThat(mapperOn("2026-08-31").mapAttributes(entry(WINTER)).validTicket()).isFalse();
    assertThat(mapperOn("2027-03-01").mapAttributes(entry(WINTER)).validTicket()).isFalse();
  }

  @Test
  void dayIsDeterminedInEuropeBerlin()
  {
    // 2027-02-28 23:30 UTC is already 2027-03-01 in Berlin
    SoniaAttributeMapper mapper = new SoniaAttributeMapper(Clock.fixed(
      Instant.parse("2027-02-28T23:30:00Z"), ZONE));

    assertThat(mapper.mapAttributes(entry(WINTER)).validTicket()).isFalse();
  }

  @Test
  void missingEntitlementIsInvalid()
  {
    CardinfoResponse response = mapperOn("2026-11-15").mapAttributes(entry());

    assertThat(response.validTicket()).isFalse();
    assertThat(response.eduPersonEntitlement()).isNull();
  }

  @Test
  void oneValidTicketOfSeveralIsEnough()
  {
    String summer = TICKET + "20260301-20260831";

    CardinfoResponse response = mapperOn("2026-11-15")
      .mapAttributes(entry(summer, WINTER));

    assertThat(response.validTicket()).isTrue();
    assertThat(response.eduPersonEntitlement()).isEqualTo(summer + "," + WINTER);
  }

  @Test
  void otherEntitlementsAreNotReturned()
  {
    CardinfoResponse response = mapperOn("2026-11-15").mapAttributes(
      entry("urn:mace:dir:entitlement:common-lib-terms", WINTER));

    assertThat(response.eduPersonEntitlement()).isEqualTo(WINTER);

    CardinfoResponse noTicket = mapperOn("2026-11-15").mapAttributes(
      entry("urn:mace:dir:entitlement:common-lib-terms"));

    assertThat(noTicket.validTicket()).isFalse();
    assertThat(noTicket.eduPersonEntitlement()).isNull();
  }

  @Test
  void malformedTimeframesAreInvalid()
  {
    SoniaAttributeMapper mapper = mapperOn("2026-11-15");

    for(String timeframe : new String[]
    {
      "", "20260901", "20260901-", "-20270228", "20260901-20270228-20270331",
      "20260901-20270230", "2026-09-01-2027-02-28", "20260901_20270228",
      "abcdefgh-20270228"
    })
    {
      assertThat(mapper.mapAttributes(entry(TICKET + timeframe)).validTicket())
        .as(timeframe).isFalse();
    }
  }

  @Test
  void baseAttributesAreMappedLikeV1()
  {
    CardinfoResponse response = mapperOn("2026-11-15").mapAttributes(entry(WINTER));

    assertThat(response).isEqualTo(new CardinfoResponse("John", "Doe", null,
      null, null, "code39", null, "b", "2024-01-01", null, true, WINTER, "OK"));
  }

  @Test
  void mapperErrorsOfV1ArePropagated()
  {
    Entry entry = entry(WINTER);
    entry.setAttribute("employeeType", "s");
    entry.setAttribute("soniaStudentValidityCode", "X:2025-04-01:30.9.2026");

    assertThatThrownBy(() -> mapperOn("2026-11-15").mapAttributes(entry))
      .isInstanceOf(MapperException.class);
  }

}
