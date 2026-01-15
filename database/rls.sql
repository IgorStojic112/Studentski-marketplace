alter table ads enable row level security;
alter table favorites enable row level security;

create policy "Public read ads"
on ads for select
using (true);

create policy "User manages own favorites"
on favorites for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);
