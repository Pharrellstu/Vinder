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
| 7 | Client-side fee calculation (`ItemDetailViewModel.kt:38-39`) | ⚠ High | ⚠ Fix written, deployment unverified | Migration `011_enforce_purchase_fees_serverside.sql` adds a `BEFORE INSERT OR UPDATE` trigger on `public.purchase` that overwrites `shipping_fee`/`protection_fee`/`total_amount` server-side regardless of client-supplied values. Not yet confirmed live — verify with `select tgname from pg_trigger where tgrelid = 'public.purchase'::regclass` against the hosted project before treating this as closed |
| 8 | `markAsSold()` + `updateItemToListed()` no ownership check | ⚠ High | ✅ Resolved | `item_update_own` policy (migration 004) now active — RLS enabled on `item` by migration 007; DB rejects updates where `seller_id ≠ current_account_id()` |
| 9 | `item-photos` storage upload path not user-scoped | ⚠ High | ✅ Resolved (confirmed live 2026-06-21) | Migration `009_fix_item_photos_upload_scoping.sql` recreates the `"item-photos: authenticated upload"` policy with per-prefix ownership scoping. Initially found NOT applied (live `with_check` was still the unscoped migration-001 policy); re-verified after the user ran the migration — live `with_check` now matches 009 exactly: `items/<itemId>` gated on `seller_id = current_account_id()`, `avatars/<accountId>` gated on `current_account_id()`, `chat/<dialogueId>` gated on dialogue participancy. |
| 10 | No file type/size limit on `item-photos` bucket | 🟡 Medium | ✅ Resolved | Migration `008` sets `file_size_limit = 5 MB`, `allowed_mime_types = [jpeg, png, webp]` |
| 11 | `SessionManager.currentAccountId` default was `0` | 🟡 Medium | ✅ Resolved | Changed to `NO_ACCOUNT_ID = -1`; `isLoggedIn()` guard added; `confirmBuy()`/`submitOffer()` pass through `SessionManager.currentAccountId` which is now `-1` (not `0`) when unset — still no null-guard in call sites but sentinel is no longer a valid account ID |
| 12 | Dead files `ChatRepository.kt`, `SupabaseConfig.kt` | ❌ Open | ✅ Resolved | Both files deleted; commit `e0d3893` |
| 13 | Missing indexes on `purchase(buyer_id, seller_id)` | ❌ Open | ✅ Resolved | Migration `008` adds `idx_purchase_buyer` + `idx_purchase_seller` |
| 14 | Push notifications — no background delivery | ❌ Missing | 🔶 Partial | **Root cause (found via on-device logcat):** background-delivered inserts WERE arriving over Realtime, but `handleIncoming` re-verified dialogue participation with a Postgrest read of `dialogue` — which from the foreground-less service has no auth token, so RLS returned empty and every notification was silently dropped. Fix: removed that redundant lookup (Realtime already enforces the `dialogue_message` participant RLS — verified: a non-participant insert is not delivered). Also: `MessageListenerService` foreground service, in-service session restore (`ensureSession`), network-regain reconnect, battery-opt prompt (`MainActivity`), `BootReceiver`. Verified on emulator (API 37): notification posts while backgrounded. **Activity notifications added:** the same listener now also streams `item_offer`, `purchase`, `account_following` inserts (migration `009`) and posts Offers / Item sold / New followers notifications, filtered to the recipient using only the streamed row (no background Postgrest). Notification settings were trimmed to only the deliverable options (New messages, Offers, Item sold, New followers); message-requests, reviews, and all marketing toggles were removed since nothing creates those events / there's no push backend. All four verified on-device. Known gaps — sender name falls back to "New message" in the background (the restored service session had an expired access token and no refresh token, so authed Postgrest reads fail there — a session-persistence issue worth a separate look), and Google-free means no FCM so no delivery when swiped-away/deep-Doze |

---

### New Issues Found

