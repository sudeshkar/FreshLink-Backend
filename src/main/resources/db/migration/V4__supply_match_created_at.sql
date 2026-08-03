-- =====================================================================
-- V4 - Age matches so stale ones can be expired.
--
-- MatchStatus.EXPIRED existed but nothing ever set it, so a match a
-- supplier never answered stayed PENDING forever. Since allocation now
-- treats pending matches as claims against a supply, an ignored match
-- locks that quantity permanently: the supply can never be offered to
-- another cafe, and the demand behind it never completes.
--
-- Expiring them needs to know how old they are.
-- =====================================================================

alter table supply_match add column created_at timestamp(6);

-- Existing rows have no known age. Treating them as created now gives them a
-- full timeout window rather than expiring them the moment this deploys.
update supply_match set created_at = now() where created_at is null;

-- The sweep looks up pending matches older than a cutoff.
create index idx_supply_match_status_created_at on supply_match (status, created_at);
