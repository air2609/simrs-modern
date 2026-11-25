-- =====================================================================
-- SatuSehat Integration - Fase 4 (Observation / hasil lab & tanda vital)
-- File : migrations/004_satusehat_phase4_observation.sql
-- =====================================================================
-- Perubahan skema:
--   ms_lab_treatment_detil + v_loinc_code, v_loinc_display, v_ucum_code
--       -> mapping analyte lab -> kode LOINC + satuan UCUM
--   ms_lab_test_detail     + v_loinc_code, v_loinc_display, v_ucum_code
--       -> mapping lab test (single analyte) -> LOINC
--   tb_laboratory_result_detail + ss_observation_id
--       -> ID Observation hasil SatuSehat
--
-- Idempotent.
-- =====================================================================

BEGIN;

ALTER TABLE ms_lab_treatment_detil ADD COLUMN IF NOT EXISTS v_loinc_code varchar(20);
ALTER TABLE ms_lab_treatment_detil ADD COLUMN IF NOT EXISTS v_loinc_display varchar(255);
ALTER TABLE ms_lab_treatment_detil ADD COLUMN IF NOT EXISTS v_ucum_code varchar(30);

ALTER TABLE ms_lab_test_detail ADD COLUMN IF NOT EXISTS v_loinc_code varchar(20);
ALTER TABLE ms_lab_test_detail ADD COLUMN IF NOT EXISTS v_loinc_display varchar(255);
ALTER TABLE ms_lab_test_detail ADD COLUMN IF NOT EXISTS v_ucum_code varchar(30);

ALTER TABLE tb_laboratory_result_detail ADD COLUMN IF NOT EXISTS ss_observation_id varchar(64);

COMMIT;
