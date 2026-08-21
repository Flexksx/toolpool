package io.github.flexksx.samplerestapiclient.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "User", description = "User management REST endpoints.")
public class UserController {

  public static final String MISSING_USER_ID = "missing";

  private static final String VERBOSE_NAME_SUFFIX = " (verbose)";
  private static final String DEFAULT_USER_NAME = "Ada Lovelace";

  @GetMapping("/{id}")
  @Operation(
      operationId = "getUser",
      summary = "Get a user by ID",
      description = "Retrieves the user's data based on the user id")
  public UserRestResponse get(
      @PathVariable String id,
      @RequestParam(defaultValue = "false") boolean verbose,
      @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
    if (MISSING_USER_ID.equals(id)) {
      throw UserRestException.notFound(id);
    }
    return new UserRestResponse(
        id, verbose ? DEFAULT_USER_NAME + VERBOSE_NAME_SUFFIX : DEFAULT_USER_NAME);
  }

  @PostMapping
  @Operation(
      operationId = "createUser",
      summary = "Create a user",
      description = "Creates a new user from the given request payload")
  public UserRestResponse create(@RequestBody CreateUserRestRequest request) {
    return new UserRestResponse(UUID.randomUUID().toString(), request.name());
  }

  @PutMapping("/{id}")
  @Operation(
      operationId = "updateUser",
      summary = "Update a user",
      description = "Updates an existing user's data")
  public UserRestResponse update(
      @PathVariable String id, @RequestBody UpdateUserRestRequest request) {
    return new UserRestResponse(id, request.name());
  }
}
