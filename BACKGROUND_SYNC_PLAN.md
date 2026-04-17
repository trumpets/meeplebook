# Background Sync Plan

## Goal
Introduce lifecycle-safe background sync built on top of the app's existing offline-first data layer
without doing a large, hard-to-review rewrite.

The plan below is intentionally split into reviewable increments so each execution prompt can focus
on one small architectural slice.

## Current codebase baseline

### What already exists
- Room is already the app's source of truth for collection and plays content.
- `CollectionRepositoryImpl` and `PlaysRepositoryImpl` already own remote fetch + local persistence.
- `PlayEntity` / `PlaySyncStatus` already model local-vs-remote play sync status.
- `core/sync/domain/` already has foreground sync use cases:
  - `SyncCollectionUseCase`
  - `SyncPlaysUseCase`
  - `SyncUserDataUseCase`
- `SyncTimeRepository` already persists **timestamps** in DataStore:
  - collection sync time
  - plays sync time
  - full sync time
- UI already observes local DB-backed flows and refreshes by calling sync use cases.

### What does **not** exist yet
- No WorkManager workers
- No `SyncManager`
- No persisted "currently syncing / last error" execution state
- No pending-play upload worker path
- No app-start / screen-open / periodic background orchestration

## Architecture analysis

Your proposal is directionally correct, especially on:
- Room staying the source of truth for content
- repository-owned sync logic
- WorkManager owning long-running work
- a central `SyncManager` owning orchestration
- push-before-pull ordering for plays

But the current app already has `SyncTimeRepository` in DataStore, so a full replacement with Room
sync-state tables is **a choice**, not a requirement.

For v1, the cleanest incremental path is likely:
- **keep existing repository sync methods**
- **add pending-play push support to PlaysRepository/local data source**
- **introduce persisted sync execution state**
- **introduce WorkManager workers**
- **introduce `SyncManager`**
- **migrate UI trigger points to `SyncManager`**

## Recommended target architecture

### 1. Content source of truth
- Keep Room as the source of truth for collection and plays data.
- Keep repositories responsible for remote fetch/push + local writes.

### 2. Sync execution source of truth
Chosen direction for this plan:
- migrate **last synced timestamps** into Room sync state now
- store **execution state** (`isSyncing`, last error, last synced timestamp) in Room as well
- retire `SyncTimeRepository` once the Room-backed replacement is in place

This creates one observable persisted source for sync status instead of splitting timestamps and
execution state across two storage technologies.

### 3. Worker orchestration
Use WorkManager for:
- `SyncPendingPlaysWorker`
- `SyncPlaysWorker`
- `SyncCollectionWorker`
- one orchestration entry point for full sync

Prefer one `SyncManager` abstraction that owns:
- unique work names
- constraints
- chaining
- app-start/screen-open/manual triggers

### 4. Pending-play sync model
Use the app's existing outbox direction:
- local create/edit writes immediately
- play stays `PENDING`
- worker uploads pending plays
- local record becomes `SYNCED` on success
- failures are persisted and observable

### 5. UI contract
UI should:
- never call repository sync methods directly for background orchestration
- only invoke `SyncManager` (manual refresh, app start hook, screen open hook)
- observe sync state via `Flow`

## Reviewable execution slices

### Prompt 1 — Audit and normalize sync domain boundaries
Scope:
- inspect existing `core/sync`, `CollectionRepository`, `PlaysRepository`, local data sources
- decide what stays in repository vs use case vs worker vs manager
- add/adjust interfaces only if required for the later slices

Expected changes:
- small interface/documentation cleanup
- no WorkManager yet
- no UI wiring yet

Why first:
- this reduces churn later and lets the rest of the slices build on stable boundaries

---

### Prompt 2 — Add persisted sync execution state
Scope:
- introduce persisted sync execution state models + DAO
- migrate last-sync timestamps out of `SyncTimeRepository` and into Room-backed sync state
- expose observable flows for collection sync state and plays sync state
- wire repository/use-case updates for start / success / failure

Expected outcome:
- UI can observe `isSyncing`, `lastSyncedAt` (or still from `SyncTimeRepository`), and error state
- no workers yet

Why separate:
- it gives a small review focused only on state persistence and observation

---

### Prompt 3 — Implement pending plays outbox sync
Scope:
- add repository/local-data-source support to fetch pending plays
- upload them sequentially
- update local sync status to `SYNCED` / `FAILED`
- retry both `PENDING` and `FAILED` plays on later sync attempts

