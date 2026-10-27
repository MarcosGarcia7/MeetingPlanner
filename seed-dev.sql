-- Development fixtures for MeetingPlanner.
--
-- Prerequisites: run this AFTER the Spring API has started once, so Flyway
-- applies the migrations (which create the `users` table and the pgcrypto
-- extension used below).
--
-- Usage (with the Docker stack running):
--   docker exec -it postgres_db psql -U postgres -d meeting_planner_db
--   -- then paste the contents of this file
--
-- All users share the password: Password123!
-- `crypt(..., gen_salt('bf', 10))` produces a $2a$ bcrypt hash that Spring
-- Security's BCryptPasswordEncoder verifies.

INSERT INTO users (username, email, password_hash, first_name, last_name, avatar_color, is_private, email_verified_at)
VALUES
    ('alice', 'alice@example.com', crypt('Password123!', gen_salt('bf', 10)), 'Alice', 'Anderson', '#F59E0B', false, NOW()),
    ('bob', 'bob@example.com', crypt('Password123!', gen_salt('bf', 10)), 'Bob', 'Brown', '#F59E0B', false, NOW()),
    ('unverified', 'unverified@example.com', crypt('Password123!', gen_salt('bf', 10)), NULL, NULL, '#F59E0B', false, NULL)
ON CONFLICT DO NOTHING;

-- Bulk users for exercising pagination (default page size is 20, so 60 users
-- spans several pages). Usernames are user01..user60, all verified, with a
-- few private accounts mixed in. One bcrypt hash is computed and shared to
-- keep the script fast.
WITH pw AS (
    SELECT crypt('Password123!', gen_salt('bf', 10)) AS hash
),
first_names AS (
    SELECT ARRAY['Aaron','Bella','Caleb','Diana','Ethan','Fiona','Gavin','Hannah','Isaac','Julia',
                 'Kevin','Laura','Mason','Nora','Owen','Paige','Quinn','Ruby','Samuel','Tessa'] AS a
),
last_names AS (
    SELECT ARRAY['Adams','Baker','Carter','Davis','Evans','Foster','Garcia','Hughes','Irwin','Jones',
                 'Kelly','Lopez','Miller','Nguyen','Ortiz','Parker'] AS a
),
colors AS (
    SELECT ARRAY['#F59E0B','#3B82F6','#10B981','#EF4444','#8B5CF6','#EC4899','#14B8A6','#F97316'] AS a
)
INSERT INTO users (username, email, password_hash, first_name, last_name, avatar_color, is_private, email_verified_at)
SELECT
    'user' || lpad(n::text, 2, '0'),
    'user' || lpad(n::text, 2, '0') || '@example.com',
    pw.hash,
    first_names.a[1 + (n - 1) % array_length(first_names.a, 1)],
    last_names.a[1 + (n - 1) % array_length(last_names.a, 1)],
    colors.a[1 + (n - 1) % array_length(colors.a, 1)],
    n % 10 = 0,
    NOW()
FROM generate_series(1, 60) AS n, pw, first_names, last_names, colors
ON CONFLICT DO NOTHING;

-- Signup creates an empty availability row for every user; mirror that here.
-- user_availability has no unique key on user_id, so guard with NOT EXISTS.
INSERT INTO user_availability (user_id)
SELECT u.id
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_availability ua WHERE ua.user_id = u.id);

-- Friendships for alice (page size 20):
--   user01..user35 -> ACCEPTED friends (2 pages)
--   user36..user60 -> PENDING requests sent to alice (2 pages)
-- Rows store the lower user id in user_id, matching FriendshipService.pair().
-- created_at is staggered so the cursor ordering is deterministic.
INSERT INTO friendships (user_id, friend_id, requester_id, status, created_at, responded_at)
SELECT
    LEAST(a.id, u.id),
    GREATEST(a.id, u.id),
    CASE WHEN n <= 35 THEN a.id ELSE u.id END,
    CASE WHEN n <= 35 THEN 'ACCEPTED' ELSE 'PENDING' END,
    NOW() - n * INTERVAL '1 hour',
    CASE WHEN n <= 35 THEN NOW() - n * INTERVAL '1 hour' + INTERVAL '10 minutes' END
FROM generate_series(1, 60) AS n
JOIN users u ON u.username = 'user' || lpad(n::text, 2, '0')
CROSS JOIN (SELECT id FROM users WHERE username = 'alice') a
ON CONFLICT DO NOTHING;

-- Groups for alice: 25 groups (2 pages of her groups). Groups 01..10 are owned
-- by alice; 11..25 are owned by user11..user25 with alice as a plain member.
-- Odd-numbered groups are public.
INSERT INTO groups (group_name, owner_id, is_private)
SELECT
    'Seed Group ' || lpad(n::text, 2, '0'),
    CASE WHEN n <= 10 THEN a.id ELSE u.id END,
    n % 2 = 0
FROM generate_series(1, 25) AS n
JOIN users u ON u.username = 'user' || lpad(n::text, 2, '0')
CROSS JOIN (SELECT id FROM users WHERE username = 'alice') a
ON CONFLICT DO NOTHING;

-- Owners are admin members, as GroupServiceImpl.create does.
INSERT INTO group_members (group_id, user_id, is_admin)
SELECT g.id, g.owner_id, true
FROM groups g
WHERE g.group_name LIKE 'Seed Group %'
ON CONFLICT DO NOTHING;

-- Alice joins the groups she doesn't own.
INSERT INTO group_members (group_id, user_id, is_admin)
SELECT g.id, a.id, false
FROM groups g
CROSS JOIN (SELECT id FROM users WHERE username = 'alice') a
WHERE g.group_name LIKE 'Seed Group %'
ON CONFLICT DO NOTHING;

-- Seed Group 01 (alice's) gets every bulk user, for member-list pagination
-- (61 members, 4 pages).
INSERT INTO group_members (group_id, user_id, is_admin)
SELECT g.id, u.id, false
FROM groups g
JOIN users u ON u.username ~ '^user[0-9]{2}$'
WHERE g.group_name = 'Seed Group 01'
ON CONFLICT DO NOTHING;
