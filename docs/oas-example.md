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

The `servers` entry points at `localhost` because this is the sample app's own document — there is no hosted
backend behind it. It is here to be read, not called.

<p>
  <a class="btn btn--primary" href="/assets/oas/sample-app.json">Download the raw document</a>
  <a class="btn btn--inverse" href="/openapi-plugin/">How it is generated</a>
</p>

<div class="oas-tabs" role="tablist">
  <button type="button" class="btn btn--primary" role="tab" data-oas-tab="redoc" aria-selected="true">Redoc</button>
  <button type="button" class="btn btn--inverse" role="tab" data-oas-tab="swagger" aria-selected="false">Swagger UI</button>
</div>

<div id="redoc-container" class="oas-panel" role="tabpanel"></div>
<div id="swagger-container" class="oas-panel" role="tabpanel" hidden></div>

<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui.css">
<script src="https://cdn.jsdelivr.net/npm/redoc@2/bundles/redoc.standalone.js"></script>
<script>
  (function () {
    var SPEC_URL = '/assets/oas/sample-app.json';
    var swaggerLoaded = false;
    var activeTab = 'redoc';

    ['pushState', 'replaceState'].forEach(function (method) {
      var original = history[method];
      history[method] = function (state, title, url) {
        if (activeTab === 'swagger' && url && String(url).indexOf('#swagger') < 0) return;
        return original.apply(history, arguments);
      };
    });

    Redoc.init(
      SPEC_URL,
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

    function loadSwagger() {
      if (swaggerLoaded) return;
      swaggerLoaded = true;
      var script = document.createElement('script');
      script.src = 'https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js';
      script.onload = function () {
        SwaggerUIBundle({
          url: SPEC_URL,
          dom_id: '#swagger-container',
          deepLinking: false,
          defaultModelsExpandDepth: 1,
          docExpansion: 'list'
        });
      };
      document.body.appendChild(script);
    }

    var tabs = document.querySelectorAll('[data-oas-tab]');

    function show(name) {
      activeTab = name;
      tabs.forEach(function (tab) {
        var active = tab.getAttribute('data-oas-tab') === name;
        tab.classList.toggle('btn--primary', active);
        tab.classList.toggle('btn--inverse', !active);
        tab.setAttribute('aria-selected', active);
        document.getElementById(tab.getAttribute('data-oas-tab') + '-container').hidden = !active;
      });
      if (name === 'swagger') loadSwagger();
    }

    tabs.forEach(function (tab) {
      tab.addEventListener('click', function () {
        var name = tab.getAttribute('data-oas-tab');
        show(name);
        history.replaceState(null, '', name === 'swagger' ? '#swagger' : location.pathname + location.search);
      });
    });

    if (location.hash === '#swagger') show('swagger');
  })();
</script>
