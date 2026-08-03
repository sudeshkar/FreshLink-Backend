-- =====================================================================
-- V3 - Trace orders created by the matching engine back to their match.
--
-- Accepting a supply match creates an order, but nothing recorded where it
-- came from, so "which demand produced this order" was unanswerable and the
-- matching half of the platform had no audit trail.
--
-- Nullable: spot-market orders placed directly by a cafe have no match.
-- =====================================================================

alter table orders add column supply_match_id bigint;

alter table orders
    add constraint fk_orders_supply_match
    foreign key (supply_match_id) references supply_match (id);

create index idx_orders_supply_match on orders (supply_match_id);
