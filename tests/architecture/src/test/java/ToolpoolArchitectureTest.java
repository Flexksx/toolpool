import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jspecify.annotations.NullMarked;

/**
 * States the dependency graph of the whole library.
 *
 * <p>Every library module is on this project's test classpath, so one file reads them all. The
 * modules arrive as jars, so the import must not carry {@link ImportOption.DoNotIncludeJars}: with
 * that option every rule reads zero classes and fails with "failed to check any classes", which
 * reads like a boundary violation and is not one.
 */
@AnalyzeClasses(
    packages = ToolpoolArchitectureTest.ROOT,
    importOptions = ImportOption.DoNotIncludeTests.class)
public class ToolpoolArchitectureTest {

  static final String ROOT = "io.github.flexksx.toolpool";
  static final String DOMAIN = "io.github.flexksx.toolpool.domain..";
  static final String TOOL_DOMAIN = "io.github.flexksx.toolpool.domain.tool..";
  static final String HTTP_DOMAIN = "io.github.flexksx.toolpool.domain.http..";
  static final String SCHEMA_DOMAIN = "io.github.flexksx.toolpool.domain.schema..";
  static final String APPLICATION = "io.github.flexksx.toolpool.application..";
  static final String OPENAPI_ADAPTER = "io.github.flexksx.toolpool.adapter.openapi..";
  static final String MCP_ADAPTER = "io.github.flexksx.toolpool.adapter.mcp..";
  static final String SPRING_MODULE = "io.github.flexksx.toolpool.spring..";

  static final String JDK = "java..";
  static final String NULLNESS = "org.jspecify..";
  static final String SWAGGER = "io.swagger..";
  static final String MCP_SDK = "io.modelcontextprotocol..";
  static final String LOGGING = "org.slf4j..";
  static final String SPRING = "org.springframework..";

  @ArchTest
  static final ArchRule theDomainDependsOnNothingButItselfAndTheJdk =
      classes()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(DOMAIN, JDK, NULLNESS);

  @ArchTest
  static final ArchRule theSharedDomainValuesDoNotKnowTheToolDomain =
      noClasses()
          .that()
          .resideInAnyPackage(HTTP_DOMAIN, SCHEMA_DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(TOOL_DOMAIN);

  @ArchTest
  static final ArchRule theApplicationDependsOnNothingButTheDomainAndTheJdk =
      classes()
          .that()
          .resideInAPackage(APPLICATION)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(APPLICATION, DOMAIN, JDK, NULLNESS);

  @ArchTest
  static final ArchRule theOpenApiAdapterKnowsTheOnionAndSwaggerOnly =
      classes()
          .that()
          .resideInAPackage(OPENAPI_ADAPTER)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              OPENAPI_ADAPTER, APPLICATION, DOMAIN, JDK, NULLNESS, SWAGGER, LOGGING);

  @ArchTest
  static final ArchRule theMcpAdapterKnowsTheOnionAndTheMcpSdkOnly =
      classes()
          .that()
          .resideInAPackage(MCP_ADAPTER)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(MCP_ADAPTER, APPLICATION, DOMAIN, JDK, NULLNESS, MCP_SDK);

  @ArchTest
  static final ArchRule onlyTheSpringModuleKnowsSpring =
      noClasses()
          .that()
          .resideOutsideOfPackage(SPRING_MODULE)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(SPRING);

  @ArchTest
  static final ArchRule onlyTheOpenApiAdapterKnowsSwagger =
      noClasses()
          .that()
          .resideOutsideOfPackage(OPENAPI_ADAPTER)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(SWAGGER);

  @ArchTest
  static final ArchRule onlyTheMcpAdapterAndTheSpringModuleKnowTheMcpSdk =
      noClasses()
          .that()
          .resideOutsideOfPackages(MCP_ADAPTER, SPRING_MODULE)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(MCP_SDK);

  @ArchTest
  static final ArchRule everyPackageDeclaresThatItIsNullMarked =
      classes().should(resideInANullMarkedPackage());

  private static ArchCondition<JavaClass> resideInANullMarkedPackage() {
    return new ArchCondition<>("reside in a @NullMarked package") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        boolean marked = javaClass.getPackage().isAnnotatedWith(NullMarked.class);
        events.add(
            new SimpleConditionEvent(
                javaClass,
                marked,
                "package "
                    + javaClass.getPackageName()
                    + " has no @NullMarked declaration, so "
                    + javaClass.getSimpleName()
                    + " has no nullness contract"));
      }
    };
  }
}
