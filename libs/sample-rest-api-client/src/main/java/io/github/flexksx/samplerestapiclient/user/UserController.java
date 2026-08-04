package io.github.flexksx.samplerestapiclient.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.instancio.Instancio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "User", description = "User management REST endpoints.")
public class UserController {
  @GetMapping("/")
  @Operation(
      operationId = "getUser",
      summary = "Get a user by ID",
      description = "Retrieves the user's data based on the user id")
  public UserRestResponse get(String id) {
    if (id == null || id.isBlank()) {
      throw UserRestException.notFound(id);
    }
    return fakeUserResponse();
  }

  @PostMapping("/")
  @Operation(
      operationId = "createUser",
      summary = "Create a user",
      description = "Creates a new user from the given request payload")
  public UserRestResponse create(CreateUserRestRequest request) {
    return fakeUserResponse();
  }

  @PutMapping("/{id}")
  @Operation(
      operationId = "updateUser",
      summary = "Update a user",
      description = "Updates an existing user's data")
  public UserRestResponse update(UpdateUserRestRequest request) {
    return fakeUserResponse();
  }

  private static UserRestResponse fakeUserResponse() {
    return Instancio.create(UserRestResponse.class);
  }
}
