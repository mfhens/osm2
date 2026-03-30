# keycloak-oauth2-starter

Spring Boot auto-configuration starter for Keycloak OAuth2 resource server. Replaces the repeated `SecurityConfig` boilerplate present in every OpenDebt API service.

## What it provides

Two profile-gated `SecurityFilterChain` beans:

| Bean name | Active profile | Behaviour |
|---|---|---|
| `keycloakSecuredFilterChain` | `!local & !dev & !demo` | Stateless JWT resource server; CSRF disabled; actuator + Swagger UI permitted without auth |
| `keycloakPermissiveFilterChain` | `local \| dev \| demo` | Permits all requests — no auth required for local development |

## Coordinates

```xml
<dependency>
    <groupId>dk.ufst</groupId>
    <artifactId>keycloak-oauth2-starter</artifactId>
    <version>1.0</version>
</dependency>
```

## Configuration

```yaml
keycloak:
  starter:
    permitted-paths:
      - /actuator/**
      - /api-docs/**
      - /swagger-ui/**
      - /swagger-ui.html
```

Override `permitted-paths` to add or replace the default open URL patterns.

## Overriding the filter chain

Use `@ConditionalOnMissingBean` back-off — declare your own bean with the same name to take precedence:

```java
@Bean("keycloakSecuredFilterChain")
public SecurityFilterChain myCustomChain(HttpSecurity http) throws Exception {
    // custom logic
}
```

## Method security

This starter does **not** enable `@EnableMethodSecurity`. If a service needs `@PreAuthorize` / `@PostAuthorize`, declare a separate `MethodSecurityConfig`:

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {}
```

## What is NOT in this starter

- `AuthContext` — domain-coupled, stays in `opendebt-common`
- Portal `SecurityConfig` — portals use OAuth2 client (browser SSO), a completely different pattern
- Role constants — define these in the consuming service or `opendebt-common`
