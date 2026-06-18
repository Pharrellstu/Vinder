-- Migration: 002_create_account_on_email_confirmed.sql
-- Two triggers that keep public.account in sync with auth.users:
--
--   1. AFTER INSERT  → creates account row (is_verified = FALSE) the moment
--      a user signs up, before they confirm their email.
--   2. AFTER UPDATE  → flips is_verified = TRUE the moment the user clicks
--      the confirmation link (email_confirmed_at: NULL → non-NULL).
--
-- Safe to run multiple times (idempotent).

-- ─── Trigger 1 function: on new auth user ────────────────────────────────────
-- Derives account_name from the email prefix.
-- If that name is already taken, appends the first 8 chars of the user UUID.

CREATE OR REPLACE FUNCTION public.handle_auth_user_created()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_name TEXT;
    v_from_metadata BOOLEAN;
BEGIN
    v_name := NULLIF(trim(NEW.raw_user_meta_data->>'nickname'), '');
    v_from_metadata := v_name IS NOT NULL;

    -- Fall back to email prefix for seed data or non-app signups
    IF NOT v_from_metadata THEN
        v_name := split_part(NEW.email, '@', 1);
        -- Add UUID suffix only for derived names to handle seed-data collisions
        IF EXISTS (SELECT 1 FROM public.account WHERE account_name = v_name) THEN
            v_name := v_name || '_' || substring(NEW.id::text, 1, 8);
        END IF;
    END IF;

    INSERT INTO public.account (account_name, account_email, password_hash, is_verified)
    VALUES (v_name, NEW.email, 'supabase_managed', FALSE)
    ON CONFLICT (account_email) DO NOTHING;

    RETURN NEW;
END;
$$;

-- ─── Trigger 2 function: on email confirmed ───────────────────────────────────

CREATE OR REPLACE FUNCTION public.handle_auth_user_email_confirmed()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF NEW.email_confirmed_at IS NOT NULL AND OLD.email_confirmed_at IS NULL THEN
        UPDATE public.account
        SET is_verified = TRUE
        WHERE account_email = NEW.email;
    END IF;
    RETURN NEW;
END;
$$;

-- ─── Attach triggers ──────────────────────────────────────────────────────────

DROP TRIGGER IF EXISTS on_auth_user_created           ON auth.users;
DROP TRIGGER IF EXISTS on_auth_user_email_confirmed   ON auth.users;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE PROCEDURE public.handle_auth_user_created();

CREATE TRIGGER on_auth_user_email_confirmed
    AFTER UPDATE ON auth.users
    FOR EACH ROW
    EXECUTE PROCEDURE public.handle_auth_user_email_confirmed();
