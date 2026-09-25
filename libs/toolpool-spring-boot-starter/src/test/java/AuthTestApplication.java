import java.util.Map;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(AuthTestApplication.UpstreamUserController.class)
class AuthTestApplication {

  static final String ABSENT = "absent";

  @RestController
  static class UpstreamUserController {

    @GetMapping("/users/{id}")
    Map<String, String> getUser(
        @PathVariable String id,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
      return Map.of("id", id, "authorization", authorization == null ? ABSENT : authorization);
    }
  }
}
