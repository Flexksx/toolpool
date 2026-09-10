package io.github.flexksx.toolpool.domain.tool;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public record ToolDocumentation(
    @Nullable String summary, @Nullable String description, List<String> tags) {

  private static final String TEXT_SEPARATOR = "\n";
  private static final String WORD_SEPARATOR = " ";

  public ToolDocumentation {
    tags = List.copyOf(tags);
  }

  public String text() {
    return joinPresent(TEXT_SEPARATOR, summary, description);
  }

  public String searchableText() {
    return joinPresent(WORD_SEPARATOR, summary, description, String.join(WORD_SEPARATOR, tags));
  }

  private static String joinPresent(String separator, @Nullable String... texts) {
    return Stream.of(texts)
        .filter(Objects::nonNull)
        .filter(text -> !text.isBlank())
        .collect(Collectors.joining(separator));
  }
}
