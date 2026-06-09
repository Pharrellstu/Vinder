CREATE TABLE IF NOT EXISTS item_condition (
    item_condition_id SERIAL PRIMARY KEY,
    item_condition_name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS status (
    status_id   SERIAL PRIMARY KEY,
    status_name VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS rating (
    rating_id     SERIAL PRIMARY KEY,
    rating_number INT UNIQUE
);

CREATE TABLE IF NOT EXISTS item_category (
    item_category_id SERIAL PRIMARY KEY,
    category_name    VARCHAR(100) NOT NULL
);

-- account related tables

CREATE TABLE IF NOT EXISTS account (
    account_id    SERIAL PRIMARY KEY,
    account_name  VARCHAR(50)  NOT NULL UNIQUE,
    account_email VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_verified   BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS account_side_information (
    account_side_information_id SERIAL PRIMARY KEY,
    account_id                  INT          NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    account_bio                 VARCHAR(255),
    account_location            VARCHAR(255),
    account_profile_picture_url VARCHAR(255),
    account_public_url          VARCHAR(255),
    UNIQUE (account_id)
);

CREATE TABLE IF NOT EXISTS account_authentication (
    account_authentication_id SERIAL PRIMARY KEY,
    account_id                INT          NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    hashed_code               VARCHAR(255) NOT NULL,
    expiration_date           TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS account_following (
    account_following_id SERIAL PRIMARY KEY,
    follower_id          INT NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    following_id         INT NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    UNIQUE (follower_id, following_id)
);

CREATE TABLE IF NOT EXISTS account_rating (
    account_rating_id  SERIAL PRIMARY KEY,
    rater_id           INT         REFERENCES account (account_id) ON DELETE SET NULL,
    rated_account_id   INT         REFERENCES account (account_id) ON DELETE CASCADE,
    rating_id          INT         REFERENCES rating (rating_id)   ON DELETE SET NULL,
    rated_at           TIMESTAMPTZ
);

-- item table

CREATE TABLE IF NOT EXISTS item (
    item_id              SERIAL PRIMARY KEY,
    seller_id            INT              NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    item_category_id     INT              NOT NULL REFERENCES item_category (item_category_id) ON DELETE RESTRICT,
    item_condition_id    INT              NOT NULL REFERENCES item_condition (item_condition_id) ON DELETE RESTRICT,
    item_name            VARCHAR(50)      NOT NULL,
    item_description     VARCHAR(500)     NOT NULL,
    item_price           DOUBLE PRECISION NOT NULL,
    item_discount        INT,
    is_listed            BOOLEAN          NOT NULL DEFAULT TRUE,
    is_sold              BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS item_photo (
    item_photo_id SERIAL PRIMARY KEY,
    item_id       INT NOT NULL REFERENCES item (item_id) ON DELETE CASCADE,
    photo_url     VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS account_favorite (
    account_favorite_id SERIAL PRIMARY KEY,
    account_id          INT NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    item_id             INT NOT NULL REFERENCES item (item_id)    ON DELETE CASCADE,
    UNIQUE (account_id, item_id)
);

CREATE TABLE IF NOT EXISTS item_offer (
    item_offer_id      SERIAL PRIMARY KEY,
    item_id            INT NOT NULL REFERENCES item (item_id)    ON DELETE CASCADE,
    offer_creator_id   INT NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    offer_status_id    INT NOT NULL REFERENCES status (status_id) ON DELETE RESTRICT,
    offer_price        DOUBLE PRECISION NOT NULL,
    created_at         TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

-- chat related tables 

CREATE TABLE IF NOT EXISTS dialogue (
    dialogue_id          SERIAL PRIMARY KEY,
    dialogue_creator_id  INT NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    dialogue_receiver_id INT NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    UNIQUE (dialogue_creator_id, dialogue_receiver_id)
);

CREATE TABLE IF NOT EXISTS dialogue_message (
    dialogue_message_id SERIAL PRIMARY KEY,
    dialogue_id         INT NOT NULL REFERENCES dialogue (dialogue_id) ON DELETE CASCADE,
    sender_id           INT NOT NULL REFERENCES account (account_id)   ON DELETE CASCADE,
    message_text        VARCHAR(255) NOT NULL,
    timestamp           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS dialogue_message_attachment (
    dialogue_message_attachment_id SERIAL PRIMARY KEY,
    dialogue_message_id            INT NOT NULL REFERENCES dialogue_message (dialogue_message_id) ON DELETE CASCADE,
    attachment_link                VARCHAR(255) NOT NULL
);

-- indexes
CREATE INDEX IF NOT EXISTS idx_item_seller           ON item (seller_id);
CREATE INDEX IF NOT EXISTS idx_item_category         ON item (item_category_id);
CREATE INDEX IF NOT EXISTS idx_item_condition        ON item (item_condition_id);
CREATE INDEX IF NOT EXISTS idx_item_listed_sold      ON item (is_listed, is_sold);

CREATE INDEX IF NOT EXISTS idx_item_photo_item       ON item_photo (item_id);

CREATE INDEX IF NOT EXISTS idx_account_favorite_account ON account_favorite (account_id);
CREATE INDEX IF NOT EXISTS idx_account_favorite_item    ON account_favorite (item_id);

CREATE INDEX IF NOT EXISTS idx_item_offer_item       ON item_offer (item_id);
CREATE INDEX IF NOT EXISTS idx_item_offer_creator    ON item_offer (offer_creator_id);

CREATE INDEX IF NOT EXISTS idx_dialogue_creator      ON dialogue (dialogue_creator_id);
CREATE INDEX IF NOT EXISTS idx_dialogue_receiver     ON dialogue (dialogue_receiver_id);

CREATE INDEX IF NOT EXISTS idx_dialogue_msg_dialogue ON dialogue_message (dialogue_id);
CREATE INDEX IF NOT EXISTS idx_dialogue_msg_sender   ON dialogue_message (sender_id);
CREATE INDEX IF NOT EXISTS idx_dialogue_msg_read     ON dialogue_message (dialogue_id, is_read);

CREATE INDEX IF NOT EXISTS idx_dialogue_attach_msg   ON dialogue_message_attachment (dialogue_message_id);

CREATE INDEX IF NOT EXISTS idx_account_auth_account  ON account_authentication (account_id);
CREATE INDEX IF NOT EXISTS idx_account_rating_rated  ON account_rating (rated_account_id);
CREATE INDEX IF NOT EXISTS idx_account_following_f   ON account_following (follower_id);
CREATE INDEX IF NOT EXISTS idx_account_following_ing ON account_following (following_id);
