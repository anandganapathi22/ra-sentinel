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
