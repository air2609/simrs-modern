# Database Migrations & Seed (SIMRS Modern)

Folder ini menyimpan **semua script SQL** untuk setiap perubahan database.
Tujuannya: database development bisa dibangun ulang dengan urutan yang sama di
server lain.

## Aturan

1. **Setiap perubahan DB wajib punya file SQL di sini** (schema DDL maupun DML
   yang bersifat data penting).
2. File harus **idempotent** — aman dijalankan berulang kali.
3. Satu file = satu perubahan/perubahan yang saling terkait.
4. Jangan mengubah file yang sudah pernah dijalankan di server; buat file baru.
5. File `seed/` hanya untuk data uji/dev, **jangan** dijalankan di production.

## Konvensi penamaan

```
migrations/NNN_<modul>_<deskripsi>.sql
seed/<deskripsi>_dev.sql
```

- `NNN` = nomor urut 3 digit (`001`, `002`, ...), dieksekusi berurutan.
- Contoh: `migrations/001_satusehat_phase1_patient.sql`

## Cara menjalankan

Manual (psql):

```bash
PGPASSWORD=<password> psql -h <host> -U <user> -d medisafe \
  -v ON_ERROR_STOP=1 -f db/migrations/001_satusehat_phase1_patient.sql
```

Jalankan file sesuai urutan nomor. Untuk server dari nol, jalankan seluruh isi
`migrations/` berurutan, lalu (opsional, hanya dev) `seed/`.

## Integrasi SATUSEHAT

| Fase | File | Perubahan skema |
|------|------|-----------------|
| 1 - Patient (IHS) | `migrations/001_satusehat_phase1_patient.sql` | Tidak ada (memakai kolom `tb_medical_record.ihs_number` dan `ms_patient.nik` yang sudah ada) |
| 2 - Encounter | `migrations/002_satusehat_phase2_encounter.sql` | `ms_unit.ss_location_id`, `ms_staff.nik`, `ms_doctor.ss_practitioner_id`, `tb_registration.ss_encounter_id/ss_encounter_status/d_ss_encounter_sync`, tabel `ss_request_log` |
| 3 - Condition & Procedure | `migrations/003_satusehat_phase3_condition_procedure.sql` | `tb_icd_diagnose.ss_condition_id`, `tb_icd9_diagnose.ss_procedure_id` |
| 4 - Observation (lab) | `migrations/004_satusehat_phase4_observation.sql` | `ms_lab_treatment_detil.v_loinc_code/v_loinc_display/v_ucum_code`, `ms_lab_test_detail.v_loinc_code/v_loinc_display/v_ucum_code`, `tb_laboratory_result_detail.ss_observation_id` |
| 4b - TTV (tanda vital) | `migrations/005_ttv.sql` | tabel baru `tb_ttv` (data tanda vital) & `tb_ttv_ss_observation` (mapping nilai TTV -> Observation), sequence `tb_ttv_n_ttv_id_seq` |
| 5 - Medication | `migrations/006_satusehat_phase5_medication.sql` | `ms_item.v_kfa_code/v_kfa_display/v_ucum_code/ss_medication_id`, `tb_item_trx.ss_medication_request_id/ss_medication_dispense_id` |
| 6 - Outbox/Retry | `migrations/007_satusehat_outbox.sql` | tabel `ss_outbox` (antrian sinkronisasi + retry) |
| 7 - Rawat Inap | `migrations/008_satusehat_inpatient_location.sql` | `ms_hall.ss_location_id` (Location ID bangsal untuk Encounter `IMP`) |
