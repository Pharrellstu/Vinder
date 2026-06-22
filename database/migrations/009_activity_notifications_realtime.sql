-- Migration: 009_activity_notifications_realtime.sql
-- Streams the activity tables the notification listener needs over Realtime, so a seller is
-- notified of new offers and sales, and a user of new followers. Only INSERTs are consumed, so the
-- default replica identity is sufficient (the new row is always present on INSERT).
--
-- Existing RLS already scopes who may read these rows, and Realtime postgres_changes honours it:
--   item_offer        -> offer creator OR the item's seller   (item_offer_select_participant)
--   purchase          -> buyer OR seller                       (purchase_select_participant)
--   account_following -> any authenticated user (read=true); the client filters to following_id=me
--
-- Safe to run multiple times (idempotent).

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public' AND tablename = 'item_offer'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.item_offer;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public' AND tablename = 'purchase'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.purchase;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public' AND tablename = 'account_following'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.account_following;
    END IF;
END $$;
