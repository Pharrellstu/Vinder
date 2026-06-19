-- Migration: 008_schema_cleanup.sql
-- Addresses AUDIT P3 schema items and tightens two security policies.
-- Idempotent: safe to run multiple times.

-- ─── account: add created_at, drop unused password_hash ─────────────────────
ALTER TABLE public.account
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE public.account
    DROP COLUMN IF EXISTS password_hash;

-- ─── account_authentication: dead table — GoTrue handles OTP ────────────────
DROP TABLE IF EXISTS public.account_authentication;

-- ─── purchase: indexes for order-history queries ────────────────────────────
CREATE INDEX IF NOT EXISTS idx_purchase_buyer  ON public.purchase (buyer_id);
CREATE INDEX IF NOT EXISTS idx_purchase_seller ON public.purchase (seller_id);

-- ─── dialogue: replace ordered UNIQUE with canonical (unordered) pair index ──
-- Current UNIQUE(creator, receiver) allows Alice→Bob AND Bob→Alice as two
-- separate rows. The app normalises inserts to (min_id, max_id) so this is
-- usually harmless, but a race between two simultaneous first-opens could
-- produce duplicate threads. Replace with an expression-based unique index
-- on the canonical pair and relax the INSERT RLS policy so either participant
-- can be named creator.
ALTER TABLE public.dialogue
    DROP CONSTRAINT IF EXISTS dialogue_dialogue_creator_id_dialogue_receiver_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_dialogue_canonical_pair
    ON public.dialogue (
        LEAST(dialogue_creator_id, dialogue_receiver_id),
        GREATEST(dialogue_creator_id, dialogue_receiver_id)
    );

-- Relax INSERT policy: canonical ordering may put the current user in the
-- receiver slot, so allow either participant to create the row.
DROP POLICY IF EXISTS "dialogue_insert_creator" ON public.dialogue;
CREATE POLICY "dialogue_insert_creator" ON public.dialogue
    FOR INSERT TO authenticated
    WITH CHECK (
        dialogue_creator_id  = (SELECT public.current_account_id())
        OR dialogue_receiver_id = (SELECT public.current_account_id())
    );

-- ─── storage: file size cap + MIME type allow-list on item-photos bucket ─────
UPDATE storage.buckets
SET file_size_limit    = 5242880,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp']
WHERE id = 'item-photos';

-- ─── Self-purchase/self-offer DB-level guard ─────────────────────────────────
-- Prevents a seller from inserting a purchase or offer on their own listing
-- even if the app-level guard is bypassed.

DROP POLICY IF EXISTS "purchase_insert_buyer" ON public.purchase;
CREATE POLICY "purchase_insert_buyer" ON public.purchase
    FOR INSERT TO authenticated
    WITH CHECK (
        buyer_id  = (SELECT public.current_account_id())
        AND seller_id != (SELECT public.current_account_id())
    );

DROP POLICY IF EXISTS "item_offer_insert_buyer" ON public.item_offer;
CREATE POLICY "item_offer_insert_buyer" ON public.item_offer
    FOR INSERT TO authenticated
    WITH CHECK (
        offer_creator_id = (SELECT public.current_account_id())
        AND item_id NOT IN (
            SELECT i.item_id FROM public.item i
            WHERE i.seller_id = (SELECT public.current_account_id())
        )
    );
