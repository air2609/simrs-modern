-- =====================================================================
-- DEV SEED - Data tanda vital (TTV) uji (JANGAN dijalankan di PRODUCTION)
-- File : seed/dev_ttv_sample.sql
-- =====================================================================
-- Menambahkan 1 baris TTV untuk registrasi uji J-KAN-20260825-616
-- (n_reg_id=387098, n_mr_id=140270, n_patient_id=166134, n_staff_id=465).
-- Idempotent.
-- =====================================================================

BEGIN;

INSERT INTO tb_ttv (
    n_ttv_id, n_reg_id, n_mr_id, n_patient_id, n_staff_id, d_ttv_date,
    n_systole, n_diastole, n_pulse, n_respiratory_rate, n_temperature,
    n_spo2, n_weight, n_height, n_pain_scale, v_consciousness, v_note,
    v_who_create, d_whn_create)
SELECT nextval('tb_ttv_n_ttv_id_seq'), 387098, 140270, 166134, 465, now(),
       120, 80, 88, 20, 36.8,
       98, 65.50, 170.00, 0, 'Compos Mentis', 'seed data TTV',
       'seed', now()
WHERE EXISTS (SELECT 1 FROM tb_registration WHERE n_reg_id = 387098)
  AND NOT EXISTS (SELECT 1 FROM tb_ttv WHERE n_reg_id = 387098);

COMMIT;
