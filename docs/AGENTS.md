# Documentation site (api4.pro) — Agent Guide

The `docs/` folder is the GitHub Pages source for https://api4.pro. Jekyll + the
[Minimal Mistakes](https://github.com/mmistakes/minimal-mistakes) remote theme.
Topic pages live at `docs/*.md`; each maps to a route on the site.

## Key files

- `_config.yml` — theme, plugins, SEO, page defaults.
- `_data/navigation.yml` — top nav + docs sidebar (groups: Documentation, Framework Internals,
  Plugins, Advanced). Add new pages here or they won't appear in navigation.
- `_includes/head/custom.html` — favicon links + Mermaid JS (CDN).
- `assets/css/main.scss` — custom styles (hero, feature cards, "Works With" pills, content width).
- `_pages/` — standalone pages (e.g. `blog.md`); `_posts/` — blog posts.

## Page conventions

- **Images:** use root-relative paths (e.g. `/compound-docs-sequence-diagram.png`) — pages render
  under subdirectories, so relative paths break.
- **Mermaid:** wrap diagrams in `<div class="mermaid">…</div>` (script is loaded in
  `head/custom.html`).
- **TOC:** disabled globally (`toc: false` in defaults); re-enable per page via front matter.
- **Landing page** (`index.md`): `splash` layout with gradient hero, feature rows, Works-With pills.

## Blog

Posts in `_posts/`, archive page at `_pages/blog.md`. The blog is **SEO-only** — do not pitch it as
a user-facing feature in release announcements or framework docs.

For framework facts, build/test commands, and conventions, see the root [`AGENTS.md`](../AGENTS.md).
