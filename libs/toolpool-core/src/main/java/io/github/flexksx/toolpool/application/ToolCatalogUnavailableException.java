package io.github.flexksx.toolpool.application;

public class ToolCatalogUnavailableException extends Exception {

  public ToolCatalogUnavailableException(String source) {
    super("Could not build a tool catalog from " + source);
  }

  public ToolCatalogUnavailableException(String source, Throwable cause) {
    super("Could not build a tool catalog from " + source, cause);
  }
}
