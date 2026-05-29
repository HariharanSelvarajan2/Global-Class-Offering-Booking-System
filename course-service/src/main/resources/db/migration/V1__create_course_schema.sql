create table courses (
    id uuid primary key,
    name varchar(160) not null unique,
    created_at timestamptz not null default now()
);

create table offerings (
    id uuid primary key,
    course_id uuid not null references courses(id),
    teacher_id uuid not null,
    name varchar(160) not null,
    teacher_timezone varchar(80) not null,
    status varchar(20) not null,
    created_at timestamptz not null default now()
);

create table course_sessions (
    id uuid primary key,
    offering_id uuid not null references offerings(id) on delete cascade,
    teacher_id uuid not null,
    start_at timestamptz not null,
    end_at timestamptz not null,
    source_timezone varchar(80) not null,
    constraint ck_course_session_time_order check (end_at > start_at)
);

create index idx_offerings_teacher on offerings(teacher_id);
create index idx_sessions_offering_start on course_sessions(offering_id, start_at);
create index idx_sessions_end_at on course_sessions(end_at);
