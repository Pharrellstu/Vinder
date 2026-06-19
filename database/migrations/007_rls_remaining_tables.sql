-- Migration: 007_rls_remaining_tables.sql
-- Closes AUDIT Issue #6: enables RLS on the remaining unprotected public tables.
--
-- Prior migrations handled:
--   003 — dialogue, dialogue_message (participant-scoped)
--   004 — item_category, item_condition (SELECT-only); item + item_photo INSERT/UPDATE
--          policies created but RLS NOT enabled on item/item_photo (inert no-ops)
--   005 — item_photo SELECT policy added (RLS still not enabled)
--   006 — account_favorite (owner-scoped)
--
-- This migration:
--   - Enables RLS on item + item_photo (activating the 004/005 policies)
--   - Adds the missing policies (item SELECT + DELETE, item_photo DELETE)
--   - Enables RLS on all other unprotected tables with appropriate policies
--
-- Depends on public.current_account_id() from 003_chat_realtime_and_policies.sql.
-- Idempotent: safe to run multiple times (DROP POLICY IF EXISTS guards).

-- ─── account ────────────────────────────────────────────────────────────────
-- Any signed-in user can read any account (feed seller cards, public profiles,
-- chat participant names). Only the owner may update their own row.
-- No INSERT (migration 002 trigger handles creation). No DELETE.
ALTER TABLE public.account ENABLE ROW LEVEL SECURITY;

GRANT SELECT, UPDATE ON public.account TO authenticated;

DROP POLICY IF EXISTS "account_select_authenticated" ON public.account;
CREATE POLICY "account_select_authenticated" ON public.account
    FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "account_update_own" ON public.account;
CREATE POLICY "account_update_own" ON public.account
    FOR UPDATE TO authenticated
    USING    (account_id = (SELECT public.current_account_id()))
    WITH CHECK (account_id = (SELECT public.current_account_id()));

-- ─── account_side_information ────────────────────────────────────────────────
-- Public read (bio, location, avatar shown on all profiles).
-- Owner-only write (EditProfileViewModel / updateAvatar).
ALTER TABLE public.account_side_information ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE ON public.account_side_information TO authenticated;

DROP POLICY IF EXISTS "account_side_select_authenticated" ON public.account_side_information;
CREATE POLICY "account_side_select_authenticated" ON public.account_side_information
    FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "account_side_insert_own" ON public.account_side_information;
CREATE POLICY "account_side_insert_own" ON public.account_side_information
    FOR INSERT TO authenticated
    WITH CHECK (account_id = (SELECT public.current_account_id()));

DROP POLICY IF EXISTS "account_side_update_own" ON public.account_side_information;
CREATE POLICY "account_side_update_own" ON public.account_side_information
    FOR UPDATE TO authenticated
    USING    (account_id = (SELECT public.current_account_id()))
    WITH CHECK (account_id = (SELECT public.current_account_id()));

-- ─── item ────────────────────────────────────────────────────────────────────
-- Enabling RLS here activates the INSERT + UPDATE policies from migration 004.
-- Full authenticated read: the feed, search, order history, and the seller's
-- own unlisted/sold items all need unrestricted item reads.
-- DELETE restricted to the seller.
ALTER TABLE public.item ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.item TO authenticated;

DROP POLICY IF EXISTS "item_select_authenticated" ON public.item;
CREATE POLICY "item_select_authenticated" ON public.item
    FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "item_delete_own" ON public.item;
CREATE POLICY "item_delete_own" ON public.item
    FOR DELETE TO authenticated
    USING (seller_id = (SELECT public.current_account_id()));

-- item_insert_own and item_update_own already exist from migration 004.

-- ─── item_photo ──────────────────────────────────────────────────────────────
-- Enabling RLS here activates the INSERT policy (004) and SELECT policy (005).
-- DELETE restricted to photos belonging to the current user's items.
ALTER TABLE public.item_photo ENABLE ROW LEVEL SECURITY;

GRANT DELETE ON public.item_photo TO authenticated;

DROP POLICY IF EXISTS "item_photo_delete_own" ON public.item_photo;
CREATE POLICY "item_photo_delete_own" ON public.item_photo
    FOR DELETE TO authenticated
    USING (
        item_id IN (
            SELECT i.item_id FROM public.item i
            WHERE i.seller_id = (SELECT public.current_account_id())
        )
    );

-- item_photo_select_all (anon + authenticated) and item_photo_insert_own already
-- exist from migrations 005 and 004 respectively.

-- ─── item_offer ──────────────────────────────────────────────────────────────
-- Offer creator sees their sent offers; item seller sees incoming offers.
-- Only the buyer creates offers; only the item seller updates status (accept/reject).
ALTER TABLE public.item_offer ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT ON public.item_offer TO authenticated;
GRANT UPDATE (offer_status_id) ON public.item_offer TO authenticated;

