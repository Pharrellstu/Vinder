-- Dialogues (alice=1, bob=2, carol=3), tied to an item
INSERT INTO dialogue (dialogue_creator_id, dialogue_receiver_id, item_id)
VALUES
    (1, 2, (SELECT item_id FROM item WHERE item_name='Sony WH-1000XM4')),
    (3, 1, (SELECT item_id FROM item WHERE item_name='Nike Air Force 1')),
    (1, 3, (SELECT item_id FROM item WHERE item_name='Atomic Habits'))
ON CONFLICT (dialogue_creator_id, dialogue_receiver_id) DO UPDATE SET item_id = EXCLUDED.item_id;

-- Messages: dialogue 1 (alice <-> bob)
INSERT INTO dialogue_message (dialogue_id, sender_id, message_text, timestamp, is_read)
SELECT d.dialogue_id, m.sender, m.text, NOW() - (m.mins || ' minutes')::interval, m.rd
FROM dialogue d JOIN (VALUES
    (1, 'Hi! Is the Sony WH-1000XM4 still available?', 180, true),
    (2, 'Yes, still available — comes with the original box.', 170, true),
    (1, 'Great, would you do 170?', 5, false)
) AS m(sender, text, mins, rd) ON TRUE
WHERE d.dialogue_creator_id=1 AND d.dialogue_receiver_id=2
  AND NOT EXISTS (SELECT 1 FROM dialogue_message dm WHERE dm.dialogue_id=d.dialogue_id AND dm.message_text=m.text);

-- Messages: dialogue 2 (carol -> alice)
INSERT INTO dialogue_message (dialogue_id, sender_id, message_text, timestamp, is_read)
SELECT d.dialogue_id, m.sender, m.text, NOW() - (m.mins || ' minutes')::interval, m.rd
FROM dialogue d JOIN (VALUES
    (3, 'Are the Air Force 1s still up for grabs?', 90, false),
    (1, 'They are! Size 42, barely worn.', 80, true)
) AS m(sender, text, mins, rd) ON TRUE
WHERE d.dialogue_creator_id=3 AND d.dialogue_receiver_id=1
  AND NOT EXISTS (SELECT 1 FROM dialogue_message dm WHERE dm.dialogue_id=d.dialogue_id AND dm.message_text=m.text);

-- Messages: dialogue 3 (alice -> carol)
INSERT INTO dialogue_message (dialogue_id, sender_id, message_text, timestamp, is_read)
SELECT d.dialogue_id, m.sender, m.text, NOW() - (m.mins || ' minutes')::interval, m.rd
FROM dialogue d JOIN (VALUES
    (1, 'Loved Atomic Habits — is your copy still available?', 1500, true),
    (3, 'Yes! A few pencil notes inside, otherwise great.', 1440, false)
) AS m(sender, text, mins, rd) ON TRUE
WHERE d.dialogue_creator_id=1 AND d.dialogue_receiver_id=3
  AND NOT EXISTS (SELECT 1 FROM dialogue_message dm WHERE dm.dialogue_id=d.dialogue_id AND dm.message_text=m.text);
