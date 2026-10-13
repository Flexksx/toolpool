import io.github.flexksx.toolpool.spring.embedded.ToolpoolTool;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({
  EmbeddedTestApplication.UserController.class,
  EmbeddedTestApplication.OrderController.class
})
class EmbeddedTestApplication {

  @Bean
  SecurityFilterChain openSecurityFilterChain(HttpSecurity http) {
    return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable)
        .build();
  }

  @RestController
  static class UserController {

    @ToolpoolTool
    @GetMapping("/users/{id:[a-z0-9]+}")
    Map<String, String> getUser(@PathVariable String id) {
      return Map.of("id", id);
    }

    @DeleteMapping("/users/{id}")
    void deleteUser(@PathVariable String id) {}
  }

  @ToolpoolTool
  @RestController
  static class OrderController {

    @GetMapping("/orders")
    List<String> listOrders() {
      return List.of("o1");
    }
  }
}
