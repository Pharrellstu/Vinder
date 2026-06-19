# Vinder — Comprehensive Audit Report

> Generated: 2026-06-18  
> Re-audit: 2026-06-19

---

## Changelog — Updates Since This Audit (2026-06-18)

The following were implemented/fixed in a follow-up session, after the audit below was generated. Feature matrix and priority list further down have been updated to match; this section gives the technical detail.

- **Home feed filtering is now real, not cosmetic.** `HomeViewModel` now holds category/search/price-bucket filter state and re-derives `gridItems`/`saleItems` from an in-memory cached feed on every change (`onCategorySelected`, `onSearchQueryChanged`, `onPriceBucketSelected` in `HomeViewModel.kt`). Previously the category chip row updated its own look via local Compose `remember` state but never touched the rendered list.
- **Search matching is word-boundary prefix, not whole-string prefix.** Typing `"A"` matches any item whose name has a word starting with `A`; typing `"Air Force"` matches `"Nike Air Force"` because the match starts at each word boundary, not just index 0 (`HomeViewModel.matchesSearch`).
- **`SearchResultsScreen` (the Search tab) was non-functional and is now wired up:**
  - The category/condition/size/price filters in `FilterBottomSheet` updated state but were never applied to the rendered list (`SearchResultsGrid` always received a static hardcoded sample list). Added `matchesFilters()` and derive `filteredResults` from it.
  - The search field was a static, non-editable `Text` with no-op back/clear callbacks. Replaced with a real `BasicTextField`, wired `onBack`/`onClear`/`onQueryChange`.
  - The "Sort" chip was a no-op. Added a `SortOrder` enum (None → Price ↑ → Price ↓) the chip cycles through.
  - Default filter selection pre-applied `categories = {Women, Kids}` / `conditions = {Like new, Good}` — tuned for the old sample data. Defaulted to an empty `SearchFilters()` so real backend items aren't silently hidden behind stale presets.
- **Search is now server-side, against real Supabase data — the hardcoded sample list is gone.** `IItemRepository.getFeedItems(searchQuery: String? = null)` now takes an optional query and applies `ilike("item_name", "%query%")` in the Postgrest filter when present, reusing the existing seller/category/favorite join logic via the shared `buildProducts()` helper (no duplicated repository code). `SearchResultsViewModel` (new) calls this on every keystroke via `SearchResultsTopBar`'s `onQueryChange`/`onClear`, with `Loading`/`Success`/`Error` UI states. AI-powered search is explicitly out of scope for now (per product decision), so the existing pgvector/Edge Function plan in Priority 4 stands as future work, not done here.
- **`Product.condition` is now populated for real items.** `buildProducts()` previously only resolved `category` from `item_category`; the Condition filter chips (New/Like new/Good/Fair) would have matched against an always-blank field for any real (non-sample) item. Added the same `item_condition_id → item_condition_name` lookup used for category, mirroring the existing pattern.
- **Fixed duplicate category chips.** `database/accounts.sql` and `database/seed_test_data.sql` both insert overlapping category names (`Electronics`, `Books`, `Sports`) and `item_category.category_name` had no `UNIQUE` constraint, so `ON CONFLICT DO NOTHING` silently never fired — running both seed scripts (as setup instructions require) produces literal duplicate rows. Fixed by adding `.distinct()` in `ItemRepository.getCategories()` (works against already-duplicated existing data) and adding `UNIQUE` to `category_name` in `database/init.sql` (prevents it for fresh installs; does not retroactively fix already-seeded databases).
- **Unified top bar across Home / Search / Inbox.** `HomeScreen`'s and `SearchResultsScreen`'s top bars were custom `Row`s with ad hoc padding, inconsistent in height and status-bar inset handling from `MessagesScreen`'s Material3 `TopAppBar`/`CenterAlignedTopAppBar`. Both now use the same `TopAppBar` component, so all three screens share identical height and inset behavior.
- **Brand font applied to the in-app logo.** `HomeScreen`'s "Vinder" wordmark now uses `instrumentSerifNormal` (the same font family already used on Login/Register/ForgotPassword), instead of falling back to the default UI font.
- **Wishlist implemented end-to-end** (was previously schema-only, see Feature Matrix below):
  - `ItemRepository`: added `getFavoriteItemIds`, `getFavoriteItems`, `addFavorite`, `removeFavorite`, all backed by the existing `account_favorite` table (no migration needed). Refactored product-row building into a shared `buildProducts()` helper so `getFeedItems()` and `getFavoriteItems()` both attach `Product.isFavorite`.
  - `GridProductCard`'s heart button (previously decorative, no `onClick`) now toggles real, persisted favorite state — filled/outlined icon, optimistic UI update with rollback on failure (`HomeViewModel.onToggleFavorite`).
  - New `WishlistScreen` + `WishlistViewModel`, reachable via a new "Wishlist" button on `ProfileScreen`, wired through `MainActivity` the same way as the existing Offers/Order History overlays.
