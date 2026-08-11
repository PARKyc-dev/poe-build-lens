# Current state

- Goal: PoE 1 Path of Building build mechanism analyser with a public README that documents the AI-evaluation purpose and current scope.
- Current implementation: raw PoB XML input, a JPA-backed local 3.27 mechanics catalog, and a single analysis page. Analysis code is separated into controller, service, parser, catalog, entity, repository, model, and exception packages.
- Verification: README content and Markdown structure were checked against the current application source and `WORKFLOW.md` on 2026-08-11. `./gradlew test --no-daemon` and `./gradlew bootJar --no-daemon` passed on 2026-08-11 before these documentation-only changes.
- Next: add reviewed catalog entries and support additional PoB export formats.
- Blockers: the initial catalog contains Fireball and Arc seed entries; unlisted mechanics are explicitly reported as unverified.