| # | Area | Sev[local.properties.example](local.properties.example)erity | Description | File:Line |
|---|------|----------|-------------|-----------|
| N1 | Notifications | Low | ✅ Resolved | `NotificationPreferences.init()` moved to `VinderApplication.onCreate()`; commit `e0d3893` | `VinderApplication.kt` |
| N2 | Chat notifications | Low | ✅ Resolved | `senderNameCache` added to `MessageNotificationController`; sender name Postgrest query now fires at most once per sender per session; commit `e0d3893` | `MessageNotificationController.kt` |

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
| Push notifications | ❌ | 🔶 | Realtime delivery kept alive in background by `MessageListenerService` (session restore + reconnect + battery prompt + boot restart); no FCM, so no killed-app/deep-Doze delivery |

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
| Avatar upload | 🔶 | Uploads to `item-photos` bucket under `avatars/` prefix — no dedicated bucket; size/type limits now enforced (migration 008) |
| Listings (create) | ✅ | `AddProductScreen` + full photo upload; category/condition chips load real DB names — can no longer fail on name mismatch |
| Listings (edit/delete) | ✅ | My Listings tab on `ProfileScreen`; in-place edit of name/description/price/condition/category; mark-as-sold and delete from `ItemDetailScreen` owner view and My Listings tab; `ItemRepository.updateItem()`/`deleteItem()` with app-level seller-ID ownership check |
| Search | ✅ | `SearchResultsScreen` does real server-side search via `ItemRepository.getFeedItems(searchQuery)` → Postgrest `ilike`; `SearchResultsViewModel` calls on every keystroke; tapping a result opens `ItemDetailScreen`. `HomeScreen` search is client-side word-boundary prefix. AI-powered search explicitly deferred |
| Filters / sorting | 🔶 | `HomeScreen` category + price-bucket filters wired to `HomeViewModel` and filter the live feed. `SearchResultsScreen` `FilterBottomSheet` and sort control functional but filter client-side on server-fetched results; `size` has no backing DB column |
| Product detail view | ✅ | `ItemDetailScreen` with photo carousel, description, seller card |
| Cart | ❌ | No multi-item cart concept; items are bought individually. `account_favorite` is used by Wishlist |
| Wishlist | ✅ | `account_favorite`-backed; heart button on `GridProductCard` and `ItemDetailScreen` toggles persisted state; `WishlistScreen` reachable from `ProfileScreen`; RLS via migration 006 |
| Buy flow | 🔶 | `confirmBuy()` inserts to `purchase` and marks item sold; server-side fee trigger written (migration `011`) but not yet confirmed deployed — still spoofable until verified live; no payment gateway. Self-purchase/self-offer blocked app-side |
| Payment integration | ❌ | No Stripe/payment SDK; purchase is a direct DB insert |
| Order history | ✅ | `OrderHistoryScreen` + `OrderHistoryViewModel`; shows item name, fee breakdown, total, date |
| Make offer | ✅ | `submitOffer()` inserts to `item_offer` |
| Offer management | ✅ | `OffersScreen` + `OffersViewModel`; seller can accept/reject; accept chains purchase creation + markAsSold |
| Messaging list | ✅ | `MessagesScreen` loads conversations from DB |
| Chat | ✅ | `ChatScreen` with Supabase Realtime subscription |
| Message attachments | ✅ | Gallery picker, full-screen send preview with optional caption, upload to `item-photos` under `chat/{dialogueId}/`, `dialogue_message_attachment` row; both sides see image via direct Postgrest lookup |
| Message notifications | 🔶 | Realtime notifications via `MessageListenerService` foreground service (works backgrounded/locked); no FCM = no killed-app delivery |
| Notification settings | 🔶 | Prefs persist via SharedPreferences; no server-side preference sync |
| Ratings & reviews | ❌ | `account_rating` + `rating` tables in schema; zero app code or UI |
| Followers | ✅ | `AccountRepository.follow/unfollow/isFollowing()`; `ProfileViewModel` exposes `isFollowing` StateFlow with optimistic update + rollback; `SellerPublicProfileScreen` Follow button persists to DB |
| Admin / moderation | ❌ | None |
| AI-powered search | ❌ | Stated as key feature in CLAUDE.md; not implemented (explicitly deferred) |

---

### Supabase / Database Findings

