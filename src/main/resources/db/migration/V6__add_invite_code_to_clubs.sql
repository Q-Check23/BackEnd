alter table clubs
    add column invite_code varchar(16) null;

update clubs
set invite_code = upper(substring(replace(uuid(), '-', ''), 1, 8))
where invite_code is null;

alter table clubs
    modify column invite_code varchar(16) not null;

alter table clubs
    add constraint uq_clubs_invite_code unique (invite_code);
