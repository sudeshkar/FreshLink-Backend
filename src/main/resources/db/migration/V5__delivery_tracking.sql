-- =====================================================================
-- V5 - Give deliveries something to track.
--
-- A delivery row was raised when the supplier marked an order as
-- delivering and then never touched again: no driver, no ETA, no arrival
-- time, and only SCHEDULED or DELIVERED to describe it. A cafe had no way
-- to find out where its fish was.
-- =====================================================================

alter table delivery add column driver_name varchar(255);
alter table delivery add column driver_phone varchar(255);
alter table delivery add column expected_at timestamp(6);
alter table delivery add column delivered_at timestamp(6);
alter table delivery add column notes varchar(500);

-- IN_TRANSIT and FAILED are new: a delivery can now be under way, and an
-- attempt that does not succeed has somewhere to land instead of being
-- silently left SCHEDULED.
alter table delivery drop constraint if exists delivery_status_check;
alter table delivery add constraint delivery_status_check
    check (status in ('SCHEDULED', 'IN_TRANSIT', 'DELIVERED', 'FAILED'));

-- Deliveries are looked up by their order on every tracking request.
create index idx_delivery_order on delivery (order_id);
