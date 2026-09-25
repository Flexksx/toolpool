package io.github.flexksx.toolpool.spring;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class ToolpoolAuthConditions {

  private ToolpoolAuthConditions() {}

  static ToolpoolAuthMode modeOf(Environment environment) {
    return Binder.get(environment)
        .bind(ToolpoolAuthProperties.MODE_PROPERTY, ToolpoolAuthMode.class)
        .orElse(ToolpoolAuthMode.NONE);
  }

  static final class CallerTokenRequired extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(
        ConditionContext context, AnnotatedTypeMetadata metadata) {
      ToolpoolAuthMode mode = modeOf(context.getEnvironment());
      return new ConditionOutcome(
          mode.requiresCallerToken(), ToolpoolAuthProperties.MODE_PROPERTY + " is " + mode);
    }
  }

  static final class TokenExchangeSelected extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(
        ConditionContext context, AnnotatedTypeMetadata metadata) {
      ToolpoolAuthMode mode = modeOf(context.getEnvironment());
      return new ConditionOutcome(
          mode == ToolpoolAuthMode.TOKEN_EXCHANGE,
          ToolpoolAuthProperties.MODE_PROPERTY + " is " + mode);
    }
  }
}