Expected outcome:
- pending local plays can be pushed independently of pull sync
- no WorkManager orchestration yet

Why separate:
- this is the highest-risk data integrity slice and deserves isolated review

---

### Prompt 4 — Introduce thin workers
Scope:
- add `SyncPendingPlaysWorker`
- add `SyncPlaysWorker`
- add `SyncCollectionWorker`
- keep workers orchestration-only: call use cases/repositories and map exceptions to `Result`

Expected outcome:
- each sync unit can run independently in WorkManager
- still no central manager yet

Why separate:
- lets worker implementation be reviewed without also reviewing trigger policy

---

### Prompt 5 — Introduce `SyncManager`
Scope:
- define `SyncManager`
- implement `WorkManagerSyncManager`
- centralize work names, constraints, unique work policy, and full-sync chaining

Recommended full sync order:
1. pending plays push
2. plays pull
3. collection pull

Expected outcome:
- a single orchestration boundary exists
- workers are no longer manually enqueued by UI

---

### Prompt 6 — Add trigger policy
Scope:
- wire app-start trigger
- wire relevant screen-open trigger(s)
- wire play-create / play-edit trigger
- wire periodic sync scheduling

Expected outcome:
- background orchestration becomes automatic and lifecycle-safe

Why separate:
- trigger policy is a product/behavior decision and should be easy to review independently from the
  worker implementation itself

---

### Prompt 7 — ViewModel/UI integration
Scope:
- expose observable sync state to screens that need it
- swap manual sync calls to `SyncManager`
- update pull-to-refresh behavior to go through orchestrated work
- surface sync status cleanly in Overview / Collection / Plays as needed

Expected outcome:
- UI reflects persisted sync state
- navigation no longer interrupts sync work

---

### Prompt 8 — Test hardening and polish
Scope:
- worker tests
- repository/use-case sync tests
- trigger-policy tests where worthwhile
- documentation / KDoc / AGENTS / progress updates

Expected outcome:
- background sync architecture is well-covered and maintainable

## Suggested prompt sequence you can give the agent
If you want very small reviews, use prompts in this order:

1. "Implement Prompt 1 from BACKGROUND_SYNC_PLAN.md only."
2. "Implement Prompt 2 from BACKGROUND_SYNC_PLAN.md only."
3. "Implement Prompt 3 from BACKGROUND_SYNC_PLAN.md only."
4. "Implement Prompt 4 from BACKGROUND_SYNC_PLAN.md only."
5. "Implement Prompt 5 from BACKGROUND_SYNC_PLAN.md only."
6. "Implement Prompt 6 from BACKGROUND_SYNC_PLAN.md only."
7. "Implement Prompt 7 from BACKGROUND_SYNC_PLAN.md only."
8. "Implement Prompt 8 from BACKGROUND_SYNC_PLAN.md only."

## Important implementation notes

### Keep repository responsibilities
Repositories should still own:
- transport calls
- parsing
- mapping
- local writes
- outbox state changes

Workers should not absorb business logic.

### Do not overcouple UI to WorkManager
The UI should observe app-level sync state, not raw WorkManager APIs where avoidable.

### Prefer incremental migration over sync rewrite
Because `SyncTimeRepository` already exists, the smallest safe path is to extend around it first
rather than replacing everything in one shot.

### Avoid duplicate sync truth
Be careful not to leave both the old DataStore sync timestamps and new Room sync state active as
authoritative sources. Once Room sync state lands, timestamp ownership should move there cleanly.

## Open decisions that change the implementation plan
These need to be locked before implementation prompts start:

## Locked decisions for v1
These choices are now assumed by the plan:

1. **Timestamp persistence**
   - Migrate timestamps into Room sync state now

2. **Auto-trigger policy breadth**
   - Trigger on app start and relevant screen open

3. **Pending play retry policy**
   - Retry `FAILED` plays automatically on later sync attempts too

4. **Periodic sync scope**
   - Run the full pipeline periodically

5. **Sync-state storage shape**
   - Use separate collection and plays sync-state records first

## Final recommended implementation defaults
- Room owns sync execution state and timestamps
- WorkManager owns all long-running sync execution
- full sync order is:
  1. pending plays push
  2. plays pull
  3. collection pull
- auto triggers run on app start, relevant screen opens, play create/edit, and periodic sync
- failed outbox items remain visible as failed locally but are retried on later sync attempts
