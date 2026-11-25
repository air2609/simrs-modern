-- =====================================================================
-- SatuSehat Integration - Fase 3 (Condition & Procedure)
-- File : migrations/003_satusehat_phase3_condition_procedure.sql
-- =====================================================================
-- Perubahan skema:
--   tb_icd_diagnose  + ss_condition_id -> ID Condition (diagnosa ICD-10)
--   tb_icd9_diagnose + ss_procedure_id -> ID Procedure (tindakan ICD-9-CM)
--
-- Idempotent (aman dijalankan berulang).
-- =====================================================================

BEGIN;

ALTER TABLE tb_icd_diagnose ADD COLUMN IF NOT EXISTS ss_condition_id varchar(64);
ALTER TABLE tb_icd9_diagnose ADD COLUMN IF NOT EXISTS ss_procedure_id varchar(64);

COMMIT;
