-- seed_test_data.sql
-- Reference data + sample rows for local dev / testing ONLY.
-- Safe to run multiple times (idempotent — uses ON CONFLICT DO NOTHING).
-- Run AFTER init.sql and accounts.sql.

-- ─── item_category ────────────────────────────────────────────────────────────
-- Names must match AddProductScreen.kt CATEGORIES list exactly.

INSERT INTO item_category (category_name) VALUES
    ('Women'),
    ('Men'),
    ('Kids'),
    ('Home'),
    ('Electronics'),
    ('Books'),
    ('Sports')
ON CONFLICT DO NOTHING;

-- ─── item_condition ───────────────────────────────────────────────────────────
-- Names must match AddProductScreen.kt CONDITIONS list exactly.

INSERT INTO item_condition (item_condition_name) VALUES
    ('New'),
    ('Like New'),
    ('Good'),
    ('Fair')
ON CONFLICT DO NOTHING;

-- ─── item ─────────────────────────────────────────────────────────────────────
-- 3 sample listings — all from alice (account_id = 1), is_listed = true, is_sold = false.

INSERT INTO item (
    seller_id,
    item_category_id,
    item_condition_id,
    item_name,
    item_description,
    item_price,
    is_listed,
    is_sold
)
SELECT
    (SELECT account_id FROM account WHERE account_name = 'alice'),
    (SELECT item_category_id FROM item_category WHERE category_name = 'Women'),
    (SELECT item_condition_id FROM item_condition WHERE item_condition_name = 'Like New'),
    'Floral Summer Dress',
    'Light cotton dress with floral print, size S. Worn twice — excellent condition.',
    22.00,
    true,
    false
WHERE NOT EXISTS (
    SELECT 1 FROM item WHERE item_name = 'Floral Summer Dress'
        AND seller_id = (SELECT account_id FROM account WHERE account_name = 'alice')
);

INSERT INTO item (
    seller_id,
    item_category_id,
    item_condition_id,
    item_name,
    item_description,
    item_price,
    is_listed,
    is_sold
)
SELECT
    (SELECT account_id FROM account WHERE account_name = 'alice'),
    (SELECT item_category_id FROM item_category WHERE category_name = 'Home'),
    (SELECT item_condition_id FROM item_condition WHERE item_condition_name = 'Good'),
    'Wooden Desk Lamp',
    'Minimalist wooden base lamp with warm-white bulb. Works perfectly, minor scratch on base.',
    18.50,
    true,
    false
WHERE NOT EXISTS (
    SELECT 1 FROM item WHERE item_name = 'Wooden Desk Lamp'
        AND seller_id = (SELECT account_id FROM account WHERE account_name = 'alice')
);

INSERT INTO item (
    seller_id,
    item_category_id,
    item_condition_id,
    item_name,
    item_description,
    item_price,
    is_listed,
    is_sold
)
SELECT
    (SELECT account_id FROM account WHERE account_name = 'alice'),
    (SELECT item_category_id FROM item_category WHERE category_name = 'Sports'),
    (SELECT item_condition_id FROM item_condition WHERE item_condition_name = 'New'),
    'Jump Rope — Speed Cable',
    'Brand new speed jump rope, never used. Adjustable cable length, aluminium handles.',
    12.00,
    true,
    false
WHERE NOT EXISTS (
    SELECT 1 FROM item WHERE item_name = 'Jump Rope — Speed Cable'
        AND seller_id = (SELECT account_id FROM account WHERE account_name = 'alice')
);

-- ─── item_photo ───────────────────────────────────────────────────────────────
-- 1 placeholder photo per seed item above.

INSERT INTO item_photo (item_id, photo_url)
SELECT i.item_id, 'https://picsum.photos/seed/dress/400/400'
FROM item i WHERE i.item_name = 'Floral Summer Dress'
    AND i.seller_id = (SELECT account_id FROM account WHERE account_name = 'alice')
    AND NOT EXISTS (
        SELECT 1 FROM item_photo p WHERE p.item_id = i.item_id
    );

INSERT INTO item_photo (item_id, photo_url)
SELECT i.item_id, 'https://picsum.photos/seed/lamp/400/400'
FROM item i WHERE i.item_name = 'Wooden Desk Lamp'
    AND i.seller_id = (SELECT account_id FROM account WHERE account_name = 'alice')
    AND NOT EXISTS (
        SELECT 1 FROM item_photo p WHERE p.item_id = i.item_id
    );