- **Verified clean after `develop` merge** (`8bd57b8`): no leftover conflict markers, `compileDebugKotlin`, `compileDebugAndroidTestKotlin`, and `assembleDebug` all succeed. Noted that `develop` independently fixed `getCategoryId`/`getConditionId` (switched `decodeSingle()` → `decodeList().firstOrNull()`) for the same duplicate-seed-row root cause described above — unrelated to this session's `.distinct()` fix on `getCategories()`, but the same underlying data issue.

## Changelog — Updates Since This Audit (2026-06-18, second session)

App is now Supabase-hosted on the web, not the local Docker stack this document otherwise assumes — schema/RLS changes are applied by the user directly in the hosted dashboard, so this session's fixes are application-layer only; no `database/*.sql` file was touched.

- **Add Product now actually creates a listing.** `AddProductScreen`'s category/condition picker chips were hardcoded string lists (`"Clothing"`, `"Toys"`, etc.) that didn't reliably match whatever rows happened to exist in `item_category`/`item_condition` for a given Supabase instance (`database/accounts.sql` and `database/seed_test_data.sql` seed different, only-partially-overlapping name sets). Picking a label with no matching DB row made `ItemRepository.getCategoryId`/`getConditionId` throw *before* `insertItem` was ever called, so the wizard would walk through all 3 steps and fail silently into a snackbar with nothing written to `item`. Fixed by adding `getCategoryNames()`/`getConditionNames()` to `ItemRepository` (real DB rows, no `"All"` prefix) and having `AddProductViewModel`/`AddProductScreen` source their chips from there instead of hardcoded constants — the picked label can no longer mismatch the DB.
- **Product images now render in the feed, search results, and wishlist.** `GridProductCard` and `SearchResultCard` had image boxes with no `AsyncImage` in them at all — confirmed by reading the code, not inferred. `Product` had no image field, and `ItemRepository.buildProducts()` never queried `item_photo`. Added `Product.coverImageUrl`, populated it via a bulk `item_photo` lookup in `buildProducts()` (cover = lowest `item_photo_id`, same pattern already used by the profile grid's `coverUrlsByItem`), and wired both cards to render it with a placeholder fallback when null.
- **Sellers can no longer buy or make offers on their own listings.** The main feed includes the current user's own listings (`getFeedItems()` doesn't filter by seller), and `ItemDetailScreen`'s "Buy now"/"Make offer" buttons had no check against the viewer's identity. Added `isOwnListing` check in `ItemDetailScreen` (shows a "This is your listing" bar instead of the buy/offer buttons) plus a defense-in-depth guard in `ItemDetailViewModel.confirmBuy()`/`submitOffer()` that rejects the action before any network call if `sellerId == buyerId`/`creatorId`.
- **Chat now supports image attachments, with a WhatsApp-style send preview.** Tapping the (previously decorative, unwired) circular icon slot left of the chat text field opens a gallery picker; the picked image opens a full-screen preview (image, cancel, optional one-line caption, send) before anything is uploaded — nothing sends on pick alone. Confirmed sends upload to the existing public `item-photos` bucket under a `chat/{dialogueId}/...` path (no new bucket/policy needed — that bucket's upload policy isn't path-restricted) and insert a `dialogue_message` + `dialogue_message_attachment` row. `ChatMessage` gained `attachmentUrl`; `DialogueRepository`'s four message-building call sites populate it via a bulk join, mirroring the existing cover-photo pattern.
  - **Found and fixed a real display bug in the same feature:** the first implementation pass relied on a second Realtime subscription on `dialogue_message_attachment` inserts to learn about new attachments, but that table was never added to the `supabase_realtime` publication (only `dialogue_message`/`dialogue` were, per `database/migrations/003_chat_realtime_and_policies.sql`) — so those events could never arrive, and every image permanently rendered as a "📷 Photo" text placeholder instead of the photo, for both sender and recipient. Fixed by dropping that dead subscription and instead doing a direct Postgrest lookup of the attachment row whenever any new `dialogue_message` insert arrives (cheap, since chat messages arrive at human-typing cadence, not high QPS) — works identically regardless of whether the image has a caption.

