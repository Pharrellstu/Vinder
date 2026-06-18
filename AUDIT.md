# Vinder — Comprehensive Audit Report

> Generated: 2026-06-18

---

## AUDIT REPORT

### Project Overview

- **Architecture**: MVVM with interface-backed repositories. No Clean Architecture layers (domain layer absent). Manual state-machine navigation in `MainActivity.kt` (no NavHost/NavGraph).
- **Total screens/flows**: 15 screens (3 auth, 1 forgot-password stub, 5 main tabs, 6 modal overlays)
- **Supabase services in use**: Auth (GoTrue), Postgrest, Realtime (chat only), Storage (`item-photos` bucket)
- **DI framework**: None — repositories instantiated as default parameter values in ViewModels

---

### Feature Completeness Matrix

| Feature | Status | Notes |
|---|---|---|
| Sign up | ✅ | Email + OTP verify flow works |
| Sign in | ✅ | GoTrue email/password |
| Sign out | ✅ | `SettingsViewModel` → `AuthRepository.logout()` |
| Password reset | 🔶 | `ForgotPasswordScreen` is UI-only; button `onClick` is empty; no ViewModel, no `AuthRepository` method, not reachable from nav |
| Session persistence | ❌ | `SessionManager` is in-memory; process kill = forced re-login. Supabase session token not restored on relaunch |
| User profile (view) | ✅ | `ProfileScreen` + `SellerPublicProfileScreen` load from DB |
| User profile (edit) | ✅ | `EditProfileViewModel` + `AccountRepository.updateProfile()` |
| Avatar upload | 🔶 | Uploads to `item-photos` bucket under `avatars/` prefix — no dedicated bucket, no size/type limit |
| Listings (create) | ✅ | `AddProductScreen` + full photo upload flow |
| Listings (edit/delete) | ❌ | No screen, no repository method for edit or delete of own listings |
| Search | 🔶 | `SearchResultsScreen` filters locally on `HomeViewModel`'s already-loaded feed — no server-side search, no AI search despite being a stated feature |
| Filters / sorting | 🔶 | `FilterBottomSheet` exists; applied client-side on in-memory list |
| Product detail view | ✅ | `ItemDetailScreen` with photo carousel, description, seller card |
| Cart | ❌ | `account_favorite` table in schema, zero app code touches it |
| Wishlist | ❌ | Same as cart — table exists, no UI or repository |
| Buy flow | 🔶 | `confirmBuy()` inserts to `purchase` and marks item sold, but fees calculated client-side (spoofable); no payment gateway |
| Payment integration | ❌ | No Stripe/payment SDK; purchase is a direct DB insert |
| Order history | ✅ | `OrderHistoryScreen` + `OrderHistoryViewModel`; shows item name, fee breakdown, total, date |
| Make offer | ✅ | `submitOffer()` inserts to `item_offer` |
| Offer management | ✅ | `OffersScreen` + `OffersViewModel`; seller can accept/reject; accept chains purchase creation + markAsSold |
| Messaging list | ✅ | `MessagesScreen` loads conversations from DB |
| Chat | ✅ | `ChatScreen` with Supabase Realtime subscription |
| Message attachments | ❌ | `dialogue_message_attachment` table in schema; zero app code touches it |
| Ratings & reviews | ❌ | `account_rating` + `rating` tables in schema; zero app code or UI |
| Push notifications | ❌ | FCM not integrated; `NotificationSettingsViewModel` is in-memory only, preferences lost on process kill |
| Followers | 🔶 | `account_following` table exists, follower count shown in profile stats, but follow/unfollow action not implemented |
| Admin / moderation | ❌ | None |
| AI-powered search | ❌ | Stated as key feature in CLAUDE.md; not implemented |

---

### Supabase / Database Findings

**Schema summary** (17 tables): `item_condition`, `status`, `rating`, `item_category`, `account`, `account_side_information`, `account_authentication`, `account_following`, `account_rating`, `item`, `item_photo`, `account_favorite`, `item_offer`, `purchase`, `dialogue`, `dialogue_message`, `dialogue_message_attachment`

**CRITICAL — RLS not enabled on any public table.** `init.sql` contains zero `ENABLE ROW LEVEL SECURITY` or `CREATE POLICY` statements for public schema tables. Only `storage.objects` has RLS (via migration `001`). Every authenticated user can read and write every row in every table via the Supabase anon key — including other users' purchases, messages, and account details.

**Storage bucket issues:**
- `item-photos` bucket: `public = true`, `file_size_limit = NULL`, `allowed_mime_types = NULL` — no size cap, any file type accepted
- Upload policy checks only `bucket_id = 'item-photos'`, not path prefix — any authenticated user can overwrite `items/<other_user_id>/photo_N.jpg` by guessing the path
- Avatars share the `item-photos` bucket under `avatars/` prefix; no dedicated bucket

