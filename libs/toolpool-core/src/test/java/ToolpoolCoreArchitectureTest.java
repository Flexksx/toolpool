import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.flexksx.toolpool",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ToolpoolCoreArchitectureTest {

  static final String HTTP_DOMAIN = "io.github.flexksx.toolpool.domain.http..";
  static final String SCHEMA_DOMAIN = "io.github.flexksx.toolpool.domain.schema..";

  @ArchTest
  static final ArchRule theHttpDomainDoesNotKnowTheJsonSchemaDomain =
      noClasses()
          .that()
          .resideInAPackage(HTTP_DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(SCHEMA_DOMAIN);

  @ArchTest
  static final ArchRule theJsonSchemaDomainDoesNotKnowTheHttpDomain =
      noClasses()
          .that()
          .resideInAPackage(SCHEMA_DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(HTTP_DOMAIN);
}
