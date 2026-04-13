---
title: "JSON:API Specification Deviations"
permalink: /spec-deviations/
---

While **JsonApi4j** adheres closely to the JSON:API specification, it introduces a few deliberate deviations and simplifications aimed at improving performance, maintainability, and developer experience:
1. Flat resource structure - encourages top-level resources like `/users` and `/articles` instead of nested structures such as `/users/{userId}/articles`. This design enables automatic link generation and simplifies Compound Document resolution.
2. Controlled relationship resolution - by default, relationship data under 'relationships' -> {relName} -> 'data' is not automatically resolved. This prevents unnecessary "+N" requests and gives developers explicit control over relationship fetching.
