# Alur Integrasi SATUSEHAT — SIMRS Modern

Ringkasan alur sinkronisasi data ke SATUSEHAT (Kemenkes) untuk **Rawat Jalan** dan **Rawat Inap**.
Semua endpoint butuh sesi login (kecuali scheduler internal).

## Prasyarat (Master Data)
| Kebutuhan | Disimpan di | Diisi via |
|---|---|---|
| Organization ID RS | `satusehat.organization-id` (env) | konfigurasi |
| Unit → Location ID | `ms_unit.ss_location_id` | tab Mapping Master / API |
| Bangsal (Rawat Inap) → Location ID | `ms_hall.ss_location_id` | tab Mapping Master / API |
| Dokter → Practitioner ID | `ms_doctor.ss_practitioner_id` | tab Mapping Master / API |
| NIK tenaga medis | `ms_staff.nik` | API `staff-nik` |
| Obat → Kode KFA (+UCUM) | `ms_item.v_kfa_code`, `v_kfa_display`, `v_ucum_code` | tab Mapping Master / API |
| Analyte lab → LOINC (+UCUM) | `ms_lab_treatment_detil.v_loinc_code` | tab Mapping Master / API |
| ICD-10 / ICD-9-CM | `ms_icd`, `ms_icd_9cm` | master lokal |

## Alur Umum (berlaku Rawat Jalan & Rawat Inap)
```
Pasien → Encounter → { Condition, Procedure, Observation(lab), TTV, Medication } → Encounter finish
```
Urutan penting: **Encounter dulu**, lalu Condition/Procedure/Observation/TTV/Medication, terakhir **finish** (butuh minimal 1 Condition).

## A. Rawat Jalan (Outpatient)
| Langkah | Aksi | Endpoint / Pemicu | Tersimpan |
|---|---|---|---|
| 1 | Registrasi pasien | simpan pasien/registrasi | `tb_registration` |
| 2 | Sinkron **Patient** (IHS dari NIK) | `POST /api/satusehat/patient/sync?mrCode=` | `tb_medical_record.ihs_number` |
| 3 | Buat **Encounter** (class `AMB`) | `POST /api/satusehat/encounter/sync?registrationCode=` | `tb_registration.ss_encounter_id` |
| 4 | **Condition** (diagnosa ICD-10) | `POST /api/satusehat/condition/sync?registrationCode=` | `tb_icd_diagnose.ss_condition_id` |
| 5 | **Procedure** (tindakan ICD-9-CM) | `POST /api/satusehat/procedure/sync?registrationCode=` | `tb_icd9_diagnose.ss_procedure_id` |
| 6 | **Observation** hasil lab (LOINC) | `POST /api/satusehat/observation/sync?registrationCode=` | `tb_laboratory_result_detail.ss_observation_id` |
| 7 | **Observation** tanda vital (TTV) | `POST /api/satusehat/observation/ttv-sync?registrationCode=` | `tb_ttv_ss_observation` |
| 8 | **MedicationRequest** (resep/KFA) | `POST /api/satusehat/medication-request/sync?registrationCode=` | `tb_item_trx.ss_medication_request_id` |
| 9 | **MedicationDispense** (serah obat) | `POST /api/satusehat/medication-dispense/sync?registrationCode=` | `tb_item_trx.ss_medication_dispense_id` |
| 10 | **Finish Encounter** (`finished`) | `POST /api/satusehat/encounter/finish?registrationCode=` | `ss_encounter_status` |

## B. Rawat Inap (Inpatient)
Sama seperti Rawat Jalan, dengan perbedaan:
1. **Encounter.class = `IMP`** (inpatient encounter) — bukan `AMB`.
2. Registrasi rawat inap (masuk bangsal/bed) → Encounter dibuat saat mulai perawatan.
3. **Diagnosa (Condition)**, **TTV**, **tindakan (Procedure)**, **lab (Observation)**, dan **obat** dapat **disinkron berulang** selama masa perawatan (tiap ada data baru).
4. **Finish Encounter** dilakukan saat **pasien pulang** (discharge); `period.end` diisi otomatis.

### Penentuan Encounter.class (otomatis)
| Kondisi registrasi | class |
|---|---|
| Punya `n_unit_id` (poliklinik) | `AMB` |
| Unit = IGD/UGD | `EMER` |
| Tanpa `n_unit_id` **atau** ada `tb_room_reservation` (rawat inap) | `IMP` |

### Location Encounter
- Rawat Jalan / IGD → Location dari **unit** (`ms_unit.ss_location_id`).
- Rawat Inap → Location dari **bangsal** pasien: `tb_bed_occupancy → ms_bed → ms_room → ms_hall.ss_location_id`.
- Bila belum ada, fallback ke config `satusehat.inpatient-location-id` (env `SATUSEHAT_INPATIENT_LOCATION_ID`).

## Pemicu Otomatis (Outbox)
Event simpan data otomatis menambahkan job ke antrian `ss_outbox`, lalu diproses **otomatis oleh scheduler** (interval 60 dtk) dengan **retry backoff**:

| Event | Job |
|---|---|
| Simpan pasien | `PATIENT` |
| Simpan registrasi | `ENCOUNTER` |
| Simpan diagnosa | `CONDITION` |
| Simpan nota lab | `OBSERVATION` |
| Simpan TTV | `TTV` |
| Simpan nota obat (apotik) | `MEDICATION_REQUEST`, `MEDICATION_DISPENSE` |

Endpoint outbox: `GET /api/satusehat/outbox`, `POST /api/satusehat/outbox/process`,
`POST /api/satusehat/outbox/enqueue`, `POST /api/satusehat/outbox/enqueue-registration`.
Konfigurasi: `satusehat.outbox.enabled/interval-ms/batch-size/max-attempts`.

## Idempotensi
Tiap resource menyimpan ID hasil SatuSehat. Jika dijalankan ulang:
- Patient/Encounter/Condition/Procedure/Medication/MedicationRequest/Dispense → memakai ID yang ada (skip/`EXISTING`).
- Observation → dicek via `ss_observation_id` / `tb_ttv_ss_observation`.
- Sinkron aman diulang; job outbox digabung per (jenis, ref).

## Aturan SatuSehat yang sudah diterapkan
- **Patient**: NIK wajib 16 digit; `address` wajib + extension `administrativeCode` (kode BPS tanpa titik, mis. `14`, `1471`, `147107`, `1471071012`); `multipleBirth` wajib.
- **Encounter**: `statusHistory.period` wajib start+end; `finish` wajib ada minimal 1 `diagnosis`.
- **Condition**: system ICD-10 `http://hl7.org/fhir/sid/icd-10`.
- **Procedure**: system ICD-9-CM `http://hl7.org/fhir/sid/icd-9-cm`.
- **Observation**: kategori `laboratory`/`vital-signs`, kode LOINC, satuan UCUM.
- **Medication**: `Medication` dibuat dari kode KFA (`http://sys-ids.kemkes.go.id/kfa`), `identifier.system = .../medication/<orgId>`, extension `MedicationType`.
- **MedicationRequest**: `medicationReference` wajib; `authoredOn` wajib; quantity UCUM (mis. `U`).
- **MedicationDispense**: `identifier` wajib (system sama seperti resep `.../prescription/<orgId>`), `authorizingPrescription` wajib.

## Referensi skema (migrasi)
`db/migrations/001`–`007` (+ `db/seed/`) — lihat `db/README.md`.
