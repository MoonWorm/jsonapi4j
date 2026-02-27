# JsonApi4j Internal-Beta Product Readiness Review

Date: 2026-02-27  
Repository: `/Users/brahm/Novem/jsonapi4j`  
Readiness Target: Internal Beta

## Executive Verdict

**Verdict: `Ready with Conditions` for Internal Beta (post-remediation; re-evaluation pending).**

Decision policy used:
- `P0/P1` findings block a “ready” verdict.
- `P2` findings allow “ready with conditions” only if no `P1` exists.

All previously identified `P1` findings are remediated in this implementation pass.

Quarkus readiness mismatch is resolved as of **2026-02-27** by intentional product de-scope (Spring Boot as the only supported integration path).
One `P2` finding (coverage confidence gates) is only partially addressed and requires follow-up.

## Implementation Status (2026-02-27)

| Finding | Status | Reason | Evidence |
| --- | --- | --- | --- |
| 1) Sensitive payloads/request objects logged at INFO | **Completed** | Removed INFO logs that emitted full request/response bodies and full `JsonApiRequest`; added metadata-focused INFO completion log with request-id/method/path/status/duration. Any body preview logging is DEBUG-only with redaction/truncation. | `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/servlet/JsonApi4jDispatcherServlet.java`, `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/servlet/request/HttpServletRequestJsonApiRequestSupplier.java` |
| 2) Quarkus support confidence mismatch | **Completed** | Intentional de-scope implemented consistently: Quarkus module removed from reactor/repo, docs updated to Spring-only support policy, CI/release guardrails enforce no Quarkus artifacts. | `pom.xml`, `.github/workflows/build.yml`, `.github/workflows/release.yml`, `README.md`, `docs/index.md`, removed `jsonapi4j-rest-quarkus/**` |
| 3) Unbounded default executor in Spring auto-config | **Completed** | Replaced cached thread pool default with bounded/configurable `ThreadPoolExecutor` defaults and overload backpressure (`CallerRunsPolicy`); added `jsonapi4j.executor.*` properties. | `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/config/ExecutorProperties.java`, `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/config/JsonApi4jProperties.java`, `jsonapi4j-rest-springboot/src/main/java/pro/api4/jsonapi4j/springboot/autoconfiguration/SpringJsonApi4jAutoConfigurer.java`, `jsonapi4j-rest-springboot/src/test/java/pro/api4/jsonapi4j/springboot/autoconfiguration/SpringJsonApi4jAutoConfigurerExecutorTests.java` |
| 4) Servlet error flow writes response and rethrows | **Completed** | Handled error path now finalizes JSON:API error response and returns (no rethrow-after-write side effects). | `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/servlet/JsonApi4jDispatcherServlet.java`, `jsonapi4j-rest/src/test/java/pro/api4/jsonapi4j/servlet/JsonApi4jDispatcherServletErrorHandlingTests.java` |
| 5) Coverage confidence selective vs framework breadth | **Partial** | Aggregate coverage scope expanded to include additional runtime/plugin modules; explicit per-module or threshold gates are not yet implemented. | `jsonapi4j-report/pom.xml` |
| 6) Compound docs docs/config mismatch | **Completed** | Removed unsupported `maxIncludedResources` claim and aligned docs with implemented compound-doc properties. | `docs/index.md`, `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/config/CompoundDocsProperties.java` |
| 7) Maven build hygiene missing plugin versions | **Completed** | Added explicit missing plugin versions for compiler/deploy plugins in affected report/examples POMs. | `jsonapi4j-report/pom.xml`, `examples/pom.xml`, `examples/jsonapi4j-sampleapp-domain/pom.xml`, `examples/jsonapi4j-springboot-sampleapp/pom.xml`, `examples/jsonapi4j-servlet-sampleapp/pom.xml` |

## Scope and Baseline

This review covers only this repository:
- `/Users/brahm/Novem/jsonapi4j`

Baseline evidence used:
- Full local build/test reactor verification passed:
  - `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -B verify`
- Aggregate JaCoCo from local run (`jsonapi4j-report/target/site/jacoco-aggregate/jacoco.csv`):
  - Instruction coverage: `36.889872%` (`14836 / 40217`)
  - Branch coverage: `26.318151%` (`1173 / 4457`)
  - Line coverage: `44.368553%` (`3183 / 7174`)
  - Method coverage: `29.427680%` (`1414 / 4805`)
