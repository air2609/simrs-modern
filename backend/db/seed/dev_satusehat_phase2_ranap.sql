-- =====================================================================
-- DEV SEED - Registrasi Rawat Inap uji (Encounter class IMP)
-- File : seed/dev_satusehat_phase2_ranap.sql
-- =====================================================================
-- Membuat 1 registrasi rawat inap (tanpa unit poliklinik) untuk MR 140270,
-- + room reservation + bed occupancy di bangsal (hall) 84, dan memetakan
-- bangsal 84 ke Location ID SatuSehat staging.
--
-- ⚠️  JANGAN dijalankan di PRODUCTION.
-- Idempotent.
-- =====================================================================

BEGIN;

-- Bangsal 84 -> Location ID (staging)
UPDATE ms_hall
SET ss_location_id = '0d89a3fa-472f-448a-9424-900d1ee910f2'
WHERE n_hall_id = 84
  AND (ss_location_id IS NULL OR ss_location_id = '');

-- Registrasi rawat inap (n_unit_id dibiarkan NULL).
INSERT INTO tb_registration (
    n_reg_id, n_mr_id, n_staff_id, d_registration_date, v_reg_secondary_id,
    reg_status, v_main_doctor_status, v_who_create, d_whn_create)
SELECT nextval('tb_registration_n_reg_id_seq'), 140270, 465, now(), 'I-TEST-001',
       1, 'Y', 'seed', now()
WHERE NOT EXISTS (SELECT 1 FROM tb_registration WHERE v_reg_secondary_id = 'I-TEST-001');

-- Room reservation.
INSERT INTO tb_room_reservation (n_room_rsv_id, n_reg_id, n_tclass_id, v_who_create, d_whn_create)
SELECT nextval('tb_room_reservation_n_room_rsv_id_seq'), r.n_reg_id, NULL, 'seed', now()
FROM tb_registration r
WHERE r.v_reg_secondary_id = 'I-TEST-001'
  AND NOT EXISTS (SELECT 1 FROM tb_room_reservation rr WHERE rr.n_reg_id = r.n_reg_id);

-- Bed occupancy di bangsal 84 (untuk resolusi Location rawat inap).
INSERT INTO tb_bed_occupancy (n_reg_primary_id, n_bed_primary_id, d_check_in_time, v_who_create, d_whn_create)
SELECT r.n_reg_id, b.n_bed_id, now(), 'seed', now()
FROM tb_registration r
JOIN (
    SELECT bd.n_bed_id
    FROM ms_bed bd
    JOIN ms_room rm ON rm.n_room_id = bd.n_room_id
    WHERE rm.n_hall_id = 84
    LIMIT 1
) b ON TRUE
WHERE r.v_reg_secondary_id = 'I-TEST-001'
  AND NOT EXISTS (SELECT 1 FROM tb_bed_occupancy o WHERE o.n_reg_primary_id = r.n_reg_id);

COMMIT;
