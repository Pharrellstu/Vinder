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
