package io.github.flexksx.samplerestapiclient.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserRestException extends ResponseStatusException {

  private UserRestException(HttpStatus status, String reason) {
    super(status, reason);
  }

  public static UserRestException notFound(String userId) {
    return new UserRestException(HttpStatus.NOT_FOUND, "User " + userId + " not found");
  }
}
