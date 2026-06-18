# DB Change Note — Add-Listing RLS Policies (2026-06-18)

**Migration:** `database/migrations/004_listing_read_write_policies.sql`
**Applied to:** Supabase Cloud project `ismxghgozvuonkgloguk` (via Supabase MCP `apply_migration`, name `listing_read_write_policies`; recorded in DB migration history).
**Branch:** `fix/add-listing-empty-list`
**Author:** Alexandros Karayiannis

---

## Why
Creating a listing failed with **"List is empty."** (later shown as "Category … is not available").

Root cause was **not** app code. `item_category` and `item_condition` had **RLS enabled but no SELECT policy**, so when the app resolved the chosen category/condition name → id as the `authenticated` role, the query returned **zero rows** and the lookup threw. The same gap also blocked the rest of the write path (item insert/update, photo insert).

## Before state (verified)
| Table | RLS | Policies before |
|---|---|---|
| `item` | on | SELECT only (`Authenticated users can read all items`) — no INSERT/UPDATE |
| `item_category` | on | **none** → reads blocked |
| `item_condition` | on | **none** → reads blocked |
| `item_photo` | on | **none** → inserts blocked |

## What changed
Added policies (no existing policy removed; `item`'s SELECT policy untouched):

- `item_category.item_category_select_all` — SELECT for `anon, authenticated` `USING (true)` (non-sensitive reference data) + `GRANT SELECT`.
- `item_condition.item_condition_select_all` — same.
- `item.item_insert_own` — INSERT for `authenticated`, `WITH CHECK (seller_id = current_account_id())`.
- `item.item_update_own` — UPDATE for `authenticated`, scoped to own rows (covers flip to `is_listed`).
- `item_photo.item_photo_insert_own` — INSERT for `authenticated`, only for items they own.

Relies on `public.current_account_id()` (auth.email() → account.account_email → account_id), already present from the `chat_rls_and_realtime` migration.

## After state (verified)
- All policies present.
- As `carol@vinder.dev` (authenticated) `item_category`/`item_condition` now return rows (were `[]`).
- Reference tables contain **duplicate** rows (e.g. "Clothing" ×3) from repeated seeding — harmless; the client now uses `decodeList().firstOrNull()`.

## Paired client change (same branch, not yet merged)
- `ItemRepository.getCategoryId/getConditionId`: `decodeSingle()` → `decodeList().firstOrNull()` (tolerates duplicate/zero rows, clearer error).
- `AddProductViewModel`: validates category & condition are chosen.
- `AddProductScreen`: "Next" on Details now requires category + condition.

## Follow-up — Migration 005 (`item_photo` public read) + cover images
**Migration:** `database/migrations/005_item_photo_public_read.sql` (applied to cloud as `item_photo_public_read`).

Same class of bug: `item_photo` had RLS enabled with **no SELECT policy**, so the app got zero photo rows → cover/gallery images were blank everywhere (profile grid, feed, item detail). Added `item_photo_select_all` (SELECT, anon+authenticated, `USING (true)`) + `GRANT SELECT`. Verified: as carol, `item_photo` now returns rows (was `[]`).

Paired client changes (same branch):
- `ListingItem` gained `coverUrl`; `AccountRepository.getListedItems/getSoldItems` now fetch the first photo per item; `ProfileListingGrid` renders it (falls back to the grey placeholder when null).
- Post-listing now navigates to the new item's detail page instead of resetting the Add-Product form (`AddProductViewModel` emits the new `itemId`; `AddProductScreen` exposes `onPosted`; `MainActivity` opens it).

## Rollback
```sql
DROP POLICY IF EXISTS "item_category_select_all"  ON public.item_category;
DROP POLICY IF EXISTS "item_condition_select_all" ON public.item_condition;
DROP POLICY IF EXISTS "item_insert_own"           ON public.item;
DROP POLICY IF EXISTS "item_update_own"           ON public.item;
DROP POLICY IF EXISTS "item_photo_insert_own"     ON public.item_photo;
DROP POLICY IF EXISTS "item_photo_select_all"     ON public.item_photo;
```

---

## ⚠️ Pre-existing issues surfaced by the security advisor (NOT caused by this change — please triage separately)

**ERROR — public tables with RLS DISABLED (readable/writable by anyone with the publishable key):**
- `public.purchase`
- `public.item_offer`
- `public.dialogue_message_attachment`

**WARN:**
- `item-photos` storage bucket has a broad public SELECT policy → clients can list all files.
- `SECURITY DEFINER` functions callable by anon/authenticated: `handle_auth_user_created`, `handle_auth_user_email_confirmed`, `rls_auto_enable`.
- Auth: leaked-password protection (HaveIBeenPwned) disabled.

**INFO — RLS enabled but no policy (feature reads silently blocked, same class of bug we just fixed):**
- `account_favorite`, `account_rating`, `rating`, `status`, `account_authentication`

Linter reference: https://supabase.com/docs/guides/database/database-linter
