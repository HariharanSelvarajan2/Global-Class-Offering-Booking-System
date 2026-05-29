create table bookings (
    id uuid primary key,
    parent_id uuid not null,
    offering_id uuid not null,
    course_name varchar(160) not null,
    offering_name varchar(160) not null,
    status varchar(20) not null,
    booked_at timestamptz not null default now(),
    constraint uq_booking_parent_offering unique (parent_id, offering_id)
);

create table parent_booking_locks (
    parent_id uuid primary key
);

create index idx_bookings_parent on bookings(parent_id, booked_at desc);