**Schema summary** (17 tables): `item_condition`, `status`, `rating`, `item_category`, `account`, `account_side_information`, `account_authentication`, `account_following`, `account_rating`, `item`, `item_photo`, `account_favorite`, `item_offer`, `purchase`, `dialogue`, `dialogue_message`, `dialogue_message_attachment`

**CRITICAL — RLS still missing on 12 public tables.** Migration `003` added RLS to `dialogue` and `dialogue_message`; migration `006` added RLS to `account_favorite`. The following tables remain completely open: `account`, `item`, `item_photo`, `item_offer`, `purchase`, `item_category`, `item_condition`, `status`, `rating`, `account_following`, `account_rating`, `account_side_information`. Any authenticated user with the anon key can read and write every row in these tables.

**Storage bucket issues (partially resolved):**
- `item-photos` bucket: `file_size_limit = 5 MB`, `allowed_mime_types = [image/jpeg, image/png, image/webp]` — set by migration `008` ✅
- Upload policy now enforces per-prefix ownership (migration `009`): `items/<itemId>` requires owning the item, `avatars/<accountId>` requires being that account, `chat/<dialogueId>` requires dialogue participation ✅ Resolved
- Avatars still share the `item-photos` bucket under `avatars/` prefix; no dedicated bucket 🔶 (cosmetic — path is now ownership-scoped)

**Missing indexes on `purchase` table:** ✅ Resolved — migration `008` adds `idx_purchase_buyer` and `idx_purchase_seller`.

**Schema concern — `account.password_hash`:** ✅ Resolved — dropped by migration `008`. `account.created_at` column added in the same migration.

**`dialogue` UNIQUE constraint:** ✅ Resolved — migration `008` drops the ordered `UNIQUE(creator, receiver)` constraint and replaces it with a canonical expression index on `(LEAST(creator,receiver), GREATEST(creator,receiver))`; `DialogueRepository.getOrCreateDialogue()` now normalises IDs (`minOf` → creator, `maxOf` → receiver) so inserts always use the canonical form.

**`account_authentication` table:** ✅ Resolved — dropped by migration `008`.

**New — Migration `002` auto-creates account rows:** `handle_auth_user_created()` trigger inserts a row into `public.account` on GoTrue user creation using `nickname` from `raw_user_meta_data`, falling back to email prefix. This closes the gap where `login()` previously queried for an account row that might not exist. Good addition.

**New — Realtime publication for chat:** Migration `003` adds `dialogue_message` and `dialogue` to `supabase_realtime` publication with `REPLICA IDENTITY FULL`. Required for `MessageNotificationController` to receive inserts.

---

### Code Quality Findings

1. ~~**`data/ChatRepository.kt`**~~ — ✅ Deleted (commit `e0d3893`).

2. ~~**`data/SupabaseConfig.kt`**~~ — ✅ Deleted (commit `e0d3893`).

3. **`ItemDetailViewModel.kt:38-39`** — `SHIPPING_FEE` and `BUYER_PROTECTION_FEE` constants still client-side. Migration `011_enforce_purchase_fees_serverside.sql` adds a trigger to overwrite fee values server-side, but it is **written, not yet confirmed applied** to the hosted project — do not treat as resolved until verified live.

4. **`ItemDetailViewModel.kt:105`** — `memberSince = "-"` still hardcoded. `account.created_at` column added by migration `008`; app code not yet wired to display it (see Priority 4 `memberSince` item).

5. ~~**`getFeedItems()` — no pagination.**~~ ✅ Resolved — `ItemRepository.PAGE_SIZE = 20`, offset-based pagination via `.range()`; `HomeViewModel.loadMore()` appends pages; `HomeScreen` triggers on scroll-to-end (commit `b816891`).

6. **`MainActivity.kt` — Manual boolean-flag navigation (unchanged).** No deep-link support, no back-stack, no predictive back gesture support.

7. ~~**`MessageNotificationController.kt:76-90`**~~ — ✅ Resolved — sender name cached via `senderNameCache` map; only one Postgrest lookup per unique sender per session (commit `e0d3893`).

8. **`markAsSold()` / `updateItemToListed()` — app-level ownership guard added; DB-level still missing.** `ItemRepository` now checks seller ID before calling these; RLS on `item` table (Priority 1) will enforce at DB level.

