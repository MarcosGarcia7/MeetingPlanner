-- Development fixtures for MeetingPlanner.
--
-- Prerequisites: run this AFTER the Spring API has started once, so Flyway
-- applies the migrations (which create the `users` table and the pgcrypto
-- extension used below).
--
-- Usage (with the Docker stack running, from the repo root):
--   docker cp seed-dev.sql postgres_db:/tmp/seed-dev.sql
--   docker exec postgres_db psql -U postgres -d meeting_planner_db -f /tmp/seed-dev.sql
--
-- Safe to re-run: rows that already exist are skipped. Meeting dates are set
-- relative to the first run and don't move on a re-run. The pending time change
-- proposal expires after 48 hours; re-run the script to create a new one.
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

-- Bob is alice's friend and a member of Seed Group 03, so two-person flows work
-- without any setup.
INSERT INTO friendships (user_id, friend_id, requester_id, status, created_at, responded_at)
SELECT
    LEAST(a.id, b.id),
    GREATEST(a.id, b.id),
    a.id,
    'ACCEPTED',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days' + INTERVAL '10 minutes'
FROM users a
JOIN users b ON b.username = 'bob'
WHERE a.username = 'alice'
ON CONFLICT DO NOTHING;

INSERT INTO group_members (group_id, user_id, is_admin)
SELECT g.id, b.id, false
FROM groups g
JOIN users b ON b.username = 'bob'
WHERE g.group_name = 'Seed Group 03'
ON CONFLICT DO NOTHING;

-- Meeting scenario. Upcoming meetings start at least 2 days out so reminders
-- don't fire straight away. No user is double-booked, because the API rejects
-- creating a meeting that overlaps the creator's schedule. Conflicts come up when:
--   - alice schedules over one of her own meetings
--   - alice invites bob to a time that overlaps Client Call
--   - alice votes on the Design Review proposal, which overlaps Dentist Appointment
-- meetings has no natural unique key, so guard with NOT EXISTS.
INSERT INTO meetings (group_id, start_time, end_time, location, title, creator_id, is_private)
SELECT
    g.id,
    date_trunc('day', NOW()) + v.start_offset,
    date_trunc('day', NOW()) + v.start_offset + INTERVAL '1 hour',
    v.location,
    v.title,
    u.id,
    v.is_private
FROM (VALUES
    ('Sprint Retro',        'alice', 'Seed Group 03', INTERVAL '-3 days 15 hours', 'Room B', false),
    ('Project Kickoff',     'alice', NULL,            INTERVAL '2 days 10 hours',  'Room A', false),
    ('Design Review',       'bob',   NULL,            INTERVAL '3 days 14 hours',  'Room C', false),
    ('Team Sync',           'alice', 'Seed Group 03', INTERVAL '4 days 9 hours',   'Room A', false),
    ('Dentist Appointment', 'alice', NULL,            INTERVAL '5 days 11 hours',  NULL,     true),
    ('Client Call',         'bob',   NULL,            INTERVAL '6 days 13 hours',  NULL,     false)
) AS v(title, creator, group_name, start_offset, location, is_private)
JOIN users u ON u.username = v.creator
LEFT JOIN groups g ON g.group_name = v.group_name
WHERE NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = v.title AND m.creator_id = u.id);

INSERT INTO meeting_participants (meeting_id, user_id)
SELECT m.id, p.id
FROM (VALUES
    ('Sprint Retro',        'alice', 'alice'),
    ('Sprint Retro',        'alice', 'bob'),
    ('Project Kickoff',     'alice', 'alice'),
    ('Project Kickoff',     'alice', 'bob'),
    ('Design Review',       'bob',   'bob'),
    ('Design Review',       'bob',   'alice'),
    ('Design Review',       'bob',   'user01'),
    ('Team Sync',           'alice', 'alice'),
    ('Team Sync',           'alice', 'bob'),
    ('Dentist Appointment', 'alice', 'alice'),
    ('Client Call',         'bob',   'bob')
) AS v(title, creator, participant)
JOIN users c ON c.username = v.creator
JOIN meetings m ON m.title = v.title AND m.creator_id = c.id
JOIN users p ON p.username = v.participant
ON CONFLICT DO NOTHING;

-- Bob proposes moving Design Review to overlap alice's Dentist Appointment.
-- user01 has approved and alice's vote is pending, so her approval applies the
-- change. Skipped while a proposal is pending, as the API allows only one.
INSERT INTO meeting_time_change_proposals (meeting_id, proposed_by, proposed_start_time, proposed_end_time)
SELECT
    m.id,
    m.creator_id,
    d.start_time + INTERVAL '30 minutes',
    d.start_time + INTERVAL '90 minutes'
FROM meetings m
JOIN users b ON b.id = m.creator_id AND b.username = 'bob'
JOIN users a ON a.username = 'alice'
JOIN meetings d ON d.title = 'Dentist Appointment' AND d.creator_id = a.id
WHERE m.title = 'Design Review'
  AND NOT EXISTS (
      SELECT 1 FROM meeting_time_change_proposals tcp
      WHERE tcp.meeting_id = m.id AND tcp.status = 'PENDING'
  );

INSERT INTO meeting_time_change_votes (proposal_id, user_id, status, responded_at)
SELECT
    tcp.id,
    v.id,
    CASE WHEN v.username = 'user01' THEN 'APPROVED' ELSE 'PENDING' END,
    CASE WHEN v.username = 'user01' THEN NOW() END
FROM meeting_time_change_proposals tcp
JOIN meetings m ON m.id = tcp.meeting_id AND m.title = 'Design Review'
JOIN users b ON b.id = m.creator_id AND b.username = 'bob'
JOIN users v ON v.username IN ('alice', 'user01')
WHERE tcp.status = 'PENDING'
ON CONFLICT DO NOTHING;