INSERT INTO item_photo (item_id, photo_url)
SELECT i.item_id, 'https://picsum.photos/seed/rope/400/400'
FROM item i WHERE i.item_name = 'Jump Rope — Speed Cable'
    AND i.seller_id = (SELECT account_id FROM account WHERE account_name = 'alice')
    AND NOT EXISTS (
        SELECT 1 FROM item_photo p WHERE p.item_id = i.item_id
    );

-- ─── dialogue ─────────────────────────────────────────────────────────────────
-- 1 conversation: alice (creator) ↔ bob (receiver).

INSERT INTO dialogue (dialogue_creator_id, dialogue_receiver_id)
SELECT
    (SELECT account_id FROM account WHERE account_name = 'alice'),
    (SELECT account_id FROM account WHERE account_name = 'bob')
ON CONFLICT (dialogue_creator_id, dialogue_receiver_id) DO NOTHING;

-- ─── dialogue_message ─────────────────────────────────────────────────────────
-- 3 alternating messages in the alice↔bob dialogue.

INSERT INTO dialogue_message (dialogue_id, sender_id, message_text, timestamp, is_read)
SELECT
    d.dialogue_id,
    (SELECT account_id FROM account WHERE account_name = 'alice'),
    'Hi Bob! Is the Sony headphones still available?',
    NOW() - INTERVAL '2 hours',
    true
FROM dialogue d
WHERE d.dialogue_creator_id = (SELECT account_id FROM account WHERE account_name = 'alice')
  AND d.dialogue_receiver_id = (SELECT account_id FROM account WHERE account_name = 'bob')
  AND NOT EXISTS (
      SELECT 1 FROM dialogue_message dm
      WHERE dm.dialogue_id = d.dialogue_id
        AND dm.message_text = 'Hi Bob! Is the Sony headphones still available?'
  );

INSERT INTO dialogue_message (dialogue_id, sender_id, message_text, timestamp, is_read)
SELECT
    d.dialogue_id,
    (SELECT account_id FROM account WHERE account_name = 'bob'),
    'Hey Alice! Yes, still available. Would you like to arrange a pickup?',
    NOW() - INTERVAL '1 hour 45 minutes',
    true
FROM dialogue d
WHERE d.dialogue_creator_id = (SELECT account_id FROM account WHERE account_name = 'alice')
  AND d.dialogue_receiver_id = (SELECT account_id FROM account WHERE account_name = 'bob')
  AND NOT EXISTS (
      SELECT 1 FROM dialogue_message dm
      WHERE dm.dialogue_id = d.dialogue_id
        AND dm.message_text = 'Hey Alice! Yes, still available. Would you like to arrange a pickup?'
  );

INSERT INTO dialogue_message (dialogue_id, sender_id, message_text, timestamp, is_read)
SELECT
    d.dialogue_id,
    (SELECT account_id FROM account WHERE account_name = 'alice'),
    'Great! Could you do Saturday afternoon in Riga city centre?',
    NOW() - INTERVAL '1 hour 30 minutes',
    false
FROM dialogue d
WHERE d.dialogue_creator_id = (SELECT account_id FROM account WHERE account_name = 'alice')
  AND d.dialogue_receiver_id = (SELECT account_id FROM account WHERE account_name = 'bob')
  AND NOT EXISTS (
      SELECT 1 FROM dialogue_message dm
      WHERE dm.dialogue_id = d.dialogue_id
        AND dm.message_text = 'Great! Could you do Saturday afternoon in Riga city centre?'
  );

-- ─── account_side_information ────────────────────────────────────────────────
-- 1 row per test account. Skipped if accounts.sql already seeded these rows.

INSERT INTO account_side_information (
    account_id,
    account_bio,
    account_location,
    account_profile_picture_url
) VALUES
    (
        (SELECT account_id FROM account WHERE account_name = 'alice'),
        'Selling clothes I no longer wear. Ships fast!',
        'Riga, Latvia',
        'https://i.pravatar.cc/150?u=alice'
    ),
    (
        (SELECT account_id FROM account WHERE account_name = 'bob'),
        'Tech enthusiast flipping gadgets.',
        'Vilnius, Lithuania',
        'https://i.pravatar.cc/150?u=bob'
    ),
    (
        (SELECT account_id FROM account WHERE account_name = 'carol'),
        'Book lover and plant parent.',
        'Tallinn, Estonia',
        'https://i.pravatar.cc/150?u=carol'
    )
ON CONFLICT (account_id) DO NOTHING;
