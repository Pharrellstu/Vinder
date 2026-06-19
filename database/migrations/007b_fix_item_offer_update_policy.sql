-- Migration: 007b_fix_item_offer_update_policy.sql
-- Security fix: item_offer_update_seller was missing WITH CHECK, allowing a
-- seller to mutate item_id on an offer row to reference another seller's item.
-- Also narrows the UPDATE grant to offer_status_id only.
-- Idempotent: safe to run multiple times.

REVOKE UPDATE ON public.item_offer FROM authenticated;
GRANT UPDATE (offer_status_id) ON public.item_offer TO authenticated;

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
