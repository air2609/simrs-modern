-- =====================================================================
-- SatuSehat Integration - Fase 2 (Encounter / Kunjungan)
-- File : migrations/002_satusehat_phase2_encounter.sql
-- =====================================================================
-- Perubahan skema:
--   ms_unit          + ss_location_id      -> mapping unit  -> Location ID SatuSehat
--   ms_staff         + nik                 -> NIK tenaga medis
--   ms_doctor        + ss_practitioner_id  -> mapping dokter -> Practitioner ID
--   tb_registration  + ss_encounter_id     -> ID Encounter hasil SatuSehat
--                    + ss_encounter_status -> status Encounter terakhir
--                    + d_ss_encounter_sync -> waktu sinkron terakhir
--   ss_request_log (tabel baru)            -> audit request/response SatuSehat
--
-- Idempotent (aman dijalankan berulang).
-- =====================================================================

BEGIN;

ALTER TABLE ms_unit ADD COLUMN IF NOT EXISTS ss_location_id varchar(64);

ALTER TABLE ms_staff ADD COLUMN IF NOT EXISTS nik varchar(16);

ALTER TABLE ms_doctor ADD COLUMN IF NOT EXISTS ss_practitioner_id varchar(64);

ALTER TABLE tb_registration ADD COLUMN IF NOT EXISTS ss_encounter_id varchar(64);
ALTER TABLE tb_registration ADD COLUMN IF NOT EXISTS ss_encounter_status varchar(20);
ALTER TABLE tb_registration ADD COLUMN IF NOT EXISTS d_ss_encounter_sync timestamp;

CREATE TABLE IF NOT EXISTS ss_request_log (
    n_ss_log_id     bigserial PRIMARY KEY,
    v_resource_type varchar(50),
    v_local_ref     varchar(100),
    v_method        varchar(10),
    v_url           text,
    t_request_body  text,
    n_status_code   integer,
    t_response_body text,
    v_status        varchar(20),
    v_error_message text,
    v_who_create    varchar(50),
    d_whn_create    timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ss_request_log_resource
    ON ss_request_log (v_resource_type, v_local_ref);

COMMIT;
