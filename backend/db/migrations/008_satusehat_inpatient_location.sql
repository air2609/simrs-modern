-- =====================================================================
-- SatuSehat - dukungan Rawat Inap (Encounter class IMP)
-- File : migrations/008_satusehat_inpatient_location.sql
-- =====================================================================
-- Perubahan skema:
--   ms_hall + ss_location_id -> mapping bangsal (hall) -> Location ID SatuSehat
--     (dipakai untuk Encounter rawat inap yang tidak punya unit poliklinik)
--
-- Idempotent.
-- =====================================================================

BEGIN;

ALTER TABLE ms_hall ADD COLUMN IF NOT EXISTS ss_location_id varchar(64);

COMMIT;
