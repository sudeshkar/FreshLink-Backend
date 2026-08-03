-- =====================================================================
-- V2 - Store refresh tokens hashed, and support rotation.
--
-- Tokens were held in plain text, so anyone who could read the table held
-- working credentials for up to seven days. They also never rotated, which
-- meant a leaked token stayed valid for its whole lifetime and a replay was
-- indistinguishable from legitimate use.
--
-- Existing rows are removed rather than migrated: a hash cannot be derived
-- from a token the server no longer needs to know, and keeping plaintext
-- rows would defeat the change. Everyone signs in again once.
-- =====================================================================

delete from refresh_token;

alter table refresh_token rename column token to token_hash;

-- Set when the token is exchanged. Rotated rows are kept, not deleted, so a
-- second presentation is recognisable as a replay.
alter table refresh_token add column used boolean not null default false;

alter table refresh_token add column created_at timestamp(6);

-- The lookup is now by hash and must be unique: a collision would let one
-- account's token resolve to another's session.
drop index if exists idx_refresh_token_token;
create unique index ux_refresh_token_hash on refresh_token (token_hash);

alter table refresh_token alter column token_hash set not null;
