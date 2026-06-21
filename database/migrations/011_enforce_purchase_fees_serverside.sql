-- Migration: 011_enforce_purchase_fees_serverside.sql
-- AUDIT.md Priority 1 finding: "Client-side fee calculation" (ItemDetailViewModel.kt
-- SHIPPING_FEE/BUYER_PROTECTION_FEE). PurchaseRepository.createPurchase() /
-- createPurchaseFromOffer() send shipping_fee, protection_fee, and total_amount
-- straight from the client — a spoofed client could insert a purchase with $0 fees.
--
-- Fix: a BEFORE INSERT/UPDATE trigger on public.purchase recomputes shipping_fee
-- and protection_fee from fixed server-side constants (mirroring the app's current
-- 3.95 / 0.90 values) and derives total_amount from them, overwriting whatever the
-- client supplied. The client-supplied item_price is kept as-is — it is independently
-- constrained by the offer/listing flow, not by this trigger.
--
-- Idempotent: safe to run multiple times (CREATE OR REPLACE FUNCTION, DROP TRIGGER IF EXISTS).

CREATE OR REPLACE FUNCTION public.enforce_purchase_fees()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.shipping_fee := 3.95;
    NEW.protection_fee := 0.90;
    NEW.total_amount := NEW.item_price + NEW.shipping_fee + NEW.protection_fee;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS purchase_enforce_fees ON public.purchase;

CREATE TRIGGER purchase_enforce_fees
    BEFORE INSERT OR UPDATE ON public.purchase
    FOR EACH ROW
    EXECUTE FUNCTION public.enforce_purchase_fees();
