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
