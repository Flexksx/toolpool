import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.flexksx.toolpool.adapter.mcp",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ToolpoolMcpArchitectureTest {

  static final String METATOOL = "io.github.flexksx.toolpool.adapter.mcp.metatool..";
  static final String TRANSLATION = "io.github.flexksx.toolpool.adapter.mcp.translation..";

  @ArchTest
  static final ArchRule theMetatoolPackageDoesNotReachIntoTranslation =
      noClasses()
          .that()
          .resideInAPackage(METATOOL)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(TRANSLATION);

  @ArchTest
  static final ArchRule theTranslationPackageDoesNotReachIntoMetatool =
      noClasses()
          .that()
          .resideInAPackage(TRANSLATION)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(METATOOL);
}
