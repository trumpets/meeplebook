# AuthInterceptor Quality Analysis

## Current Implementation Overview

The `AuthInterceptor` is an OkHttp interceptor that adds authentication cookies to BGG API requests. It retrieves credentials from the `AuthRepository` and adds them as cookies in the format `bggusername=<username>; bggpassword=<password>`.

### Current Code Structure

```kotlin
class AuthInterceptor(
    private val repository: Lazy<AuthRepository>
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val currentUser = runBlocking { repository.get().getCurrentUser() }
        
        if (currentUser == null) {
            return chain.proceed(originalRequest)
        }
        
        val username = Uri.encode(currentUser.username, "UTF-8")
        val password = Uri.encode(currentUser.password, "UTF-8")
        
        val cookieValue = "bggusername=$username; bggpassword=$password"
        
        val requestWithCookie = originalRequest.newBuilder()
            .addHeader("Cookie", cookieValue)
            .build()
            
        return chain.proceed(requestWithCookie)
    }
}
```

## Issues Identified

### 1. **Circular Dependency (Acknowledged)**
The use of `Lazy<AuthRepository>` is required because:
- `AuthRepository` (specifically `BggAuthRemoteDataSourceImpl`) depends on `OkHttpClient`
- `OkHttpClient` depends on `AuthInterceptor`
- `AuthInterceptor` would depend on `AuthRepository` (creating a cycle)

The `Lazy` injection breaks this cycle by deferring the resolution of `AuthRepository` until it's actually used.

**Status**: This is a valid solution, but we can optimize it.

### 2. **`runBlocking` in Network Thread (Critical)**
The interceptor uses `runBlocking` to call the suspend function `getCurrentUser()`. While the comment acknowledges this is acceptable because "the auth data is stored locally and the operation should be quick," this is still problematic:

**Problems**:
- Blocks the OkHttp thread pool, reducing concurrent request capacity
- If DataStore access is slow (e.g., on first read or during I/O contention), it impacts all network requests
- Goes against best practices for Android network operations
- The DataStore flow needs to be collected on first access, which might not be instant

**Impact**: Medium-High - Can cause performance degradation under load

### 3. **No Caching of Credentials**
Every single network request calls `getCurrentUser()`, which:
- Collects from a DataStore Flow on every request
- Even though credentials rarely change during a session
- Adds unnecessary overhead

**Impact**: Medium - Impacts performance, especially for apps making many API calls

### 4. **Lack of Error Handling**
If `getCurrentUser()` throws an exception:
- The interceptor doesn't catch it
- The entire request chain fails
- No graceful degradation

**Impact**: Medium - Can cause unexpected crashes

### 5. **No Unit Tests**
Unlike `BearerInterceptor` and `UserAgentInterceptor`, `AuthInterceptor` has no unit tests.

**Impact**: Low-Medium - Makes refactoring risky

## Recommended Solutions

### Solution 1: In-Memory Cache with Flow (Recommended)

Create a cached version of the credentials that's updated via Flow:

**Pros**:
- Eliminates `runBlocking`
- Eliminates repeated DataStore reads
- Keeps sync with repository changes
- Minimal code changes
- Still breaks the circular dependency

**Cons**:
- Slightly more complex initialization
- Needs lifecycle management

**Implementation**:
```kotlin
class AuthInterceptor(
    private val repository: Lazy<AuthRepository>
) : Interceptor {
    
    @Volatile
    private var cachedCredentials: AuthCredentials? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        // Start observing credentials and cache them
        scope.launch {
            repository.get().observeCurrentUser().collect { credentials ->
                cachedCredentials = credentials
            }
        }
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val currentUser = cachedCredentials
        
        if (currentUser == null) {
            return chain.proceed(originalRequest)
        }
        
        val username = Uri.encode(currentUser.username, "UTF-8")
        val password = Uri.encode(currentUser.password, "UTF-8")
        
        val cookieValue = "bggusername=$username; bggpassword=$password"
        
        val requestWithCookie = originalRequest.newBuilder()
            .addHeader("Cookie", cookieValue)
            .build()
            
        return chain.proceed(requestWithCookie)
    }
    
    fun cleanup() {
        scope.cancel()
    }
}
```

### Solution 2: Direct Access to AuthLocalDataSource

Instead of depending on `AuthRepository`, depend on `AuthLocalDataSource` directly:

