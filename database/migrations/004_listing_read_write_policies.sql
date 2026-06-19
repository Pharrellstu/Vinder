-- Migration: 004_listing_read_write_policies.sql
-- Fixes "can't add a new listing" — the Add-Listing flow failed because the
-- authenticated role could not READ the reference tables it needs.
--
-- Root cause: item_category / item_condition have RLS enabled but NO select
-- policy, so AddProductViewModel.postListing() -> ItemRepository.getCategoryId()
-- queried them as `authenticated`, got zero rows, and the lookup threw
-- (surfaced to the user as "List is empty." / "Category ... is not available").
--
-- This migration:
--   1. Lets anyone signed-in (and anon) READ the reference tables (non-sensitive).
--   2. Lets a signed-in user CREATE/UPDATE their own item + attach photos, so the
--      whole post-listing write path works, not just the category lookup.
--
-- Depends on public.current_account_id() from 003_chat_realtime_and_policies.sql
-- (auth.email() -> account.account_email -> account.account_id).
-- Idempotent: safe to run multiple times.

-- ─── Reference tables: readable by the app roles ────────────────────────────
ALTER TABLE public.item_category  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.item_condition ENABLE ROW LEVEL SECURITY;

-- GRANTs are normally already present (the empty 200 response proves it was RLS,
-- not privileges) but assert them anyway so the policies can take effect.
GRANT SELECT ON public.item_category  TO anon, authenticated;
GRANT SELECT ON public.item_condition TO anon, authenticated;

DROP POLICY IF EXISTS "item_category_select_all"  ON public.item_category;
DROP POLICY IF EXISTS "item_condition_select_all" ON public.item_condition;

CREATE POLICY "item_category_select_all" ON public.item_category
    FOR SELECT TO anon, authenticated USING (true);

CREATE POLICY "item_condition_select_all" ON public.item_condition
    FOR SELECT TO anon, authenticated USING (true);

-- ─── item: create / update your own listings ────────────────────────────────
-- NOTE: deliberately does NOT toggle item's RLS or add a SELECT policy, so the
-- existing (working) marketplace feed read is left untouched. Adding INSERT/UPDATE
-- policies never restricts SELECT; if item has RLS disabled these are inert no-ops.
DROP POLICY IF EXISTS "item_insert_own" ON public.item;
CREATE POLICY "item_insert_own" ON public.item
    FOR INSERT TO authenticated
    WITH CHECK (seller_id = (SELECT public.current_account_id()));

DROP POLICY IF EXISTS "item_update_own" ON public.item;
CREATE POLICY "item_update_own" ON public.item
    FOR UPDATE TO authenticated
    USING (seller_id = (SELECT public.current_account_id()))
    WITH CHECK (seller_id = (SELECT public.current_account_id()));

-- ─── item_photo: attach photos to your own items ────────────────────────────
DROP POLICY IF EXISTS "item_photo_insert_own" ON public.item_photo;
CREATE POLICY "item_photo_insert_own" ON public.item_photo
    FOR INSERT TO authenticated
    WITH CHECK (
        item_id IN (
            SELECT i.item_id FROM public.item i
            WHERE i.seller_id = (SELECT public.current_account_id())
        )
    );
