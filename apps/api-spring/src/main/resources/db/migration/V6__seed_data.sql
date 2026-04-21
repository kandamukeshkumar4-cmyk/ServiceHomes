INSERT INTO users (id, auth0_id, email, email_verified)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'local-user', 'local@example.com', true),
  ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'auth0|seed-host-1', 'host@example.com', true),
  ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'auth0|seed-guest-1', 'guest@example.com', true)
ON CONFLICT (auth0_id) DO NOTHING;

INSERT INTO profiles (id, user_id, first_name, last_name, display_name)
VALUES
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Local', 'User', 'LocalUser'),
  (gen_random_uuid(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Alice', 'Host', 'Alice'),
  (gen_random_uuid(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Bob', 'Guest', 'Bob')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES
  ('00000000-0000-0000-0000-000000000001', (SELECT id FROM roles WHERE name = 'HOST')),
  ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM roles WHERE name = 'HOST')),
  ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', (SELECT id FROM roles WHERE name = 'TRAVELER'))
ON CONFLICT DO NOTHING;

INSERT INTO listings (id, host_id, title, description, category_id, property_type, max_guests, bedrooms, beds, bathrooms, nightly_price, cleaning_fee, service_fee, status, published_at)
SELECT
  gen_random_uuid(),
  'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  'Cozy Beachfront Villa',
  'A beautiful villa right on the beach with amazing views and all amenities.',
  id,
  'VILLA',
  6,
  3,
  4,
  2,
  250.00,
  50.00,
  25.00,
  'PUBLISHED',
  NOW()
FROM listing_categories WHERE name = 'Beachfront';
