package com.arovia.api;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "com.arovia.api",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class PlatformArchitectureTest {

    @ArchTest
    static final ArchRule identity_must_not_depend_on_notifications_internals =
            noClasses()
                    .that()
                    .resideInAPackage("com.arovia.api.identity..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.arovia.api.notifications.application..",
                            "com.arovia.api.notifications.domain..",
                            "com.arovia.api.notifications.infrastructure.."
                    );

    @ArchTest
    static final ArchRule notifications_must_not_depend_on_identity =
            noClasses()
                    .that()
                    .resideInAPackage("com.arovia.api.notifications..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.arovia.api.identity..");

    @ArchTest
    static final ArchRule security_must_not_depend_on_identity_or_notifications =
            noClasses()
                    .that()
                    .resideInAPackage("com.arovia.api.security..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.arovia.api.identity..",
                            "com.arovia.api.notifications.."
                    );

    @ArchTest
    static final ArchRule notifications_api_must_not_depend_on_implementation =
            noClasses()
                    .that()
                    .resideInAPackage("com.arovia.api.notifications.api..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.arovia.api.notifications.application..",
                            "com.arovia.api.notifications.domain..",
                            "com.arovia.api.notifications.infrastructure.."
                    );

    @ArchTest
    static final ArchRule notifications_application_must_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage(
                            "com.arovia.api.notifications.application.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(
                            "com.arovia.api.notifications.infrastructure.."
                    );

    @ArchTest
    static final ArchRule identity_web_must_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("com.arovia.api.identity.web..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(
                            "com.arovia.api.identity.infrastructure.."
                    );

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..application..",
                            "..infrastructure..",
                            "..web..",
                            "..configuration.."
                    );

    @ArchTest
    static final ArchRule application_must_not_depend_on_web =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..web..");

    @ArchTest
    static final ArchRule top_level_modules_must_be_free_of_cycles =
            slices()
                    .matching("com.arovia.api.(*)..")
                    .should()
                    .beFreeOfCycles();
}