9. **`ItemDetailViewModel.kt` — `confirmBuy()` self-purchase now guarded.** `isOwnListing` check in `ItemDetailScreen` hides Buy/Offer on the viewer's own listings; `confirmBuy()`/`submitOffer()` also reject `sellerId == buyerId`/`creatorId` before any network call.

---

### Security Findings

| Finding | Severity | Status | Detail |
|---|---|---|---|
| No RLS on 12 public tables | CRITICAL | ✅ Resolved | Migration `007` enables RLS and adds policies on all remaining tables |
| RLS on `dialogue` + `dialogue_message` | HIGH | ✅ Resolved | Migration `003` — participant-scoped policies added |
| RLS on `account_favorite` | MEDIUM | ✅ Resolved | Migration `006` — owner-scoped SELECT/INSERT/DELETE policies added |
| Client-side fee calculation | HIGH | ⚠ Fix written, deployment unverified | Migration `011_enforce_purchase_fees_serverside.sql` — `BEFORE INSERT OR UPDATE` trigger on `public.purchase` recomputes `shipping_fee`/`protection_fee`/`total_amount` server-side. Written 2026-06-21, not yet confirmed live — run in the hosted SQL Editor and verify before trusting fee integrity |
| `markAsSold()` no DB-level ownership check | HIGH | ✅ Resolved | `item_update_own` policy now enforced at DB level (migration 007 enabled RLS on `item`) |
| `item-photos` upload path not restricted | HIGH | ✅ Resolved (confirmed live 2026-06-21) | Migration `009_fix_item_photos_upload_scoping.sql` enforces per-prefix ownership (items/avatars/chat) in the upload `WITH CHECK` — re-verified against the hosted `pg_policies` after the user applied it; live policy now matches |
| No file type/size limit on storage | MEDIUM | ✅ Resolved | Migration `008` sets 5 MB cap and jpeg/png/webp allow-list |
| `SessionManager` in-memory only | MEDIUM | ✅ Resolved | `AccountPreferences` + `SplashViewModel` restore session on relaunch |
| `SessionManager.currentAccountId` default `0` | MEDIUM | ✅ Resolved | Changed to `NO_ACCOUNT_ID = -1`; no longer maps to a valid account ID |
| `account.password_hash` column | LOW | ✅ Resolved | Column dropped by migration `008` |
| API keys via BuildConfig | ✅ OK | — | `SUPABASE_URL` and `SUPABASE_ANON_KEY` injected from `local.properties` via `BuildConfig` |
| No hardcoded secrets | ✅ OK | — | Grep over all `.kt` files returned no matches |

---

## IMPLEMENTATION PLAN (Updated)

### Priority 1 — Blockers (cannot ship without)

- [x] **Enable RLS on remaining 12 public tables** — migration `007_rls_remaining_tables.sql` applied; all tables protected with appropriate participant/owner-scoped policies ✅
- [ ] **Move fee calculation server-side** — migration `011_enforce_purchase_fees_serverside.sql` adds a DB trigger (`enforce_purchase_fees()`) on `public.purchase` that overwrites `shipping_fee`/`protection_fee`/`total_amount` server-side. Code/SQL written, **not yet confirmed run against the hosted project** — apply via SQL Editor and verify with `select tgname from pg_trigger where tgrelid = 'public.purchase'::regclass` before marking done
- [x] **Restrict storage upload path** — migration `009_fix_item_photos_upload_scoping.sql` recreates the upload policy with per-prefix ownership scoping (`items/<itemId>` → item owner, `avatars/<accountId>` → that account, `chat/<dialogueId>` → dialogue participant) via `public.current_account_id()`, cast on the integer id segment. **Confirmed live on 2026-06-21** — `pg_policies.with_check` for `"item-photos: authenticated upload"` now matches the migration exactly ✅

### Priority 2 — Core Incomplete Features

