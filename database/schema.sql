create table if not exists ads (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references auth.users(id) on delete cascade,
    title text not null,
    description text,
    faculty text,
    price numeric,
    created_at timestamp with time zone default now()
);

create table if not exists favorites (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references auth.users(id) on delete cascade,
    ad_id uuid references ads(id) on delete cascade,
    created_at timestamp with time zone default now(),
    unique (user_id, ad_id)
);