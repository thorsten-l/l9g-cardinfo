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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import l9g.cardinfo.v2.controller.CardinfoResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps the SONIA attributes like {@link l9g.cardinfo.mapper.SoniaAttributeMapper}
 * (including its handling of the student validity code) and adds the
 * Deutschlandticket information from {@code eduPersonEntitlement}.
 * <p>
 * A ticket entitlement has the form
 * {@code urn:mace:ride-ticketing.de:entitlement:dticket:timeframe:yyyyMMdd-yyyyMMdd}.
 * {@code validTicket} is {@code true} if today (Europe/Berlin) lies within the
 * timeframe of at least one ticket entitlement (both days inclusive). A missing
 * attribute, an expired or future timeframe, or a malformed value results in
 * {@code false}.
 * <p>
 * Only the ticket entitlements are returned (comma separated), other
 * entitlements are not passed to the client.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
public class SoniaAttributeMapper implements LdapEntryToCardinfoResponse
{
  /**
   * LDAP attribute containing the ticket entitlements.
   */
  public static final String ENTITLEMENT_ATTRIBUTE = "eduPersonEntitlement";

  /**
   * Prefix of a Deutschlandticket entitlement, followed by
   * {@code yyyyMMdd-yyyyMMdd}.
   */
  public static final String DTICKET_ENTITLEMENT_PREFIX =
    "urn:mace:ride-ticketing.de:entitlement:dticket:timeframe:";

  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  private static final DateTimeFormatter TIMEFRAME_DATE =
    DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

  private final l9g.cardinfo.mapper.SoniaAttributeMapper baseMapper =
    new l9g.cardinfo.mapper.SoniaAttributeMapper();

  private final Clock clock;

  /**
   * Creates a mapper using the current date in Europe/Berlin.
   */
  public SoniaAttributeMapper()
  {
    this(Clock.system(ZONE));
  }

  SoniaAttributeMapper(Clock clock)
  {
    this.clock = clock;
  }

  @Override
  public CardinfoResponse mapAttributes(Entry entry)
  {
    l9g.cardinfo.controller.CardinfoResponse base = baseMapper.mapAttributes(entry);

    String[] entitlements = entry.getAttributeValues(ENTITLEMENT_ATTRIBUTE);
    List<String> ticketEntitlements = (entitlements == null) ? List.of()
      : Arrays.stream(entitlements).filter(SoniaAttributeMapper::isTicketEntitlement).toList();

    LocalDate today = LocalDate.now(clock);
    boolean validTicket = ticketEntitlements.stream()
      .anyMatch(entitlement -> isValidOn(entitlement, today));

    log.debug("ticket entitlements = {}, valid on {} = {}",
      ticketEntitlements, today, validTicket);

    return new CardinfoResponse(base.firstName(), base.lastName(),
      base.birthday(), base.customerNumber(), base.barcodeNumber(),
      base.barcodeFormat(), base.campusManagementId(), base.employeeType(),
      base.validFrom(), base.validUntil(), validTicket,
      ticketEntitlements.isEmpty() ? null : String.join(",", ticketEntitlements),
      base.status());
  }

  static boolean isTicketEntitlement(String entitlement)
  {
    return entitlement != null && entitlement.regionMatches(true, 0,
      DTICKET_ENTITLEMENT_PREFIX, 0, DTICKET_ENTITLEMENT_PREFIX.length());
  }

  /**
   * @return {@code true} if {@code day} lies within the timeframe of the ticket
   * entitlement (inclusive), {@code false} otherwise or if malformed
   */
  static boolean isValidOn(String entitlement, LocalDate day)
  {
    String[] timeframe = entitlement.substring(
      DTICKET_ENTITLEMENT_PREFIX.length()).split("-", -1);

    if(timeframe.length != 2)
    {
      log.warn("malformed ticket entitlement: {}", entitlement);
      return false;
    }

    try
    {
      LocalDate from = LocalDate.parse(timeframe[0], TIMEFRAME_DATE);
      LocalDate until = LocalDate.parse(timeframe[1], TIMEFRAME_DATE);
      return !day.isBefore(from) && !day.isAfter(until);
    }
    catch(DateTimeParseException e)
    {
      log.warn("malformed ticket entitlement: {}", entitlement);
      return false;
    }
  }

}