- [x] **Listing edit/delete** — My Listings tab on `ProfileScreen`; in-place edit from `ItemDetailScreen` owner view; `ItemRepository.updateItem()`/`deleteItem()` with app-level ownership check — **M** ✅
- [x] **Wishlist/favorites** — `account_favorite`-backed; heart button on `GridProductCard` and `ItemDetailScreen` toggles persisted state; `WishlistScreen` reachable from `ProfileScreen`; RLS migration 006 — **M** ✅
- [x] **Follower follow/unfollow action** — `AccountRepository.follow/unfollow/isFollowing()`; `ProfileViewModel` with optimistic update + rollback; `SellerPublicProfileScreen` wired — **S** ✅
- [x] **Server-side search** — `IItemRepository.getFeedItems(searchQuery)` applies `ilike("item_name", "%query%")` in Postgrest; `SearchResultsViewModel` calls on every keystroke; tapping result navigates to `ItemDetailScreen` — **L** ✅
- [ ] **Push notifications (FCM)** — integrate `firebase-messaging`; store FCM token in `account_side_information` or new table; Edge Function to send on message/offer/sale events — **L**
- [ ] **Notification settings sync** — write per-user preferences to Supabase rather than local SharedPreferences only; required for multi-device support — **S**

### Priority 3 — Quality & Security Fixes

- [x] **Guard against self-purchase (DB-level)** — migration `008` tightens `purchase_insert_buyer` + `item_offer_insert_buyer` to reject `buyer = seller` at DB level — **S** ✅
- [x] **Delete dead files** — `ChatRepository.kt`, `SupabaseConfig.kt` deleted; commit `e0d3893` — **S** ✅
- [x] **Feed pagination** — `ItemRepository.PAGE_SIZE = 20`, `.range()` in Postgrest; `HomeViewModel.loadMore()`; `HomeScreen` infinite scroll trigger — **S** ✅
- [x] **Storage file type + size limits** — migration `008`: `file_size_limit = 5 MB`, `allowed_mime_types = [jpeg, png, webp]` — **S** ✅
- [x] **Add `created_at` to `account` table** — migration `008` adds column; app display deferred (Priority 4) — **S** ✅
- [x] **Remove `account.password_hash`** — dropped by migration `008` — **S** ✅
- [x] **Drop `account_authentication` table** — dropped by migration `008` — **S** ✅
- [x] **Fix `dialogue` UNIQUE constraint** — migration `008` replaces ordered UNIQUE with canonical expression index; `getOrCreateDialogue` normalises to `(min,max)` — **S** ✅
- [x] **Add indexes on `purchase(buyer_id, seller_id)`** — migration `008` adds `idx_purchase_buyer` + `idx_purchase_seller` — **S** ✅
- [ ] **Add DI framework (Hilt)** — repositories new-ed inside ViewModels; add `@HiltViewModel` + `@Inject` — **M** (deferred)
- [ ] **Replace manual nav with Jetpack Navigation** — `MainActivity.kt` boolean-flag overlay system; add `NavHost` + typed routes — **L** (deferred)
- [x] **Move `NotificationPreferences.init()` to `VinderApplication`** — done; commit `e0d3893` — **S** ✅
- [x] **Cache sender names in `MessageNotificationController`** — `senderNameCache` added; commit `e0d3893` — **S** ✅

### Priority 4 — Nice-to-Have / Polish

- [ ] **AI-powered search** — stated as key feature; add pgvector extension + embedding column on `item`; Edge Function to vectorize descriptions on insert; search via cosine similarity — **L**
- [ ] **Dedicated avatar storage bucket** — separate `avatars` bucket from `item-photos`; cleaner access policies and quotas — **S**
- [ ] **`memberSince` display** — once `account.created_at` added, format and display in `ItemDetailScreen`
- [x] **Message attachments** — gallery picker + send preview + caption; upload to `item-photos` under `chat/{dialogueId}/`; `dialogue_message_attachment` wired end-to-end — **M** ✅
- [ ] **Dark mode persistence** — `SettingsViewModel` dark mode toggle; persist to DataStore — **S**
- [ ] **Error recovery UI** — most screens show error state text but no retry button; add `Button("Retry") { viewModel.load() }` pattern — **S**
- [ ] **Listing image picker UX** — verify `AddProductScreen` allows picking multiple photos from gallery; test on physical device
