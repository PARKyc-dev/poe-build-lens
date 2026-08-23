# Memory Directory Maintenance Plan

> **For Codex:** Execute this plan directly in the current repository.  
> Preserve existing information conservatively. Do not delete historical records unless they are clearly redundant, obsolete, or safely summarized elsewhere.

## Goal

Restructure the project's `memory/` directory so that:

- current work context stays small and easy to load;
- durable project knowledge is retained permanently;
- important architectural/technical decisions are preserved with rationale;
- historical work logs remain searchable without being loaded by default;
- duplicated, noisy, low-value session history does not accumulate indefinitely.

## Core Principle

Use this rule when deciding what survives long-term:

> If a new Codex session three months from now could make a wrong decision because it does not know this information, preserve it as long-term memory.

Also apply:

> Preserve conclusions, rationale, constraints, and reusable knowledge.  
> Do not preserve every intermediate step, command output, temporary error, or completed task.

Git history is the source of truth for file-level change history.  
`memory/` should preserve context that Git cannot explain well: intent, rationale, constraints, conventions, decisions, domain knowledge, and current work state.

---

# Target Structure

Restructure `memory/` toward the following structure:

```text
memory/
├── README.md
├── active/
│   ├── current-context.md
│   └── current-decisions.md
├── knowledge/
│   ├── architecture.md
│   ├── conventions.md
│   ├── domain.md
│   └── infrastructure.md
├── decisions/
│   └── ADR-XXX-*.md
└── archive/
    └── YYYY-MM.md
```

Do not create empty files only to satisfy this tree.  
Create a file when there is actual information to place in it.

---

# Classification Rules

## 1. `active/`

Contains only information required for the next few development sessions.

Examples:

- currently implemented feature;
- current problem or blocker;
- unresolved design question;
- immediate next tasks;
- temporary decision that is still under evaluation;
- work that has started but is not complete.

Do not keep completed work here.

Target size:

- keep `current-context.md` concise;
- prefer fewer than roughly 300 lines;
- if it grows significantly beyond that, consolidate or move information elsewhere.

### `active/current-context.md`

Recommended sections:

```markdown
# Current Context

## Current Goal

## In Progress

## Blockers

## Next Actions

## Relevant Files
```

### `active/current-decisions.md`

Use only for decisions that are currently active but not yet durable enough for an ADR.

Recommended format:

```markdown
# Current Decisions

## <Decision>

- Status:
- Context:
- Current choice:
- Reason:
- Revisit when:
```

Once a decision becomes stable and significant, migrate it to `decisions/`.

---

## 2. `knowledge/`

Contains durable project knowledge that remains useful across many sessions.

Store information here when it describes what the project is or how the project is expected to work.

### `knowledge/architecture.md`

Examples:

- system boundaries;
- module responsibilities;
- backend/web/worker relationships;
- important request/data flows;
- major infrastructure topology;
- external service integration structure.

### `knowledge/conventions.md`

Examples:

- package conventions;
- naming rules;
- API conventions;
- error handling rules;
- testing conventions;
- Entity/DTO handling rules;
- repository/service/domain boundaries;
- project-specific coding rules.

### `knowledge/domain.md`

Examples:

- domain terminology;
- aggregate definitions;
- invariants;
- business rules;
- domain relationships;
- concepts that are easy for an AI session to misunderstand.

### `knowledge/infrastructure.md`

Examples:

- Docker layout;
- deployment topology;
- Redis usage rules;
- database assumptions;
- environment conventions;
- CI/CD structure;
- reverse proxy or networking assumptions.

Do not store chronological work logs in `knowledge/`.

---

## 3. `decisions/`

Contains durable and meaningful design decisions.

Use ADR-style records for decisions with meaningful trade-offs.

Create an ADR when at least one of these applies:

- changing the decision later would have a significant cost;
- multiple valid alternatives existed;
- future developers could reasonably question why this choice was made;
- the decision changes architecture, persistence, concurrency, API contracts, deployment, or domain modeling;
- losing the rationale could cause the same debate to be repeated.

