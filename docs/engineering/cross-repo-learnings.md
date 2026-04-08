# Cross-repo learnings — osm2 and opendebt

osm2 and opendebt share the same technical stack and Beads-style delivery tracking. This note describes **how to share learnings** between the two without duplicating noise or letting tooling drift silently.

## Goals

1. **Single source of truth for decisions** — Use the same ADR shape and numbering discipline. When one repo learns something (for example Catala module `Using` vs raw `Include`, Spotless scope, or Beads comment conventions), the other adopts the **pattern**, not a one-off copy without context.

2. **Traceability** — Every ported pattern should be linkable: Beads issue → pull request → short note. Beads is the natural place for `LEARNED` or `NOTE` when you promote a fix from one repo to the other.

3. **Low ceremony** — Prefer a light recurring check over a heavy standing “sync committee.”

## Mechanisms

| Mechanism | Practice |
|-----------|----------|
| **Beads conventions** | Align epic prefixes, status workflow, and cross-links (for example “ported from `org/osm2#123`”). Use the same **comment taxonomy** (`LEARNED`, `BLOCKED`, `DECISION`) in both backlogs so searches behave the same. |
| **Pattern issues** | When CI or tooling teaches something non-obvious, open or tag a **meta** issue in each repo, or designate one repo as canonical and keep a **mirror checklist** in the other. Close when both repos have the same guardrail (workflow line, script, ADR pointer). |
| **ADR alignment** | Keep section structure parallel where it helps (for example “Tooling”, “CI gates”). For shared topics (Catala, C4, mise), add a one-line **sibling reference** — “See opendebt ADR-00xx” — instead of duplicating full prose. |
| **CI and mise** | Diff `.github/workflows` and pinned tool versions (`.mise.toml`, parent POMs) on a schedule or when one repo bumps a pin; record **why** in Beads or a small `CHANGELOG-tooling.md` in each repo if you need history. |
| **Scripts** | If you mirror automation (for example Basecamp or backlog scripts), declare **which repo is canonical** and copy subtree, submodule, or copy-with-header from there; document the rule in README or an ADR. |

## Where to store shared “playbook” prose

- **In-repo** (this document) works when osm2 is the natural owner of delivery docs.
- A **tiny dedicated repo** (`delivery-playbooks`, `engineering-notes`) works if neither product repo should own the text.
- A **neutral third repo** (for example a personal clone used only for notes) is fine if the team agrees; avoid pushing internal process docs to unrelated open-source upstreams unless that project explicitly hosts them.

Pick one canonical place and link to it from the other repo’s README or ADR index.

## Cadence (usually enough)

- **Monthly, 15–30 minutes:** Diff tooling pins and workflows; open or refresh Beads items tagged `tooling` / `parity` where gaps appear.
- **After every painful CI lesson:** One Beads `LEARNED` on the relevant epic; amend ADRs in **both** repos when the rule is stable and product-specific.

## Anti-patterns

- Silently forking workflows between repos without a Beads or ADR trail.
- Duplicating long ADR bodies in two places — prefer one owner plus a short pointer.
- Porting a script without noting which repo is authoritative for the next change.

## Related

- [ADR-0032 — Catala as formal oracle (CI)](../../architecture/adr/0032-catala-oracle-only.md) (example of a gate both stacks can mirror)
- [Basecamp integration](../integrations/basecamp/README.md) (example of scripted integration that may be shared)
