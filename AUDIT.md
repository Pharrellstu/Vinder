# Vinder — Comprehensive Audit Report

> Generated: 2026-06-18

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
| Listings (create) | ✅ | `AddProductScreen` + full photo upload flow; category/condition pickers now load real names from `item_category`/`item_condition` instead of hardcoded strings, so the listing can no longer fail to insert on a name mismatch |
| Listings (edit/delete) | ❌ | No screen, no repository method for edit or delete of own listings |
| Search | ✅ | `HomeScreen` search is client-side word-boundary prefix match on the loaded feed (`HomeViewModel`). `SearchResultsScreen`'s search bar now does real **server-side** search via `ItemRepository.getFeedItems(searchQuery)` → Postgrest `ilike` against the real `item` table — no more hardcoded sample data. AI-powered search is still not implemented (explicitly deferred, not a bug) |
| Filters / sorting | 🔶 | `HomeScreen` category + price-bucket filters are wired to `HomeViewModel` and actually filter the live feed. `SearchResultsScreen`'s `FilterBottomSheet` (category/condition/size/price) and sort control are fully functional and now operate on real server-fetched items — but the filtering itself still happens client-side on the fetched page, not pushed down into the Postgrest query. `size` has no backing DB column at all, so the size filter can never match a real item |
| Product detail view | ✅ | `ItemDetailScreen` with photo carousel, description, seller card |
| Cart | ❌ | No multi-item cart concept in this app's buy flow (items are bought individually). `account_favorite` is used by Wishlist, not cart — original audit's note that the table was untouched is now out of date |
| Wishlist | ✅ | `account_favorite`-backed via `ItemRepository.getFavoriteItemIds/getFavoriteItems/addFavorite/removeFavorite`; heart button on `GridProductCard` and `ItemDetailScreen` toggles persisted state; dedicated `WishlistScreen` reachable from `ProfileScreen`; RLS migration 006 |
| Buy flow | 🔶 | `confirmBuy()` inserts to `purchase` and marks item sold, but fees calculated client-side (spoofable); no payment gateway. Self-purchase/self-offer now blocked: `ItemDetailScreen` hides Buy/Offer on the viewer's own listings, and `ItemDetailViewModel.confirmBuy()`/`submitOffer()` reject `sellerId == buyerId`/`creatorId` before any network call |
| Payment integration | ❌ | No Stripe/payment SDK; purchase is a direct DB insert |
| Order history | ✅ | `OrderHistoryScreen` + `OrderHistoryViewModel`; shows item name, fee breakdown, total, date |
| Make offer | ✅ | `submitOffer()` inserts to `item_offer` |
| Offer management | ✅ | `OffersScreen` + `OffersViewModel`; seller can accept/reject; accept chains purchase creation + markAsSold |
| Messaging list | ✅ | `MessagesScreen` loads conversations from DB |
| Chat | ✅ | `ChatScreen` with Supabase Realtime subscription |
| Message attachments | ✅ | `ChatScreen` supports picking a gallery image, a WhatsApp-style full-screen preview (with optional caption) before sending, upload to `item-photos` under `chat/{dialogueId}/`, and a `dialogue_message_attachment` row; both sides see the image live via a direct Postgrest lookup (not Realtime — see changelog) |
| Ratings & reviews | ❌ | `account_rating` + `rating` tables in schema; zero app code or UI |
| Push notifications | ❌ | FCM not integrated; `NotificationSettingsViewModel` is in-memory only, preferences lost on process kill |
| Followers | 🔶 | `account_following` table exists, follower count shown in profile stats, but follow/unfollow action not implemented |
| Admin / moderation | ❌ | None |
| AI-powered search | ❌ | Stated as key feature in CLAUDE.md; not implemented |

---

### Supabase / Database Findings

**Schema summary** (17 tables): `item_condition`, `status`, `rating`, `item_category`, `account`, `account_side_information`, `account_authentication`, `account_following`, `account_rating`, `item`, `item_photo`, `account_favorite`, `item_offer`, `purchase`, `dialogue`, `dialogue_message`, `dialogue_message_attachment`

**CRITICAL — RLS not enabled on most public tables.** `dialogue` and `dialogue_message` protected by migration 003; `account_favorite` protected by migration 006. Remaining 13 tables (`account`, `item`, `item_photo`, `item_offer`, `purchase`, `item_category`, `item_condition`, `status`, `rating`, `account_following`, `account_rating`, `account_side_information`) still have no RLS — any authenticated user can read and write every row via the anon key.

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
- [x] **Wishlist/favorites** — `WishlistScreen` + `WishlistViewModel`; `ItemRepository.getFavoriteItems/addFavorite/removeFavorite`; heart on `GridProductCard` + `ItemDetailScreen`; RLS migration 006 — **M** ✅
- [ ] **Follower follow/unfollow action** — `AccountRepository.follow/unfollow()`; wire button in `SellerPublicProfileScreen` (stat count already displayed) — **S**
- [x] **Server-side search** — `IItemRepository.getFeedItems(searchQuery)` applies `ilike("item_name", "%query%")` in the Postgrest query; `SearchResultsViewModel` calls it on every keystroke — **L** ✅ (AI/pgvector semantic search deliberately not in scope, see Priority 4)
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

- [ ] **AI-powered search** — stated as key feature; add pgvector extension + embedding column on `item`; Edge Function to vectorize descriptions on insert; search via cosine similarity — **L** — *explicitly deferred for now, not a bug; plain server-side text search (`ilike`) is in place as the interim*
- [ ] **Dedicated avatar storage bucket** — separate `avatars` bucket from `item-photos`; cleaner access policies and quotas — **S**
- [ ] **Listing image picker UX** — verify `AddProductScreen` allows picking multiple photos from gallery; test on physical device
- [ ] **`memberSince` display** — once `account.created_at` added, format and display in `ItemDetailScreen`
- [x] **Message attachments** — `dialogue_message_attachment` table wired up; `ChatScreen` supports gallery picker + send preview + caption, `DialogueRepository`/`ChatViewModel` handle upload and live display — **M** ✅
- [ ] **Dark mode persistence** — `SettingsViewModel` dark mode toggle; persist to DataStore — **S**
- [ ] **Error recovery UI** — most screens show error state text but no retry button; add `Button("Retry") { viewModel.load() }` pattern — **S**