Do not create ADRs for trivial implementation details.

### ADR naming

```text
ADR-001-<short-kebab-case-title>.md
ADR-002-<short-kebab-case-title>.md
```

Continue numbering from existing ADRs if any exist.

### ADR template

```markdown
# ADR-XXX: <Title>

## Status

Accepted

## Context

Describe the problem and constraints.

## Decision

Describe the selected approach.

## Alternatives

- Alternative A
- Alternative B

## Rationale

Explain why the selected approach was chosen.

## Consequences

### Positive

- ...

### Negative

- ...

## Revisit When

Describe concrete conditions that would justify revisiting this decision.
```

Allowed status values:

- Proposed
- Accepted
- Superseded
- Deprecated

If a decision is replaced, prefer marking the old ADR `Superseded` rather than deleting it.

---

## 4. `archive/`

Contains historical information that may still be useful but should not be loaded by default.

Prefer monthly files:

```text
archive/2026-08.md
archive/2026-09.md
```

Archive entries should be concise summaries, not raw transcripts.

Recommended format:

```markdown
# 2026-08

## <Feature or Work Area>

- Goal:
- Result:
- Important changes:
- Remaining concerns:
- Related ADRs:
- Related knowledge:
```

Historical information may stay here indefinitely if it is compact and useful for traceability.

---

# Information Retention Matrix

Use the following classification rules.

| Information | Destination |
|---|---|
| Current feature being implemented | `active/` |
| Immediate next tasks | `active/` |
| Current blocker | `active/` |
| Stable architecture | `knowledge/architecture.md` |
| Project coding rules | `knowledge/conventions.md` |
| Domain definitions and invariants | `knowledge/domain.md` |
| Deployment/Redis/DB conventions | `knowledge/infrastructure.md` |
| Important technical decision with rationale | `decisions/` |
| Completed feature summary | `archive/` |
| Temporary debugging log | remove after extracting useful conclusion |
| Raw command output | remove unless it documents an exceptional incident |
| Simple typo or formatting fix | remove |
| Repeated build success logs | remove |
| Detailed file-change history | rely on Git instead |
| Important root cause from an incident | promote to `knowledge/` or `decisions/` |
| Superseded architectural decision | retain ADR and mark `Superseded` |

---

# Cleanup Decision Rules

When examining every existing memory entry, classify it into one of four categories.

## KEEP

Keep in place or migrate to `knowledge/` / `decisions/` when:

- it is still correct;
- it is likely to matter months later;
- it defines architecture, domain rules, conventions, constraints, or rationale.

## ACTIVE

Move to `active/` when:

- work is not finished;
- it affects the next development session;
- the information is expected to change soon.

## ARCHIVE

Move or summarize into `archive/YYYY-MM.md` when:

- the work is complete;
- history may still be useful;
- the information does not need to be injected into every session.

## REMOVE

Remove only when all of the following are true:

- the information is no longer needed;
- it is not an important historical decision;
- Git or another durable source already records anything worth keeping;
- there is no unique rationale, constraint, incident lesson, or domain knowledge inside it.

When uncertain between `ARCHIVE` and `REMOVE`, choose `ARCHIVE`.

---

# Execution Plan

## Task 1: Inspect the Current Memory Directory

- [ ] List every file under `memory/`.
- [ ] Read the existing memory files before modifying them.
- [ ] Identify any files outside `memory/` that explicitly instruct Codex how memory should be managed, such as:
  - `AGENTS.md`
  - `WORKFLOW.md`
  - `README.md`
  - Codex-specific instruction files.
- [ ] Do not change application source code during this task.
- [ ] Build an internal classification of existing entries as:
  - ACTIVE
  - KNOWLEDGE
  - DECISION
  - ARCHIVE
  - REMOVE
- [ ] Before removing anything, confirm that any unique conclusion or rationale has been migrated somewhere durable.

