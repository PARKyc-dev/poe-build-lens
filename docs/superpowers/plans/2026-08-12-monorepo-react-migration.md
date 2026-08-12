# PoE Lens Monorepo and React Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the existing Spring Boot application into `api/`, replace its static page with a Vite React TypeScript app in `web/`, and reserve `worker/` for later PoB engine work without changing the analysis API.

**Architecture:** The repository root owns shared instructions and project memory. `api/` remains the complete Spring Boot Gradle project, while `web/` is an independently built React app that calls the unchanged relative endpoint `/api/analyses`; Vite proxies that route to port 8080 during development. `worker/` contains documentation only.

**Tech Stack:** Java 21, Spring Boot 4.1, Gradle, React 19.2, TypeScript 7.0, Vite 8.2, Vitest 4.1, Testing Library, npm.

## Global Constraints

- Preserve `POST /api/analyses`, its request body, and its success and error JSON contracts.
- Preserve the existing Java package structure, H2 configuration, catalog data, and analysis behavior.
- Do not add Docker, a production reverse proxy, a PoB runtime, queues, caches, authentication, or new analysis features.
- `worker/` must contain only a short reservation document.
- Do not modify or remove unrelated user changes or untracked files.
- Run the complete API test suite and create the Spring Boot JAR before reporting completion.

---

### Task 1: Move the Spring Boot project behind the `api/` boundary

**Files:**
- Move: `build.gradle` → `api/build.gradle`
- Move: `settings.gradle` → `api/settings.gradle`
- Move: `gradlew` → `api/gradlew`
- Move: `gradlew.bat` → `api/gradlew.bat`
- Move: `gradle/wrapper/gradle-wrapper.jar` → `api/gradle/wrapper/gradle-wrapper.jar`
- Move: `gradle/wrapper/gradle-wrapper.properties` → `api/gradle/wrapper/gradle-wrapper.properties`
- Move: `src/main/**` → `api/src/main/**`
- Move: `src/test/**` → `api/src/test/**`
- Remove through the move: `api/src/main/resources/static/index.html`

**Interfaces:**
- Consumes: the existing Gradle wrapper and Spring Boot source tree.
- Produces: an independently testable Spring project invoked with `cd api && ./gradlew ...`, still serving `POST /api/analyses`.

- [ ] **Step 1: Establish the pre-move behavior baseline**

Run:

```bash
./gradlew test --no-daemon
./gradlew bootJar --no-daemon
```

Expected: both commands exit with status 0 and `build/libs/poe-lens-0.0.1-SNAPSHOT.jar` exists.

- [ ] **Step 2: Move only tracked Spring and Gradle files**

Create `api/`, move the exact files listed above with Git-aware moves, and leave unrelated files such as `.DS_Store` untouched. Move `src/main` and `src/test` separately so an unrelated untracked `src/.DS_Store` is not moved or deleted.

- [ ] **Step 3: Remove the obsolete static page from the API**

Delete `api/src/main/resources/static/index.html`. The React app in Task 2 becomes the only user interface; no replacement static resource belongs in the API.

- [ ] **Step 4: Verify the project from its new boundary**

Run:

```bash
cd api
./gradlew test --no-daemon
./gradlew bootJar --no-daemon
```

Expected: all existing tests pass and `api/build/libs/poe-lens-0.0.1-SNAPSHOT.jar` is created.

- [ ] **Step 5: Commit the boundary move**

```bash
git add api build.gradle settings.gradle gradlew gradlew.bat gradle src/main src/test
git commit -m "refactor: Spring API를 api 디렉터리로 이동"
```

### Task 2: Build the typed React analysis flow test-first

**Files:**
- Create: `web/package.json`
- Create: `web/package-lock.json`
- Create: `web/index.html`
- Create: `web/tsconfig.json`
- Create: `web/tsconfig.app.json`
- Create: `web/tsconfig.node.json`
- Create: `web/vite.config.ts`
- Create: `web/src/api/analysis.ts`
- Create: `web/src/App.test.tsx`
- Create: `web/src/App.tsx`
- Create: `web/src/main.tsx`
- Create: `web/src/styles.css`
- Create: `web/src/test/setup.ts`

**Interfaces:**
- Consumes: `POST /api/analyses` with `{ pobInput: string }` and the existing `code`, `message`, `returnObject` response envelope.
- Produces: `analyzeBuild(pobInput: string): Promise<AnalysisResult>` and a React `App` component that renders the existing analysis sections.

- [ ] **Step 1: Create the minimal Vite and test configuration**

Define npm scripts `dev`, `build`, and `test`. Pin these dependencies: `react` and `react-dom` 19.2.8; TypeScript 7.0.2, Vite 8.2.1, `@vitejs/plugin-react` 6.0.5, Vitest 4.1.10, Testing Library React 16.3.2, Testing Library jest-dom 7.0.1, user-event 14.6.4, jsdom 30.0.1, `@types/react` 19.2.18, and `@types/react-dom` 19.2.4. Configure Vitest with jsdom and `src/test/setup.ts`, import `@testing-library/jest-dom/vitest` from that setup file, and configure Vite to proxy `/api` to `http://localhost:8080`.

- [ ] **Step 2: Write failing user-flow tests**

In `App.test.tsx`, mock `fetch` and verify these exact behaviors:

