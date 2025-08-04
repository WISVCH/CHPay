INSERT INTO users (id, rfid, name, email, open_id, balance, banned) VALUES
                                                          ('11111111-1111-1111-1111-111111111111', '048940a2ed6a80', 'Okan',  'o.d.baykal@student.tudelft.nl', 'WISVCH.25236', 300.00, false),
                                                          ('22222222-2222-2222-2222-222222222222', '2', 'Mate',  'mate@email.com', 'moszko', 75.00, false),
                                                          ('33333333-3333-3333-3333-333333333333', '044c2312ec0f90', 'Alexei',  'zxc@zxc.zxc', 'WISVCH.15680', 50.00, false),
                                                          ('44444444-4444-4444-4444-444444444444', '4', 'Stelian',  'sgrozev@tudelft.nl', 'WISVCH.26926', 100.00, false),
                                                          ('44444444-4444-4444-4444-444444444445', '5', 'playwright',  'playwright@test.com', 'playwright', 100.00, false)
ON CONFLICT (open_id) DO NOTHING;


INSERT INTO transactions (id, user_id, amount, description, status, timestamp, type) VALUES
                                                                               (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', -25.00, 'Coffee', 'SUCCESSFUL', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', -50.00, 'Paycheck', 'SUCCESSFUL', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', -100.00, 'Bookstore', 'SUCCESSFUL', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', -10.50, 'Cafeteria', 'SUCCESSFUL', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '33333333-3333-3333-3333-333333333333', -200.00, 'Scholarship', 'SUCCESSFUL', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444444', -2.50, 'Sandwich', 'SUCCESSFUL', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444444', -12.50, 'Cool Sandwich', 'SUCCESSFUL', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444444', -2.00, 'Coffee', 'SUCCESSFUL', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444444', -3.50, 'Monster Energy Drink', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444444', -12.00, 'Xmas Party', 'PENDING', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 1', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 2', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 3', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 4', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 5', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 6', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 7', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 8', 'REFUNDED', now(), 'PAYMENT'),
                                                                               (gen_random_uuid(), '44444444-4444-4444-4444-444444444445', -3.50, 'Monster 9', 'REFUNDED', now(), 'PAYMENT')
ON CONFLICT (id) DO NOTHING;

INSERT INTO requests (request_id, amount, description, multi_use, created_at, fulfilled) VALUES
                                                                                             ('11111111-1111-1111-1111-111111111111',  115.00, 'Salted Chips', true, now(), false),
                                                                                             (gen_random_uuid(),  2.00, 'Ham Sandwich', false, now(), true),
                                                                                             (gen_random_uuid(),  1.00, 'Coffee', false, now(), false),
                                                                                             (gen_random_uuid(),  7.00, 'Cool Event', false, now(), false),
                                                                                             (gen_random_uuid(),  15.00, 'Sweet Chips', false, now(), false),
                                                                                             (gen_random_uuid(),  11.00, 'Xmas Party', false, now(), false)
ON CONFLICT (request_id) DO NOTHING;