Deliverable: complete understanding of the existing memory system without data loss.

---

## Task 2: Create the Memory Index and Operating Rules

Create or rewrite:

```text
memory/README.md
```

Use the following content as the baseline, adapting only where the repository already has stronger project-specific rules:

```markdown
# Project Memory

This directory stores durable context for Codex and future development sessions.

It is not a replacement for Git history.

## Reading Order

Read memory in this order:

1. `active/current-context.md`
2. `active/current-decisions.md`, if present
3. relevant files under `knowledge/`
4. relevant ADRs under `decisions/`

Do not read all of `archive/` by default.

Search `archive/` only when historical context is explicitly needed.

## Storage Rules

### Active

Store short-lived context needed for current work.

Remove completed work from `active/`.

### Knowledge

Store durable facts, architecture, conventions, domain rules, and infrastructure assumptions.

Do not store chronological logs here.

### Decisions

Store significant design decisions and their rationale as ADRs.

Do not delete superseded ADRs. Mark them as superseded.

### Archive

Store concise summaries of completed work for historical traceability.

Do not store raw session transcripts unless they contain information that cannot be reconstructed elsewhere.

## Retention Principle

Preserve information when a future development session could make a wrong decision without it.

Prefer conclusions and rationale over detailed process logs.

Git is responsible for file-level implementation history.
Memory is responsible for intent, constraints, decisions, and reusable project context.

## Cleanup Rules

During cleanup:

1. remove completed items from `active/`;
2. promote durable knowledge into `knowledge/`;
3. promote significant stable decisions into `decisions/`;
4. summarize completed work into `archive/`;
5. remove duplicate and low-value logs only after preserving unique information;
6. never delete uncertain historical information when it can be safely archived instead.
```

- [ ] Ensure the README matches the actual directory names used after migration.
- [ ] Avoid instructions requiring Codex to load the entire memory directory every session.

Deliverable: a stable contract describing how future agents use memory.

---

## Task 3: Build `active/`

- [ ] Create `memory/active/` if missing.
- [ ] Create `current-context.md` from currently unfinished work.
- [ ] Create `current-decisions.md` only if unresolved or temporary decisions exist.
- [ ] Remove completed work from active context.
- [ ] Do not copy full historical logs into these files.
- [ ] Ensure every item in `active/` has immediate relevance.

Use this structure for `current-context.md`:

```markdown
# Current Context

## Current Goal

Describe the current development objective.

## In Progress

- ...

## Blockers

- ...

## Next Actions

1. ...
2. ...

## Relevant Files

- `path/to/file`
```

If no blockers exist, write:

```markdown
## Blockers

None.
```

Deliverable: a small, immediately useful working memory.

---

## Task 4: Consolidate Durable Knowledge

- [ ] Create `memory/knowledge/` if missing.
- [ ] Extract durable facts from existing memory.
- [ ] Merge duplicate statements rather than copying them repeatedly.
- [ ] Resolve contradictions using the newest clearly valid project state.
- [ ] If two conflicting statements cannot be safely resolved, preserve the conflict explicitly rather than guessing.
- [ ] Organize knowledge by responsibility, using only files that have meaningful content.

Preferred files:

```text
knowledge/architecture.md
knowledge/conventions.md
knowledge/domain.md
knowledge/infrastructure.md
```

Each file should:

- describe current truth rather than chronological history;
- avoid raw session notes;
- avoid completed TODOs;
- contain enough rationale where rules would otherwise appear arbitrary.

Deliverable: a compact project knowledge base representing the current state.

---

## Task 5: Extract Important Decisions into ADRs

- [ ] Search existing memory for decisions involving:
  - architecture;
  - DDD boundaries;
  - persistence;
  - JPA strategy;
  - Redis;
  - concurrency control;
  - locking;
  - API contracts;
  - module boundaries;
  - infrastructure;
  - deployment;
  - external integrations.
