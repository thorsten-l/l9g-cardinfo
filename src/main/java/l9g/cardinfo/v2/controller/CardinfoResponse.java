/*
 * Copyright 2025 Thorsten Ludewig (t.ludewig@gmail.com).
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
package l9g.cardinfo.v2.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Represents the response containing card information for a user (API v2).
 * <p>
 * Contains all fields of {@link l9g.cardinfo.controller.CardinfoResponse}
 * plus the Deutschlandticket information.
 *
 * @param firstName The first name of the cardholder.
 * @param lastName The last name of the cardholder.
 * @param birthday The birthday of the cardholder (yyyy-mm-dd).
 * @param customerNumber The customer number associated with this account.
 * @param barcodeNumber The barcode number of the card.
 * @param barcodeFormat The barcode format of the card (always "code39").
 * @param campusManagementId The campus management id (e.g. HIS person id).
 * @param employeeType The employee type from LDAP (e.g. "s" for students).
 * @param validFrom The date from which the card is valid (yyyy-mm-dd);
 * {@code null} if no validity information is available.
 * @param validUntil The date until which the card is valid (yyyy-mm-dd);
 * {@code null} if the account does not expire or, for students, no validity
 * information is available.
 * @param validTicket {@code true} if today lies within the timeframe of at
 * least one Deutschlandticket entitlement; always present in the JSON, also in
 * error responses.
 * @param eduPersonEntitlement The Deutschlandticket entitlements of the
 * cardholder (comma separated); {@code null} if there are none.
 * @param status A status message, typically "OK" on success or an error
 * description.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A record containing the card information for a user. Attributes containing a 'null' value will not be transferred.")
public record CardinfoResponse(
  @Schema(description = "The first name of the cardholder.", example = "John")
  String firstName,

  @Schema(description = "The last name of the cardholder.", example = "Doe")
  String lastName,

  @Schema(description = "The birthday of the cardholder. (format: yyyy-mm-dd)", example = "1970-01-01")
  String birthday,

  @Schema(description = "The customer number associated with this account.", example = "0012345678")
  String customerNumber,

  @Schema(description = "The barcode number associated with the card.", example = "87654321")
  String barcodeNumber,
  
  @Schema(description = "The barcode format associated with the card.", example = "code39")
  String barcodeFormat,
  
  @Schema(description = "The campus management id (e.g. HIS person id) associated with the this account.", example = "654321")
  String campusManagementId,
  
  @Schema(description = "The type of employee.", example = "s = Student")
  String employeeType,

  @Schema(description = "The date from which the card will be valid. Not transferred if no validity information is available. (format: yyyy-mm-dd)", example = "2024-01-01")
  String validFrom,

  @Schema(description = "The date until which the card is valid. A 'null' value means the account does not expire or, for students, that no validity information is available. Remember 'null' attributes will not be transferred. (format: yyyy-mm-dd)", example = "2028-12-31")
  String validUntil,

  @Schema(description = "True if today (Europe/Berlin) lies within the timeframe of at least one Deutschlandticket entitlement, otherwise false.", example = "true")
  boolean validTicket,

  @Schema(description = "The Deutschlandticket entitlements of the cardholder (comma separated). Other entitlements are not transferred, not transferred at all if there are none.", example = "urn:mace:ride-ticketing.de:entitlement:dticket:timeframe:20260901-20270228")
  String eduPersonEntitlement,

  @Schema(description = "A status message, typically 'OK' on success.", example = "OK")
  String status)
  {

  /**
   * A convenience constructor to create a response with only a status message.
   * This is often used for error responses.
   *
   * @param status The status message.
   */
  public CardinfoResponse(String status)
  {
    this(null, null, null, null, null, null, null, null, null, null, false, null,
      status);
  }

}
