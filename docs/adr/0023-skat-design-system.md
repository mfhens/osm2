# ADR-0023 — SKAT Design System as shared UI foundation for all portals

**Status:** Accepted  
**Date:** 2026-03-30  
**Deciders:** Platform team

---

## Context

osm2 exposes two user-facing portals: `osm2-taxable-person-portal` (taxable persons and intermediaries) and `osm2-authority-portal` (Skatteforvaltningen caseworkers). Both portals are Spring Boot / Thymeleaf applications.

Both portals must:
- Visually align with the public skat.dk design language (colors, typography, spacing).
- Use the Academy Sans and Republic typefaces and the DAP icon font — all of which are licensed for use within Skatteforvaltningen systems.
- Meet WCAG 2.1 AA accessibility requirements.
- Share a single versioned source of design tokens so that a token change propagates uniformly across both portals with a single dependency bump.

Without a shared design artifact, each portal would independently copy CSS and font files — creating drift, duplication, and inconsistent WCAG compliance over time.

---

## Decision

Package the SKAT design tokens, typefaces (Academy Sans, Republic, DAP ikon), timeline component CSS, and HTMX accessibility helper as a self-contained **WebJar** (`dk.ufst.opendebt:skat-design-system:1.0.0`).

Both portals declare this as a Maven dependency. Spring Boot's default static resource handler serves WebJar contents at `/webjars/skat-design-system/**` automatically — no additional configuration is required.

Each portal's Thymeleaf base layout (`templates/layout/base.html`) references the WebJar:

```html
<link rel="stylesheet" th:href="@{/webjars/skat-design-system/css/skat-tokens.css}" />
<link rel="stylesheet" th:href="@{/webjars/skat-design-system/css/timeline.css}" />
<script th:src="@{/webjars/skat-design-system/js/a11y.js}" defer></script>
```

`webjars-locator-core` (transitive via `spring-boot-starter-web`) resolves version-free URLs — no version suffix in template paths.

---

## Consequences

**Positive:**
- Single point of update: bump `skat-design-system.version` in the parent `pom.xml` to roll out token or font changes to all portals simultaneously.
- Design consistency enforced structurally, not by convention.
- WCAG 2.1 AA compliance is the responsibility of the design system, not individual portal teams.
- No font or CSS files copied into portal source trees — reduces repository bloat.

**Negative / constraints:**
- Portal-specific styling must extend (not override) the design tokens to preserve consistency.
- Any deviation from the SKAT visual language requires a new ADR justifying the exception.

---

## Alternatives considered

| Option | Rejected because |
|---|---|
| Copy CSS/fonts into each portal's `static/` | Immediate drift; duplicate maintenance burden |
| NPM-based design system with Webpack | Incompatible with the Maven-only build pipeline; introduces a JS toolchain |
| Inline `<style>` blocks | Unmaintainable; defeats WCAG audit trail |
