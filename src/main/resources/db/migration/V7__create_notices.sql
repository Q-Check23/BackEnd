create table notices
(
    id         bigint auto_increment primary key,
    club_id    bigint        not null,
    author_id  bigint        not null,
    title      varchar(255)  not null,
    content    text          not null,
    created_at datetime(6)   not null,
    constraint fk_notices_club_id
        foreign key (club_id) references clubs (id),
    constraint fk_notices_author_id
        foreign key (author_id) references users (id)
);

create index idx_notices_club_id_created_at on notices (club_id, created_at desc);
