-- Migration: 009_fix_item_photos_upload_scoping.sql
-- Security fix: the original "item-photos: authenticated upload" policy (migration
-- 001) only checked bucket_id = 'item-photos'. Any authenticated user could upload
-- to — and overwrite — ANY path in the bucket, including other users' item photos,
-- avatars, and chat attachments.
--
-- This recreates the same-named INSERT policy with per-prefix ownership scoping,
-- matching the three path shapes the app actually writes:
--   items/<itemId>/...       -> uploader must own the item (item.seller_id)
--   avatars/<accountId>/...  -> uploader must be that account
--   chat/<dialogueId>/...    -> uploader must be a participant in the dialogue
-- The id segment is cast to integer to compare against the integer key columns.
-- The "item-photos: public read" SELECT policy is left untouched.
--
-- Depends on public.current_account_id() from 003_chat_realtime_and_policies.sql.
-- Idempotent: safe to run multiple times (DROP POLICY IF EXISTS guard).

DROP POLICY IF EXISTS "item-photos: authenticated upload" ON storage.objects;

CREATE POLICY "item-photos: authenticated upload"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'item-photos'
        AND (
            (
                (storage.foldername(name))[1] = 'items'
                AND ((storage.foldername(name))[2])::integer IN (
                    SELECT i.item_id FROM public.item i
                    WHERE i.seller_id = (SELECT public.current_account_id())
                )
            )
            OR (
                (storage.foldername(name))[1] = 'avatars'
                AND ((storage.foldername(name))[2])::integer = (SELECT public.current_account_id())
            )
            OR (
                (storage.foldername(name))[1] = 'chat'
                AND ((storage.foldername(name))[2])::integer IN (
                    SELECT d.dialogue_id FROM public.dialogue d
                    WHERE d.dialogue_creator_id = (SELECT public.current_account_id())
                       OR d.dialogue_receiver_id = (SELECT public.current_account_id())
                )
            )
        )
    );
