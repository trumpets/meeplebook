---
name: android-data-layer
description: Guidance on implementing the Data Layer using Repository pattern, Room (Local), and Retrofit (Remote) with offline-first synchronization.
---

# Android Data Layer & Offline-First

## MeepleBook repo note

MeepleBook is offline-first, but the current implementation is **package-based inside `:app`**, not
split across dedicated Gradle data modules. Repository implementations combine local and remote data
sources, Room remains the local source of truth, and BGG network responses are XML parsed manually.

When adapting this skill to MeepleBook:

- prefer the repo's existing `RepositoryImpl + local/remote data source` pattern
- keep Room as the UI-facing source of truth
- follow the existing XML parsing and retry/backoff conventions instead of assuming JSON DTO flows

## Instructions

The Data Layer coordinates data from multiple sources.

### 1. Repository Pattern
*   **Role**: Single Source of Truth (SSOT).
*   **Logic**: The repository decides whether to return cached data or fetch fresh data.
*   **Implementation**:
    ```kotlin
    class NewsRepository @Inject constructor(
        private val newsDao: NewsDao,
        private val newsApi: NewsApi
    ) {
        // Expose data from Local DB as the source of truth
        val newsStream: Flow<List<News>> = newsDao.getAllNews()

        // Sync operation
        suspend fun refreshNews() {
            val remoteNews = newsApi.fetchLatest()
            newsDao.insertAll(remoteNews)
        }
    }
    ```

### 2. Local Persistence (Room)
*   **Usage**: Primary cache and offline storage.
*   **Entities**: Define `@Entity` data classes.
*   **DAOs**: Return `Flow<T>` for observable data.

### 3. Remote Data (Retrofit / transport)
*   **Usage**: Fetching data from backend.
*   **Response**: Use `suspend` functions in interfaces.
*   **Error Handling**: Adapt error handling to the repo's conventions. In MeepleBook, remote data
    sources do explicit HTTP-code handling and retry/backoff, while repository implementations map
    failures to app-level result types.

### 4. Synchronization
*   **Read**: "Stale-While-Revalidate". Show local data immediately, trigger a background refresh.
*   **Write**: "Outbox Pattern" (Advanced). Save local change immediately, mark as "unsynced", use `WorkManager` to push changes to server.

### 5. Dependency Injection
*   Bind Repository interfaces to implementations in a Hilt Module.
    ```kotlin
    @Binds
    abstract fun bindNewsRepository(impl: OfflineFirstNewsRepository): NewsRepository
    ```
