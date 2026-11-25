-- =====================================================================
-- DEV SEED - Diagnosa & tindakan uji Fase 3 (JANGAN dijalankan di PRODUCTION)
-- File : seed/dev_satusehat_phase3_diagnosis.sql
-- =====================================================================
-- Menambahkan 1 diagnosa (ICD-10 J06.9) dan 1 tindakan (ICD-9-CM 89.52)
-- untuk registrasi uji J-KAN-20260825-616 (n_reg_id=387098, n_mr_id=140270).
-- Sekaligus mengisi master ms_icd_9cm yang masih kosong.
--
-- Idempotent.
-- =====================================================================

BEGIN;

-- Master ICD-9-CM untuk uji.
INSERT INTO ms_icd_9cm (n_icd9cm_id, v_icd9cm_code, v_icd9cm_name, v_who_create, d_whn_create)
SELECT nextval('ms_icd_9cm_n_icd9cm_id_seq'), '89.52', 'ELEKTROKARDIOGRAFI', 'seed', now()
WHERE NOT EXISTS (SELECT 1 FROM ms_icd_9cm WHERE v_icd9cm_code = '89.52');

-- Header diagnosa untuk registrasi uji.
INSERT INTO tb_diagnose (n_diagnose_id, n_mr_id, n_reg_id, v_who_create, d_whn_create)
SELECT nextval('tb_diagnose_n_diagnose_id_seq'), 140270, 387098, 'seed', now()
WHERE EXISTS (SELECT 1 FROM tb_registration WHERE n_reg_id = 387098)
  AND NOT EXISTS (SELECT 1 FROM tb_diagnose WHERE n_reg_id = 387098);

-- ICD-10 J06.9.
INSERT INTO tb_icd_diagnose (n_icd_diagnose_id, n_diagnose_id, n_icd_id, n_type, v_who_create, d_whn_create)
SELECT nextval('tb_icd_diagnose_n_icd_diagnose_id_seq'), d.n_diagnose_id, m.n_icd_id, 0, 'seed', now()
FROM tb_diagnose d
JOIN ms_icd m ON m.v_icd_code = 'J06.9'
WHERE d.n_reg_id = 387098
  AND NOT EXISTS (SELECT 1 FROM tb_icd_diagnose i WHERE i.n_diagnose_id = d.n_diagnose_id);

-- ICD-9-CM 89.52.
INSERT INTO tb_icd9_diagnose (n_icd9_diagnose_id, n_diagnose_id, n_icd9cm_id, v_who_create, d_whn_create)
SELECT nextval('tb_icd9_diagnose_n_icd9_diagnose_id_seq'), d.n_diagnose_id, c.n_icd9cm_id, 'seed', now()
FROM tb_diagnose d
JOIN ms_icd_9cm c ON c.v_icd9cm_code = '89.52'
WHERE d.n_reg_id = 387098
  AND NOT EXISTS (SELECT 1 FROM tb_icd9_diagnose i WHERE i.n_diagnose_id = d.n_diagnose_id);

COMMIT;
