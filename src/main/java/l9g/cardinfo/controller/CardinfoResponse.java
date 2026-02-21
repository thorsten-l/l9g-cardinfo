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
package l9g.cardinfo.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Represents the response containing card information for a user.
 *
 * @param firstName The first name of the cardholder.
 * @param lastName The last name of the cardholder.
 * @param customerNumber The customer number associated with the card.
 * @param libraryNumber The library number associated with the card.
 * @param employeeType The type of employee (e.g., "Student", "Staff").
 * @param validFrom The date from which the card is valid (e.g., "2024-01-01").
 * @param validUntil The date until which the card is valid (e.g., "2028-12-31").
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

  @Schema(description = "The date from which the card will be valid. (format: yyyy-mm-dd)", example = "2024-01-01")
  String validFrom,

  @Schema(description = "The date until which the card is valid. A 'null' value means the account does not expire. Remember 'null' attributes will not be transferred. (format: yyyy-mm-dd)", example = "2028-12-31")
  String validUntil,

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
    this(null, null, null, null, null, null, null, null, null, null, status);
  }

}