```tsx
it('submits a PoB build and renders the analysis', async () => {
  fetchMock.mockResolvedValue(response({
    code: 'OK',
    message: 'SUCCESS',
    returnObject: analysisResult,
  }))

  render(<App />)
  await userEvent.type(screen.getByLabelText('Path of Building export'), '<PathOfBuilding />')
  await userEvent.click(screen.getByRole('button', { name: 'Analyze build' }))

  expect(fetchMock).toHaveBeenCalledWith('/api/analyses', expect.objectContaining({ method: 'POST' }))
  expect(await screen.findByText('Level 90 Witch using Fireball')).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: 'Core mechanics' })).toBeInTheDocument()
})

it('shows the API error message', async () => {
  fetchMock.mockResolvedValue(response({ code: 'INVALID_POB_INPUT', message: 'Invalid PoB input.' }, 400))

  render(<App />)
  await userEvent.click(screen.getByRole('button', { name: 'Analyze build' }))

  expect(await screen.findByRole('alert')).toHaveTextContent('Invalid PoB input.')
})

it('prevents duplicate submissions while analysis is pending', async () => {
  fetchMock.mockReturnValue(new Promise(() => {}))

  render(<App />)
  await userEvent.click(screen.getByRole('button', { name: 'Analyze build' }))

  expect(screen.getByRole('button', { name: 'Analyzing…' })).toBeDisabled()
})
```

- [ ] **Step 3: Run the focused tests and verify the red state**

Run:

```bash
cd web
npm test -- --run src/App.test.tsx
```

Expected: FAIL because `App`, API types, and the analysis implementation do not exist yet.

- [ ] **Step 4: Implement the typed API client**

Define `AnalysisResult`, `Mechanic`, `Evidence`, `ApiSuccess<T>`, and `ApiError` using the exact property names currently returned by Spring. Implement `analyzeBuild` with JSON headers and body. For a non-OK HTTP response, throw an `Error` using `message`; for network or invalid JSON failures without an API message, throw `Error('Unable to connect to the analysis API.')`.

- [ ] **Step 5: Implement the minimal React page**

Build a single accessible page with a labelled textarea, an `Analyze build` button, an `aria-live` result region, and reusable section rendering inside `App.tsx`. Render overview and game version plus `Core mechanics`, `Contributors`, `Defence`, `Resource sustain`, `Unverified`, and `Evidence`. Disable the button and change its label to `Analyzing…` during an active request.

- [ ] **Step 6: Make the focused tests green**

Run:

```bash
cd web
npm test -- --run src/App.test.tsx
```

Expected: all three user-flow tests pass.

- [ ] **Step 7: Verify types and production output**

Run:

```bash
cd web
npm run build
npm test -- --run
```

Expected: TypeScript and Vite build succeed, and the complete web test suite passes.

- [ ] **Step 8: Commit the React application**

```bash
git add web
git commit -m "feat: React 분석 화면 추가"
```

### Task 3: Reserve the worker boundary and align the repository harness

**Files:**
- Create: `worker/README.md`
- Create: `memory/decisions/003-monorepo-application-boundaries.md`
- Modify: `AGENTS.md`
- Modify: `README.md`
- Modify: `memory/active/2026-08-12-monorepo-react-migration.md`
- Modify: `memory/INDEX.md`
- Move: `memory/active/2026-08-12-monorepo-react-migration.md` → `memory/completed/2026-08-12-monorepo-react-migration.md`

**Interfaces:**
- Consumes: the completed `web` and `api` boundaries.
- Produces: discoverable commands and durable records that describe the monorepo without adding runtime behavior.

- [ ] **Step 1: Document the worker reservation and architectural decision**

State in `worker/README.md` that the directory is reserved for later PoB engine process management and intentionally contains no runtime. Record in decision 003 that a single repository owns `web`, `api`, and a reserved `worker` because they form one product, while each application remains independently executable and testable.

- [ ] **Step 2: Update root instructions and README**

Replace root-only Gradle commands with:

```text
API test: cd api && ./gradlew test --no-daemon
API package: cd api && ./gradlew bootJar --no-daemon
Web development: cd web && npm run dev
Web verification: cd web && npm test -- --run && npm run build
```

Explain that API changes must preserve or deliberately update the typed contract in `web/src/api/analysis.ts`. Document starting the API and web development servers in separate terminals and opening the Vite URL.

- [ ] **Step 3: Perform the integrated development smoke test**

Start Spring Boot on port 8080 and Vite on port 5173. Send a valid raw PoB XML request through `http://localhost:5173/api/analyses` and verify HTTP 200 plus `code: "OK"` and the expected overview. Stop both processes after the check.

- [ ] **Step 4: Run final verification from clean commands**

Run:

```bash
cd api && ./gradlew test --no-daemon && ./gradlew bootJar --no-daemon
cd web && npm test -- --run && npm run build
```

Expected: every command exits with status 0. Confirm no tracked Spring source or Gradle project file remains at the repository root, and `worker/` contains only `README.md`.

- [ ] **Step 5: Complete the project work record**

Record the exact commands and successful results, move the active record to `memory/completed`, update `memory/INDEX.md`, and leave unrelated user changes untouched.

- [ ] **Step 6: Commit the harness and completion record**

```bash
git add AGENTS.md README.md worker/README.md memory/INDEX.md memory/completed/2026-08-12-monorepo-react-migration.md memory/decisions/003-monorepo-application-boundaries.md
git commit -m "docs: 모노레포 작업 흐름 갱신"
```