## Changelog — Updates Since This Audit (2026-06-18, third session)

- **Wishlist heart added to `ItemDetailScreen` and RLS enabled for `account_favorite`.** The heart toggle was previously only on `GridProductCard` in the feed; `ItemDetailScreen` now also shows a filled/outlined heart in the top bar that persists to `account_favorite`. Migration `006` adds `ENABLE ROW LEVEL SECURITY` + owner-scoped SELECT/INSERT/DELETE policies on `account_favorite` — the first application-data table (outside `dialogue`/`dialogue_message`) to be protected.
- **Listing edit/delete (My Listings) implemented end-to-end.** `ProfileScreen` now has a "My Listings" tab that shows the current user's own items. Tapping a listing opens `ItemDetailScreen` with an edit mode: name, description, price, condition, and category are all editable in-place. Mark-as-sold and delete are available from both `ItemDetailScreen` (owner view) and the My Listings tab. `ItemRepository` gained `updateItem()` and `deleteItem()`; ownership is enforced at the app level (seller ID check) and will additionally be covered by the Priority 1 RLS migration on `item`.
- **Opening item detail from search results.** `SearchResultsScreen` cards were previously non-tappable; tapping a `SearchResultCard` now navigates to `ItemDetailScreen` for that item, consistent with the home feed.

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
| 6 | RLS missing on all other public tables | ❌ Critical | ✅ Resolved | All 12 remaining tables now protected; migration `007_rls_remaining_tables.sql` enables RLS and adds appropriate policies on `account`, `account_side_information`, `item`, `item_photo`, `item_offer`, `purchase`, `status`, `rating`, `account_following`, `account_rating` (also activates inert 004/005 policies on `item`/`item_photo`) |
| 7 | Client-side fee calculation (`ItemDetailViewModel.kt:38-39`) | ⚠ High | ❌ Open | `SHIPPING_FEE = 3.95`, `BUYER_PROTECTION_FEE = 0.90` still hardcoded in client; client still sends fee amounts to DB; spoofable |
| 8 | `markAsSold()` + `updateItemToListed()` no ownership check | ⚠ High | ✅ Resolved | `item_update_own` policy (migration 004) now active — RLS enabled on `item` by migration 007; DB rejects updates where `seller_id ≠ current_account_id()` |
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
| Listings (create) | ✅ | `AddProductScreen` + full photo upload; category/condition chips load real DB names — can no longer fail on name mismatch |
| Listings (edit/delete) | ✅ | My Listings tab on `ProfileScreen`; in-place edit of name/description/price/condition/category; mark-as-sold and delete from `ItemDetailScreen` owner view and My Listings tab; `ItemRepository.updateItem()`/`deleteItem()` with app-level seller-ID ownership check |
| Search | ✅ | `SearchResultsScreen` does real server-side search via `ItemRepository.getFeedItems(searchQuery)` → Postgrest `ilike`; `SearchResultsViewModel` calls on every keystroke; tapping a result opens `ItemDetailScreen`. `HomeScreen` search is client-side word-boundary prefix. AI-powered search explicitly deferred |
| Filters / sorting | 🔶 | `HomeScreen` category + price-bucket filters wired to `HomeViewModel` and filter the live feed. `SearchResultsScreen` `FilterBottomSheet` and sort control functional but filter client-side on server-fetched results; `size` has no backing DB column |
| Product detail view | ✅ | `ItemDetailScreen` with photo carousel, description, seller card |
| Cart | ❌ | No multi-item cart concept; items are bought individually. `account_favorite` is used by Wishlist |
| Wishlist | ✅ | `account_favorite`-backed; heart button on `GridProductCard` and `ItemDetailScreen` toggles persisted state; `WishlistScreen` reachable from `ProfileScreen`; RLS via migration 006 |
| Buy flow | 🔶 | `confirmBuy()` inserts to `purchase` and marks item sold, but fees calculated client-side (spoofable); no payment gateway. Self-purchase/self-offer blocked app-side |
| Payment integration | ❌ | No Stripe/payment SDK; purchase is a direct DB insert |
| Order history | ✅ | `OrderHistoryScreen` + `OrderHistoryViewModel`; shows item name, fee breakdown, total, date |
| Make offer | ✅ | `submitOffer()` inserts to `item_offer` |
| Offer management | ✅ | `OffersScreen` + `OffersViewModel`; seller can accept/reject; accept chains purchase creation + markAsSold |
| Messaging list | ✅ | `MessagesScreen` loads conversations from DB |
| Chat | ✅ | `ChatScreen` with Supabase Realtime subscription |
| Message attachments | ✅ | Gallery picker, full-screen send preview with optional caption, upload to `item-photos` under `chat/{dialogueId}/`, `dialogue_message_attachment` row; both sides see image via direct Postgrest lookup |
| Message notifications | 🔶 | Local notifications via Realtime while foregrounded; no FCM background delivery |
| Notification settings | 🔶 | Prefs persist via SharedPreferences; no server-side preference sync |
| Ratings & reviews | ❌ | `account_rating` + `rating` tables in schema; zero app code or UI |
| Followers | 🔶 | `account_following` table exists, follower count shown in profile stats, but follow/unfollow action not implemented |
| Admin / moderation | ❌ | None |
| AI-powered search | ❌ | Stated as key feature in CLAUDE.md; not implemented (explicitly deferred) |

