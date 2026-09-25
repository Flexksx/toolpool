package io.github.flexksx.toolpool.spring;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@AutoConfiguration(
    beforeName = {
      "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
      "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
      "org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration",
      "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration"
    })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(BearerTokenAuthenticationEntryPoint.class)
@Conditional(ToolpoolAuthConditions.CallerTokenRequired.class)
@EnableWebSecurity
@EnableConfigurationProperties({
  ToolpoolAuthProperties.class,
  McpServerStreamableHttpProperties.class
})
public class ToolpoolSecurityAutoConfiguration {

  static final String METADATA_PATH = "/.well-known/oauth-protected-resource";
  private static final String ANY_SUB_PATH = "/**";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ToolpoolSecurityAutoConfiguration.class);

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @ConditionalOnMissingBean(name = "toolpoolSecurityFilterChain")
  SecurityFilterChain toolpoolSecurityFilterChain(
      HttpSecurity http,
      ToolpoolAuthProperties auth,
      McpServerStreamableHttpProperties mcpProperties,
      ObjectProvider<JwtDecoder> jwtDecoders,
      ObjectProvider<OpaqueTokenIntrospector> introspectors,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String jwtIssuer) {
    String mcpEndpoint = mcpProperties.getMcpEndpoint();
    String metadataPath = METADATA_PATH + mcpEndpoint;
    String authorizationServer = authorizationServerOf(auth, jwtIssuer);
    boolean opaqueTokens = introspectors.getIfAvailable() != null;
    if (!opaqueTokens && jwtDecoders.getIfAvailable() == null) {
      throw new IllegalStateException(
          "toolpool.auth.mode="
              + auth.mode()
              + " needs a way to check caller tokens: set"
              + " spring.security.oauth2.resourceserver.jwt.issuer-uri or configure opaque token"
              + " introspection");
    }

    BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
    entryPoint.setResourceMetadataParameterResolver(
        request -> absoluteUrlOf(request, metadataPath));

    LOGGER
        .atInfo()
        .setMessage("Toolpool accepts only {} caller tokens on {} and publishes {}")
        .addArgument(opaqueTokens ? "introspected" : "JWT")
        .addArgument(mcpEndpoint)
        .addArgument(metadataPath)
        .log();

    return http.securityMatcher(mcpEndpoint + ANY_SUB_PATH, METADATA_PATH + ANY_SUB_PATH)
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(METADATA_PATH + ANY_SUB_PATH)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
        .oauth2ResourceServer(
            resourceServer ->
                configureResourceServer(
                    resourceServer, entryPoint, opaqueTokens, authorizationServer))
        .build();
  }

  private static void configureResourceServer(
      OAuth2ResourceServerConfigurer<HttpSecurity> resourceServer,
      BearerTokenAuthenticationEntryPoint entryPoint,
      boolean opaqueTokens,
      @Nullable String authorizationServer) {
    resourceServer.authenticationEntryPoint(entryPoint);
    if (opaqueTokens) {
      resourceServer.opaqueToken(Customizer.withDefaults());
    } else {
      resourceServer.jwt(Customizer.withDefaults());
    }
    if (authorizationServer != null) {
      resourceServer.protectedResourceMetadata(
          metadata ->
              metadata.protectedResourceMetadataCustomizer(
                  builder -> builder.authorizationServer(authorizationServer)));
    }
  }

  private static @Nullable String authorizationServerOf(
      ToolpoolAuthProperties auth, String jwtIssuer) {
    String configured = auth.authorizationServer();
    if (configured != null && !configured.isBlank()) {
      return configured;
    }
    return jwtIssuer.isBlank() ? null : jwtIssuer;
  }

  private static String absoluteUrlOf(HttpServletRequest request, String path) {
    return UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
        .replacePath(request.getContextPath() + path)
        .replaceQuery(null)
        .fragment(null)
        .build()
        .toUriString();
  }
}
