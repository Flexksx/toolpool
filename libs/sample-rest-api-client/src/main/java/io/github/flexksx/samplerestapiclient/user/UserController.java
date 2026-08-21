package io.github.flexksx.samplerestapiclient.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.instancio.Instancio;
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

  @GetMapping("/{id}")
  @Operation(
      operationId = "getUser",
      summary = "Get a user by ID",
      description = "Retrieves the user's data based on the user id")
  public UserRestResponse get(
      @PathVariable String id,
      @RequestParam(defaultValue = "false") boolean verbose,
      @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
    if (id.isBlank()) {
      throw UserRestException.notFound(id);
    }
    return fakeUserResponse();
  }

  @PostMapping
  @Operation(
      operationId = "createUser",
      summary = "Create a user",
      description = "Creates a new user from the given request payload")
  public UserRestResponse create(@RequestBody CreateUserRestRequest request) {
    return fakeUserResponse();
  }

  @PutMapping("/{id}")
  @Operation(
      operationId = "updateUser",
      summary = "Update a user",
      description = "Updates an existing user's data")
  public UserRestResponse update(
      @PathVariable String id, @RequestBody UpdateUserRestRequest request) {
    return fakeUserResponse();
  }

  private static UserRestResponse fakeUserResponse() {
    return Instancio.create(UserRestResponse.class);
  }
}
