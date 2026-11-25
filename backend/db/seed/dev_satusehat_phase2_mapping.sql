-- =====================================================================
-- DEV SEED - Mapping master SATUSEHAT Fase 2 (JANGAN dijalankan di PRODUCTION)
-- File : seed/dev_satusehat_phase2_mapping.sql
-- =====================================================================
-- Berisi data uji (dummy) untuk SATUSEHAT staging:
--   - Location ID  : 0d89a3fa-472f-448a-9424-900d1ee910f2  (POLI UMUM TEST)
--   - Practitioner : 10009880728 (dr. Alexander, dummy)
--     NIK          : 7209061211900001
--
-- Di server/production, ganti dengan ID yang benar, atau gunakan endpoint:
--   POST /api/satusehat/mapping/unit?unitId=<id>&locationId=<loc>
--   POST /api/satusehat/mapping/doctor?doctorId=<id>&practitionerId=<pr>
--   POST /api/satusehat/mapping/staff-nik?staffId=<id>&nik=<nik>
-- =====================================================================

BEGIN;

-- Unit POLIKLINIK ANAK -> Location ID (staging)
UPDATE ms_unit
SET ss_location_id = '0d89a3fa-472f-448a-9424-900d1ee910f2'
WHERE v_unit_code = 'KAN'
  AND (ss_location_id IS NULL OR ss_location_id = '');

-- Dokter uji (staff 465 / doctor 109, dipakai pada registrasi J-KAN-20260825-616)
-- -> Practitioner ID dummy dari SATUSEHAT staging.
UPDATE ms_doctor
SET ss_practitioner_id = '10009880728'
WHERE n_staff_id = 465
  AND (ss_practitioner_id IS NULL OR ss_practitioner_id = '');

-- NIK tenaga medis untuk staff tersebut.
UPDATE ms_staff
SET nik = '7209061211900001'
WHERE n_staff_id = 465
  AND (nik IS NULL OR nik = '');

COMMIT;