- Metrics refresh note:
  - Coverage metrics were refreshed from the aggregate CSV on `2026-02-27` during this remediation run.
- Some modules have no direct tests (for example: `jsonapi4j-rest-springboot`, several example modules).

## Findings (Ordered by Severity)

## P1

### 1) Sensitive payloads and request objects are logged at INFO

**Evidence**
- `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/servlet/JsonApi4jDispatcherServlet.java:143-145`
- `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/servlet/request/HttpServletRequestJsonApiRequestSupplier.java:129,189`

**Risk**
- Response bodies and rich request object dumps can contain sensitive data and end up in centralized logs by default.

**Why this blocks internal beta**
- Internal beta traffic often includes real-like data. INFO-level body logging is a high-likelihood privacy/security incident vector.

**Recommended fix**
- Switch to metadata-only INFO logs (method/path/status/request-id/duration).
- Gate full body logging behind explicit DEBUG + redaction policy.

---

### 2) Quarkus support confidence mismatch

**Status**
- Resolved on `2026-02-27` by intentional de-scope.

**Resolution evidence**
- Quarkus module removed from root reactor:
  - `pom.xml`
- `jsonapi4j-rest-quarkus` removed from the repository.
- Support claims updated to Spring Boot only:
  - `README.md`
  - `docs/index.md`
- CI and release workflows now enforce Spring-only integration policy:
  - `.github/workflows/build.yml`
  - `.github/workflows/release.yml`

**Outcome**
- Product claims, build coverage scope, and published artifact set are now aligned with the supported integration path.

## P2

### 3) Unbounded default executor in Spring auto-configuration

**Evidence**
- `jsonapi4j-rest-springboot/src/main/java/pro/api4/jsonapi4j/springboot/autoconfiguration/SpringJsonApi4jAutoConfigurer.java:104-107`

**Risk**
- `Executors.newCachedThreadPool()` can grow threads aggressively during load/downstream slowness.

**Recommended fix**
- Provide bounded default (fixed/bounded queue) and expose sizing via properties.

---

### 4) Servlet error flow writes error response and then rethrows

**Evidence**
- `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/servlet/JsonApi4jDispatcherServlet.java:125-136`

**Risk**
- Can produce noisy duplicate handling/logging and container-level behavior after response is already written.

**Recommended fix**
- Finalize response and return without rethrow in handled paths, or clearly separate handled vs unhandled exception branches.

---

### 5) Coverage confidence is partial relative to framework breadth

**Evidence**
- Coverage aggregation module is selective:
  - `jsonapi4j-report/pom.xml:17-38`
- Codecov upload uses that single aggregate file:
  - `.github/workflows/build.yml:25`
- Aggregate line coverage from local run: `44.368553%`

**Risk**
- Important surfaces can appear healthier than they are if not represented in aggregated coverage gates.

**Recommended fix**
- Expand aggregate coverage scope or add module-specific minimum thresholds for critical runtime paths.

---

### 6) Documentation/config mismatch for compound docs safety limits

**Evidence**
- Docs mention `jsonapi4j.compound-docs.maxIncludedResources`:
  - `docs/index.md:1229`
- `CompoundDocsProperties` does not expose this property:
  - `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/config/CompoundDocsProperties.java`

**Risk**
- Operators may rely on a non-existent limit and overestimate safeguards.

**Recommended fix**
- Align docs to current code, or implement property with tests.

## P3

### 7) Build hygiene warnings due to missing plugin version declarations in some POMs

**Evidence**
- Examples/report POMs rely on plugin versions that caused warnings in local verify (e.g., deploy/compiler plugin declarations without explicit version in those child poms).

Representative files:
- `jsonapi4j-report/pom.xml`
- `examples/pom.xml`
- `examples/jsonapi4j-sampleapp-domain/pom.xml`
- `examples/jsonapi4j-springboot-sampleapp/pom.xml`
- `examples/jsonapi4j-servlet-sampleapp/pom.xml`

**Risk**
- Future Maven behavior changes can make these warnings build-breaking.