**Missing indexes on `purchase` table:** No index on `buyer_id` or `seller_id` — order history queries will full-scan.

**Schema concern — `account.password_hash`:** Schema defines `password_hash VARCHAR(255) NOT NULL` on the `account` table, but authentication goes through Supabase GoTrue. The app never reads or writes this column. Vestige of a pre-GoTrue design — misleading and potentially dangerous if something tries to use it.

**`dialogue` UNIQUE constraint:** `UNIQUE(dialogue_creator_id, dialogue_receiver_id)` — ordered pair, not unordered. Bob→Alice and Alice→Bob can create two separate dialogues. App code uses fixed creator/receiver order but this is fragile.

**`account_authentication` table:** Stores OTP hash + expiry, but app uses Supabase's built-in email OTP. This table appears unused — dead schema.

---

### Code Quality Findings

1. **`ForgotPasswordScreen.kt:124`** — `onClick = {}` is a no-op. Screen has no ViewModel, no `AuthRepository.resetPassword()` method, and is not reachable from any navigation path. Fully dead feature.

2. **`data/ChatRepository.kt`** — Entire file is a stub (`return emptyList()`, `= Unit` bodies). Never imported anywhere — `DialogueRepository` is the real implementation. Dead file.

3. **`data/SupabaseConfig.kt`** — `@Deprecated` shim referencing `SessionManager`. Referenced nowhere in production code. Dead file.

4. **`SessionManager.currentAccountId: Int = 0`** — Default is `0` (a valid-looking integer). If session is not set (e.g., after process kill), `confirmBuy()` and `submitOffer()` will silently use buyer/creator ID `0`, corrupting data. Should be `Int? = null` with null-guard.

5. **`ItemDetailViewModel.kt:59`** — `markAsSold()` has no ownership check. Any authenticated user who knows an `itemId` can mark any item as sold. Must be enforced server-side with RLS.

6. **`ItemRepository.kt:125`** — `updateItemToListed()` has no ownership check. Same problem.

7. **`getFeedItems()` — no pagination.** Fetches all `is_listed=true` items in one query. Will break at scale. No limit, no cursor.

8. **`NotificationSettingsViewModel.kt`** — Preferences stored only in `MutableStateFlow`; lost on process kill. No DataStore, SharedPreferences, or Supabase persistence.

9. **`MainActivity.kt`** — Manual boolean-flag navigation for 6+ overlays. No deep-link support, no back-stack, no predictive back gesture support. Will not scale past ~20 screens.

10. **`ItemDetailViewModel.kt:106`** — `memberSince = "-"` hardcoded. `account` table has no `created_at` column so this can't be populated without a schema change.

---

### Security Findings

| Finding | Severity | Detail |
|---|---|---|
| No RLS on any public table | CRITICAL | All 17 tables readable/writable by any authenticated anon-key user |
| Client-side fee calculation | HIGH | `SHIPPING_FEE` and `BUYER_PROTECTION_FEE` constants in `ItemDetailViewModel.kt:38-39`; client sends fee values to DB — anyone can send $0 fees |
| `markAsSold()` no ownership check | HIGH | `ItemRepository.kt:124` — no RLS or app-level guard; any user can mark any item sold |
| `item-photos` upload path not restricted | HIGH | Storage policy allows upload to any path in bucket; malicious user can overwrite other users' photos |
| No file type/size limit on storage | MEDIUM | Bucket accepts any file; denial-of-storage risk |
| `SessionManager` in-memory only | MEDIUM | Session ID lost on process kill; `currentAccountId` resets to `0` before auth check completes |
| `account.password_hash` column | LOW | Dead column — misleading; if ever populated it would be client-generated and untrustworthy |
| API keys via BuildConfig | ✅ OK | `SUPABASE_URL` and `SUPABASE_ANON_KEY` injected from `local.properties` via `BuildConfig` — correct approach |
| No hardcoded secrets found | ✅ OK | Grep over all `.kt` files returned no matches |

---

## IMPLEMENTATION PLAN

### Priority 1 — Blockers (cannot ship without)

- [ ] **Enable RLS on all public tables** — every table is currently wide open; add `ENABLE ROW LEVEL SECURITY` + policies for `account`, `item`, `item_photo`, `purchase`, `dialogue`, `dialogue_message`, `item_offer`, `account_following`, `account_rating`, `account_favorite` — `database/init.sql` + Supabase dashboard
- [ ] **Move fee calculation server-side** — `ItemDetailViewModel.kt:38-39`, `PurchaseRepository.kt` — fees must be computed in a Supabase Edge Function or DB trigger, not sent by client; any client-supplied fee amount should be rejected
- [ ] **Fix `SessionManager.currentAccountId` null-safety** — `data/SessionManager.kt` — change to `Int? = null`, add null-guards in `confirmBuy()` and `submitOffer()` before dispatching requests
- [ ] **Persist Supabase session on relaunch** — `SupabaseClientInitialiser.kt` — install `SessionStorage` (Supabase Kotlin SDK supports `SettingsSessionStorage` with DataStore); without this, users re-login on every process kill
- [ ] **Restrict storage upload path** — `database/migrations/001_create_item_photos_bucket.sql` — policy `WITH CHECK` should enforce path prefix matches the authenticated user's ID; same for `avatars/`

