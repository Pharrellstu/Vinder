-- Migration: 002_chat_realtime_and_policies.sql
-- Makes the chat feature work securely against the Supabase project:
--   1. Participant-scoped RLS on dialogue / dialogue_message (read + write).
--   2. Streams dialogue_message (and dialogue) inserts over Realtime, which the
--      in-app chat and the message-notification subscriber both rely on.
-- Safe to run multiple times (idempotent).
--
-- Auth mapping convention (matches the existing account/item policies):
--   auth.email()  ->  account.account_email  ->  account.account_id

-- ─── Helper: the signed-in user's app account id ────────────────────────────
-- SECURITY INVOKER (default) is sufficient because `account` is already readable
-- by the authenticated role; this avoids the SECURITY DEFINER advisor warnings.
CREATE OR REPLACE FUNCTION public.current_account_id()
RETURNS integer
LANGUAGE sql
STABLE
SET search_path = public
AS $$ SELECT account_id FROM public.account WHERE account_email = auth.email() $$;

-- ─── dialogue ───────────────────────────────────────────────────────────────
ALTER TABLE public.dialogue ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can read own dialogues"  ON public.dialogue;
DROP POLICY IF EXISTS "dialogue_select_participant"    ON public.dialogue;
DROP POLICY IF EXISTS "dialogue_insert_creator"        ON public.dialogue;

CREATE POLICY "dialogue_select_participant" ON public.dialogue
    FOR SELECT TO authenticated
    USING (
        dialogue_creator_id = (SELECT public.current_account_id())
        OR dialogue_receiver_id = (SELECT public.current_account_id())
    );

CREATE POLICY "dialogue_insert_creator" ON public.dialogue
    FOR INSERT TO authenticated
    WITH CHECK (dialogue_creator_id = (SELECT public.current_account_id()));

-- ─── dialogue_message ───────────────────────────────────────────────────────
ALTER TABLE public.dialogue_message ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can read messages in their dialogues" ON public.dialogue_message;
DROP POLICY IF EXISTS "dialogue_message_select_participant"        ON public.dialogue_message;
DROP POLICY IF EXISTS "dialogue_message_insert_sender"            ON public.dialogue_message;
DROP POLICY IF EXISTS "dialogue_message_update_participant"        ON public.dialogue_message;

CREATE POLICY "dialogue_message_select_participant" ON public.dialogue_message
    FOR SELECT TO authenticated
    USING (
        dialogue_id IN (
            SELECT d.dialogue_id FROM public.dialogue d
            WHERE d.dialogue_creator_id = (SELECT public.current_account_id())
               OR d.dialogue_receiver_id = (SELECT public.current_account_id())
        )
    );

CREATE POLICY "dialogue_message_insert_sender" ON public.dialogue_message
    FOR INSERT TO authenticated
    WITH CHECK (
        sender_id = (SELECT public.current_account_id())
        AND dialogue_id IN (
            SELECT d.dialogue_id FROM public.dialogue d
            WHERE d.dialogue_creator_id = (SELECT public.current_account_id())
               OR d.dialogue_receiver_id = (SELECT public.current_account_id())
        )
    );

-- UPDATE covers markRead, which flips is_read on the OTHER party's rows, so it is
-- scoped by dialogue participation rather than by sender.
CREATE POLICY "dialogue_message_update_participant" ON public.dialogue_message
    FOR UPDATE TO authenticated
    USING (
        dialogue_id IN (
            SELECT d.dialogue_id FROM public.dialogue d
            WHERE d.dialogue_creator_id = (SELECT public.current_account_id())
               OR d.dialogue_receiver_id = (SELECT public.current_account_id())
        )
    )
    WITH CHECK (
        dialogue_id IN (
            SELECT d.dialogue_id FROM public.dialogue d
            WHERE d.dialogue_creator_id = (SELECT public.current_account_id())
               OR d.dialogue_receiver_id = (SELECT public.current_account_id())
        )
    );

-- ─── Realtime: stream the chat tables ───────────────────────────────────────
-- postgres_changes only streams tables in the supabase_realtime publication.
-- Without this the in-app chat live-updates and the message-notification
-- subscriber fail with "Unable to subscribe to changes...".
ALTER TABLE public.dialogue_message REPLICA IDENTITY FULL;
ALTER TABLE public.dialogue          REPLICA IDENTITY FULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public' AND tablename = 'dialogue_message'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.dialogue_message;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public' AND tablename = 'dialogue'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.dialogue;
    END IF;
END $$;
