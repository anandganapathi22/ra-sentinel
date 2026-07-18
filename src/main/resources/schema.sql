create table if not exists agent_audit_runs (
    run_id uuid primary key,
    ra_id varchar(64) not null,
    status varchar(64) not null,
    likely_cause text not null,
    evidence_json jsonb not null,
    recommended_action text not null,
    confidence varchar(32) not null,
    requires_human_approval boolean not null,
    allowed_actions_json jsonb not null,
    blocked_actions_json jsonb not null,
    tool_results_json jsonb not null,
    created_at timestamptz not null
);

create index if not exists idx_agent_audit_runs_ra_id on agent_audit_runs (ra_id);
create index if not exists idx_agent_audit_runs_created_at on agent_audit_runs (created_at desc);

create table if not exists fleet_ledger_agreements (
    ra_id varchar(64) primary key,
    exists_flag boolean not null,
    status varchar(64) not null,
    customer_email varchar(255) not null,
    language varchar(32) not null
);

create table if not exists counter_console_states (
    ra_id varchar(64) primary key,
    state varchar(128) not null,
    counter_location varchar(32) not null
);

create table if not exists submission_gateway_metadata (
    ra_id varchar(64) primary key,
    submit_status varchar(64) not null,
    email varchar(255) not null,
    language varchar(32) not null,
    correlation_id varchar(128) not null
);

create table if not exists contract_vault_status (
    ra_id varchar(64) primary key,
    state varchar(64) not null,
    agreement_id varchar(128) not null,
    hash_expires_at timestamptz
);

create table if not exists s3_signed_pdf_status (
    ra_id varchar(64) primary key,
    present boolean not null,
    bucket varchar(255) not null,
    object_key text not null
);

create table if not exists keyspace_submission_records (
    ra_id varchar(64) primary key,
    present boolean not null,
    status varchar(64) not null,
    updated_at timestamptz
);

create table if not exists signing_portal_sessions (
    ra_id varchar(64) primary key,
    status varchar(64) not null,
    started_at timestamptz,
    last_activity_at timestamptz,
    last_step varchar(128) not null,
    license_scan_present boolean not null,
    customer_eligible boolean not null
);

create table if not exists correlation_events (
    id bigserial primary key,
    ra_id varchar(64) not null,
    source varchar(64) not null,
    correlation_id varchar(128) not null,
    message text not null,
    occurred_at timestamptz not null,
    unique (ra_id, source, correlation_id, message, occurred_at)
);

create index if not exists idx_correlation_events_ra_id on correlation_events (ra_id, occurred_at);

create table if not exists system_health_snapshots (
    location varchar(64) primary key,
    fleet_status varchar(64) not null,
    console_status varchar(64) not null,
    vault_status varchar(64) not null,
    vault_latency_ms integer not null,
    database_status varchar(64) not null,
    queue_backlog integer not null,
    recent_http_failures_json jsonb not null,
    affected_locations_json jsonb not null
);

insert into fleet_ledger_agreements (ra_id, exists_flag, status, customer_email, language) values
    ('123', true, 'PENDING_SIGNATURE', 'customer@example.com', 'en-US'),
    ('200', true, 'SIGNED', 'signed@example.com', 'en-US'),
    ('300', true, 'SIGNED', 'metadata@example.com', 'en-US'),
    ('400', true, 'PENDING_SIGNATURE', 'right@example.com', 'en-US'),
    ('489965957', true, 'SIGNED', 'hertz.customer@example.com', 'en-US'),
    ('123456', true, 'PENDING_COUNTER_REVIEW', 'license@example.com', 'en-US'),
    ('777', true, 'PENDING_SIGNATURE', 'abandoned@example.com', 'en-US'),
    ('500', true, 'SIGNED', 'complete@example.com', 'en-US')
on conflict (ra_id) do update set
    exists_flag = excluded.exists_flag,
    status = excluded.status,
    customer_email = excluded.customer_email,
    language = excluded.language;

