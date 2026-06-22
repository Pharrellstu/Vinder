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
