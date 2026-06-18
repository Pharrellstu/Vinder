-- Migration: 005_item_photo_public_read.sql
-- Cover/gallery images did not load anywhere in the app (profile grid, feed, item
-- detail) because public.item_photo had RLS enabled with NO select policy, so the
-- anon/authenticated roles received zero photo rows.
--
-- Photos are public marketplace images (the `item-photos` storage bucket is already
-- public-read), so grant read on the metadata table to both roles.
-- Idempotent: safe to run multiple times.

GRANT SELECT ON public.item_photo TO anon, authenticated;

DROP POLICY IF EXISTS "item_photo_select_all" ON public.item_photo;
CREATE POLICY "item_photo_select_all" ON public.item_photo
    FOR SELECT TO anon, authenticated USING (true);