insert into counter_console_states (ra_id, state, counter_location) values
    ('123', 'WAITING_ON_CUSTOMER', 'DFW'),
    ('200', 'PENDING_BACKEND_SYNC', 'DFW'),
    ('300', 'PENDING_BACKEND_SYNC', 'DFW'),
    ('400', 'WAITING_ON_CUSTOMER', 'DFW'),
    ('489965957', 'SIGNED_NOT_VISIBLE_IN_VAULT', 'ORD'),
    ('123456', 'NOT_READY', 'ORD'),
    ('777', 'CUSTOMER_ABANDONED', 'MDW'),
    ('500', 'COMPLETE', 'DFW')
on conflict (ra_id) do update set
    state = excluded.state,
    counter_location = excluded.counter_location;

insert into submission_gateway_metadata (ra_id, submit_status, email, language, correlation_id) values
    ('123', 'SUBMITTED', 'customer@example.com', 'en-US', 'corr-123'),
    ('200', 'SUBMITTED', 'signed@example.com', 'en-US', 'corr-200'),
    ('300', 'SUBMITTED', 'metadata@example.com', 'en-US', 'corr-300'),
    ('400', 'SUBMITTED', 'wrong@example.com', 'en-US', 'corr-400'),
    ('489965957', 'SUBMITTED', 'hertz.customer@example.com', 'en-US', 'corr-489965957'),
    ('123456', 'NOT_READY', 'license@example.com', 'en-US', 'corr-123456'),
    ('777', 'STARTED', 'abandoned@example.com', 'en-US', 'corr-777'),
    ('500', 'SUBMITTED', 'complete@example.com', 'en-US', 'corr-500')
on conflict (ra_id) do update set
    submit_status = excluded.submit_status,
    email = excluded.email,
    language = excluded.language,
    correlation_id = excluded.correlation_id;

insert into contract_vault_status (ra_id, state, agreement_id, hash_expires_at) values
    ('123', 'WAITING_FOR_SIGNATURE', 'vault-123', now() - interval '2 hours'),
    ('200', 'SIGNED', 'vault-200', now() + interval '12 hours'),
    ('300', 'SIGNED', 'vault-300', now() + interval '12 hours'),
    ('400', 'WAITING_FOR_SIGNATURE', 'vault-400', now() + interval '12 hours'),
    ('489965957', 'API_TIMEOUT', 'vault-489965957', now() + interval '12 hours'),
    ('123456', 'NOT_CREATED', '', null),
    ('777', 'WAITING_FOR_SIGNATURE', 'vault-777', now() + interval '4 hours'),
    ('500', 'SIGNED', 'vault-500', now() + interval '12 hours')
on conflict (ra_id) do update set
    state = excluded.state,
    agreement_id = excluded.agreement_id,
    hash_expires_at = excluded.hash_expires_at;

insert into s3_signed_pdf_status (ra_id, present, bucket, object_key) values
    ('123', false, 'ra-signed-pdfs', ''),
    ('200', false, 'ra-signed-pdfs', ''),
    ('300', true, 'ra-signed-pdfs', 'agreements/300/signed.pdf'),
    ('400', false, 'ra-signed-pdfs', ''),
    ('489965957', true, 'ra-signed-pdfs', 'agreements/489965957/signed.pdf'),
    ('123456', false, 'ra-signed-pdfs', ''),
    ('777', false, 'ra-signed-pdfs', ''),
    ('500', true, 'ra-signed-pdfs', 'agreements/500/signed.pdf')
on conflict (ra_id) do update set
    present = excluded.present,
    bucket = excluded.bucket,
    object_key = excluded.object_key;

insert into keyspace_submission_records (ra_id, present, status, updated_at) values
    ('123', false, 'MISSING', null),
    ('200', false, 'MISSING', null),
    ('300', false, 'MISSING', null),
    ('400', false, 'MISSING', null),
    ('489965957', false, 'MISSING', null),
    ('123456', false, 'MISSING', null),
    ('777', false, 'MISSING', null),
    ('500', true, 'SYNCED', now() - interval '2 minutes')
on conflict (ra_id) do update set
    present = excluded.present,
    status = excluded.status,
    updated_at = excluded.updated_at;

