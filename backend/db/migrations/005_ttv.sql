-- =====================================================================
-- Tanda-Tanda Vital (TTV) + integrasi SatuSehat
-- File : migrations/005_ttv.sql
-- =====================================================================
-- Skema baru:
--   tb_ttv                 -> data tanda vital per kunjungan (registrasi)
--   tb_ttv_ss_observation  -> mapping tiap nilai TTV -> Observation ID SatuSehat
--
-- Nama kolom mengikuti konvensi legacy (n_ = numeric, v_ = varchar, d_ = date).
-- Idempotent.
-- =====================================================================

BEGIN;

CREATE SEQUENCE IF NOT EXISTS tb_ttv_n_ttv_id_seq;

CREATE TABLE IF NOT EXISTS tb_ttv (
    n_ttv_id           bigint PRIMARY KEY DEFAULT nextval('tb_ttv_n_ttv_id_seq'),
    n_reg_id           bigint,
    n_mr_id            bigint,
    n_patient_id       bigint,
    n_staff_id         bigint,
    d_ttv_date         timestamp NOT NULL DEFAULT now(),
    n_systole          integer,        -- tekanan darah sistole (mmHg)
    n_diastole         integer,        -- tekanan darah diastole (mmHg)
    n_pulse            integer,        -- nadi (/menit)
    n_respiratory_rate integer,        -- pernapasan (/menit)
    n_temperature      numeric(4,1),   -- suhu (derajat Celsius)
    n_spo2             integer,        -- saturasi oksigen (%)
    n_weight           numeric(6,2),   -- berat badan (kg)
    n_height           numeric(6,2),   -- tinggi badan (cm)
    n_pain_scale       integer,        -- skala nyeri (0-10)
    v_consciousness    varchar(50),    -- tingkat kesadaran
    v_note             varchar(255),
    v_who_create       varchar(50),
    d_whn_create       timestamp DEFAULT now(),
    v_who_change       varchar(50),
    d_whn_change       timestamp
);

CREATE INDEX IF NOT EXISTS idx_tb_ttv_reg ON tb_ttv (n_reg_id);
CREATE INDEX IF NOT EXISTS idx_tb_ttv_date ON tb_ttv (d_ttv_date);

CREATE TABLE IF NOT EXISTS tb_ttv_ss_observation (
    n_ttv_obs_id       bigserial PRIMARY KEY,
    n_ttv_id           bigint NOT NULL,
    v_loinc_code       varchar(20) NOT NULL,
    ss_observation_id  varchar(64),
    d_whn_create       timestamp DEFAULT now(),
    CONSTRAINT uq_tb_ttv_ss_observation UNIQUE (n_ttv_id, v_loinc_code)
);

CREATE INDEX IF NOT EXISTS idx_tb_ttv_ss_observation_ttv ON tb_ttv_ss_observation (n_ttv_id);

COMMIT;
