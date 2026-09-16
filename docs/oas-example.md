---
title: "Live OpenAPI Example"
permalink: /oas-example/
layout: single
sidebar: false
classes: wide
---

This is the OpenAPI document the [OpenAPI Plugin](/openapi-plugin/) generates for the
[sample app](https://github.com/MoonWorm/jsonapi4j/tree/main/examples) — three resources, their relationships and
their operations. Nothing here was written by hand: it is produced from the same registries the framework serves
from, and regenerated on every build, so what you are reading is what the plugin actually emits.

Worth opening first: **`UsersCreateAttributes`** against **`UsersAttributes`** (what a create must carry, versus what
a response guarantees — nothing), the **`security`** on the `citizenships` operations (scopes published only on the
grant flow that declares them), and **`PaginationLinksObject`** on any collection.

The `servers` entry points at `localhost` because this is the sample app's own document — there is no hosted
backend behind it. It is here to be read, not called.

<p>
  <a class="btn btn--primary" href="/assets/oas/sample-app.json">Download the raw document</a>
  <a class="btn btn--inverse" href="/openapi-plugin/">How it is generated</a>
</p>

<div id="redoc-container"
     style="height: 85vh; overflow: auto; border: 1px solid #e9e9e9; border-radius: 4px;"></div>

<script src="https://cdn.jsdelivr.net/npm/redoc@2/bundles/redoc.standalone.js"></script>
<script>
  Redoc.init(
    '/assets/oas/sample-app.json',
    {
      scrollYOffset: 0,
      hideDownloadButton: true,
      expandResponses: '200,201',
      jsonSampleExpandLevel: 3,
      theme: {
        typography: {
          fontSize: '15px',
          headings: { fontFamily: 'inherit' }
        },
        rightPanel: { backgroundColor: '#1f2430' }
      }
    },
    document.getElementById('redoc-container')
  );
</script>
