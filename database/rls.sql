alter table profiles enable row level security;
alter table ads enable row level security;
alter table favorites enable row level security;
alter table messages enable row level security;

create policy "User reads own profile"
on profiles for select
using (auth.uid() = id);

create policy "User updates own profile"
on profiles for update
using (auth.uid() = id)
with check (auth.uid() = id);

create policy "Public read ads"
on ads for select
using (true);

create policy "Insert own ads"
on ads for insert
with check (auth.uid() = user_id);

create policy "Update own ads"
on ads for update
using (auth.uid() = user_id);

create policy "Delete own ads"
on ads for delete
using (auth.uid() = user_id);

create policy "Manage own favorites"
on favorites for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "Chat participants read"
on messages for select
using (auth.uid() = sender_id OR auth.uid() = receiver_id);

create policy "Send message"
on messages for insert
with check (auth.uid() = sender_id);