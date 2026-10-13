package io.github.flexksx.toolpool.spring.embedded;

import io.github.flexksx.toolpool.domain.http.HttpMethod;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

final class ToolpoolToolScanner {

  private static final Pattern REGEX_PATH_VARIABLE = Pattern.compile("\\{([^}:]+):[^}]*}");
  private static final String PLAIN_PATH_VARIABLE = "{$1}";

  private ToolpoolToolScanner() {}

  static Set<HttpTarget> targetsOf(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
    return handlerMethods.entrySet().stream()
        .filter(entry -> isToolpoolTool(entry.getValue()))
        .flatMap(entry -> targetsOfMapping(entry.getKey(), entry.getValue()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static boolean isToolpoolTool(HandlerMethod handlerMethod) {
    return handlerMethod.hasMethodAnnotation(ToolpoolTool.class)
        || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), ToolpoolTool.class);
  }

  private static Stream<HttpTarget> targetsOfMapping(
      RequestMappingInfo mapping, HandlerMethod handlerMethod) {
    Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
    if (methods.isEmpty()) {
      throw new IllegalStateException(
          handlerMethod
              + " is a @ToolpoolTool but maps no HTTP method, so Toolpool cannot tell which"
              + " operation to expose");
    }
    return methods.stream()
        .flatMap(
            method ->
                mapping.getPatternValues().stream()
                    .map(
                        pattern ->
                            new HttpTarget(
                                HttpMethod.valueOf(method.name()), plainPathOf(pattern))));
  }

  private static String plainPathOf(String pattern) {
    return REGEX_PATH_VARIABLE.matcher(pattern).replaceAll(PLAIN_PATH_VARIABLE);
  }
}
