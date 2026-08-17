package dev.iamrat.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * README와 docs/architecture/module-dependencies.md에 선언된
 * 모듈 의존성 방향을 실행 가능한 규칙으로 고정한다.
 */
class ModuleBoundaryTest {

    private static final String BASE = "dev.iamrat";

    private static final List<String> MODULES = List.of(
        "app", "core", "support", "auth", "board", "source",
        "ingest", "catalog", "price", "ai", "messaging"
    );

    private static final Map<String, Set<String>> ALLOWED_MODULE_DEPENDENCIES = Map.ofEntries(
        Map.entry("app", Set.of("core", "support", "auth", "board", "source", "ingest", "catalog", "price", "ai", "messaging")),
        Map.entry("core", Set.of()),
        Map.entry("support", Set.of("core")),
        Map.entry("auth", Set.of("core", "support")),
        Map.entry("board", Set.of("core", "support")),
        Map.entry("source", Set.of()),
        Map.entry("ingest", Set.of("source", "catalog", "price", "core")),
        Map.entry("catalog", Set.of("core")),
        Map.entry("price", Set.of("source", "catalog", "core")),
        Map.entry("ai", Set.of("catalog", "core")),
        Map.entry("messaging", Set.of("core"))
    );

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages(BASE);

    @Test
    @DisplayName("각 모듈은 문서에 선언된 모듈에만 의존한다")
    void modulesOnlyDependOnDeclaredModules() {
        for (String module : MODULES) {
            Set<String> allowed = ALLOWED_MODULE_DEPENDENCIES.get(module);
            String[] forbidden = MODULES.stream()
                .filter(other -> !other.equals(module))
                .filter(other -> !allowed.contains(other))
                .map(other -> BASE + "." + other + "..")
                .toArray(String[]::new);
            if (forbidden.length == 0) {
                continue;
            }
            noClasses()
                .that().resideInAPackage(BASE + "." + module + "..")
                .should().dependOnClassesThat().resideInAnyPackage(forbidden)
                .as("module '%s' may only depend on %s".formatted(module, allowed))
                .check(CLASSES);
        }
    }

    @Test
    @DisplayName("domain 패키지는 presentation/infrastructure에 의존하지 않는다")
    void domainDoesNotDependOnOuterLayers() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..presentation..", "..infrastructure..")
            .check(CLASSES);
    }

    @Test
    @DisplayName("application/domain 패키지는 presentation에 의존하지 않는다")
    void applicationDoesNotDependOnPresentation() {
        noClasses()
            .that().resideInAnyPackage("..application..", "..domain..")
            // board 모듈은 application query service가 presentation/dto를 아직 반환하는
            // 기존 부채가 남아 있어 분리 리팩터링 완료 전까지 예외로 둔다.
            .and().resideOutsideOfPackage(BASE + ".board..")
            .should().dependOnClassesThat().resideInAPackage("..presentation..")
            .check(CLASSES);
    }
}
