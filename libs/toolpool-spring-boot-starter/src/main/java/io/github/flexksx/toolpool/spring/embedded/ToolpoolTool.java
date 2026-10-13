package io.github.flexksx.toolpool.spring.embedded;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method, or every handler method of a controller, as an MCP tool. Toolpool
 * reads this mark only when {@code toolpool.embedded.enabled=true}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ToolpoolTool {}
