# Vinder — Project Description

Vinder is a smart and secure marketplace Android application where people can sell, buy, communicate and make deals between each other.
The application contains an Ai-powered search system, which help users to find desired product based on the description and price.

## Key Features
- Password Strength checker
- 2-factor authentication system
- Real-time chatting
- Notification system

## Tech Stack
- **Language**: Kotlin
- **Platform**: Android
- **UI**: Jetpack Compose
- **Build**: Gradle (Kotlin DSL)

## Brand Book
- **Primary Color**: #4CAF50 (Green)
- **Secondary Color**: #FFC107
- **Accent Color + Logo**: #4E8098
- **Font**: Instrument Serif for Logo, Inter for body text, Public Sans for headings
- **Logo**: A stylized "Vinder" in italic

## Supabase Setup (Self-Hosted)

Supabase runs locally via Docker Compose in `../vinder-AppDev/` (one level above this repo).
Kong gateway is exposed at **`http://localhost:8000`** on the host machine.

### Android emulator networking
The emulator's `localhost` is the emulator itself, not the host.
Always use **`10.0.2.2`** as the Supabase URL in `local.properties`:
```
supabase.url=http://10.0.2.2:8000
supabase.anon.key=<your-anon-key>
```
Never set `supabase.url=http://localhost:8000` — it will fail with `ConnectException`.

### Secrets
- `local.properties` is git-ignored and holds real secrets (`sdk.dir`, `supabase.url`, `supabase.anon.key`).
- `local.properties.example` is committed as a safe placeholder — copy it to `local.properties` and fill in real values.
- Anon key comes from `../vinder-AppDev/.env` → `ANON_KEY`.
- No secrets are hardcoded in any committed file.

### BuildConfig injection
`app/build.gradle.kts` reads `local.properties` and injects:
- `BuildConfig.SUPABASE_URL`
- `BuildConfig.SUPABASE_ANON_KEY`

`buildFeatures { buildConfig = true }` must be set.

### Supabase client
`SupabaseClientProvider` (singleton object) creates the client with `Auth` plugin installed.
Auth operations go through `IAuthRepository` / `AuthRepository` — never call the client directly from a ViewModel.

### Authentication architecture
| Class | Role |
|---|---|
| `IAuthRepository` | Interface — enables fake injection in tests |
| `AuthRepository` | Real implementation using GoTrue (`client.auth.*`) |
| `LoginViewModel` | Screen-scoped; owns `LoginUiState` sealed class |
| `AuthViewModel` | Shared across `RegisterScreen` and `AuthenticateAccountScreen`; owns `AuthUiState` |

### Mock seed data
`database/accounts.sql` seeds three test accounts into both `auth.users` (GoTrue) and the app `account` table:
| Email | Password |
|---|---|
| `alice@vinder.dev` | `Alice123!` |
| `bob@vinder.dev` | `Bob123!` |
| `carol@vinder.dev` | `Carol123!` |

Requires `pgcrypto` extension. Run against the `supabase-db` container as `supabase_admin`.

### Service-role password mismatch (runbook)
If `supabase-auth`, `supabase-rest`, or `supabase-storage` crash-loop with:
```
password authentication failed for user "supabase_auth_admin"
```
The Docker volume was initialized with an old `POSTGRES_PASSWORD`. Fix with `database/fix_supabase_admin.sql`:
```powershell
# PowerShell
$pw = (Select-String 'POSTGRES_PASSWORD' ../vinder-AppDev/.env).Line.Split('=')[1].Trim()
docker exec supabase-db psql -U supabase_admin -h 127.0.0.1 -d postgres `
  -v postgres_password="$pw" `
  -f database/fix_supabase_admin.sql
```
This resets all 4 service-role passwords to match the current `.env` value.

### Network security
`res/xml/network_security_config.xml` allows cleartext HTTP only to `localhost` and `10.0.2.2`.
All other traffic requires HTTPS. `AndroidManifest.xml` references this config and declares `INTERNET` permission.

## Coding Conventions
This project follows strict naming and formatting rules — see README.md for the full reference with examples.

Summary:
- Variables: `lowerCamelCase`
- Classes: `UpperCamelCase`
- Constants: `SCREAMING_SNAKE_CASE`
- Functions: `lowerCamelCase`
- Brackets: Egyptian / K&R style (opening brace on same line)
- Unit tests: Arrange / Act / Assert structure
