# Java 21 Migration Plan: submon_gmsrisk-scoring-ingester

## A. Current State Summary

| Component | Current Version | Notes |
|---|---|---|
| **Java** | 11 | `maven.compiler.source/target=11`, `java.version=11` (from parent POM) |
| **Parent POM** | `foundation-parent:6.0.0.RELEASE` | Defines most dependency versions via properties |
| **Maven** | 3.5 (Jenkinsfile) / 3.6.3 (local) | Jenkinsfile specifies `maven 3.5` |
| **Spring Boot** | 2.3.1.RELEASE (autoconfigure, hardcoded in child POM) | Very outdated (May 2020); parent defines `spring-boot.version=2.5.3` |
| **Spring Framework** | 5.3.20 | Managed by parent POM |
| **Spring Data Cassandra** | 3.2.8 | Via `spring-data.cassandra` property |
| **Spring Data Commons** | 2.7.10 | |
| **Spring Kafka** | 2.7.10 | |
| **Spring Retry** | 1.2.5.RELEASE | |
| **Jackson** | 2.12.4 (core/databind/annotations) | |
| **Lombok** | 1.18.20 | Not Java 21 compatible |
| **Mockito** | 4.11.0 (core/inline) | child POM hardcoded |
| **Mockito JUnit Jupiter** | 5.11.0 | From parent |
| **JUnit Jupiter** | 5.7.2 (child POM) / 5.10.0 (parent dependencyManagement) | Child POM hardcodes older version |
| **JUnit Platform** | 1.7.2 (child POM) / 1.10.0 (parent) | Child POM hardcodes older version |
| **JUnit 4** | 4.12 | Present for vintage/legacy test support |
| **PowerMock** | 2.0.7 / 2.0.9 (from parent) | **Incompatible with Java 21** - but NOT imported in any source files |
| **JaCoCo** | 0.8.3 | Not Java 21 compatible (needs 0.8.11+) |
| **Maven Compiler Plugin** | 3.8.1 | Needs upgrade for Java 21 |
| **Maven Surefire Plugin** | 3.0.0-M6 | Pre-release milestone |
| **Maven Failsafe Plugin** | 3.0.0-M6 | Pre-release milestone |
| **ShedLock** | 4.34.0 | |
| **GridGain/Ignite** | 8.8.18 | |
| **Guava** | 32.1.1-jre | |
| **Gson** | 2.8.5 | |
| **Log4j2** | 2.20.0 | |
| **SLF4J** | 1.7.26 | |
| **Netty** | 4.1.87.Final | |
| **Drools** | 7.57.0.Final (via parent) | Internal `gmsrisk-drools:1.0.1.21-SNAPSHOT` |
| **Testcontainers** | 1.19.0 | |
| **Cobertura Plugin** | 2.7 | Legacy, coexists with JaCoCo |
| **fmt-maven-plugin** | 2.13 | Google Java Format |

### Build & CI Configuration
- **Jenkinsfile**: `javaVersionOverride = "jdk11"`, `mavenVersionOverride = 'maven 3.5'`, `skipSonar = false`
- **Surefire config**: runs via test suite `RiskScoringImplsTestSuite` using `WildcardPatternSuite` from junit-toolbox
- **Surefire argLine**: `--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED` (already present)
- **Surefire providers**: `surefire-junit-platform`, `surefire-api`, `surefire-junit47`, `surefire-testng`
- **Repositories**: Artifactory (`prod`, `corporate`, `snapshots`); Maven Central still present as fallback in parent