**Recommended fix**
- Pin versions or enforce pluginManagement inheritance clearly and consistently.

## What JsonApi4j Does

- Provides a JSON:API-focused Java framework to build compliant APIs with minimal boilerplate.
- Supports resource and relationship operations (read/create/update/delete, linkage operations).
- Supports strict vs legacy compatibility behavior:
  - strict defaults and migration mode:
    - `docs/spec-compliance.md`
    - `jsonapi4j-rest/src/main/java/pro/api4/jsonapi4j/config/CompatibilityProperties.java`
- Offers extension plugins:
  - Access Control plugin
  - OpenAPI generation plugin
- Supports compound document resolution (`include`) via dedicated resolver module.

## How It Is Built

- Multi-module Maven architecture:
  - Parent modules listed in `pom.xml:41-50`
- Runtime layering:
  - `jsonapi4j-core` for processing/model orchestration
  - `jsonapi4j-rest` for Servlet HTTP layer
  - `jsonapi4j-rest-springboot` for Spring Boot auto-configuration (supported integration path)
  - plugins and compound docs modules
- CI/release:
  - Build workflow: `.github/workflows/build.yml`
  - Release workflow: `.github/workflows/release.yml`

## How To Use It (Practical Path)

Recommended quick path for internal adoption:

1. Add Spring Boot integration dependency:
   - `pro.api4:jsonapi4j-rest-springboot`
2. Define resource and relationship beans:
   - Example: `examples/jsonapi4j-springboot-sampleapp/src/main/java/pro/api4/jsonapi4j/sampleapp/domain/config/DomainSpringConfig.java`
3. Implement operations:
   - Example: `examples/jsonapi4j-springboot-sampleapp/src/main/java/pro/api4/jsonapi4j/sampleapp/operations/config/OperationsConfig.java`
4. Configure root path and optional features:
   - `examples/jsonapi4j-springboot-sampleapp/src/main/resources/application.yaml`
5. Expose/consume endpoints under `/jsonapi/*` by default.

Core contracts to implement:
- `jsonapi4j-base/src/main/java/pro/api4/jsonapi4j/operation/ResourceOperations.java`
- `jsonapi4j-base/src/main/java/pro/api4/jsonapi4j/operation/ToOneRelationshipOperations.java`
- `jsonapi4j-base/src/main/java/pro/api4/jsonapi4j/operation/ToManyRelationshipOperations.java`

Plugin extension surface:
- `jsonapi4j-base/src/main/java/pro/api4/jsonapi4j/plugin/JsonApi4jPlugin.java`

## Public API / Interface Delta Recommendations

Implementation status update (`2026-02-27`):
- Quarkus support has been intentionally de-scoped.
- Spring Boot is the only supported integration path.

Recommended deltas:

- **Non-breaking**
  - Add configuration for safe default logging/redaction behavior.
  - Add configurable bounded executor defaults for Spring path.
  - Align docs/config for compound docs limits.

- **Breaking**
  - None required for initial internal-beta hardening.

## Test Confidence Assessment

What is strong:
- Core reactor build is green.
- Spring sample app has meaningful integration-style operation tests.
- Spec-compliance matrix exists and includes explicit strict/legacy semantics.

What is weak:
- Coverage depth is moderate overall and uneven by module.
- Some modules have minimal/no direct tests.

## Go/No-Go Criteria for Internal Beta

Current status:
- `Ready with Conditions` gates for prior `P1`/docs mismatches are satisfied.
- Remaining condition: strengthen explicit coverage confidence gates (see backlog).

## Remediation Backlog

### Must Fix Before Internal Beta

1. Coverage confidence hardening:
   - add explicit coverage thresholds and/or module-level gates for critical runtime paths.

### Near-Term Hardening (Post-Beta Start Window)

1. Bounded executor defaults in Spring auto-config.
2. Clean servlet exception handling path (avoid handled-path rethrow).
3. Increase test depth for mutation/linkage/compound-doc edge cases and plugin paths.

### Hygiene Follow-Ups

1. Normalize plugin version declarations to eliminate Maven warnings.
2. Add explicit quality gates (coverage thresholds per critical module or path).

## Notes

- This review document was updated after implementation changes to reflect current status.
- Quarkus de-scope status is captured as a resolved finding.
