# Integration Tests — API Automation Framework

> **Playwright-based API test automation framework with Microsoft SSO authentication.**
> Built with Java 17 · Playwright · Cucumber · JUnit 5

***

## Table of Contents

1. [For Product Owners & Stakeholders](#for-product-owners--stakeholders)
2. [Architecture Overview](#architecture-overview)
3. [Project Structure](#project-structure)
4. [Authentication Flow](#authentication-flow)
5. [Writing a New Test — Step by Step](#writing-a-new-test--step-by-step)
6. [Configuration](#configuration)
7. [Running Tests](#running-tests)
8. [Framework Layers Explained](#framework-layers-explained)

***

## For Product Owners & Stakeholders

### Why this approach makes adding new tests fast

Every time your team builds a new microservice or adds a new API endpoint, a test engineer can cover it with **just two things**:

1. **A service class** (~10 lines) that describes what the service does and its base path.
2. **A test class** (~5–10 lines per scenario) that reads exactly like the requirement.

Here is what a complete test looks like once the framework is set up:

```java
// A test that verifies a book can be added to the bookstore
Book book = testUser().calls().service(Book.class);
ISBN result = book.addListOfBooks(bookRequestBody);
assertThat(result.getIsbn()).isEqualTo("9781449337711");
```

That is the **entire test**. Authentication, token management, HTTP request construction, response validation, and cleanup are all handled by the framework. The test engineer writes only the business scenario — not the plumbing.

### What this means in practice

| Without this framework | With this framework |
|---|---|
| Every test file must handle login/tokens manually | Authentication runs once per user, automatically |
| Adding a new service requires 50–100 lines of boilerplate | Adding a new service takes ~10 lines |
| Changing the auth mechanism breaks every test | Change auth in one class — all tests unaffected |
| Onboarding a new engineer takes days | New engineers write their first test in under an hour |

### How the authentication works (plain English)

Your APIs are protected by Microsoft corporate login — the same login your team uses every day. This framework automates that login silently in the background:

1. A headless (invisible) browser opens the Microsoft login page and signs in with test credentials.
2. The resulting access token is swapped for your internal application token via the SSO service.
3. That token is cached and reused for the rest of the test run — login happens **once**, not once per test.

No credentials are stored in code. They are loaded from a separate properties file that is excluded from source control.

***

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        TEST LAYER                               │
│  BookTests.java  ·  UserTests.java  ·  OrderTests.java  · ...   │
│  "What" — pure business scenarios, assertions only              │
└──────────────────────────┬──────────────────────────────────────┘
                           │  testUser().calls().service(X.class)
┌──────────────────────────▼──────────────────────────────────────┐
│                      SERVICE LAYER                              │
│  Book.java  ·  User.java  ·  Order.java  · ...                  │
│  "Which endpoints" — named methods per API operation            │
└──────────────────────────┬──────────────────────────────────────┘
                           │  extends MicroService
┌──────────────────────────▼──────────────────────────────────────┐
│                    FRAMEWORK CORE                               │
│  MicroService  ·  ITRestPlay  ·  QaUser  ·  ServicePath         │
│  "How" — HTTP verbs, response validation, request construction  │
└──────────────────────────┬──────────────────────────────────────┘
                           │  resolveToken()
┌──────────────────────────▼──────────────────────────────────────┐
│                      AUTH LAYER                                 │
│  TokenCache → MicrosoftImplicitAuth → SsoTokenExchanger         │
│  "Who" — identity, token lifecycle, SSO handshake               │
└─────────────────────────────────────────────────────────────────┘
```

The design follows a strict **one-way dependency rule**: tests depend on services, services depend on the framework core, the core depends on auth. Nothing flows upward. This means:

- You can swap the auth mechanism (e.g., when the team upgrades from Implicit Flow to PKCE) without touching any test or service class.
- You can add a new service without touching auth or other services.
- Tests remain readable as plain English, with zero authentication noise.

***

## Project Structure

```
src/
├── main/java/in/av/qe/
│   ├── api/
│   │   ├── MicroService.java          ← Base class all service classes extend
│   │   └── Book.java                  ← Example: BookStore service
│   │
│   ├── auth/
│   │   ├── AuthToken.java             ← Token value object with expiry awareness
│   │   ├── TokenCache.java            ← Thread-safe per-user token cache
│   │   ├── MicrosoftImplicitAuth.java ← Step 1: MS OAuth 2.0 Implicit Grant via browser
│   │   └── SsoTokenExchanger.java     ← Step 2: Exchange MS token for internal SSO token
│   │
│   ├── user/
│   │   └── QaUser.java                ← Entry point for all tests — owns auth chain
│   │
│   ├── utils/
│   │   ├── ITRestPlay.java            ← HTTP helper methods (GET/POST/PUT/DELETE)
│   │   ├── ServicePath.java           ← Enum of all registered microservice base paths
│   │   ├── InfrastructureConstants.java ← All config loaded from properties
│   │   └── PropertyLoader.java        ← Loads application.properties / system props
│   │
│   └── vo/                            ← Value Objects (POJOs for request/response bodies)
│
└── test/java/in/av/qe/
    └── resttest/
        └── BOOKTests/                 ← Test classes live here, grouped by service
```

***

## Authentication Flow

The framework automates a two-step SSO handshake that your team currently performs manually in the browser.

```
QaUser is created
    │
    ├─► TokenCache.get(username)
    │       │
    │       ├─ HIT (token valid) ──────────────────────► Use cached token ✓
    │       │
    │       └─ MISS or EXPIRED
    │               │
    │               ▼
    │       MicrosoftImplicitAuth.acquireAuthToken()
    │           - Launches headless Chromium browser
    │           - Navigates to Microsoft authorization URL
    │           - Fills login credentials if prompted
    │           - Serves a fake page at localhost:3000 via route.fulfill()
    │           - Reads access_token from window.location.hash via JS evaluate
    │           - Returns AuthToken(msAccessToken, expiresIn)
    │           - Closes browser immediately
    │               │
    │               ▼
    │       SsoTokenExchanger.exchange(msToken)
    │           - POST {ssoUrl}/authorize
    │           - Content-Type: application/x-www-form-urlencoded
    │           - Body: access_token=<msToken>
    │           - Parses custom token from JSON response
    │           - Returns AuthToken(customToken, 3600s)
    │               │
    │               ▼
    │       TokenCache.put(username, customToken)
    │               │
    └───────────────► Authorization: Bearer <customToken> ✓
                      set on all APIRequestContext headers
```

**Token lifetime:** MS tokens expire per the `expires_in` value in the redirect (typically ~84 minutes). The custom SSO token defaults to 1 hour. Both are tracked by `AuthToken.isExpired()` with a 60-second safety buffer, so the cache auto-refreshes before expiry.

***

## Writing a New Test — Step by Step

### Step 1: Register the service path

Open `ServicePath.java` and add your service's base URL path:

```java
public enum ServicePath {
    BOOKS("/BookStore/v1"),
    USERS("/UserService/v1"),     // ← add this
    ORDERS("/OrderService/v2");   // ← and this

    private final String appPrefix;
    ServicePath(String appPrefix) { this.appPrefix = appPrefix; }
    public String getAppPrefix() { return appPrefix; }
}
```

That's it for registration. The framework will append this prefix to `qa.baseUrl` automatically.

***

### Step 2: Create the service class

Create a new file in `src/main/java/in/av/qe/api/`. Model it exactly after `Book.java`:

```java
package in.av.qe.api;

import in.av.qe.utils.ServicePath;
import in.av.qe.vo.UserProfile;

import static in.av.qe.user.QaUser.testUser;
import static in.av.qe.utils.ITRestPlay.*;

public class UserService extends MicroService {

    private static final String USERS = "/Users";
    private static final String USER  = "/User";

    public UserService() {
        super(ServicePath.USERS);   // ← links to the path you registered above
    }

    // Static factory — lets tests call UserService.userService() directly
    public static UserService userService() {
        return testUser().calls().service(UserService.class);
    }

    // One method per API operation — name it after the business action
    public UserProfile getUserById(String userId) {
        return extractObject(UserProfile.class,
            readByGet(requestContext, USER + "/" + userId, 200, null, null));
    }

    public UserProfile createUser(String requestBody) {
        return extractObject(UserProfile.class,
            createByPost(requestContext, USERS, requestBody, "application/json", 201));
    }
}
```

**Rules for service classes:**
- One class per microservice.
- Constructor always calls `super(ServicePath.YOUR_SERVICE)`.
- One method per API endpoint — named as a business action, not an HTTP verb.
- Use `ITRestPlay` static methods for all HTTP calls (`readByGet`, `createByPost`, `updateByPut`, `deleteByDelete`).
- Never handle auth, headers, or base URLs in a service class — the framework does that.

***

### Step 3: Create value objects (if needed)

If your service returns or accepts a JSON body that isn't yet modelled, add a POJO in `src/main/java/in/av/qe/vo/`:

```java
package in.av.qe.vo;

public class UserProfile {
    private String userId;
    private String username;
    private String email;

    // Getters — Gson deserialises by field name automatically
    public String getUserId()   { return userId; }
    public String getUsername() { return username; }
    public String getEmail()    { return email; }
}
```

No annotations needed — `ITRestPlay.extractObject()` uses Gson which maps by field name.

***

### Step 4: Write the test

Create a test class under `src/test/java/in/av/qe/resttest/`:

```java
package in.av.qe.resttest;

import in.av.qe.api.UserService;
import in.av.qe.vo.UserProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserServiceTests {

    @Test
    void getUserById_shouldReturnCorrectProfile() {
        UserProfile profile = UserService.userService().getUserById("user-001");

        assertThat(profile.getUsername()).isEqualTo("testuser");
        assertThat(profile.getEmail()).isNotBlank();
    }

    @Test
    void createUser_shouldReturn201WithUserId() {
        String body = """
            { "username": "newuser", "email": "new@example.com" }
            """;

        UserProfile created = UserService.userService().createUser(body);

        assertThat(created.getUserId()).isNotBlank();
    }
}
```

**That's the complete test.** No setup, no auth, no HTTP client configuration. The framework handles everything else.

***

### Summary: What you write vs. what the framework handles

| You write | Framework handles |
|---|---|
| `ServicePath` enum entry (1 line) | Base URL construction |
| Service class methods (~5 lines each) | Auth header injection |
| Test assertions | Token acquisition & caching |
| Value object fields | HTTP response validation |
| — | 401/403 failure reporting |
| — | SSO token refresh on expiry |

***

## Configuration

All configuration lives in `src/main/resources/application.properties`.  
**Never commit real credentials.** Use environment variables or a secrets manager in CI.

```properties
# Base URL of the application under test
qa.baseUrl=https://your-app.qa.com

# Test user credentials (override via -Dqa.user=... in CI)
qa.user=testuser@yourcompany.com
qa.password=your_password
qa.userId=some-user-id

# Full Microsoft OAuth 2.0 Implicit Grant URL
# Obtain this from your dev team — it contains client_id, scope, and redirect_uri
qa.auth.msAuthUrl=https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/authorize?client_id=...&scope=...&redirect_uri=http://localhost:3000/sso&response_mode=fragment&response_type=token

# Internal SSO base URL (the framework appends /authorize automatically)
qa.auth.ssoUrl=https://your-app.qa.com
```

### CI/CD — passing credentials securely

```bash
mvn test \
  -Dqa.user=$QA_USER \
  -Dqa.password=$QA_PASSWORD \
  -Dqa.auth.msAuthUrl="$MS_AUTH_URL"
```

System properties override `application.properties` values, so credentials can be injected at runtime from your pipeline's secret store.

***

## Running Tests

### Prerequisites

- Java 17+
- Maven 3.8+
- Playwright browsers installed: `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"`

### Run all tests

```bash
mvn test
```

### Run a specific test class

```bash
mvn test -Dtest=BOOKTests
```

### Run with debug logging

```bash
mvn test -Dqa.debug=true
```

### Run against a different environment

```bash
mvn test -Dqa.baseUrl=https://staging.yourapp.com
```

***

## Framework Layers Explained

### `QaUser` — The Entry Point

Every test begins with `testUser().calls().service(SomeService.class)`. `QaUser` is responsible for:
- Triggering the auth chain on first use.
- Injecting the `Authorization: Bearer <token>` header into every `APIRequestContext`.
- Being the single place where a test identity is established.

### `ITRestPlay` — The HTTP Toolkit

A static utility class wrapping Playwright's `APIRequestContext`. It provides named methods (`readByGet`, `createByPost`, `updateByPut`, `deleteByDelete`) that:
- Accept the context, URL, body, and expected status code(s).
- Validate the response status automatically — no need for assertions in service classes.
- Validate `Content-Type` headers when specified.
- Exit immediately with a clear error on 401, preventing cascading failures.

### `MicroService` — The Service Base Class

All service classes extend `MicroService`. It holds the `APIRequestContext` (injected by `QaUser`) and the `ServicePath` (the base URL segment). Service classes never construct HTTP clients themselves.

### `ServicePath` — The Service Registry

A simple enum that acts as the single source of truth for all microservice base paths. Adding a new service to the framework starts here.

### `AuthToken` — Token with Expiry Awareness

Wraps a token string with an `expiresAt` timestamp. `isExpired()` checks this before every cache lookup, ensuring the framework never uses a stale token silently.

### `TokenCache` — Thread-Safe Token Store

A `ConcurrentHashMap` keyed by username. Ensures the full auth chain (browser launch → MS login → SSO exchange) runs at most once per user per test run, regardless of how many tests or parallel threads are running.