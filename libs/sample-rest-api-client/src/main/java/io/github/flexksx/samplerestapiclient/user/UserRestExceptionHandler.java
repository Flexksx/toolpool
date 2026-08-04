package io.github.flexksx.samplerestapiclient.user;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserController.class)
public class UserRestExceptionHandler {

  @ExceptionHandler(UserRestException.class)
  @ApiResponse(responseCode = "404", description = "User not found")
  public ProblemDetail handleNotFound(UserRestException ex) {
    return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
  }
}
