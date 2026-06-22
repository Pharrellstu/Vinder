# Vinder

## Project Overview

Vinder is a smart and secure marketplace Android application where people can sell, buy, communicate and make deals between each other. The application contains an Ai-powered search system, which help users to find desired product based on the description and price.

This application contains features such as:
- Strong password checker
- 2-factor authentication system
- Real-time chatting
- Notification system

---

## Setup

Vinder connects to a **cloud-hosted Supabase backend** 

### 1. Create your `local.properties`

The repo root ships a template, `local.properties.example`. Copy it to `local.properties` (this file is git-ignored and must **never** be committed):

```bash
cp local.properties.example local.properties
```

### 2. Set the Android SDK path

In `local.properties`, set `sdk.dir` to your local Android SDK location. Android Studio normally fills this in automatically when you open the project:

```
sdk.dir=/path/to/Android/Sdk
```

### 3. Configure the Supabase connection

The Supabase project is already hosted in the cloud, so the URL is fixed:

```
supabase.url=https://ismxghgozvuonkgloguk.supabase.co
supabase.anon.key=<your-anon-key-from-project-settings>
```

Get the anon key from the **Supabase dashboard → Project Settings → API → `anon` key** or copy from the submission text in blackboard , then paste it into `local.properties`. Use the **anon** key — never the service role key — and keep the value out of version control.

### 4. Build and run

Both **physical devices** and the **Android emulator** connect directly to the cloud URL over HTTPS — no `10.0.2.2` alias or other emulator networking setup is required.

On build, Gradle reads `local.properties` and injects these values as:

- `BuildConfig.SUPABASE_URL`
- `BuildConfig.SUPABASE_ANON_KEY`

The app reads its Supabase credentials from `BuildConfig` at runtime, so once `local.properties` is filled in, a normal build is all that's needed.

---

## Coding Conventions

The team uses the following coding conventions (laws) for clarity and overall code quality.

---

### Naming

#### CamelCase — Default Variables

All regular variables use lowerCamelCase.

```kotlin
val itemPrice = 25.00
val userName = "Dunno"
val isLoggedIn = true
```

#### UpperCamelCase — Classes

All class names use UpperCamelCase.

```kotlin
class UserProfile {
    // ...
}

class MarketplaceItem {
    // ...
}
```

#### ScreamingSnakeCase — Constants

All constants use SCREAMING_SNAKE_CASE.

```
const val MAX_RETRY_COUNT = 3
const val API_BASE_URL = "https://api.vinder.com"
const val DEFAULT_PAGE_SIZE = 20
```

#### CamelCase — Functions

All function names use lowerCamelCase.

```kotlin
fun fetchUserData(userId: String) { }

fun calculateTotalPrice(items: List<Item>): Double { }

fun sendNotification(message: String) { }
```

---

### Curly Brackets

For both classes and functions, Egyptian brackets (K&R style) are used — the opening brace is on the same line as the declaration.

```kotlin
class UserProfile {
    fun getDisplayName(): String {
        return "Vinder User"
    }
}

fun calculateTotalPrice(items: List<Item>): Double {
    var total = 0.0
    for (item in items) {
        total += item.price
    }
    return total
}
```

---

### Conditional Statements

```kotlin
if (isLoggedIn) {
    showDashboard()
} else if (isGuest) {
    showGuestView()
} else {
    redirectToLogin()
}
```

---

### For Loops

```kotlin
for (item in itemList) {
    displayItem(item)
}

for (i in 0 until itemList.size) {
    processItem(itemList[i], i)
}
```

---

### Unit Test Structure

All unit tests must follow this structure for consistency.

```kotlin
class UserProfileTest {

    @Test
    fun `given valid credentials, when login is called, then returns success`() {
        // Arrange
        val credentials = Credentials(email = "test@vinder.com", password = "Secure@123")

        // Act
        val result = authService.login(credentials)

        // Assert
        assertEquals(LoginResult.SUCCESS, result)
    }

    @Test
    fun `given empty password, when login is called, then returns error`() {
        // Arrange
        val credentials = Credentials(email = "test@vinder.com", password = "")

        // Act
        val result = authService.login(credentials)

        // Assert
        assertEquals(LoginResult.INVALID_PASSWORD, result)
    }
}
```

---

### Code Quality

All variable and function names must be named appropriately so it is clear what kind of service or value they provide.