### Priority 2 — Core Incomplete Features

- [ ] **Password reset flow** — wire `ForgotPasswordScreen` button to `AuthRepository.resetPassword(email)` calling `client.auth.resetPasswordForEmail()`; add route from `LoginScreen` — **S**
- [x] **Order history screen** — `OrderHistoryScreen` + `OrderHistoryViewModel` + `PurchaseRepository.getMyPurchases()` — **M** ✅
- [ ] **Listing edit/delete** — add `updateItem()` and `deleteItem()` to `IItemRepository`; new `EditListingScreen` reachable from `ProfileScreen` owned listings tab — **M**
- [x] **Offer management** — `OffersScreen` + `OffersViewModel` + `ItemRepository.getOffersForSeller/updateOfferStatus()` — **M** ✅
- [ ] **Wishlist/favorites** — wire `account_favorite` table; add heart icon on `ItemDetailScreen` + `HomeScreen` cards; `AccountRepository.toggleFavorite()` — **M**
- [ ] **Follower follow/unfollow action** — `AccountRepository.follow/unfollow()`; wire button in `SellerPublicProfileScreen` (stat count already displayed) — **S**
- [ ] **Server-side search** — `SearchResultsScreen` currently filters in-memory list; replace with `client.from("item").select { filter { ilike("item_name", "%$query%") } }` at minimum; or add pgvector + Edge Function for AI search — **L**
- [ ] **Push notifications** — integrate FCM; add `firebase-messaging` dependency; store FCM token in `account_side_information` or new `account_device_token` table; send notifications on message/offer/sale events via Edge Function — **L**
- [ ] **Notification settings persistence** — `NotificationSettingsViewModel.kt` — write/read preferences to DataStore or Supabase — **S**

### Priority 3 — Quality & Security Fixes

- [ ] **Add DI framework (Hilt)** — repositories new-ed inside ViewModels makes coroutine testing painful and tightly couples construction; add `@HiltViewModel` + `@Inject` — **M**
- [ ] **Replace manual nav with Jetpack Navigation** — `MainActivity.kt` boolean-flag overlay system; add `NavHost` + typed routes for deep-link and predictive back support — **L**
- [ ] **Delete dead files** — `data/ChatRepository.kt`, `data/SupabaseConfig.kt` — **S**
- [ ] **Feed pagination** — `ItemRepository.getFeedItems()` — add `.range(offset, offset+PAGE_SIZE-1)` and cursor-based loading in `HomeViewModel` — **S**
- [ ] **Storage file type + size limits** — update `001_create_item_photos_bucket.sql`; set `file_size_limit = 5242880` (5 MB), `allowed_mime_types = ['image/jpeg','image/png','image/webp']` — **S**
- [ ] **Add `created_at` to `account` table** — needed to populate `memberSince` in `ItemDetailViewModel.kt:106`; add migration — **S**
- [ ] **Remove `account.password_hash`** — dead column; remove via migration — **S**
- [ ] **Drop `account_authentication` table** — unused since GoTrue handles OTP; add migration — **S**
- [ ] **Fix `dialogue` UNIQUE constraint** — current `UNIQUE(creator, receiver)` is ordered; add `UNIQUE(LEAST(a,b), GREATEST(a,b))` or application-level canonical ordering — **S**
- [ ] **`markAsSold` / `updateItemToListed` ownership** — RLS policies (Priority 1) will cover DB-level; also add `sellerId` check in `ItemDetailViewModel.confirmBuy()` app-side — **S**
- [ ] **Add indexes on `purchase(buyer_id, seller_id)`** — missing from `init.sql`; needed for order history — **S**

### Priority 4 — Nice-to-Have / Polish

- [ ] **AI-powered search** — stated as key feature; add pgvector extension + embedding column on `item`; Edge Function to vectorize descriptions on insert; search via cosine similarity — **L**
- [ ] **Dedicated avatar storage bucket** — separate `avatars` bucket from `item-photos`; cleaner access policies and quotas — **S**
- [ ] **Listing image picker UX** — verify `AddProductScreen` allows picking multiple photos from gallery; test on physical device
- [ ] **`memberSince` display** — once `account.created_at` added, format and display in `ItemDetailScreen`
- [ ] **Message attachments** — `dialogue_message_attachment` table exists; wire photo-sending in `ChatScreen` — **M**
- [ ] **Dark mode persistence** — `SettingsViewModel` dark mode toggle; persist to DataStore — **S**
- [ ] **Error recovery UI** — most screens show error state text but no retry button; add `Button("Retry") { viewModel.load() }` pattern — **S**