**Pros**:
- Breaks the circular dependency without `Lazy`
- AuthLocalDataSource doesn't depend on OkHttp
- More explicit about what we're accessing
- Can still use caching

**Cons**:
- Bypasses repository abstraction layer
- Less clean architecture
- Still needs to solve the `runBlocking` issue

**Dependency Graph**:
```
OkHttpClient -> AuthInterceptor -> AuthLocalDataSource
                                           ↓
AuthRepository -> AuthLocalDataSource + BggAuthRemoteDataSource
                                                    ↓
                                            OkHttpClient (different instance)
```

### Solution 3: Separate OkHttpClient for Auth vs App

Create two OkHttpClient instances:
1. A "base" client without auth for authentication calls
2. An "authenticated" client with auth for regular API calls

**Pros**:
- Clean separation of concerns
- No circular dependency
- No need for `Lazy`

**Cons**:
- More complex DI setup
- Two HTTP client instances (more memory)
- Auth API calls won't have auth cookies (which is actually correct for login!)

**Implementation**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object OkHttpModule {
    
    @Provides
    @Singleton
    @BaseOkHttp
    fun provideBaseOkHttp(...): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(bearer)
            .addInterceptor(userAgent)
            // NO auth interceptor
            .build()
    }
    
    @Provides
    @Singleton
    @AuthenticatedOkHttp
    fun provideAuthenticatedOkHttp(
        baseOkHttp: OkHttpClient,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return baseOkHttp.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
    }
}
```

### Solution 4: Cookie Manager Approach

Use OkHttp's `CookieJar` instead of an interceptor:

**Pros**:
- More idiomatic for cookie management
- Built-in cookie handling
- Can persist cookies properly

**Cons**:
- More code changes
- Credentials are stored as "cookies" conceptually which may not match the mental model
- Still need to address `runBlocking`

## Comparison Matrix

| Solution | Complexity | Performance | Architecture | Breaks Circular Dep | No runBlocking |
|----------|-----------|-------------|--------------|---------------------|----------------|
| Current  | Low       | Low         | OK           | ✓ (via Lazy)        | ✗              |
| Solution 1 (Cache) | Medium | High | Good | ✓ (via Lazy) | ✓ |
| Solution 2 (LocalDS) | Low | Medium | Fair | ✓ (direct) | ✗ (needs cache) |
| Solution 3 (Two Clients) | High | Medium | Excellent | ✓ (naturally) | ✗ (needs cache) |
| Solution 4 (CookieJar) | High | Medium | Good | Depends | ✗ (needs cache) |

## Final Recommendation

**Implement Solution 1 (In-Memory Cache with Flow)** because:

1. **Solves the main problem**: Eliminates `runBlocking` completely
2. **Improves performance**: Credentials cached in memory, no repeated DataStore reads
3. **Minimal changes**: Works with existing architecture
4. **Keeps Lazy**: The existing solution for circular dependency still works
5. **Automatic sync**: Flow updates cache when credentials change
6. **Testable**: Easy to unit test with mocked Flow

### Additional Improvements

1. **Add Unit Tests**: Follow the pattern from `BearerInterceptorTest`
2. **Add Error Handling**: Catch and log exceptions from credential retrieval
3. **Add Logging**: Debug logs for authentication state changes
4. **Consider**: If we later need auth-less endpoints, Solution 3 becomes more attractive

## Implementation Priority

1. **High Priority**: 
   - Add caching to eliminate `runBlocking`
   - Add unit tests

2. **Medium Priority**:
   - Add error handling
   - Add logging

3. **Low Priority** (Future Consideration):
   - Consider two-client approach if auth-less endpoints are added
   - Consider CookieJar if more complex cookie management is needed

## Code Quality Assessment

**Current Score: 6/10**

**Strengths**:
- ✓ Correctly uses `Lazy` to break circular dependency
- ✓ Clear comments explaining the design decision
- ✓ Simple and straightforward implementation
- ✓ Proper URI encoding of credentials

**Weaknesses**:
- ✗ Uses `runBlocking` in production code
- ✗ No caching leads to repeated DataStore access
- ✗ No error handling
- ✗ No unit tests
- ✗ No consideration for memory leaks (if CoroutineScope is used)

**With Recommended Changes: 8.5/10**
