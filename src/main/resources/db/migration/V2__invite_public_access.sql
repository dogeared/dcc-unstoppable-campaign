alter table campaign_invite
    add column public_access boolean not null default false;
