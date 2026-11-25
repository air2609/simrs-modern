-- =====================================================================
-- DEV SEED - IHS number pasien uji SATUSEHAT (JANGAN dijalankan di PRODUCTION)
-- File : seed/dev_satusehat_patient_ihs.sql
-- =====================================================================
-- IHS number bersifat environment-specific: nilai di bawah dihasilkan oleh
-- SATUSEHAT **staging** untuk NIK 3604171701820005. Di server lain, biarkan
-- kolom ihs_number kosong dan isi melalui endpoint:
--     POST /api/satusehat/patient/sync?mrCode=14-02-70
-- =====================================================================

BEGIN;

UPDATE tb_medical_record
SET ihs_number = 'P20396323287'
WHERE v_mr_code = '14-02-70'
  AND (ihs_number IS NULL OR ihs_number = '');

COMMIT;
