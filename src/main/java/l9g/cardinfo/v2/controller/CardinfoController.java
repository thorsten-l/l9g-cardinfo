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
package l9g.cardinfo.v2.controller;

import com.unboundid.ldap.sdk.Entry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import l9g.cardinfo.handler.LdapHandler;
import l9g.cardinfo.token.AuthenticatedBearerToken;
import l9g.cardinfo.token.BearerTokenConfig.BearerToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import l9g.cardinfo.v2.mapper.LdapEntryToCardinfoResponse;

/**
 * REST controller for retrieving card information, version 2.
 * <p>
 * Same as {@link CardinfoController}, the response additionally contains the
 * Deutschlandticket information ({@code validTicket},
 * {@code eduPersonEntitlement}).
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
// explicit bean name, v1 uses the same simple class name
@RestController("cardinfoControllerV2")
@RequestMapping(path = "/api/v2/cardinfo",
                produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class CardinfoController
{
  private final LdapHandler ldapHandler;

  private final LdapEntryToCardinfoResponse mapper;

  /**
   * Retrieves card information including the Deutschlandticket information for
   * a given user ID.
   * <p>
   * The search is restricted to the LDAP base DN and scope of the
   * authenticated Bearer token.
   *
   * @param userId The ID of the user (soniaExternalUid).
   * @param token The authenticated bearer token of the client making the request.
   *
   * @return A {@link ResponseEntity} containing the {@link CardinfoResponse}
   * (HTTP 200), or a status-only response (HTTP 404, 500).
   */
  @Operation(summary = "Retrieve card info (v2)",
             description = "Retrieve card info including the Deutschlandticket information. Authentication is required via a Bearer Token in the Authorization header.",
             security =
             @SecurityRequirement(name = "bearerAuth"),
             responses =
             {
               @ApiResponse(responseCode = "200", description = "Cardinfo successfully retrived.",
                            content =
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                     schema =
                                     @Schema(implementation = CardinfoResponse.class))),
               @ApiResponse(responseCode = "400", description = "Bad request, e.g., no userId parameter provided",
                            content =
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                     schema =
                                     @Schema(implementation = CardinfoResponse.class),
                                     examples =
                                     @ExampleObject(
                                       name = "BadRequest",
                                       value = "{\"status\": \"ERROR: Bad request, please provide a single userId parameter.\"}"
                                     ))),
               @ApiResponse(responseCode = "401", description = "Unauthorized, if no or invalid Bearer token is provided",
                            content =
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                     schema =
                                     @Schema(implementation = CardinfoResponse.class),
                                     examples =
                                     @ExampleObject(
                                       name = "Unauthorized",
                                       value = "{\"status\": \"ERROR: Unauthorized, a valid Bearer token is required.\"}"
                                     ))),
               @ApiResponse(responseCode = "404", description = "UserId not found",
                            content =
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                     schema =
                                     @Schema(implementation = CardinfoResponse.class),
                                     examples =
                                     @ExampleObject(
                                       name = "NotFound",
                                       value = "{\"validTicket\": false, \"status\": \"ERROR: User not found.\"}"
                                     ))),
               @ApiResponse(responseCode = "500", description = "Internal server error",
                            content =
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                     schema =
                                     @Schema(implementation = CardinfoResponse.class),
                                     examples =
                                     @ExampleObject(
                                       name = "InternalServerError",
                                       value = "{\"validTicket\": false, \"status\": \"ERROR: Internal server error.\"}"
                                     )))
             })
  @GetMapping
  public ResponseEntity<CardinfoResponse> serveCardinfo(
    @Parameter(description = "The ID of the user.", required = true)
    @RequestParam(name = "userId", required = true) String userId,
    @Parameter(hidden = true) @AuthenticatedBearerToken BearerToken token
  )
  {
    log.info("owner={}", token.getOwner());
    log.info("userId={}", userId);

    try
    {
      Entry entry = ldapHandler.getEntry(
        token.getLdap().getBaseDn(), token.getLdap().getScope(), userId);

      if(entry == null)
      {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new CardinfoResponse("ERROR: User not found."));
      }

      return ResponseEntity.ok(mapper.mapAttributes(entry));
    }
    catch(Exception e)
    {
      // details are logged only, never returned to the client
      log.error("Error retrieving cardinfo for userId {}", userId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new CardinfoResponse("ERROR: Internal server error."));
    }
  }

}
