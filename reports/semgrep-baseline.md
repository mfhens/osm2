# Semgrep Baseline Scan — osm2
Generated: 2026-04-01
Rulesets: p/java (105 rules), p/secrets, magic-strings (local)
Files scanned: 305

## Summary

| Category | Count |
|---|---|
| p/java + p/secrets findings | 0 |
| Magic string hits (all occurrences) | 18 |
| Magic string candidates (>=3 occurrences) | 0 |

## p/java + p/secrets: CLEAN

0 findings. No security issues, no secrets detected in source files.

## Magic String Analysis

No literal reaches the 3-repetition threshold. The codebase uses enums and constants well.

| Literal | Count | Location | Assessment |
|---|---|---|---|
| "review" | 2 | ReviewController.java:82,122 | Two return branches — use ReviewStatus enum |
| "PENDING_VAT_NUMBER" | 1 | RegistrationService.java:127 | Use RegistrantStatus.PENDING_VAT_NUMBER.name() |
| "ACTIVE" | 1 | RegistrationService.java:133 | Use RegistrantStatus.ACTIVE.name() |
| "anonymous" | 1 | AuditContextService.java:72 | Audit fallback — extract to local constant |
| "SERVICES" | 1 | Step1Form.java:16 | Confirm SupplyType enum planned |
| demo-* (5x) | 1 each | DemoConstants.java | Already in constants file — OK |
| VAT test numbers (4x) | 1 each | DemoConstants.java | Test data — OK |

## Action Items

1. RegistrationService.java:127,133 — replace "PENDING_VAT_NUMBER" and "ACTIVE" string literals
   with RegistrantStatus enum references (enum exists; this is a 1-line fix each).

2. ReviewController.java:82,122 — "review" returned from two branches; extract to constant
   or use a ReviewStatus enum value.

3. Step1Form.java:16 — "SERVICES" supply type string; align with levering concept model
   once SupplyType enum is introduced in osm2-return-service.

## Re-running

  docker pull semgrep/semgrep  (already cached)
  pwsh semgrep-report.ps1

Or manually:
  docker run --rm -v .:/src semgrep/semgrep semgrep \
    --config /src/config/semgrep/p-java.yaml \
    --config /src/config/semgrep/p-secrets.yaml \
    --config /src/config/semgrep/magic-strings.yaml \
    --exclude target --exclude config/semgrep /src
