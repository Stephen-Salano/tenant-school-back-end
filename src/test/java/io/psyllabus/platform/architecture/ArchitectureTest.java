package io.psyllabus.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.psyllabus.platform");

    @Test
    void controllers_should_not_call_repositories() {
        noClasses().that().haveNameMatching(".*Controller")
                .should().dependOnClassesThat().haveNameMatching(".*Repository")
                .check(importedClasses);
    }

    @Test
    void services_should_not_call_controllers() {
        noClasses().that().haveNameMatching(".*Service.*")
                .should().dependOnClassesThat().haveNameMatching(".*Controller")
                .check(importedClasses);
    }
}
