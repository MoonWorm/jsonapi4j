---
title: "Compatibility"
permalink: /compatibility/
---

Which versions of Java, Spring Boot, Quarkus and the Servlet API JsonApi4j runs on, and what the framework
guarantees across them.

The short answer: the framework adds no version requirement of its own beyond what your web framework already
imposes. Every integration dependency is `provided`, so your application decides what is on the classpath.

## Supported Versions

| | Supported | Verified against |
|---|---|---|
| **Java** | 17+ | 17, 18, 19, 20, 21, 22, 23, 24, 25 |
| **Spring Boot** | 3.0.x – 4.x | 3.0.0 (EOL), 3.2.0 (EOL), 3.4.2 (EOL), 4.1.0 |
| **Spring Security** | 6.x, 7.x | 6.0.0 (EOL), 6.4.2 (EOL), 7.1.0 |
| **Quarkus** | 3.20+ (LTS and newer) | 3.20.6 (EOL), 3.33.3.2, 3.39.3 |
| **Jakarta Servlet** | 6.0, 6.1 | Tomcat 10.1/11, Jetty 12, Undertow |

**(EOL)** marks a version whose upstream open-source support has ended. It does not mean the framework stops
working there — these are exactly the versions the build compiles against, and they are verified on every
release. It means *the vendor* no longer ships fixes for them, so the security posture of your application is
your own concern. Upstream OSS support ends — or ended — on:

| | Spring Boot | | Quarkus |
|---|---|---|---|
| 3.0 | 31 Dec 2023 | 3.15 LTS | 25 Sep 2025 |
| 3.1 | 30 Jun 2024 | 3.20 LTS | 28 Mar 2026 |
| 3.2 | 31 Dec 2024 | 3.27 LTS | 24 Sep 2026 |
| 3.3 | 30 Jun 2025 | 3.33 LTS | 25 Mar 2027 |
| 3.4 | 31 Dec 2025 | 3.39 | current |
| 3.5 | 30 Jun 2026 | | |
| 4.0 | 31 Dec 2026 | | |
| 4.1 | 31 Jul 2027 | | |

As of September 2026 that makes **the whole Spring Boot 3.x line EOL upstream** — 4.0 and 4.1 are the only
lines Spring still patches. If you are starting a new application, start on Spring Boot 4. The 3.x range remains
supported here so existing applications are not forced to migrate two things at once.

On the Quarkus side the floor, **3.20 LTS, is itself EOL** (28 Mar 2026). **3.33 LTS** is the current LTS,
supported through March 2027, and is the version most applications should be on.

Java 17 is not in that list: it is an LTS release with community support well beyond this framework's horizon.

The framework is compiled against the **oldest** version in each supported range — Java 17, Spring Boot 3.0,
Quarkus 3.20, Servlet 6.0 — so the ranges above are enforced by the build rather than asserted. Newer
versions are verified on a schedule. Because these dependencies are `provided`, the version the framework
compiles against does not constrain your application: you bring your own.

**Quarkus 3.15 and earlier do not work.** On those versions the framework's plugin classes end up loaded by two
different classloaders during the Quarkus build step, so a plugin instance can no longer be cast to the plugin
interface the framework expects. CDI then reports the plugin's beans as unsatisfied and the application fails to
start, with a `ClassCastException` naming the same class on both sides. Quarkus changed that classloading
behaviour between 3.15 and 3.20; from **3.20** onwards it works. This was observed with the plugins on the
classpath — an application using none of them has not been tested.

Two plausible causes were tested and ruled out, so neither is worth revisiting: the framework's bytecode level
(3.15 fails the same way against Java 17 bytecode as against 23) and a missing Jandex index on the plugin jars
(adding one does not change the outcome).

The Java floor is **17+**, which is the same floor every supported Spring Boot and Quarkus line already sets for
itself. The framework therefore adds no Java requirement of its own: whatever JDK your Spring Boot or Quarkus
version runs on, the framework runs on too. Newer JDKs are fine — the published bytecode targets 17 and is
verified on current releases.

Spring Boot below **3.0** is not supported, and that is a namespace boundary rather than a version choice: Boot
2.x is built on `javax.servlet`, while the framework is `jakarta.*` throughout.

## Spring Boot Versions

One artifact covers **Spring Boot 3.0.x through 4.x** — no classifier, no separate dependency, nothing to
configure. `jsonapi4j-rest-springboot` is compiled against the 3.x floor and declares Spring Boot as
`provided`, so your application's own Spring Boot version decides what is on the classpath. Spring Security 6 and 7
are both supported for [principal resolution](/principal-resolution/), as are springdoc 2.x and 3.x on the
application side.

The Java floor is **17+** — the same baseline Spring Boot declares for every 3.x and 4.x line, so the framework
never asks for a newer JDK than your Spring Boot version already does.

Spring Boot 4 defaults to Jackson 3 (`tools.jackson`), which changes nothing here: the framework builds its
own Jackson 2 `ObjectMapper` rather than injecting your application's, and the two versions use different
packages, so they coexist on one classpath.

The Servlet API itself is a `provided` dependency — your container supplies it. The framework is built
against **Jakarta Servlet 6.0**, the floor of the supported range, and is verified on Servlet 6.0 and 6.1
containers. Note the `jakarta.*` namespace: containers still on `javax.servlet` are not supported.
