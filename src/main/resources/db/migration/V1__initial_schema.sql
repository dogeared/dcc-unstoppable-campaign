-- Campaign sheet schema. Field names mirror docs/form-field-map.md.

create table campaign (
    id uuid primary key,
    owner_sub varchar(128) not null,
    party_name varchar(120) not null,
    difficulty varchar(32) not null,
    status varchar(32) not null,
    version bigint not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_campaign_owner on campaign (owner_sub);

create table participant (
    id uuid primary key,
    campaign_id uuid not null references campaign (id) on delete cascade,
    seat int not null,
    crawler varchar(120),
    race varchar(80),
    class_name varchar(80),
    starting_health int not null default 0,
    starting_mana int not null default 0,
    starting_gold int not null default 0,
    signature_level1 varchar(120),
    signature_level2 varchar(120),
    constraint uq_participant_seat unique (campaign_id, seat)
);

create table deck_entry (
    id uuid primary key,
    participant_id uuid not null references participant (id) on delete cascade,
    position int not null,
    base_card varchar(80) not null,
    card_name varchar(120),
    eliminated boolean not null default false,
    upgrade_name varchar(160),
    extra_starting_hand boolean not null default false,
    mastered boolean not null default false,
    notes varchar(600),
    constraint uq_deck_position unique (participant_id, position)
);

create table floor_progress (
    id uuid primary key,
    campaign_id uuid not null references campaign (id) on delete cascade,
    floor_number int not null,
    boss_quest_xp int not null default 0,
    completed_at date,
    notes varchar(600),
    constraint uq_floor_progress unique (campaign_id, floor_number)
);

create table floor_attempt (
    id uuid primary key,
    campaign_id uuid not null references campaign (id) on delete cascade,
    floor_number int not null,
    played_at date not null,
    successful boolean not null,
    notes varchar(600)
);

create index idx_floor_attempt_campaign on floor_attempt (campaign_id, floor_number);

create table achievement (
    id uuid primary key,
    campaign_id uuid not null references campaign (id) on delete cascade,
    floor_number int not null,
    name varchar(160) not null,
    xp int not null,
    completed_at date not null,
    constraint uq_achievement_name unique (campaign_id, name)
);

create index idx_achievement_campaign on achievement (campaign_id, floor_number);

create table purchase (
    id uuid primary key,
    campaign_id uuid not null references campaign (id) on delete cascade,
    participant_id uuid not null references participant (id) on delete cascade,
    benefit varchar(48) not null,
    cost int not null,
    x_value int,
    target_deck_entry_id uuid references deck_entry (id) on delete set null,
    target_card varchar(160),
    upgrade_name varchar(160),
    notes varchar(600),
    purchased_at timestamptz not null
);

create index idx_purchase_campaign on purchase (campaign_id);

create table campaign_membership (
    id uuid primary key,
    campaign_id uuid not null references campaign (id) on delete cascade,
    subject varchar(128) not null,
    role varchar(16) not null,
    created_at timestamptz not null,
    constraint uq_membership unique (campaign_id, subject)
);

create index idx_membership_subject on campaign_membership (subject);

create table campaign_invite (
    id uuid primary key,
    campaign_id uuid not null references campaign (id) on delete cascade,
    token_hash varchar(64) not null unique,
    role varchar(16) not null,
    created_by_sub varchar(128) not null,
    created_at timestamptz not null,
    expires_at timestamptz,
    revoked_at timestamptz
);

create index idx_invite_campaign on campaign_invite (campaign_id);