DROP POLICY IF EXISTS "item_offer_select_participant" ON public.item_offer;
CREATE POLICY "item_offer_select_participant" ON public.item_offer
    FOR SELECT TO authenticated
    USING (
        offer_creator_id = (SELECT public.current_account_id())
        OR item_id IN (
            SELECT i.item_id FROM public.item i
            WHERE i.seller_id = (SELECT public.current_account_id())
        )
    );

DROP POLICY IF EXISTS "item_offer_insert_buyer" ON public.item_offer;
CREATE POLICY "item_offer_insert_buyer" ON public.item_offer
    FOR INSERT TO authenticated
    WITH CHECK (offer_creator_id = (SELECT public.current_account_id()));

DROP POLICY IF EXISTS "item_offer_update_seller" ON public.item_offer;
CREATE POLICY "item_offer_update_seller" ON public.item_offer
    FOR UPDATE TO authenticated
    USING (
        item_id IN (
            SELECT i.item_id FROM public.item i
            WHERE i.seller_id = (SELECT public.current_account_id())
        )
    )
    WITH CHECK (
        item_id IN (
            SELECT i.item_id FROM public.item i
            WHERE i.seller_id = (SELECT public.current_account_id())
        )
    );

-- ─── purchase ────────────────────────────────────────────────────────────────
-- Buyer and seller can see their own purchase records (Order History).
-- Only the buyer creates a purchase row (confirmBuy / createPurchaseFromOffer).
ALTER TABLE public.purchase ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT ON public.purchase TO authenticated;

DROP POLICY IF EXISTS "purchase_select_participant" ON public.purchase;
CREATE POLICY "purchase_select_participant" ON public.purchase
    FOR SELECT TO authenticated
    USING (
        buyer_id  = (SELECT public.current_account_id())
        OR seller_id = (SELECT public.current_account_id())
    );

DROP POLICY IF EXISTS "purchase_insert_buyer" ON public.purchase;
CREATE POLICY "purchase_insert_buyer" ON public.purchase
    FOR INSERT TO authenticated
    WITH CHECK (buyer_id = (SELECT public.current_account_id()));

-- ─── status (lookup: Pending / Accepted / Rejected) ─────────────────────────
ALTER TABLE public.status ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.status TO anon, authenticated;

DROP POLICY IF EXISTS "status_select_all" ON public.status;
CREATE POLICY "status_select_all" ON public.status
    FOR SELECT TO anon, authenticated USING (true);

-- ─── rating (lookup: 1–5 stars) ─────────────────────────────────────────────
ALTER TABLE public.rating ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.rating TO anon, authenticated;

DROP POLICY IF EXISTS "rating_select_all" ON public.rating;
CREATE POLICY "rating_select_all" ON public.rating
    FOR SELECT TO anon, authenticated USING (true);

-- ─── account_following ───────────────────────────────────────────────────────
-- Follow counts shown on all public profiles — authenticated read.
-- A user may only follow/unfollow as themselves.
ALTER TABLE public.account_following ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, DELETE ON public.account_following TO authenticated;

DROP POLICY IF EXISTS "following_select_authenticated" ON public.account_following;
CREATE POLICY "following_select_authenticated" ON public.account_following
    FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "following_insert_own" ON public.account_following;
CREATE POLICY "following_insert_own" ON public.account_following
    FOR INSERT TO authenticated
    WITH CHECK (follower_id = (SELECT public.current_account_id()));

DROP POLICY IF EXISTS "following_delete_own" ON public.account_following;
CREATE POLICY "following_delete_own" ON public.account_following
    FOR DELETE TO authenticated
    USING (follower_id = (SELECT public.current_account_id()));

-- ─── account_rating ──────────────────────────────────────────────────────────
-- Ratings visible on public profiles. Only the rater manages their own rating.
ALTER TABLE public.account_rating ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, DELETE ON public.account_rating TO authenticated;

DROP POLICY IF EXISTS "account_rating_select_authenticated" ON public.account_rating;
CREATE POLICY "account_rating_select_authenticated" ON public.account_rating
    FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "account_rating_insert_rater" ON public.account_rating;
CREATE POLICY "account_rating_insert_rater" ON public.account_rating
    FOR INSERT TO authenticated
    WITH CHECK (rater_id = (SELECT public.current_account_id()));

DROP POLICY IF EXISTS "account_rating_delete_rater" ON public.account_rating;
CREATE POLICY "account_rating_delete_rater" ON public.account_rating
    FOR DELETE TO authenticated
    USING (rater_id = (SELECT public.current_account_id()));
