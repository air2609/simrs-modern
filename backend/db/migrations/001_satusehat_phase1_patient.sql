-- =====================================================================
-- SatuSehat Integration - Fase 1 (Patient / IHS)
-- File    : migrations/001_satusehat_phase1_patient.sql
-- =====================================================================
-- Perubahan skema : TIDAK ADA.
--   Fase ini memakai kolom yang sudah tersedia:
--     - tb_medical_record.ihs_number  (menyimpan IHS number)
--     - ms_patient.nik                (identifier NIK)
--   IHS number diisi oleh aplikasi melalui:
--     POST /api/satusehat/patient/sync?mrCode=<no_mr>
--
-- DML di bawah hanya untuk melengkapi DATA UJI (dev): pasien contoh
-- dengan NIK 3604171701820005 (No. MR 14-02-70) diberi kode wilayah
-- yang valid agar alamat FHIR Patient lolos validasi SATUSEHAT.
--
-- Aman dijalankan di server: bila NIK tersebut tidak ada, 0 baris berubah.
-- =====================================================================

BEGIN;

-- Kode wilayah BPS (tanpa titik) berasal dari ms_village.v_village_code
-- yang berformat bertitik, contoh: '14.71.07.1012'.
UPDATE ms_patient p
SET n_province_id   = prov.n_province_id,
    n_regency_id    = reg.n_regency_id,
    n_subdistrict_id = sub.n_subdistrict_id,
    n_village_id    = vil.n_village_id
FROM ms_village vil
JOIN ms_sub_district sub ON sub.n_subdistrict_id = vil.n_subdistrict_id
JOIN ms_regency      reg ON reg.n_regency_id     = sub.n_regency_id
JOIN ms_province     prov ON prov.n_province_id  = reg.n_province_id
WHERE p.nik = '3604171701820005'
  AND vil.v_village_code = '14.71.07.1012';

COMMIT;
