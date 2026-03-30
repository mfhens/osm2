# SKAT Design System — WebJar

A self-contained WebJar that packages the SKAT design tokens, typefaces, and UI
helpers used by OpenDebt portals. Drop it into any Spring Boot project as a single
Maven dependency.

## Contents

| Path (under `/webjars/skat-design-system/`) | Description |
|---|---|
| `css/skat-tokens.css` | Design tokens — colors, spacing, typography, layout (≈ 32 KB) |
| `css/timeline.css` | Unified case timeline component (WCAG 2.1 AA) |
| `fonts/academy-sans/` | Academy Sans — 6 weights, woff + woff2 |
| `fonts/republic/` | Republic — 7 weights, woff + woff2 |
| `fonts/dap-ikon-font.*` | DAP icon font — ttf + woff |
| `js/a11y.js` | HTMX focus-management helper for screen readers |

## Installation

### 1. Build and install to local Maven repo

```bash
mvn install
```

### 2. Add dependency to your project's `pom.xml`

```xml
<dependency>
    <groupId>dk.ufst.opendebt</groupId>
    <artifactId>skat-design-system</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Allows version-free /webjars/ URLs -->
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>webjars-locator-core</artifactId>
</dependency>
```

> `webjars-locator-core` is included transitively by `spring-boot-starter-web` in
> Spring Boot 3.x — you probably already have it.

### 3. Reference in Thymeleaf templates

```html
<!-- In your layout fragment / base template -->
<link rel="stylesheet" th:href="@{/webjars/skat-design-system/css/skat-tokens.css}" />

<!-- Optional: timeline component (if used) -->
<link rel="stylesheet" th:href="@{/webjars/skat-design-system/css/timeline.css}" />

<!-- Optional: HTMX accessibility helper -->
<script th:src="@{/webjars/skat-design-system/js/a11y.js}" defer></script>
```

### 4. Using in plain HTML (no Thymeleaf)

```html
<link rel="stylesheet" href="/webjars/skat-design-system/css/skat-tokens.css" />
```

Spring Boot's default resource handler serves WebJar contents automatically.
No additional configuration required.

## Fonts

The fonts are referenced by relative URL inside `skat-tokens.css` (`../fonts/...`).
They are resolved automatically — no extra configuration needed.

Fonts included:

| Family | Weights | Format |
|---|---|---|
| Academy Sans | 400, 400i, 500, 600, 700, 900 | woff2 + woff |
| Republic | 300, 300i, 400, 400i, 600, 700, 900 | woff2 + woff |
| DAP Ikon | — | ttf + woff |

## Versioning

Update `<version>` in `pom.xml` when CSS or fonts change. Consumers pin to a specific
version in their own `pom.xml`.

## Source

Derived from the public skat.dk design language (SKAT — Danish Tax Authority).
Source: skat.dk/borger CSS custom properties, March 2026.
See OpenDebt ADR-0023 for the design rationale.
