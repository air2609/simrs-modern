-- =====================================================================
-- DEV SEED - Resep/obat uji Fase 5 (JANGAN dijalankan di PRODUCTION)
-- File : seed/dev_satusehat_phase5_prescription.sql
-- =====================================================================
-- Mengisi mapping KFA untuk item uji dan 1 baris transaksi obat untuk
-- registrasi uji J-KAN-20260825-616 (n_reg_id=387098).
--
-- Kode KFA yang dipakai: 92000354 (Vitamin A 100.000 IU Kapsul Lunak, POV).
-- Catatan: pemetaan item -> KFA di sini HANYA untuk uji. Di server, setiap
--          item obat harus dipetakan ke kode KFA produknya yang sebenarnya.
-- Idempotent.
-- =====================================================================

BEGIN;

-- Mapping KFA item uji (PARA 2). Unconditional agar placeholder lama ikut diperbarui.
UPDATE ms_item
SET v_kfa_code    = '92000354',
    v_kfa_display = 'Vitamin A 100.000 IU Kapsul Lunak',
    v_ucum_code   = 'U'
WHERE v_item_code = 'PARA 2';

-- Baris transaksi obat (resep) untuk registrasi uji.
INSERT INTO tb_item_trx (n_note_id, n_item_id, n_qty, aturan_pakai, d_whn_create, v_who_create)
SELECT e.n_exam_id, i.n_item_id, 10, '3 x 1 kapsul sesudah makan', now(), 'seed'
FROM tb_examination e
JOIN ms_item i ON i.v_item_code = 'PARA 2'
WHERE e.n_reg_id = 387098
  AND NOT EXISTS (
      SELECT 1 FROM tb_item_trx t WHERE t.n_note_id = e.n_exam_id AND t.n_item_id = i.n_item_id
  )
LIMIT 1;

COMMIT;
