-- Migration: 006_account_favorite_rls_policies.sql
-- Adds RLS policies to account_favorite (wishlist feature).
-- Depends on current_account_id() helper from 003_chat_realtime_and_policies.sql.
-- Safe to run multiple times (DROP POLICY IF EXISTS guards).

DROP POLICY IF EXISTS "wishlist_select_owner" ON public.account_favorite;
DROP POLICY IF EXISTS "wishlist_insert_owner" ON public.account_favorite;
DROP POLICY IF EXISTS "wishlist_delete_owner" ON public.account_favorite;

CREATE POLICY "wishlist_select_owner" ON public.account_favorite
    FOR SELECT TO authenticated
    USING (account_id = (SELECT public.current_account_id()));

CREATE POLICY "wishlist_insert_owner" ON public.account_favorite
    FOR INSERT TO authenticated
    WITH CHECK (account_id = (SELECT public.current_account_id()));

CREATE POLICY "wishlist_delete_owner" ON public.account_favorite
    FOR DELETE TO authenticated
    USING (account_id = (SELECT public.current_account_id()));
