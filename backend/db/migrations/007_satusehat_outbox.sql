-- =====================================================================
-- SatuSehat Outbox / Retry queue + scheduler
-- File : migrations/007_satusehat_outbox.sql
-- =====================================================================
-- Tabel antrian sinkronisasi SATUSEHAT agar pengiriman tidak hilang saat
-- SATUSEHAT sedang tidak tersedia (retry otomatis dengan backoff).
--
-- Idempotent.
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS ss_outbox (
    n_outbox_id    bigserial PRIMARY KEY,
    v_job_type     varchar(40)  NOT NULL,
    v_local_ref    varchar(100) NOT NULL,
    v_status       varchar(20)  NOT NULL DEFAULT 'PENDING',
    n_attempts     integer      NOT NULL DEFAULT 0,
    v_last_error   text,
    d_next_attempt timestamp    NOT NULL DEFAULT now(),
    v_who_create   varchar(50),
    d_whn_create   timestamp    NOT NULL DEFAULT now(),
    d_whn_update   timestamp,
    CONSTRAINT uq_ss_outbox UNIQUE (v_job_type, v_local_ref)
);

CREATE INDEX IF NOT EXISTS idx_ss_outbox_pending ON ss_outbox (v_status, d_next_attempt);

COMMIT;