insert into signing_portal_sessions (ra_id, status, started_at, last_activity_at, last_step, license_scan_present, customer_eligible) values
    ('123', 'UNKNOWN', null, null, 'UNKNOWN', false, false),
    ('200', 'UNKNOWN', null, null, 'UNKNOWN', false, false),
    ('300', 'UNKNOWN', null, null, 'UNKNOWN', false, false),
    ('400', 'UNKNOWN', null, null, 'UNKNOWN', false, false),
    ('489965957', 'SIGNED', now() - interval '9 minutes', now() - interval '7 minutes', 'SIGNATURE_COMPLETE', true, true),
    ('123456', 'BLOCKED', now() - interval '20 minutes', now() - interval '15 minutes', 'LICENSE_SCAN', false, true),
    ('777', 'ABANDONED', now() - interval '34 minutes', now() - interval '31 minutes', 'SIGNATURE', true, true),
    ('500', 'SIGNED', now() - interval '10 minutes', now() - interval '4 minutes', 'SIGNATURE_COMPLETE', true, true)
on conflict (ra_id) do update set
    status = excluded.status,
    started_at = excluded.started_at,
    last_activity_at = excluded.last_activity_at,
    last_step = excluded.last_step,
    license_scan_present = excluded.license_scan_present,
    customer_eligible = excluded.customer_eligible;

insert into correlation_events (ra_id, source, correlation_id, message, occurred_at) values
    ('489965957', 'SigningPortal', 'corr-489965957', 'Customer opened agreement', now() - interval '9 minutes'),
    ('489965957', 'SigningPortal', 'corr-489965957', 'Customer signed agreement', now() - interval '7 minutes'),
    ('489965957', 'SubmissionGateway', 'corr-489965957', 'Submission gateway submit success', now() - interval '7 minutes'),
    ('489965957', 'FleetLedger', 'corr-489965957', 'Fleet ledger signed status persisted', now() - interval '6 minutes'),
    ('489965957', 'ContractVault', 'corr-489965957', 'Contract vault API timeout', now() - interval '5 minutes'),
    ('489965957', 'ContractVault', 'corr-489965957', 'Retry failed with timeout', now() - interval '3 minutes'),
    ('123', 'SubmissionGateway', 'corr-123', 'Submission metadata retrieved', now() - interval '30 minutes'),
    ('123', 'ContractVault', 'corr-123', 'Agreement status retrieved', now() - interval '20 minutes'),
    ('200', 'SubmissionGateway', 'corr-200', 'Submission metadata retrieved', now() - interval '30 minutes'),
    ('200', 'ContractVault', 'corr-200', 'Agreement status retrieved', now() - interval '20 minutes'),
    ('300', 'SubmissionGateway', 'corr-300', 'Submission metadata retrieved', now() - interval '30 minutes'),
    ('300', 'ContractVault', 'corr-300', 'Agreement status retrieved', now() - interval '20 minutes')
on conflict (ra_id, source, correlation_id, message, occurred_at) do nothing;

insert into system_health_snapshots (
    location,
    fleet_status,
    console_status,
    vault_status,
    vault_latency_ms,
    database_status,
    queue_backlog,
    recent_http_failures_json,
    affected_locations_json
) values
    ('CHICAGO', 'UNAVAILABLE', 'DEGRADED', 'SLOW', 6200, 'OK', 284, '{"FLEET_503":147,"VAULT_TIMEOUT":21}'::jsonb, '["ORD","MDW"]'::jsonb),
    ('ORD', 'UNAVAILABLE', 'DEGRADED', 'SLOW', 6200, 'OK', 284, '{"FLEET_503":147,"VAULT_TIMEOUT":21}'::jsonb, '["ORD","MDW"]'::jsonb),
    ('GLOBAL', 'OK', 'OK', 'OK', 800, 'OK', 12, '{}'::jsonb, '[]'::jsonb)
on conflict (location) do update set
    fleet_status = excluded.fleet_status,
    console_status = excluded.console_status,
    vault_status = excluded.vault_status,
    vault_latency_ms = excluded.vault_latency_ms,
    database_status = excluded.database_status,
    queue_backlog = excluded.queue_backlog,
    recent_http_failures_json = excluded.recent_http_failures_json,
    affected_locations_json = excluded.affected_locations_json;
