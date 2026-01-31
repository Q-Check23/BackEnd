create table clubs
(
    created_at       datetime(6)  not null,
    id               bigint auto_increment
        primary key,
    discord_guild_id varchar(50)  not null,
    name             varchar(100) not null,
    cover_image_url  varchar(255) null,
    description      varchar(255) null,
    constraint uq_clubs_discord_guild_id
        unique (discord_guild_id)
);

create table events
(
    is_active          bit            not null,
    register_fee       decimal(10, 2) not null,
    club_id            bigint         not null,
    end_time           datetime(6)    not null,
    id                 bigint auto_increment
        primary key,
    start_time         datetime(6)    not null,
    discord_channel_id varchar(50)    null,
    location           varchar(100)   null,
    description        varchar(255)   null,
    title              varchar(255)   not null,
    constraint FKmt9rjn9hbh6g8isda7c1g14bd
        foreign key (club_id) references clubs (id)
);

create table form_fields
(
    is_required bit                                not null,
    event_id    bigint                             not null,
    id          bigint auto_increment
        primary key,
    sort_order  bigint                             not null,
    label       varchar(255)                       not null,
    options     json                               null,
    field_type  enum ('BOOLEAN', 'SELECT', 'TEXT') not null,
    constraint FKopa74epaj24wxp7hawu8tv0q0
        foreign key (event_id) references events (id)
);

create table users
(
    created_at datetime(6)  not null,
    id         bigint auto_increment
        primary key,
    discord_id varchar(255) not null,
    real_name  varchar(255) null,
    username   varchar(255) not null,
    constraint UK8h3kehimxhos38stbts56bkba
        unique (discord_id)
);

create table club_members
(
    club_id   bigint                            null,
    id        bigint auto_increment
        primary key,
    joined_at datetime(6)                       not null,
    user_id   bigint                            null,
    role      enum ('ADMIN', 'MEMBER', 'OWNER') not null,
    constraint FKbwqgge4dgaaxukg2hytd9enhp
        foreign key (club_id) references clubs (id),
    constraint FKrhejy2k7mkjakkwdckyps1jfo
        foreign key (user_id) references users (id)
);

create table event_photos
(
    created_at       datetime(6)  not null,
    event_id         bigint       not null,
    id               bigint auto_increment
        primary key,
    uploader_user_id bigint       not null,
    photo_url        varchar(255) not null,
    constraint FK7sqgrrutdbot04bosfqcd4i6v
        foreign key (event_id) references events (id),
    constraint FKiid0s9n63icqvjc8mnwjuh77t
        foreign key (uploader_user_id) references users (id)
);

create table registrations
(
    register_fee_paid bit                             not null,
    event_id          bigint                          not null,
    id                bigint auto_increment
        primary key,
    registered_at     datetime(6)                     not null,
    user_id           bigint                          not null,
    qr_token          varchar(100)                    not null,
    status            enum ('CANCELED', 'REGISTERED') not null,
    constraint uq_registrations_event_user
        unique (event_id, user_id),
    constraint uq_registrations_qr_token
        unique (qr_token),
    constraint FK8mi58jt1s8fxmi56jnau0cxqw
        foreign key (event_id) references events (id),
    constraint FKl2iby9n9hp8jwkfj8i96pkxpi
        foreign key (user_id) references users (id)
);

create table attendance_logs
(
    check_in_time   datetime(6)           not null,
    log_id          bigint auto_increment
        primary key,
    registration_id bigint                not null,
    method          enum ('AR', 'MANUAL') not null,
    constraint FK31419tnlkq452v0ysricedldh
        foreign key (registration_id) references registrations (id)
);

create table form_answers
(
    field_id        bigint   not null,
    id              bigint auto_increment
        primary key,
    registration_id bigint   not null,
    answer_value    tinytext null,
    constraint uq_form_answers_registration_field
        unique (registration_id, field_id),
    constraint FKj55hvhs0x6d210cpucqfvxio2
        foreign key (registration_id) references registrations (id),
    constraint FKshrteaf1s21aa6u3j1lqilhew
        foreign key (field_id) references form_fields (id)
);

create table settlements
(
    total_amount       decimal(10, 2) not null,
    created_at         datetime(6)    not null,
    created_by_user_id bigint         not null,
    event_id           bigint         not null,
    settlement_id      bigint auto_increment
        primary key,
    title              varchar(100)   not null,
    ocr_data           json           null,
    receipt_image_url  varchar(255)   null,
    constraint FKexgy0sr8le6c62geybwggfp77
        foreign key (event_id) references events (id),
    constraint FKgfmhtndegj9pyqkijenj00d54
        foreign key (created_by_user_id) references users (id)
);

create table settlement_items
(
    amount           decimal(10, 2)                          not null,
    item_id          bigint auto_increment
        primary key,
    last_reminded_at datetime(6)                             null,
    remind_count     bigint                                  not null,
    settlement_id    bigint                                  not null,
    updated_at       datetime(6)                             null,
    user_id          bigint                                  not null,
    status           enum ('COMPLETED', 'PENDING', 'UNPAID') not null,
    constraint uq_settlement_items_settlement_user
        unique (settlement_id, user_id),
    constraint FK39hseus1b8suaff1g7844qauo
        foreign key (settlement_id) references settlements (settlement_id),
    constraint FKns6p3yty9k2ktqb9usd19cvba
        foreign key (user_id) references users (id)
);

