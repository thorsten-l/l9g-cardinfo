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

import com.unboundid.ldap.sdk.Entry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import l9g.cardinfo.handler.LdapHandler;
import l9g.cardinfo.mapper.LdapEntryToCardinfoResponse;
import l9g.cardinfo.token.AuthenticatedBearerToken;

/**
 *
 * REST controller for retrieving card information.
 * <p>
 * This controller provides endpoints for accessing user card data. Access is
 * secured and requires a valid Bearer Token for authentication.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@RestController
@RequestMapping(path = "/api/v1/cardinfo",
                produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class CardinfoController
{
  private final LdapHandler ldapHandler;

  private final LdapEntryToCardinfoResponse mapper;

  @Operation(summary = "Retrieve card info",
             description = "Retrieve card info. Authentication is required via a Bearer Token in the Authorization header.",
             security =
             @SecurityRequirement(name = "bearerAuth"),
             responses =
             {
               @ApiResponse(responseCode = "200", description = "Cardinfo successfully retrived.",
                            content =
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                     schema =
                                     @Schema(oneOf =
                                     {
                                       CardinfoResponse.class
                                   }))),
               @ApiResponse(responseCode = "400", description = "Bad request, e.g., no parameter or multiple parameters provided, or invalid token",
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
                                       value = "{\"status\": \"ERROR: UserId not found.\"}"
                                     ))),
               @ApiResponse(responseCode = "500", description = "Internal server error",
                            content =
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                     schema =
                                     @Schema(implementation = CardinfoResponse.class),
                                     examples =
                                     @ExampleObject(
                                       name = "InternalServerError",
                                       value = "{\"status\": \"ERROR: Internal server error.\"}"
                                     )))
             })
  @GetMapping
  /**
   * Retrieves card information for a given user ID.
   * <p>
   * This endpoint fetches card details based on the provided user ID.
   * Authentication is required, and the authenticated client is identified
   * by the Bearer token.
   *
   * @param userId The ID of the user to retrieve card information for.
   * @param token The authenticated bearer token of the client making the request.
   *
   * @return A {@link ResponseEntity} containing the {@link CardinfoResponse} with
   * the card information.
   */
  public ResponseEntity<CardinfoResponse> serveCardinfo(
    @Parameter(description = "The ID of the user.", required = true)
    @RequestParam(name = "userId", required = true) String userId,
    @Parameter(hidden = true) @AuthenticatedBearerToken BearerToken token
  )
  {
    log.info("owner={}", token.getOwner());
    log.debug("token={}", token);
    log.info("userId={}", userId);

    Entry entry = null;
    try
    {
      entry = ldapHandler.getEntry(
        token.getLdap().getBaseDn(), token.getLdap().getScope(), userId);
    }
    catch(Throwable e)
    {
      log.error("Error retrieving LDAP entry for userId {}: {}",
        userId, e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new CardinfoResponse("ERROR: Internal server error. " + e.getMessage()));
    }
    
    if(entry != null)
    {
      return ResponseEntity.ok(mapper.mapAttributes(entry));
    }
    else
    {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new CardinfoResponse("ERROR: User not found."));
    }
  }

}
