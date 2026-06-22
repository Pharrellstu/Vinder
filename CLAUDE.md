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
Source of truth: `docs/DESIGN.md`. Summary:
- **Primary Color**: #4E8098 (Deep Teal Blue) — `VinderAzure`. CTAs, active nav states, key highlights.
- **Secondary/Accent Color**: #90C2E7 (Light Sky Blue) — hover states, secondary buttons, promotional badges.
- **Background**: #F5F6F8 — `Grey97`. **Surface**: #FFFFFF — card/dropdown/modal backgrounds.
- **Text**: #1C1C1E primary (`Grey11`), #8E8E93 secondary (`Grey57`).
- **Borders/Dividers**: #E5E5EA — `Grey91`.
- **Error/Destructive Color**: #D32F2F — `VinderError` (delete actions, log out, error text — always this token, never a one-off literal). Not in DESIGN.md; established separately for destructive-action consistency.
- **Font**: A sans-serif system stack — Inter (`inter`) is the one wired in, used for both headings and body text via `Typography` in `Type.kt`, so it applies app-wide without per-screen overrides. Instrument Serif (`instrumentSerifNormal`) is reserved for the "Vinder" logo wordmark only — a separate brand asset not covered by DESIGN.md's general typography section.
- **Logo**: A stylized "Vinder" in italic

## Figma design
You can access the figma design with the mcp
https://www.figma.com/design/ZKz1RyWWmsyLc6aMUrfEaq/Untitled?node-id=78-88&t=2QbsQXw4YUgogl7X-1

## Supabase Setup (Cloud)

Supabase project is hosted online at **`https://ismxghgozvuonkgloguk.supabase.co`**.
Use the Supabase cloud MCP or dashboard to access the project.

### App connection
Both physical devices and the Android emulator reach the cloud URL directly over HTTPS — no special networking alias needed.
`local.properties` holds the real credentials:
```
supabase.url=https://ismxghgozvuonkgloguk.supabase.co
supabase.anon.key=<anon key — see local.properties, never commit>
```

### Secrets
- `local.properties` is git-ignored and holds real secrets (`sdk.dir`, `supabase.url`, `supabase.anon.key`).
- `local.properties.example` is committed as a safe placeholder — copy it to `local.properties` and fill in real values.
- Anon key comes from Supabase Project Settings → API.
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

### Network security
All Supabase traffic is HTTPS to the cloud endpoint — no cleartext exceptions needed.
`res/xml/network_security_config.xml` retains the `localhost`/`10.0.2.2` cleartext entries for local dev if needed.
`AndroidManifest.xml` references this config and declares `INTERNET` permission.

## Coding Conventions
This project follows strict naming and formatting rules — see README.md for the full reference with examples.

Summary:
- Variables: `lowerCamelCase`
- Classes: `UpperCamelCase`
- Constants: `SCREAMING_SNAKE_CASE`
- Functions: `lowerCamelCase`
- Brackets: Egyptian / K&R style (opening brace on same line)
- Unit tests: Arrange / Act / Assert structure

## Skills
Before responding to any prompt, use /find-skills skill to search for skills related to the prompt
Before writing any code use /clean-code skill
If a prompt has tasks related to supabase or backend, use supabase skills

## Rules
Never read /supabase or /supabase-project folder

## AUDIT.md Maintenance
When completing any task listed in AUDIT.md, immediately update AUDIT.md:
- Feature matrix: flip status (❌/🔶 → ✅)
- Implementation plan: check off the completed item (`[ ]` → `[x]`)
- Add evidence (file:line or migration name) in the Notes column