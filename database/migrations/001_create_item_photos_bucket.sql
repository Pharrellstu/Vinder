-- Migration: 001_create_item_photos_bucket.sql
-- Creates the `item-photos` storage bucket and RLS policies.
-- Safe to run multiple times (idempotent).

-- ─── Create bucket ────────────────────────────────────────────────────────────
-- Supabase storage metadata lives in storage.buckets.
-- public = true lets publicUrl() return a direct URL without signed tokens.

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'item-photos',
    'item-photos',
    true,
    NULL,
    NULL
)
ON CONFLICT (id) DO NOTHING;

-- ─── RLS: enable on storage.objects (already enabled by default in Supabase) ──
-- Omitted: cloud Supabase does not allow altering this table (not owner).
-- RLS is already enabled on storage.objects by default.

-- ─── Drop policies before recreating (idempotent re-run safety) ──────────────
DROP POLICY IF EXISTS "item-photos: authenticated upload"   ON storage.objects;
DROP POLICY IF EXISTS "item-photos: public read"            ON storage.objects;

-- ─── Policy: authenticated users can upload ──────────────────────────────────
CREATE POLICY "item-photos: authenticated upload"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (bucket_id = 'item-photos');

-- ─── Policy: anyone (incl. anon) can download / read ─────────────────────────
CREATE POLICY "item-photos: public read"
    ON storage.objects
    FOR SELECT
    TO public
    USING (bucket_id = 'item-photos');
