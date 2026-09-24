---
title: "Built with JsonApi4j"
layout: splash
permalink: /showcase/
header:
  overlay_color: "#0e0f20"
  actions:
    - label: "Get Started"
      url: /getting-started/
    - label: "Add Your Project"
      url: /contact/
excerpt: "Real products, real users, one consistent API layer."
description: "Products and services running JsonApi4j in production — TV Radar, GrammarMama, Text2Event, and more."
---

<p class="showcase-intro" markdown="0">
  From web apps to mobile clients, browser extensions, and chat bots, these products serve their users through JSON:API backends built on JsonApi4j.
</p>

<div class="showcase-grid" markdown="0">
{% for product in site.data.showcase %}
  <a href="{{ product.url }}" class="showcase-card" style="--accent: {{ product.accent }};" target="_blank" rel="noopener">
    <div class="showcase-card__media" style="background: {{ product.logo_bg }};">
      <img src="{{ product.logo }}" alt="{{ product.name }} logo" loading="lazy">
    </div>
    <div class="showcase-card__body">
      <span class="showcase-card__category">{{ product.category }}</span>
      <strong class="showcase-card__name">{{ product.name }}</strong>
      <em class="showcase-card__tagline">{{ product.tagline }}</em>
      <p class="showcase-card__description">{{ product.description }}</p>
      <ul class="showcase-card__platforms">
        {% for platform in product.platforms %}<li>{{ platform }}</li>{% endfor %}
      </ul>
      <span class="showcase-card__link">Visit {{ product.url | remove: "https://" }} &rarr;</span>
    </div>
  </a>
{% endfor %}
</div>

<div class="dark-band showcase-cta" markdown="0">
  <h2>Running JsonApi4j in Production?</h2>
  <p class="dark-band__subtitle">Get your product featured here. Tell us what you've built and how the framework helps.</p>
  <div class="site-cta-footer__buttons">
    <a href="/contact/" class="cta-btn cta-btn--primary">Submit Your Project</a>
    <a href="https://github.com/MoonWorm/jsonapi4j/discussions" class="cta-btn cta-btn--ghost">Share on GitHub Discussions</a>
  </div>
</div>
