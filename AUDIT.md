# Vinder — Comprehensive Audit Report

> Generated: 2026-06-18  
> Re-audit: 2026-06-19

---

## RE-AUDIT UPDATE REPORT

**Re-audit date:** 2026-06-19  
**Baseline audit date:** 2026-06-18  
**Commits reviewed:** 8 commits (2026-06-18, commits `bb91f37` → `32656c9`)

### Summary of Changes

- Files changed: 55 (26 added, 27 modified, 2 deleted)
- Feature areas affected: Auth flow, Session persistence, Password reset, Push notifications, Offers/Orders, Chat/Messaging, Database migrations
- Issues resolved since last audit: **5**
- Issues still open: **9**
- New issues found: **2**
- Regressions: **0**

---

### Issue Status Updates

| # | Original Finding | Old Status | New Status | Notes |
|---|----------------|------------|------------|-------|
| 1 | Session persistence — process kill forces re-login | ❌ Missing | ✅ Resolved | `SplashViewModel` waits for `SessionStatus`, restores from `AccountPreferences` (SharedPreferences); DB fallback on first launch post-update; commit `bb91f37` |
| 2 | Password reset — `ForgotPasswordScreen` button was a no-op | 🔶 Partial | ✅ Resolved | Full 3-step flow (email → OTP → new password → success); `ForgotPasswordViewModel` wired; screen reachable from `LoginScreen` nav; commit `a1cdeee` |
| 3 | Notification settings persistence — in-memory only | ❌ Missing | ✅ Resolved | `NotificationPreferences` (`data/NotificationPreferences.kt`) writes to SharedPreferences; `NotificationSettingsViewModel` reads from it; survives process kill; commit `4dea618` |
| 4 | Duplicate auth ViewModels — `AuthViewModel` + `LoginViewModel` | ❌ Open | ✅ Resolved | `AuthViewModel.kt` and `AuthenticateAccountScreen.kt` deleted; `RegistrationViewModel.kt` added to replace the registration half; no dead references remain |
| 5 | RLS missing on `dialogue` / `dialogue_message` | ❌ Critical | ✅ Resolved | Migration `003_chat_realtime_and_policies.sql` enables RLS and adds participant-scoped policies for SELECT/INSERT/UPDATE on both tables; Realtime publication added |
| 6 | RLS missing on all other public tables | ❌ Critical | ❌ Open | `dialogue` + `dialogue_message` now protected (see #5); remaining 13 tables (`account`, `item`, `item_photo`, `item_offer`, `purchase`, `item_category`, `item_condition`, `status`, `rating`, `account_following`, `account_rating`, `account_favorite`, `account_side_information`) still have no RLS |
| 7 | Client-side fee calculation (`ItemDetailViewModel.kt:38-39`) | ⚠ High | ❌ Open | `SHIPPING_FEE = 3.95`, `BUYER_PROTECTION_FEE = 0.90` still hardcoded in client; client still sends fee amounts to DB; spoofable |
| 8 | `markAsSold()` + `updateItemToListed()` no ownership check | ⚠ High | ❌ Open | No RLS on `item` table; app-level guard absent; any authenticated user can mark any item sold |
| 9 | `item-photos` storage upload path not user-scoped | ⚠ High | ❌ Open | `001_create_item_photos_bucket.sql` policy still only checks `bucket_id = 'item-photos'`; any authenticated user can overwrite `items/<other_user_id>/...` |
| 10 | No file type/size limit on `item-photos` bucket | 🟡 Medium | ❌ Open | `file_size_limit = NULL`, `allowed_mime_types = NULL` unchanged |
| 11 | `SessionManager.currentAccountId` default was `0` | 🟡 Medium | ✅ Resolved | Changed to `NO_ACCOUNT_ID = -1`; `isLoggedIn()` guard added; `confirmBuy()`/`submitOffer()` pass through `SessionManager.currentAccountId` which is now `-1` (not `0`) when unset — still no null-guard in call sites but sentinel is no longer a valid account ID |
| 12 | Dead files `ChatRepository.kt`, `SupabaseConfig.kt` | ❌ Open | ❌ Open | Both files still present; confirmed no production references; safe to delete |
| 13 | Missing indexes on `purchase(buyer_id, seller_id)` | ❌ Open | ❌ Open | `init.sql` still has no index on `purchase` table; order history full-scans |
| 14 | Push notifications — no FCM / background delivery | ❌ Missing | 🔶 Partial | `MessageNotificationController` + `VinderNotifications` deliver local notifications via Supabase Realtime while app is foregrounded; no FCM = no delivery when app is killed |

---

### New Issues Found

| # | Area | Severity | Description | File:Line |
|---|------|----------|-------------|-----------|
| N1 | Notifications | Low | `NotificationPreferences.init()` is called in `MainActivity.onCreate()`, not in `VinderApplication.onCreate()`. Any future background component (Service, BroadcastReceiver) that reads `NotificationPreferences` before `MainActivity` starts would silently use default values rather than persisted prefs. Not currently exploitable (no background components), but fragile as the notification layer grows. | `VinderApplication.kt:8`, `MainActivity.kt:61` |
| N2 | Chat notifications | Low | `MessageNotificationController.handleIncoming()` fires two sequential Postgrest queries per incoming message (dialogue participation check + sender name lookup). Under high chat volume this creates significant query churn and could hit Supabase rate limits. Consider pre-fetching participant IDs when the subscription is opened or caching sender names. | `MessageNotificationController.kt:76-90` |

---

### Regressions

None.

---

### Updated Feature Completeness Matrix

Only features whose status changed since the 2026-06-18 audit:

| Feature | Old Status | New Status | Notes |
|---------|-----------|------------|-------|
| Session persistence | ❌ | ✅ | `SplashViewModel` + `AccountPreferences`; SDK session auto-persisted by supabase-kt |
| Password reset | 🔶 | ✅ | Full 3-step OTP flow wired end-to-end |
| Notification settings | ❌ | 🔶 | Prefs now persist; in-app message notifications delivered via Realtime; no background push (FCM) |
| Push notifications | ❌ | 🔶 | Foreground-only Realtime delivery; FCM not integrated |

---

## AUDIT REPORT (Full — Updated)

### Project Overview

- **Architecture**: MVVM with interface-backed repositories. No Clean Architecture layers (domain layer absent). Manual state-machine navigation in `MainActivity.kt` (no NavHost/NavGraph).
- **Total screens/flows**: 15 screens (3 auth, 1 forgot-password — now functional, 5 main tabs, 6 modal overlays)
- **Supabase services in use**: Auth (GoTrue), Postgrest, Realtime (chat + notifications), Storage (`item-photos` bucket)
- **DI framework**: None — repositories instantiated as default parameter values in ViewModels

---

### Feature Completeness Matrix

| Feature | Status | Notes |
|---|---|---|
| Sign up | ✅ | Email + OTP verify flow works |
| Sign in | ✅ | GoTrue email/password |
| Sign out | ✅ | `SettingsViewModel` → `AuthRepository.logout()` |
| Password reset | ✅ | `ForgotPasswordViewModel` — email → OTP → new password — fully wired |
| Session persistence | ✅ | `SplashViewModel` restores GoTrue session + `AccountPreferences`; survives process kill |
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
| Message notifications | 🔶 | Local notifications via Realtime while foregrounded; no FCM background delivery |
| Notification settings | 🔶 | Prefs persist via SharedPreferences; no server-side preference sync |
| Message attachments | ❌ | `dialogue_message_attachment` table in schema; zero app code touches it |
| Ratings & reviews | ❌ | `account_rating` + `rating` tables in schema; zero app code or UI |
| Followers | 🔶 | `account_following` table exists, follower count shown in profile stats, but follow/unfollow action not implemented |
| Admin / moderation | ❌ | None |
| AI-powered search | ❌ | Stated as key feature in CLAUDE.md; not implemented |

---

### Supabase / Database Findings

**Schema summary** (17 tables): `item_condition`, `status`, `rating`, `item_category`, `account`, `account_side_information`, `account_authentication`, `account_following`, `account_rating`, `item`, `item_photo`, `account_favorite`, `item_offer`, `purchase`, `dialogue`, `dialogue_message`, `dialogue_message_attachment`

**CRITICAL — RLS still missing on 13 public tables.** Migration `003` added RLS to `dialogue` and `dialogue_message`. The following tables remain completely open: `account`, `item`, `item_photo`, `item_offer`, `purchase`, `item_category`, `item_condition`, `status`, `rating`, `account_following`, `account_rating`, `account_favorite`, `account_side_information`. Any authenticated user with the anon key can read and write every row in these tables.

**Storage bucket issues (unchanged):**
- `item-photos` bucket: `file_size_limit = NULL`, `allowed_mime_types = NULL` — no size cap, any file type accepted
- Upload policy checks only `bucket_id = 'item-photos'`, not path prefix — any authenticated user can overwrite `items/<other_user_id>/photo_N.jpg` by guessing the path
- Avatars share the `item-photos` bucket under `avatars/` prefix; no dedicated bucket

**Missing indexes on `purchase` table (unchanged):** No index on `buyer_id` or `seller_id` — order history queries will full-scan at scale.

**Schema concern — `account.password_hash` (unchanged):** Column still defined with `NOT NULL`. Migration `002` now populates it with the literal `'supabase_managed'` as a placeholder — reduces confusion slightly but the column should be dropped.

**`dialogue` UNIQUE constraint (unchanged):** `UNIQUE(dialogue_creator_id, dialogue_receiver_id)` — ordered pair, not unordered. Alice→Bob and Bob→Alice can create two separate threads.

**`account_authentication` table (unchanged):** Stores OTP hash + expiry; app uses Supabase built-in email OTP. Unused dead schema.

**New — Migration `002` auto-creates account rows:** `handle_auth_user_created()` trigger inserts a row into `public.account` on GoTrue user creation using `nickname` from `raw_user_meta_data`, falling back to email prefix. This closes the gap where `login()` previously queried for an account row that might not exist. Good addition.

**New — Realtime publication for chat:** Migration `003` adds `dialogue_message` and `dialogue` to `supabase_realtime` publication with `REPLICA IDENTITY FULL`. Required for `MessageNotificationController` to receive inserts.

---

### Code Quality Findings

1. **`data/ChatRepository.kt`** — Still a stub (`return emptyList()`, `= Unit` bodies). Never imported anywhere. Dead file.

2. **`data/SupabaseConfig.kt`** — Still a `@Deprecated` shim. Referenced nowhere in production code. Dead file.

3. **`ItemDetailViewModel.kt:38-39`** — `SHIPPING_FEE` and `BUYER_PROTECTION_FEE` constants still client-side. Client sends fee values to DB — anyone can send $0 fees.

4. **`ItemDetailViewModel.kt:105`** — `memberSince = "-"` still hardcoded. `account` table has no `created_at` column; `item` table now has `created_at` but account join date is not derivable without a schema change.

5. **`getFeedItems()` — no pagination (unchanged).** Fetches all `is_listed=true` items in one query. Will break at scale.

6. **`MainActivity.kt` — Manual boolean-flag navigation (unchanged).** No deep-link support, no back-stack, no predictive back gesture support.

7. **`MessageNotificationController.kt:76-90`** — Two sequential Postgrest queries per incoming message. At scale, consider caching participant IDs at subscription open time.

8. **`markAsSold()` / `updateItemToListed()` ownership (unchanged).** No RLS on `item` table; any authenticated user can mark any item sold.

9. **`ItemDetailViewModel.kt` — `confirmBuy()` doesn't guard against self-purchase.** A seller can buy their own item, inserting a purchase record. Should check `buyerId != sellerId`.

---

### Security Findings

| Finding | Severity | Status | Detail |
|---|---|---|---|
| No RLS on 13 public tables | CRITICAL | ❌ Open | `account`, `item`, `item_photo`, `item_offer`, `purchase` and 8 others readable/writable by any authenticated anon-key user |
| RLS on `dialogue` + `dialogue_message` | HIGH | ✅ Resolved | Migration `003` — participant-scoped policies added |
| Client-side fee calculation | HIGH | ❌ Open | `SHIPPING_FEE` and `BUYER_PROTECTION_FEE` constants in `ItemDetailViewModel.kt:38-39`; client sends fee values to DB |
| `markAsSold()` no ownership check | HIGH | ❌ Open | `ItemRepository.kt` — no RLS or app-level guard; any user can mark any item sold |
| `item-photos` upload path not restricted | HIGH | ❌ Open | Storage policy allows upload to any path; malicious user can overwrite other users' photos |
| No file type/size limit on storage | MEDIUM | ❌ Open | Bucket accepts any file; denial-of-storage risk |
| `SessionManager` in-memory only | MEDIUM | ✅ Resolved | `AccountPreferences` + `SplashViewModel` restore session on relaunch |
| `SessionManager.currentAccountId` default `0` | MEDIUM | ✅ Resolved | Changed to `NO_ACCOUNT_ID = -1`; no longer maps to a valid account ID |
| `account.password_hash` column | LOW | ❌ Open | Dead column; migration `002` writes `'supabase_managed'` placeholder — still misleading, should be dropped |
| API keys via BuildConfig | ✅ OK | — | `SUPABASE_URL` and `SUPABASE_ANON_KEY` injected from `local.properties` via `BuildConfig` |
| No hardcoded secrets | ✅ OK | — | Grep over all `.kt` files returned no matches |

---

## IMPLEMENTATION PLAN (Updated)

### Priority 1 — Blockers (cannot ship without)

- [ ] **Enable RLS on remaining 13 public tables** — `account`, `item`, `item_photo`, `item_offer`, `purchase`, `item_category`, `item_condition`, `status`, `rating`, `account_following`, `account_rating`, `account_favorite`, `account_side_information` — add via new migration; minimum policies: authenticated SELECT on lookup tables, owner-scoped INSERT/UPDATE/DELETE on `item`/`item_photo`/`item_offer`/`purchase`
- [ ] **Move fee calculation server-side** — `ItemDetailViewModel.kt:38-39`, `PurchaseRepository.kt` — fees must be computed in a Supabase Edge Function or DB trigger; any client-supplied fee amount should be rejected
- [ ] **Restrict storage upload path** — update `001_create_item_photos_bucket.sql` — `WITH CHECK` should enforce `(storage.foldername(name))[1] = auth.uid()::text`; same for `avatars/`

### Priority 2 — Core Incomplete Features

- [ ] **Listing edit/delete** — add `updateItem()` and `deleteItem()` to `IItemRepository`; new `EditListingScreen` reachable from `ProfileScreen` owned listings tab — **M**
- [ ] **Wishlist/favorites** — wire `account_favorite` table; add heart icon on `ItemDetailScreen` + `HomeScreen` cards; `AccountRepository.toggleFavorite()` — **M**
- [ ] **Follower follow/unfollow action** — `AccountRepository.follow/unfollow()`; wire button in `SellerPublicProfileScreen` (stat count already displayed) — **S**
- [ ] **Server-side search** — `SearchResultsScreen` currently filters in-memory list; replace with `client.from("item").select { filter { ilike("item_name", "%$query%") } }` at minimum — **L**
- [ ] **Push notifications (FCM)** — integrate `firebase-messaging`; store FCM token in `account_side_information` or new table; Edge Function to send on message/offer/sale events — **L**
- [ ] **Notification settings sync** — write per-user preferences to Supabase rather than local SharedPreferences only; required for multi-device support — **S**

### Priority 3 — Quality & Security Fixes

- [ ] **Guard against self-purchase** — `ItemDetailViewModel.confirmBuy()` — check `buyerId != sellerId` before creating purchase — **S**
- [ ] **Delete dead files** — `data/ChatRepository.kt`, `data/SupabaseConfig.kt` — **S**
- [ ] **Feed pagination** — `ItemRepository.getFeedItems()` — add `.range(offset, offset+PAGE_SIZE-1)` and cursor-based loading in `HomeViewModel` — **S**
- [ ] **Storage file type + size limits** — update `001_create_item_photos_bucket.sql`; set `file_size_limit = 5242880` (5 MB), `allowed_mime_types = ['image/jpeg','image/png','image/webp']` — **S**
- [ ] **Add `created_at` to `account` table** — needed to populate `memberSince` in `ItemDetailViewModel.kt:105`; add migration — **S**
- [ ] **Remove `account.password_hash`** — dead column; remove via migration — **S**
- [ ] **Drop `account_authentication` table** — unused since GoTrue handles OTP; add migration — **S**
- [ ] **Fix `dialogue` UNIQUE constraint** — current `UNIQUE(creator, receiver)` is ordered; add canonical ordering in app or change constraint to `UNIQUE(LEAST(a,b), GREATEST(a,b))` — **S**
- [ ] **Add indexes on `purchase(buyer_id, seller_id)`** — missing from `init.sql`; needed for order history at scale — **S**
- [ ] **Add DI framework (Hilt)** — repositories new-ed inside ViewModels; add `@HiltViewModel` + `@Inject` — **M**
- [ ] **Replace manual nav with Jetpack Navigation** — `MainActivity.kt` boolean-flag overlay system; add `NavHost` + typed routes for deep-link and predictive back support — **L**
- [ ] **Move `NotificationPreferences.init()` to `VinderApplication`** — avoids fragility if a future background component reads prefs before `MainActivity` starts — **S**
- [ ] **Cache sender names in `MessageNotificationController`** — avoids N+1 queries per incoming message — **S**

### Priority 4 — Nice-to-Have / Polish

- [ ] **AI-powered search** — stated as key feature; add pgvector extension + embedding column on `item`; Edge Function to vectorize descriptions on insert; search via cosine similarity — **L**
- [ ] **Dedicated avatar storage bucket** — separate `avatars` bucket from `item-photos`; cleaner access policies and quotas — **S**
- [ ] **`memberSince` display** — once `account.created_at` added, format and display in `ItemDetailScreen`
- [ ] **Message attachments** — `dialogue_message_attachment` table exists; wire photo-sending in `ChatScreen` — **M**
- [ ] **Dark mode persistence** — `SettingsViewModel` dark mode toggle; persist to DataStore — **S**
- [ ] **Error recovery UI** — most screens show error state text but no retry button; add `Button("Retry") { viewModel.load() }` pattern — **S**
- [ ] **Listing image picker UX** — verify `AddProductScreen` allows picking multiple photos from gallery; test on physical device
