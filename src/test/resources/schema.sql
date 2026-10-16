create table if not exists payment_settings (
    id uuid primary key,
    gateway_code varchar(100) not null
);

create table if not exists invoice (
    id uuid primary key,
    status varchar(20) not null,
    expires_at timestamptz not null,
    payment_settings_id uuid references payment_settings (id),
    canceled_at timestamptz,
    cancel_reason varchar(255)
);
