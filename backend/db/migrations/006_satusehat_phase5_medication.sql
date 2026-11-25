-- =====================================================================
-- SatuSehat Integration - Fase 5 (Medication / MedicationRequest / Dispense)
-- File : migrations/006_satusehat_phase5_medication.sql
-- =====================================================================
-- Perubahan skema:
--   ms_item         + v_kfa_code, v_kfa_display  -> mapping obat -> kode KFA
--                   + ss_medication_id            -> ID resource Medication
--   tb_item_trx     + ss_medication_request_id    -> ID MedicationRequest
--                   + ss_medication_dispense_id   -> ID MedicationDispense
--
-- Idempotent.
-- =====================================================================

BEGIN;

ALTER TABLE ms_item ADD COLUMN IF NOT EXISTS v_kfa_code varchar(30);
ALTER TABLE ms_item ADD COLUMN IF NOT EXISTS v_kfa_display varchar(255);
ALTER TABLE ms_item ADD COLUMN IF NOT EXISTS v_ucum_code varchar(20);
ALTER TABLE ms_item ADD COLUMN IF NOT EXISTS ss_medication_id varchar(64);

ALTER TABLE tb_item_trx ADD COLUMN IF NOT EXISTS ss_medication_request_id varchar(64);
ALTER TABLE tb_item_trx ADD COLUMN IF NOT EXISTS ss_medication_dispense_id varchar(64);

COMMIT;
