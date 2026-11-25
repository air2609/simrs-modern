-- =====================================================================
-- DEV SEED - Hasil lab uji Fase 4 (JANGAN dijalankan di PRODUCTION)
-- File : seed/dev_satusehat_phase4_lab_result.sql
-- =====================================================================
-- Menambahkan mapping LOINC + 1 hasil lab (HEMATOKRIT) untuk registrasi uji
-- J-KAN-20260825-616 (n_reg_id=387098).
-- Idempotent.
-- =====================================================================

BEGIN;

-- Mapping LOINC analyte HEMATOKRIT (ms_lab_treatment_detil n_lab_detil_id=40).
UPDATE ms_lab_treatment_detil
SET v_loinc_code    = '4544-3',
    v_loinc_display = 'Hematocrit [Volume Fraction] of Blood',
    v_ucum_code     = '%'
WHERE n_lab_detil_id = 40
  AND (v_loinc_code IS NULL OR v_loinc_code = '');

-- Header hasil lab untuk registrasi uji (pakai tb_examination milik reg tsb).
INSERT INTO tb_laboratory_result (n_lab_rslt_id, n_exam_id, v_lab_rslt_code, v_who_create, d_whn_create)
SELECT nextval('tb_laboratory_result_n_lab_rslt_id_seq'), e.n_exam_id, 'LAB-SEED-001', 'seed', now()
FROM tb_examination e
WHERE e.n_reg_id = 387098
  AND NOT EXISTS (
      SELECT 1 FROM tb_laboratory_result r WHERE r.n_exam_id = e.n_exam_id AND r.v_lab_rslt_code = 'LAB-SEED-001'
  )
LIMIT 1;

-- Detail hasil HEMATOKRIT = 42 vol%.
INSERT INTO tb_laboratory_result_detail (
    n_lab_rslt_det_id, n_lab_rslt_id, n_treatment_id,
    v_nrml_range_man, v_nrml_range_woman, v_lab_rslt_quantify, v_lab_rslt_desc,
    n_lab_detil_id, v_who_create, d_whn_create)
SELECT nextval('tb_laboratory_result_detail_n_lab_rslt_det_id_seq'),
       r.n_lab_rslt_id, 748, '40-48', '37-43', '42', 'NORMAL', 40, 'seed', now()
FROM tb_laboratory_result r
WHERE r.v_lab_rslt_code = 'LAB-SEED-001'
  AND NOT EXISTS (
      SELECT 1 FROM tb_laboratory_result_detail d WHERE d.n_lab_rslt_id = r.n_lab_rslt_id
  )
LIMIT 1;

COMMIT;