### Code Patterns
- **javax annotations**: Only 2 files use `javax.annotation.PreDestroy` and `javax.annotation.PostConstruct` - these come from `javax.annotation-api:1.3.2` which is still available on Java 21 (not removed; it's a separate JAR, not part of `java.xml.ws.annotation` module)
- **No internal JDK API usage** in source code (`sun.*`, `com.sun.*`, `jdk.internal.*` — none found in `src/main/java`)
- **Reflection in tests**: Extensive use of `setAccessible(true)`, `getDeclaredField()`, `getDeclaredMethod()` in test classes (ScoringControllerTest, RiskEnrichmentTest, SubsequenceProcessInvokerTest, SchedulingHandlerTest, ParametersTest) — will need `--add-opens` JVM args
- **Test annotations**: Mixed JUnit 4 (`@Before`, `@Test`, `@RunWith`) and JUnit 5 (`@ExtendWith(MockitoExtension.class)`) styles
- **MockitoAnnotations.initMocks()** in RiskEnrichmentTest (deprecated, should be `openMocks()`)
- **PowerMock**: Declared as dependency in parent POM but **NOT imported or used in any source/test file** — safe to exclude/ignore

### Repositories Configuration (from effective POM)
```xml
<!-- Current repos - inherited from parent -->
<repositories>
  <repository><id>prod</id><url>https://artifactory.aexp.com/prod</url></repository>
  <repository><id>corporate</id><url>https://artifactory.aexp.com/corporate/</url></repository>
  <repository><id>snapshots</id><url>https://artifactory.aexp.com/snapshots/</url></repository>
  <repository><id>central</id><url>https://repo.maven.apache.org/maven2</url></repository> <!-- SHOULD BE REPLACED -->
</repositories>
```

---

## B. Target State (Java 21 + Updated Stack)

| Component | Target Version | Rationale |
|---|---|---|
| **Java** | 21 | LTS target |
| **Parent POM** | `foundation-parent:6.0.0.RELEASE` | **Keep unchanged** — internal dependency, upgrading is out of scope |
| **Maven Compiler Plugin** | 3.13.0 | Latest stable, full Java 21 support |
| **Maven Surefire Plugin** | 3.5.2 | Latest stable GA release |
| **Maven Failsafe Plugin** | 3.5.2 | Match surefire version |
| **Spring Boot** | 2.3.1.RELEASE → **3.4.4** | Latest stable 3.x in Artifactory; Java 21 requires Spring Boot 3.2+ |
| **Spring Framework** | 5.3.20 → **6.2.x** (managed by Boot 3.4.4) | Spring 6 required for Boot 3 |
| **Lombok** | 1.18.20 → **1.18.36** | Java 21 support requires 1.18.30+ |
| **Jackson** | 2.12.4 → **2.18.x** (managed by Boot 3.4.4 BOM) | Java 21 compatible |
| **Mockito** | 4.11.0 → **5.15.2** | Latest stable; `mockito-inline` merged into core in 5.x |
| **Mockito JUnit Jupiter** | 5.11.0 → **5.15.2** | Align with mockito-core |
| **JUnit Jupiter** | 5.7.2 → **5.11.x** (managed by Boot 3.4.4 BOM) | Latest stable |
| **JUnit Platform** | 1.7.2 → **1.11.x** | Align with Jupiter |
| **JaCoCo** | 0.8.3 → **0.8.12** | Java 21 bytecode support requires 0.8.11+ |
| **ShedLock** | 4.34.0 → **6.x or 5.x** | 5.x+ supports Spring Boot 3; check Artifactory |
| **Guava** | 32.1.1-jre → **33.4.8-jre** | Latest stable |
| **Gson** | 2.8.5 → **2.11.0** | Latest stable |
| **Log4j2** | 2.20.0 → **2.24.x** | Latest stable |
| **SLF4J** | 1.7.26 → **2.0.x** | Required for Log4j2 2.24.x, Spring Boot 3 |
| **Netty** | 4.1.87.Final → **4.1.11x.Final** | Latest stable |
| **javax.annotation-api** | 1.3.2 → **jakarta.annotation-api 2.1.x** | Spring Boot 3 uses Jakarta namespace |
| **javax → jakarta** | javax.annotation.* → jakarta.annotation.* | **Required** for Spring Boot 3 |
| **fmt-maven-plugin** | 2.13 → **2.23+** | Java 21 formatting support |
| **Cobertura Plugin** | 2.7 → **REMOVE** | Incompatible with Java 21; JaCoCo already handles coverage |

### Important Constraints
- **Parent POM (`foundation-parent:6.0.0.RELEASE`)** is kept as-is. Properties from parent will be overridden in the child POM `<properties>` section where needed.
- **Internal dependencies** (`gmsrisk-*`, `imc-*`, `drools`, `ebnc-common`, `ssaascrypto`) are kept at their current versions since they are internal artifacts — upgrading them is out of scope.
- **Spring Boot 3 migration** is a significant change (Spring 5→6, javax→jakarta). However, this is **required** because Spring Boot 2.x is EOL and does not officially support Java 21.

---

## C. Required Changes

### C1. Build Configuration Updates

1. **Override Java version properties in child POM `<properties>`:**
   ```xml
   <java.version>21</java.version>
   <maven.compiler.source>21</maven.compiler.source>
   <maven.compiler.target>21</maven.compiler.target>
   ```

2. **Upgrade maven-compiler-plugin** from 3.8.1 → 3.13.0

3. **Upgrade maven-surefire-plugin** from 3.0.0-M6 → 3.5.2

4. **Upgrade JaCoCo** from 0.8.3 → 0.8.12

5. **Remove Cobertura plugin** (incompatible with Java 21, JaCoCo already configured)

6. **Update Surefire argLine** to add required `--add-opens` for reflection-heavy tests

7. **Add repositories and distributionManagement** per user requirements (Artifactory only, no direct Maven Central)

8. **Jenkinsfile**: Update `javaVersionOverride = "jdk21"` and `mavenVersionOverride = 'maven 3.9'`

### C2. Dependency and Plugin Upgrades

All versions to be centralized in `<properties>` section:

| Dependency | Current | Target | Notes |
|---|---|---|---|
| spring-boot-autoconfigure | 2.3.1.RELEASE | 3.4.4 | Hardcoded in child POM |
| spring-boot-maven-plugin | 2.5.3 | 3.4.4 | Via property |
| lombok | 1.18.20 | 1.18.36 | Override parent property |
| mockito-core | 4.11.0 | 5.15.2 | Hardcoded in child POM |
| mockito-inline | 4.11.0 | **REMOVE** | Merged into mockito-core in 5.x |
| mockito-junit-jupiter | 5.11.0 | 5.15.2 | From parent; add explicit override |
| junit-jupiter | 5.7.2 | 5.11.4 | Override child POM hardcoded version |
| junit-platform | 1.7.2 | 1.11.4 | Override child POM hardcoded version |
| jacoco | 0.8.3 | 0.8.12 | Override parent property |
| shedlock-spring | 4.34.0 | 5.16.0 | Spring Boot 3 compatible |
| guava | 32.1.1-jre | 33.4.8-jre | Override parent property |
| gson | 2.8.5 | 2.11.0 | Override parent property |
| javax.annotation-api | 1.3.2 | **REPLACE** with jakarta.annotation-api 2.1.1 | Required for Spring Boot 3 |
| fmt-maven-plugin | 2.13 | 2.23 | Java 21 support |
| snakeyaml | 1.30.0.redhat-00002 | 2.2 | Spring Boot 3 requires SnakeYAML 2.x |
| maven-compiler-plugin | 3.8.1 | 3.13.0 | |
| maven-surefire-plugin | 3.0.0-M6 | 3.5.2 | |

### C3. Framework Upgrades

**Spring Boot 2.x → 3.4.4** is the most significant change:
- Spring Framework 5.3 → 6.2.x (managed by Boot BOM)
- Jakarta EE namespace migration (`javax.*` → `jakarta.*`)
- Spring Data Cassandra, Spring Kafka, Spring Retry versions managed by Boot BOM

### C4. Code Changes (Only Where Required)

1. **javax → jakarta migration** (2 files):
   - `ScoringLauncherBoot.java`: `import javax.annotation.PreDestroy` → `import jakarta.annotation.PreDestroy`
   - `ScoringController.java`: `import javax.annotation.PostConstruct` → `import jakarta.annotation.PostConstruct`

2. **Spring Boot 3 auto-configuration class renames** (if applicable):
   - Check if `CassandraDataAutoConfiguration`, `CassandraAutoConfiguration` class names changed in Boot 3

3. **MockitoAnnotations.initMocks(this)** → `MockitoAnnotations.openMocks(this)` in `RiskEnrichmentTest.java` (deprecated in Mockito 4, may cause issues in 5.x)

4. **mockito-inline removal**: Replace `mockito-inline` dependency with `mockito-core` (inline mocking is default in Mockito 5+)

---

## D. Compatibility Risks

### D1. Breaking Dependency Changes

| Risk | Severity | Mitigation |
|---|---|---|
| **Spring Boot 2→3 (javax→jakarta)** | HIGH | Only 2 source files affected; Spring XML configs may also reference javax classes via parent libraries |
| **Internal `gmsrisk-foundation` libraries compiled with Java 11** | MEDIUM | Java 21 JVM is backward-compatible and can run Java 11 bytecode. However, if internal libs use removed APIs, runtime errors could occur |
| **ShedLock 4→5 API changes** | LOW | ShedLock 5 is mostly API-compatible with 4; main change is package rename possibility |
| **GridGain/Ignite 8.8.18 with Java 21** | MEDIUM | GridGain 8.8.x may have issues with Java 21 strong encapsulation; may need additional `--add-opens` flags |
| **Drools 7.57 with Java 21** | MEDIUM | Drools 7.x uses reflection heavily; may need `--add-opens` flags. Internal `gmsrisk-drools` wrapper may abstract this |

### D2. Removed/Changed APIs
- `javax.annotation` package removed from JDK in Java 11 but already handled via explicit `javax.annotation-api` dependency → will be replaced with `jakarta.annotation-api`
- No `sun.*` or `com.sun.*` usage in source code
- No `java.security.acl` or other removed Java 17+ APIs detected

### D3. Test Instability Risks
- **Reflection-heavy tests**: Many tests use `setAccessible(true)` on private fields/methods. Java 21 enforces strong encapsulation — these will fail without proper `--add-opens` flags in surefire argLine
- **WildcardPatternSuite** (junit-toolbox): Uses JUnit 4 `@RunWith` — needs `junit-vintage-engine` to remain in classpath
- **Testcontainers 1.19.0**: Cannot reach DockerHub (no internet) — these tests likely already skipped/mocked
- **PowerMock**: Declared in parent POM but NOT used in any source file — no actual risk

---

## E. Execution Plan (Ordered Steps)

### Phase 1: Build System Upgrade
1. Add/override Java 21 properties in child POM `<properties>`
2. Upgrade `maven-compiler-plugin` to 3.13.0
3. Upgrade `maven-surefire-plugin` to 3.5.2
4. Upgrade `jacoco-maven-plugin` and `org.jacoco.agent` to 0.8.12
5. Remove Cobertura plugin reference
6. Upgrade `fmt-maven-plugin` to 2.23
7. Add `<repositories>`, `<pluginRepositories>`, and `<distributionManagement>` per requirements
8. Move all hardcoded versions to `<properties>`

### Phase 2: Dependency Upgrades
1. Upgrade `spring-boot-autoconfigure` to 3.4.4
2. Upgrade `spring-boot-maven-plugin` to 3.4.4
3. Replace `javax.annotation-api` with `jakarta.annotation-api`
4. Upgrade `shedlock-spring` to 5.16.0
5. Upgrade `mockito-core` to 5.15.2 and REMOVE `mockito-inline`
6. Upgrade `lombok` to 1.18.36 (override parent property)
7. Upgrade `guava` to 33.4.8-jre
8. Upgrade `gson` to 2.11.0
9. Upgrade remaining dependencies (jackson, snakeyaml, etc.) — many managed by Spring Boot BOM
10. Add `junit-vintage-engine` if not already present (for JUnit 4 test compat)

### Phase 3: Code Fixes
1. Replace `javax.annotation.PreDestroy` → `jakarta.annotation.PreDestroy` in `ScoringLauncherBoot.java`
2. Replace `javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct` in `ScoringController.java`
3. Update `MockitoAnnotations.initMocks(this)` → `openMocks(this)` in `RiskEnrichmentTest.java`
4. Update Spring Boot auto-configuration class references if renamed in Boot 3
5. Update `ScoringLauncherBoot.java` exclusions if class names changed

### Phase 4: Test Fixes
1. Expand surefire `argLine` with additional `--add-opens` for reflection in tests
2. Verify test suite discovery still works with upgraded JUnit/Surefire
3. Run full test suite and fix any failures
4. Add test cases for any newly added lines to ensure 100% coverage

### Phase 5: CI Configuration
1. Update `Jenkinsfile`: `javaVersionOverride = "jdk21"`, `mavenVersionOverride = 'maven 3.9'`

---

## F. Rollback Strategy

1. **Branch-based rollback**: All changes are on `feature/java21-upgrade-devin` branch. The `master`/`main` branch remains untouched. If issues arise, simply do not merge the PR.

2. **Commit-level rollback**: Changes will be committed in logical groups:
   - Build configuration changes (revertible independently)
   - Dependency upgrades (revertible independently)
   - Code changes (revertible independently)
   - Test fixes (revertible independently)

3. **CI/CD rollback**: The Jenkinsfile change to `jdk21` only takes effect on this branch. If the PR is not merged, CI continues to use JDK 11 on all other branches.

4. **Emergency revert**: If merged and issues found in production, a single `git revert` of the merge commit restores the previous state.

---

## G. Open Questions / Items Requiring Attention

1. **Spring Boot 3 + internal `gmsrisk-foundation` libraries**: The parent POM and foundation libraries (`gmsrisk-dao`, `gmsrisk-services`, etc.) are compiled against Spring 5 / Java 11. Spring Boot 3 uses Spring 6 which has the jakarta namespace. If foundation libraries expose Spring-specific types (e.g., Spring `@Configuration` classes using javax), there could be runtime classpath conflicts. **Mitigation**: The child POM already excludes `org.springframework.boot` from `gmsrisk-pom-common`, suggesting the project manages its own Spring Boot version independently.

2. **GridGain/Ignite compatibility**: GridGain 8.8.18 may need additional `--add-opens` JVM arguments at runtime for Java 21. This is a runtime concern (not build-time) and may require updates to deployment scripts.

3. **Drools runtime**: Internal `gmsrisk-drools:1.0.1.21-SNAPSHOT` wraps Drools 7.57.0.Final. Drools 7.x has known issues with Java 17+ strong encapsulation. May need `--add-opens` flags at runtime.

4. **`jdk21` availability in Jenkins**: The Jenkinsfile references `javaVersionOverride = "jdk21"` — need to confirm that `jdk21` is a valid tool name in the Jenkins CI environment.
