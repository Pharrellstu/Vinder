CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ─── Lookup tables ────────────────────────────────────────────────────────────

INSERT INTO item_condition (item_condition_name) VALUES
    ('New'),
    ('Like New'),
    ('Good'),
    ('Fair'),
    ('Poor')
ON CONFLICT DO NOTHING;

INSERT INTO status (status_name) VALUES
    ('Pending'),
    ('Accepted'),
    ('Rejected'),
    ('Cancelled')
ON CONFLICT DO NOTHING;

INSERT INTO rating (rating_number) VALUES
    (1), (2), (3), (4), (5)
ON CONFLICT DO NOTHING;

INSERT INTO item_category (category_name) VALUES
    ('Clothing'),
    ('Electronics'),
    ('Books'),
    ('Home & Garden'),
    ('Sports'),
    ('Toys'),
    ('Vehicles'),
    ('Other')
ON CONFLICT DO NOTHING;

-- ─── GoTrue: auth.users ───────────────────────────────────────────────────────
-- Credentials:
--   alice@vinder.dev  /  Alice123!
--   bob@vinder.dev    /  Bob123!
--   carol@vinder.dev  /  Carol123!

INSERT INTO auth.users (
    instance_id,
    id,
    aud,
    role,
    email,
    encrypted_password,
    email_confirmed_at,
    raw_app_meta_data,
    raw_user_meta_data,
    confirmation_token,
    recovery_token,
    email_change_token_new,
    email_change,
    created_at,
    updated_at
) VALUES
    (
        '00000000-0000-0000-0000-000000000000',
        'aaaaaaaa-0001-0001-0001-000000000001',
        'authenticated', 'authenticated',
        'alice@vinder.dev', crypt('Alice123!', gen_salt('bf')),
        NOW(),
        '{"provider":"email","providers":["email"]}', '{"username":"alice"}',
        '', '', '', '', NOW(), NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000000',
        'bbbbbbbb-0002-0002-0002-000000000002',
        'authenticated', 'authenticated',
        'bob@vinder.dev', crypt('Bob123!', gen_salt('bf')),
        NOW(),
        '{"provider":"email","providers":["email"]}', '{"username":"bob"}',
        '', '', '', '', NOW(), NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000000',
        'cccccccc-0003-0003-0003-000000000003',
        'authenticated', 'authenticated',
        'carol@vinder.dev', crypt('Carol123!', gen_salt('bf')),
        NOW(),
        '{"provider":"email","providers":["email"]}', '{"username":"carol"}',
        '', '', '', '', NOW(), NOW()
    )
ON CONFLICT DO NOTHING;

-- ─── GoTrue: auth.identities ──────────────────────────────────────────────────
-- Required by GoTrue so sign-in resolves the identity provider correctly.

INSERT INTO auth.identities (
    provider_id,
    user_id,
    identity_data,
    provider,
    last_sign_in_at,
    created_at,
    updated_at
) VALUES
    (
        'alice@vinder.dev',
        'aaaaaaaa-0001-0001-0001-000000000001',
        '{"sub":"aaaaaaaa-0001-0001-0001-000000000001","email":"alice@vinder.dev"}',
        'email', NOW(), NOW(), NOW()
    ),
    (
        'bob@vinder.dev',
        'bbbbbbbb-0002-0002-0002-000000000002',
        '{"sub":"bbbbbbbb-0002-0002-0002-000000000002","email":"bob@vinder.dev"}',
        'email', NOW(), NOW(), NOW()
    ),
    (
        'carol@vinder.dev',
        'cccccccc-0003-0003-0003-000000000003',
        '{"sub":"cccccccc-0003-0003-0003-000000000003","email":"carol@vinder.dev"}',
        'email', NOW(), NOW(), NOW()
    )
ON CONFLICT (provider_id, provider) DO NOTHING;

-- ─── App: account ─────────────────────────────────────────────────────────────

INSERT INTO account (account_name, account_email, password_hash, is_verified) VALUES
    ('alice', 'alice@vinder.dev', crypt('Alice123!', gen_salt('bf')), TRUE),
    ('bob',   'bob@vinder.dev',   crypt('Bob123!',   gen_salt('bf')), TRUE),
    ('carol', 'carol@vinder.dev', crypt('Carol123!', gen_salt('bf')), TRUE)
ON CONFLICT (account_email) DO NOTHING;

-- ─── App: account_side_information ───────────────────────────────────────────

INSERT INTO account_side_information (
    account_id,
    account_bio,
    account_location,
    account_profile_picture_url
)
SELECT a.account_id, v.bio, v.location, v.pic
FROM (VALUES
    ('alice', 'Selling clothes I no longer wear. Ships fast!',  'Riga, Latvia',       'https://i.pravatar.cc/150?u=alice'),
    ('bob',   'Tech enthusiast flipping gadgets.',              'Vilnius, Lithuania',  'https://i.pravatar.cc/150?u=bob'),
    ('carol', 'Book lover and plant parent.',                   'Tallinn, Estonia',    'https://i.pravatar.cc/150?u=carol')
) AS v(name, bio, location, pic)
JOIN account a ON a.account_name = v.name
ON CONFLICT (account_id) DO NOTHING;

-- ─── App: items ───────────────────────────────────────────────────────────────

INSERT INTO item (
    seller_id,
    item_category_id,
    item_condition_id,
    item_name,
    item_description,
    item_price
)
SELECT
    a.account_id,
    cat.item_category_id,
    cond.item_condition_id,
    v.item_name,
    v.item_description,
    v.item_price
FROM (VALUES
    ('alice', 'Clothing',    'Like New', 'Vintage Levi Jacket',    'Classic 90s Levi denim jacket, size M. Minor fading adds to the charm.',     35.00),
    ('alice', 'Clothing',    'Good',     'Nike Air Force 1',       'White AF1 in good shape, size 42. Some creasing on the toe box.',             55.00),
    ('alice', 'Home & Garden','Good',    'IKEA KALLAX 2x4',        '2x4 shelf unit in white. Light scuffs on the base. Self-pickup only.',        40.00),
    ('bob',   'Electronics', 'Like New', 'Sony WH-1000XM4',        'Noise-cancelling headphones, original box and all accessories included.',    180.00),
    ('bob',   'Electronics', 'Good',     'Logitech MX Master 3',   'Wireless mouse, works perfectly. Minor scratches on the underside.',          45.00),
    ('bob',   'Electronics', 'Fair',     'Kindle Paperwhite Gen 4','Works great, cracked bezel (screen is perfect). Great for reading.',           30.00),
    ('carol', 'Books',       'Good',     'Clean Code – R. Martin', 'Lightly annotated in pencil. Essential reading for any developer.',            12.00),
    ('carol', 'Books',       'Like New', 'Atomic Habits',          'Barely touched. Life-changing book — highly recommend.',                       9.00),
    ('carol', 'Sports',      'Good',     'Yoga Mat 6mm',           'Non-slip TPE mat, purple. Used for one year, no tears.',                      15.00)
) AS v(seller, category, condition, item_name, item_description, item_price)
JOIN account        a    ON a.account_name            = v.seller
JOIN item_category  cat  ON cat.category_name         = v.category
JOIN item_condition cond ON cond.item_condition_name  = v.condition;