---

### Supabase / Database Findings

**Schema summary** (17 tables): `item_condition`, `status`, `rating`, `item_category`, `account`, `account_side_information`, `account_authentication`, `account_following`, `account_rating`, `item`, `item_photo`, `account_favorite`, `item_offer`, `purchase`, `dialogue`, `dialogue_message`, `dialogue_message_attachment`

**CRITICAL — RLS still missing on 12 public tables.** Migration `003` added RLS to `dialogue` and `dialogue_message`; migration `006` added RLS to `account_favorite`. The following tables remain completely open: `account`, `item`, `item_photo`, `item_offer`, `purchase`, `item_category`, `item_condition`, `status`, `rating`, `account_following`, `account_rating`, `account_side_information`. Any authenticated user with the anon key can read and write every row in these tables.

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

8. **`markAsSold()` / `updateItemToListed()` — app-level ownership guard added; DB-level still missing.** `ItemRepository` now checks seller ID before calling these; RLS on `item` table (Priority 1) will enforce at DB level.

9. **`ItemDetailViewModel.kt` — `confirmBuy()` self-purchase now guarded.** `isOwnListing` check in `ItemDetailScreen` hides Buy/Offer on the viewer's own listings; `confirmBuy()`/`submitOffer()` also reject `sellerId == buyerId`/`creatorId` before any network call.

