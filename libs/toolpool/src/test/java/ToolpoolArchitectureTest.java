import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jspecify.annotations.NullMarked;

@AnalyzeClasses(
    packages = ToolpoolArchitectureTest.ROOT,
    importOptions = ImportOption.DoNotIncludeTests.class)
public class ToolpoolArchitectureTest {

  static final String ROOT = "io.github.flexksx";
  static final String DOMAIN = "io.github.flexksx.domain..";
  static final String TOOL_DOMAIN = "io.github.flexksx.domain.tool..";
  static final String HTTP_DOMAIN = "io.github.flexksx.domain.http..";
  static final String SCHEMA_DOMAIN = "io.github.flexksx.domain.schema..";
  static final String APPLICATION = "io.github.flexksx.application..";
  static final String OPENAPI_ADAPTER = "io.github.flexksx.adapter.openapi..";
  static final String HTTP_ADAPTER = "io.github.flexksx.adapter.http..";
  static final String MCP_ADAPTER = "io.github.flexksx.adapter.mcp..";
  static final String COMPOSITION_ROOT = "io.github.flexksx";
  static final String JDK = "java..";
  static final String NULLNESS = "org.jspecify..";

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
  static final ArchRule onlyTheOpenApiAdapterKnowsSwagger =
      noClasses()
          .that()
          .resideOutsideOfPackage(OPENAPI_ADAPTER)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("io.swagger..");

  @ArchTest
  static final ArchRule onlyTheMcpAdapterKnowsTheMcpSdk =
      noClasses()
          .that()
          .resideOutsideOfPackages(MCP_ADAPTER, COMPOSITION_ROOT)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("io.modelcontextprotocol..", "org.springframework.ai..");

  @ArchTest
  static final ArchRule onlyTheHttpAdapterKnowsSpringWeb =
      noClasses()
          .that()
          .resideOutsideOfPackages(HTTP_ADAPTER, COMPOSITION_ROOT)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework.web..", "org.springframework.http..");

  @ArchTest
  static final ArchRule theAdaptersDoNotDependOnEachOther =
      slices().matching("io.github.flexksx.adapter.(*)..").should().notDependOnEachOther();

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
