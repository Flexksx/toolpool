package io.github.flexksx.samplerestapiclient.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "User", description = "User management REST endpoints.")
public class UserController {

  public static final String MISSING_USER_ID = "missing";

  private static final String VERBOSE_NAME_SUFFIX = " (verbose)";
  private static final String DEFAULT_USER_NAME = "Ada Lovelace";
  private static final String DEFAULT_LIST_LIMIT = "3";
  private static final long STORED_USER_COUNT = 42L;

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

  @GetMapping
  @Operation(
      operationId = "listUsers",
      summary = "List users",
      description = "Answers the stored users, narrowed by a name fragment")
  public List<UserRestResponse> list(
      @RequestParam(required = false) String nameContains,
      @RequestParam(defaultValue = DEFAULT_LIST_LIMIT) int limit) {
    if (nameContains != null && !DEFAULT_USER_NAME.contains(nameContains)) {
      return List.of();
    }
    return IntStream.rangeClosed(1, limit)
        .mapToObj(index -> new UserRestResponse("u" + index, DEFAULT_USER_NAME))
        .toList();
  }

  @GetMapping("/count")
  @Operation(
      operationId = "countUsers",
      summary = "Count users",
      description = "Answers how many users are stored and reads no parameters")
  public long count() {
    return STORED_USER_COUNT;
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "deleteUser",
      summary = "Delete a user",
      description = "Removes the user that carries the given id")
  public void delete(@PathVariable String id) {
    if (MISSING_USER_ID.equals(id)) {
      throw UserRestException.notFound(id);
    }
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