---

### Security Findings

| Finding | Severity | Status | Detail |
|---|---|---|---|
| No RLS on 12 public tables | CRITICAL | ✅ Resolved | Migration `007` enables RLS and adds policies on all remaining tables |
| RLS on `dialogue` + `dialogue_message` | HIGH | ✅ Resolved | Migration `003` — participant-scoped policies added |
| RLS on `account_favorite` | MEDIUM | ✅ Resolved | Migration `006` — owner-scoped SELECT/INSERT/DELETE policies added |
| Client-side fee calculation | HIGH | ❌ Open | `SHIPPING_FEE` and `BUYER_PROTECTION_FEE` constants in `ItemDetailViewModel.kt:38-39`; client sends fee values to DB |
| `markAsSold()` no DB-level ownership check | HIGH | ✅ Resolved | `item_update_own` policy now enforced at DB level (migration 007 enabled RLS on `item`) |
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

- [x] **Enable RLS on remaining 12 public tables** — migration `007_rls_remaining_tables.sql` applied; all tables protected with appropriate participant/owner-scoped policies ✅
- [ ] **Move fee calculation server-side** — `ItemDetailViewModel.kt:38-39`, `PurchaseRepository.kt` — fees must be computed in a Supabase Edge Function or DB trigger; any client-supplied fee amount should be rejected
- [ ] **Restrict storage upload path** — update `001_create_item_photos_bucket.sql` — `WITH CHECK` should enforce `(storage.foldername(name))[1] = auth.uid()::text`; same for `avatars/`

### Priority 2 — Core Incomplete Features

- [x] **Listing edit/delete** — My Listings tab on `ProfileScreen`; in-place edit from `ItemDetailScreen` owner view; `ItemRepository.updateItem()`/`deleteItem()` with app-level ownership check — **M** ✅
- [x] **Wishlist/favorites** — `account_favorite`-backed; heart button on `GridProductCard` and `ItemDetailScreen` toggles persisted state; `WishlistScreen` reachable from `ProfileScreen`; RLS migration 006 — **M** ✅
- [ ] **Follower follow/unfollow action** — `AccountRepository.follow/unfollow()`; wire button in `SellerPublicProfileScreen` (stat count already displayed) — **S**
- [x] **Server-side search** — `IItemRepository.getFeedItems(searchQuery)` applies `ilike("item_name", "%query%")` in Postgrest; `SearchResultsViewModel` calls on every keystroke; tapping result navigates to `ItemDetailScreen` — **L** ✅
- [ ] **Push notifications (FCM)** — integrate `firebase-messaging`; store FCM token in `account_side_information` or new table; Edge Function to send on message/offer/sale events — **L**
- [ ] **Notification settings sync** — write per-user preferences to Supabase rather than local SharedPreferences only; required for multi-device support — **S**

### Priority 3 — Quality & Security Fixes

- [ ] **Guard against self-purchase (DB-level)** — app-level guard added; RLS policies (Priority 1) will enforce at DB level for `markAsSold()`/`updateItemToListed()` — **S**
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
- [x] **Message attachments** — gallery picker + send preview + caption; upload to `item-photos` under `chat/{dialogueId}/`; `dialogue_message_attachment` wired end-to-end — **M** ✅
- [ ] **Dark mode persistence** — `SettingsViewModel` dark mode toggle; persist to DataStore — **S**
- [ ] **Error recovery UI** — most screens show error state text but no retry button; add `Button("Retry") { viewModel.load() }` pattern — **S**
- [ ] **Listing image picker UX** — verify `AddProductScreen` allows picking multiple photos from gallery; test on physical device
