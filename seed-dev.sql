-- Development fixtures for MeetingPlanner.
--
-- Prerequisites: run this AFTER the Spring API has started once, so Flyway
-- applies the migrations (which create the `users` table and the pgcrypto
-- extension used below).
--
-- Usage (from the repo root, with local Postgres running):
--   $env:PGPASSWORD = (Select-String -Path .env -Pattern '^DB_PASSWORD=').Line.Split('=',2)[1]
--   psql -h localhost -U postgres -d meeting_planner_db -f seed-dev.sql
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
