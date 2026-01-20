create table if not exists profiles (
    id uuid references auth.users(id) primary key,
    full_name text,
    faculty text,
    created_at timestamp with time zone default now()
);

create table if not exists ads (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references auth.users(id) on delete cascade,
    title text not null,
    description text,
    price numeric,
    created_at timestamp with time zone default now()
);

create table if not exists favorites (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id),
    ad_id uuid not null references ads(id),
    created_at timestamp with time zone default now(),
    unique(user_id, ad_id)
);

create table if not exists messages (
    id uuid primary key default gen_random_uuid(),
    sender_id uuid references auth.users(id),
    receiver_id uuid references auth.users(id),
    content text not null,
    created_at timestamp with time zone default now()
);