- [ ] Create ADRs only for decisions that meet the ADR criteria defined above.
- [ ] Preserve known alternatives and rationale.
- [ ] Do not fabricate rationale that is absent from the existing records.
- [ ] If rationale is unknown, state only what can be established from the repository/memory.
- [ ] Mark replaced decisions as `Superseded` instead of deleting them.

Deliverable: durable decision history that explains why important choices exist.

---

## Task 6: Archive Completed Work

- [ ] Create `memory/archive/` if missing.
- [ ] Group completed historical work by month when dates can be established.
- [ ] If an exact date cannot be determined safely, do not invent one.
- [ ] Consolidate repetitive logs into short feature-level summaries.
- [ ] Preserve:
  - goal;
  - final result;
  - significant issue/root cause;
  - meaningful remaining concern;
  - related ADR/knowledge references.
- [ ] Drop noise such as:
  - repeated commands;
  - repeated build/test success;
  - trial-and-error steps whose conclusion is already retained;
  - routine file modifications visible in Git.

Example transformation:

Before:

```text
RedisTemplate serialization error
changed Jackson config
still error
tested GenericJackson2JsonRedisSerializer
changed to StringRedisTemplate
worked
```

After durable extraction:

```markdown
## Redis Serialization

The project uses `StringRedisTemplate` for string/counter-oriented Redis data.
Generic object serialization was avoided because it introduced unnecessary serialization configuration complexity.
```

Place the durable rule in `knowledge/infrastructure.md`.

If historical context still matters, archive only:

```markdown
## Redis Integration Cleanup

- Result: standardized simple Redis access on `StringRedisTemplate`.
- Durable rule: see `../knowledge/infrastructure.md`.
```

Deliverable: searchable history without session-level noise.

---

## Task 7: Remove Redundancy Conservatively

Only perform this task after Tasks 3–6 are complete.

- [ ] Find duplicate content now represented in `active/`, `knowledge/`, `decisions/`, or `archive/`.
- [ ] Remove obsolete duplicate files when their useful information has been migrated.
- [ ] Remove raw logs that contain no additional durable information.
- [ ] Do not remove files solely because they are old.
- [ ] Do not delete an ADR because it has been superseded.
- [ ] When uncertain, keep or archive.

Before deleting a file, check:

```text
1. Is its current state represented elsewhere?
2. Is its rationale represented elsewhere?
3. Is its historical significance represented elsewhere?
4. Can Git reconstruct everything else?
```

Delete only if all four answers are yes.

Deliverable: reduced duplication with no meaningful context loss.

---

## Task 8: Update Project-Level Codex Instructions

If the repository has a project instruction file such as `AGENTS.md` or `WORKFLOW.md`, update its memory section to reference `memory/README.md`.

Keep the project-level rule concise.

Recommended rule:

```markdown
## Memory

Use `memory/README.md` as the source of truth for project memory management.

At the start of a task, read the relevant active context and only the knowledge/decision files needed for that task.

Do not load `memory/archive/` by default.

At the end of meaningful work:

1. update active context;
2. promote durable knowledge when necessary;
3. create/update ADRs for significant decisions;
4. archive completed context;
5. remove completed items from active memory.
```

Do not duplicate the entire `memory/README.md` into project-level instructions.

Deliverable: Codex knows how to maintain memory automatically going forward.

---

# Recurring Maintenance Policy

After the initial migration, use two cleanup levels.

## End-of-Task Maintenance

At the end of meaningful development work:

- [ ] update `active/current-context.md`;
- [ ] remove completed tasks from active context;
- [ ] promote reusable discoveries to `knowledge/`;
- [ ] create or update an ADR when a significant decision became stable;
- [ ] archive important completed work;
- [ ] avoid recording routine file changes already visible in Git.

This should be lightweight.

---

## Periodic Memory Cleanup

Perform periodically, approximately monthly or whenever `memory/` becomes difficult to navigate.

During periodic cleanup:

- [ ] review all `active/` entries;
- [ ] remove completed/stale active context;
- [ ] inspect recent archive entries for durable knowledge worth promoting;
- [ ] merge duplicate knowledge;
- [ ] identify obsolete knowledge;
- [ ] update obsolete knowledge to current truth;
- [ ] preserve historical decisions through ADR status changes;
- [ ] compact verbose archive entries;
- [ ] remove low-value logs after confirming no unique information remains;
- [ ] verify `memory/README.md` still matches actual behavior.

---

# Permanent Retention Criteria

Prefer permanent retention when information falls into any of these categories:

1. **Architecture**
   - module boundaries;
   - system topology;
   - dependency direction;
   - integration design.

2. **Domain knowledge**
   - business terminology;
   - invariants;
   - aggregate boundaries;
   - non-obvious business rules.

3. **Project conventions**
   - patterns Codex must consistently follow;
   - naming conventions;
   - package responsibilities;
   - test conventions.

4. **Constraints**
   - compatibility requirements;
   - deployment limitations;
   - environment limitations;
   - external API limitations.

5. **Important decisions**
   - selected design;
   - alternatives;
   - rationale;
   - trade-offs.

6. **Important incident lessons**
   - root causes that could recur;
   - non-obvious operational hazards;
   - permanent preventative rules.

Do not retain permanently merely because something required significant effort at the time.

---

# What Not to Preserve as Permanent Memory

Usually do not preserve:

- every command that was run;
- normal build/test output;
- temporary compiler errors;
- simple syntax mistakes;
- raw stack traces after resolution;
- completed checklists;
- exact lists of changed files;
- routine Git information;
- exploratory dead ends with no reusable lesson;
- repeated statements already represented in a canonical knowledge file.

---

# Canonical Source Rule

Avoid storing the same fact in multiple places.

Prefer one canonical location:

```text
Current temporary state     -> active/
Current durable truth       -> knowledge/
Decision + rationale        -> decisions/
Past completed work         -> archive/
Implementation diff/history -> Git
```

Other files should link to the canonical record instead of duplicating it.

---

# Verification

After migration, verify the following.

- [ ] `memory/README.md` exists and explains the reading order.
- [ ] `active/` contains only unfinished/recent context.
- [ ] completed work is not left in active memory.
- [ ] durable project facts are represented in `knowledge/`.
- [ ] significant decisions have rationale preserved in ADRs.
- [ ] historical work is searchable under `archive/`.
- [ ] archive files are not required reading for every session.
- [ ] no clearly valuable unique information was deleted.
- [ ] duplicate raw logs have been reduced.
- [ ] project-level Codex instructions point to `memory/README.md`.
- [ ] the resulting memory structure is significantly easier to navigate than before.

Run:

```bash
find memory -maxdepth 3 -type f | sort
```

Review the output manually.

Then run:

```bash
git diff -- memory
```

Also inspect any modified project-level instruction file:

```bash
git diff -- AGENTS.md WORKFLOW.md README.md 2>/dev/null || true
```

Before finishing, ensure no application source code changed accidentally:

```bash
git status --short
```

---

# Final Report

When execution is complete, report:

```markdown
## Memory Cleanup Result

### Created
- ...

### Consolidated
- ...

### Archived
- ...

### Removed
- ...

### ADRs Created/Updated
- ...

### Important Knowledge Preserved
- ...

### Remaining Ambiguities
- ...

### Recommended Next Cleanup
- approximately one month from now, or earlier if active memory becomes noisy
```

If anything was intentionally not deleted because its value was uncertain, mention it under `Remaining Ambiguities`.

---

# Safety Constraints

- Never fabricate project history.
- Never fabricate ADR rationale.
- Never invent dates for historical entries.
- Never delete uncertain information merely to reduce file count.
- Prefer archival over deletion.
- Preserve semantic information before restructuring files.
- Do not alter application behavior as part of this memory cleanup.
- Do not make unrelated refactors.
- Use Git history as support when necessary, but do not turn memory into a duplicate Git log.
