alter table users
    add column deleted_at datetime(6) null;

create index idx_users_deleted_at on users (deleted_at);
