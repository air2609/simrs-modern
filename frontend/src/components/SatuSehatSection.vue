<script setup>
import { onMounted, reactive, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['session-expired', 'close']);

const activeTab = ref('sync');
const loading = ref(false);
const error = ref('');
const message = ref('');
const syncing = ref('');

// ===================== SINKRONISASI =====================
const mrCode = ref('');
const registrationCode = ref('');
const results = ref([]);

function pushResult(label, status, detail) {
  results.value.unshift({
    label,
    status,
    detail,
    time: new Date().toLocaleTimeString('id-ID')
  });
  if (results.value.length > 30) {
    results.value.pop();
  }
}

function summarize(data) {
  if (!data) {
    return 'OK';
  }
  const parts = [];
  if (data.action) parts.push(`action=${data.action}`);
  if (data.status) parts.push(`status=${data.status}`);
  if (data.ihsNumber) parts.push(`IHS=${data.ihsNumber}`);
  if (data.encounterId) parts.push(`Encounter=${data.encounterId}`);
  if (data.created != null) parts.push(`created=${data.created}`);
  if (data.skipped != null) parts.push(`skipped=${data.skipped}`);
  if (data.message) parts.push(data.message);
  return parts.join(' · ');
}

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  });
  const payload = await response.json().catch(() => null);

  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Your session has been expired. You need to login again.');
    throw new Error(payload?.message || 'Unauthorized');
  }

  if (!response.ok) {
    throw new Error(payload?.message || `HTTP ${response.status}`);
  }

  return payload.data;
}

async function callSync(label, path) {
  syncing.value = label;
  error.value = '';
  message.value = '';

  try {
    const data = await request(path, { method: 'POST' });
    pushResult(label, 'success', summarize(data));
    message.value = `${label} berhasil.`;
  } catch (requestError) {
    pushResult(label, 'error', requestError.message);
    error.value = requestError.message;
  } finally {
    syncing.value = '';
  }
}

function syncPatient() {
  if (!mrCode.value.trim()) {
    error.value = 'No. MR wajib diisi.';
    return;
  }
  callSync('Sinkron Pasien', `/satusehat/patient/sync?mrCode=${encodeURIComponent(mrCode.value.trim())}`);
}

function syncEncounter() {
  if (!registrationCode.value.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  callSync('Buat Encounter',
    `/satusehat/encounter/sync?registrationCode=${encodeURIComponent(registrationCode.value.trim())}`);
}

function syncCondition() {
  if (!registrationCode.value.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  callSync('Sinkron Diagnosa',
    `/satusehat/condition/sync?registrationCode=${encodeURIComponent(registrationCode.value.trim())}`);
}

function syncProcedure() {
  if (!registrationCode.value.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  callSync('Sinkron Tindakan',
    `/satusehat/procedure/sync?registrationCode=${encodeURIComponent(registrationCode.value.trim())}`);
}

function syncObservation() {
  if (!registrationCode.value.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  callSync('Sinkron Hasil Lab',
    `/satusehat/observation/sync?registrationCode=${encodeURIComponent(registrationCode.value.trim())}`);
}

function syncVitalSigns() {
  if (!registrationCode.value.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  callSync('Sinkron TTV',
    `/satusehat/observation/ttv-sync?registrationCode=${encodeURIComponent(registrationCode.value.trim())}`);
}

function syncMedicationRequest() {
  if (!registrationCode.value.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  callSync('Sinkron Resep Obat',
    `/satusehat/medication-request/sync?registrationCode=${encodeURIComponent(registrationCode.value.trim())}`);
}

function syncMedicationDispense() {
  if (!registrationCode.value.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  callSync('Sinkron Serah Obat',
    `/satusehat/medication-dispense/sync?registrationCode=${encodeURIComponent(registrationCode.value.trim())}`);
}

function finishEncounter() {
  if (!registrationCode.value.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  callSync('Selesaikan Encounter',
    `/satusehat/encounter/finish?registrationCode=${encodeURIComponent(registrationCode.value.trim())}`);
}

// ===================== MAPPING =====================
const units = ref([]);
const doctors = ref([]);
const unitLocation = reactive({});
const doctorPractitioner = reactive({});
const savingId = ref('');
const items = ref([]);
const labs = ref([]);
const outbox = ref([]);
const itemForm = reactive({});
const labForm = reactive({});
const halls = ref([]);
const hallLocation = reactive({});
const itemKeyword = ref('');
const labKeyword = ref('');
const outboxBusy = ref(false);
const outboxRef = reactive({ registrationCode: '' });

async function loadMappings() {
  loading.value = true;
  error.value = '';

  try {
    units.value = await request('/satusehat/mapping/units');
    doctors.value = await request('/satusehat/mapping/doctors');
    await loadHalls();
    await loadItems();
    await loadLabs();
    await loadOutbox();
    units.value.forEach((unit) => {
      unitLocation[unit.localId] = unit.satuSehatId || '';
    });
    doctors.value.forEach((doctor) => {
      doctorPractitioner[doctor.localId] = doctor.satuSehatId || '';
    });
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function saveUnitLocation(unit) {
  const locationId = (unitLocation[unit.localId] || '').trim();
  savingId.value = `unit-${unit.localId}`;
  error.value = '';
  message.value = '';
  try {
    await request(`/satusehat/mapping/unit?unitId=${unit.localId}&locationId=${encodeURIComponent(locationId)}`,
      { method: 'POST' });
    message.value = `Location ID unit ${unit.localCode} tersimpan.`;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    savingId.value = '';
  }
}

async function loadHalls() {
  halls.value = await request('/satusehat/mapping/halls');
  halls.value.forEach((hall) => {
    hallLocation[hall.localId] = hall.satuSehatId || '';
  });
}

async function saveHallLocation(hall) {
  const locationId = (hallLocation[hall.localId] || '').trim();
  savingId.value = `hall-${hall.localId}`;
  error.value = '';
  message.value = '';
  try {
    await request(`/satusehat/mapping/hall?hallId=${hall.localId}&locationId=${encodeURIComponent(locationId)}`,
      { method: 'POST' });
    message.value = `Location ID bangsal ${hall.localCode || hall.localName} tersimpan.`;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    savingId.value = '';
  }
}

async function saveDoctorPractitioner(doctor) {
  const practitionerId = (doctorPractitioner[doctor.localId] || '').trim();
  savingId.value = `doctor-${doctor.localId}`;
  error.value = '';
  message.value = '';
  try {
    await request(`/satusehat/mapping/doctor?doctorId=${doctor.localId}&practitionerId=${encodeURIComponent(practitionerId)}`,
      { method: 'POST' });
    message.value = `Practitioner ID dokter ${doctor.localCode} tersimpan.`;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    savingId.value = '';
  }
}

async function loadItems() {
  const query = itemKeyword.value ? `?keyword=${encodeURIComponent(itemKeyword.value)}` : '';
  items.value = await request(`/satusehat/mapping/items${query}`);
  items.value.forEach((item) => {
    itemForm[item.localId] = {
      kfaCode: item.kfaCode || '',
      kfaDisplay: item.kfaDisplay || '',
      ucumCode: item.ucumCode || ''
    };
  });
}

async function saveItem(item) {
  const form = itemForm[item.localId] || {};
  savingId.value = `item-${item.localId}`;
  error.value = '';
  message.value = '';
  try {
    await request(`/satusehat/mapping/item?itemId=${item.localId}`
      + `&kfaCode=${encodeURIComponent(form.kfaCode || '')}`
      + `&kfaDisplay=${encodeURIComponent(form.kfaDisplay || '')}`
      + `&ucumCode=${encodeURIComponent(form.ucumCode || '')}`, { method: 'POST' });
    item.synced = false;
    message.value = `Mapping KFA item ${item.itemCode} tersimpan.`;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    savingId.value = '';
  }
}

async function loadLabs() {
  const query = labKeyword.value ? `?keyword=${encodeURIComponent(labKeyword.value)}` : '';
  labs.value = await request(`/satusehat/mapping/lab-analytes${query}`);
  labs.value.forEach((lab) => {
    labForm[lab.detailId] = {
      loincCode: lab.loincCode || '',
      loincDisplay: lab.loincDisplay || '',
      ucumCode: lab.ucumCode || ''
    };
  });
}

async function saveLab(row) {
  const form = labForm[row.detailId] || {};
  savingId.value = `lab-${row.detailId}`;
  error.value = '';
  message.value = '';
  try {
    await request(`/satusehat/mapping/lab?detailId=${row.detailId}`
      + `&loincCode=${encodeURIComponent(form.loincCode || '')}`
      + `&loincDisplay=${encodeURIComponent(form.loincDisplay || '')}`
      + `&ucumCode=${encodeURIComponent(form.ucumCode || '')}`, { method: 'POST' });
    message.value = `Mapping LOINC ${row.analyteName || row.treatmentName} tersimpan.`;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    savingId.value = '';
  }
}

async function loadOutbox() {
  outbox.value = await request('/satusehat/outbox');
}

async function processOutbox() {
  outboxBusy.value = true;
  error.value = '';
  message.value = '';
  try {
    const result = await request('/satusehat/outbox/process', { method: 'POST' });
    message.value = `Outbox diproses: processed=${result.processed}, sent=${result.sent}, failed=${result.failed}.`;
    await loadOutbox();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    outboxBusy.value = false;
  }
}

async function enqueueRegistration() {
  if (!outboxRef.registrationCode.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  error.value = '';
  message.value = '';
  try {
    await request(`/satusehat/outbox/enqueue-registration?registrationCode=${encodeURIComponent(outboxRef.registrationCode.trim())}`,
      { method: 'POST' });
    message.value = 'Job sinkronisasi registrasi ditambahkan ke antrian.';
    await loadOutbox();
  } catch (requestError) {
    error.value = requestError.message;
  }
}

// ===================== TANDA VITAL (TTV) =====================
const ttvForm = reactive({
  registrationCode: '',
  ttvDate: '',
  systole: '',
  diastole: '',
  pulse: '',
  respiratoryRate: '',
  temperature: '',
  spo2: '',
  weight: '',
  height: '',
  painScale: '',
  consciousness: '',
  note: ''
});
const ttvList = ref([]);
const ttvSaving = ref(false);

function numberOrNull(value) {
  return value === '' || value === null || value === undefined ? null : Number(value);
}

async function saveTtv() {
  if (!ttvForm.registrationCode.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  ttvSaving.value = true;
  error.value = '';
  message.value = '';
  try {
    await request('/vital-sign', {
      method: 'POST',
      body: JSON.stringify({
        registrationCode: ttvForm.registrationCode.trim(),
        ttvDate: ttvForm.ttvDate || null,
        systole: numberOrNull(ttvForm.systole),
        diastole: numberOrNull(ttvForm.diastole),
        pulse: numberOrNull(ttvForm.pulse),
        respiratoryRate: numberOrNull(ttvForm.respiratoryRate),
        temperature: ttvForm.temperature === '' ? null : Number(ttvForm.temperature),
        spo2: numberOrNull(ttvForm.spo2),
        weight: ttvForm.weight === '' ? null : Number(ttvForm.weight),
        height: ttvForm.height === '' ? null : Number(ttvForm.height),
        painScale: numberOrNull(ttvForm.painScale),
        consciousness: ttvForm.consciousness || null,
        note: ttvForm.note || null
      })
    });
    message.value = 'Data tanda vital tersimpan.';
    await loadTtv();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    ttvSaving.value = false;
  }
}

async function loadTtv() {
  if (!ttvForm.registrationCode.trim()) {
    error.value = 'Kode Registrasi wajib diisi.';
    return;
  }
  error.value = '';
  try {
    ttvList.value = await request(`/vital-sign?registrationCode=${encodeURIComponent(ttvForm.registrationCode.trim())}`);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

onMounted(() => {
  loadMappings();
});
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <div>
        <h2>🔗 SatuSehat</h2>
        <p class="page-subtitle">Sinkronisasi data ke SATUSEHAT Kemenkes &amp; pemetaan ID master.</p>
      </div>
    </div>

    <div v-if="error" class="alert alert--error" role="alert">
      <span class="alert-icon">⚠️</span>
      <span class="alert-text">{{ error }}</span>
      <button class="alert-close" type="button" @click="error = ''">✕</button>
    </div>
    <div v-else-if="message" class="alert alert--success" role="alert">
      <span class="alert-icon">✅</span>
      <span class="alert-text">{{ message }}</span>
      <button class="alert-close" type="button" @click="message = ''">✕</button>
    </div>

    <div class="tabs">
      <button class="tab-button" :class="{ active: activeTab === 'sync' }" type="button"
        @click="activeTab = 'sync'">SINKRONISASI</button>
      <button class="tab-button" :class="{ active: activeTab === 'ttv' }" type="button"
        @click="activeTab = 'ttv'">TANDA VITAL</button>
      <button class="tab-button" :class="{ active: activeTab === 'outbox' }" type="button"
        @click="activeTab = 'outbox'">OUTBOX</button>
      <button class="tab-button" :class="{ active: activeTab === 'mapping' }" type="button"
        @click="activeTab = 'mapping'">MAPPING MASTER</button>
    </div>

    <!-- ======================== SINKRONISASI ======================== -->
    <template v-if="activeTab === 'sync'">
      <div class="card">
        <h3>1. Data Pasien (IHS)</h3>
        <div class="form-row">
          <label>
            No. MR
            <input v-model="mrCode" type="text" placeholder="cth: 14-02-70" @keyup.enter="syncPatient" />
          </label>
          <button class="primary-button" type="button"
            :disabled="syncing === 'Sinkron Pasien'" @click="syncPatient">
            {{ syncing === 'Sinkron Pasien' ? 'Memproses...' : '👤 Sinkron Pasien' }}
          </button>
        </div>
      </div>

      <div class="card">
        <h3>2. Kunjungan (Encounter, Diagnosa, Tindakan)</h3>
        <div class="form-row">
          <label>
            Kode Registrasi
            <input v-model="registrationCode" type="text" placeholder="cth: J-KAN-20260825-616" />
          </label>
        </div>
        <div class="button-row">
          <button class="primary-button" type="button"
            :disabled="syncing === 'Buat Encounter'" @click="syncEncounter">
            {{ syncing === 'Buat Encounter' ? 'Memproses...' : '🏥 Buat Encounter' }}
          </button>
          <button class="primary-button" type="button"
            :disabled="syncing === 'Sinkron Diagnosa'" @click="syncCondition">
            {{ syncing === 'Sinkron Diagnosa' ? 'Memproses...' : '📝 Sinkron Diagnosa' }}
          </button>
          <button class="primary-button" type="button"
            :disabled="syncing === 'Sinkron Tindakan'" @click="syncProcedure">
            {{ syncing === 'Sinkron Tindakan' ? 'Memproses...' : '🔬 Sinkron Tindakan' }}
          </button>
          <button class="primary-button" type="button"
            :disabled="syncing === 'Sinkron Hasil Lab'" @click="syncObservation">
            {{ syncing === 'Sinkron Hasil Lab' ? 'Memproses...' : '🧪 Sinkron Hasil Lab' }}
          </button>
          <button class="primary-button" type="button"
            :disabled="syncing === 'Sinkron TTV'" @click="syncVitalSigns">
            {{ syncing === 'Sinkron TTV' ? 'Memproses...' : '🩺 Sinkron TTV' }}
          </button>
          <button class="primary-button" type="button"
            :disabled="syncing === 'Sinkron Resep Obat'" @click="syncMedicationRequest">
            {{ syncing === 'Sinkron Resep Obat' ? 'Memproses...' : '💊 Sinkron Resep Obat' }}
          </button>
          <button class="primary-button" type="button"
            :disabled="syncing === 'Sinkron Serah Obat'" @click="syncMedicationDispense">
            {{ syncing === 'Sinkron Serah Obat' ? 'Memproses...' : '📦 Sinkron Serah Obat' }}
          </button>
          <button class="accent-button" type="button"
            :disabled="syncing === 'Selesaikan Encounter'" @click="finishEncounter">
            {{ syncing === 'Selesaikan Encounter' ? 'Memproses...' : '🏁 Selesaikan Encounter' }}
          </button>
        </div>
        <p class="hint">Urutan: Buat Encounter → Diagnosa → Tindakan → Hasil Lab → TTV → Resep/Serah Obat → Selesaikan Encounter.</p>
      </div>

      <div class="card">
        <h3>Hasil Terakhir</h3>
        <div v-if="!results.length" class="empty-state">Belum ada proses sinkronisasi.</div>
        <ul v-else class="result-list">
          <li v-for="(item, index) in results" :key="index" :class="item.status">
            <span class="result-badge">{{ item.status === 'success' ? '✅' : '⚠️' }}</span>
            <div>
              <strong>{{ item.label }}</strong>
              <span class="result-time">{{ item.time }}</span>
              <p>{{ item.detail }}</p>
            </div>
          </li>
        </ul>
      </div>
    </template>

    <!-- ======================== TANDA VITAL ======================== -->
    <template v-else-if="activeTab === 'ttv'">
      <div class="card">
        <h3>Input Tanda-Tanda Vital</h3>
        <div class="form-row">
          <label>
            Kode Registrasi
            <input v-model="ttvForm.registrationCode" type="text" placeholder="cth: J-KAN-20260825-616" />
          </label>
          <label>
            Tanggal/Jam
            <input v-model="ttvForm.ttvDate" type="datetime-local" />
          </label>
        </div>
        <div class="ttv-grid">
          <label>Tekanan Darah Sistole (mmHg)
            <input v-model="ttvForm.systole" type="number" min="0" />
          </label>
          <label>Tekanan Darah Diastole (mmHg)
            <input v-model="ttvForm.diastole" type="number" min="0" />
          </label>
          <label>Nadi (/menit)
            <input v-model="ttvForm.pulse" type="number" min="0" />
          </label>
          <label>Pernapasan (/menit)
            <input v-model="ttvForm.respiratoryRate" type="number" min="0" />
          </label>
          <label>Suhu (&deg;C)
            <input v-model="ttvForm.temperature" type="number" step="0.1" />
          </label>
          <label>SpO2 (%)
            <input v-model="ttvForm.spo2" type="number" min="0" max="100" />
          </label>
          <label>Berat Badan (kg)
            <input v-model="ttvForm.weight" type="number" step="0.01" />
          </label>
          <label>Tinggi Badan (cm)
            <input v-model="ttvForm.height" type="number" step="0.01" />
          </label>
          <label>Skala Nyeri (0-10)
            <input v-model="ttvForm.painScale" type="number" min="0" max="10" />
          </label>
          <label>Kesadaran
            <input v-model="ttvForm.consciousness" type="text" placeholder="cth: Compos Mentis" />
          </label>
          <label class="wide">Catatan
            <input v-model="ttvForm.note" type="text" />
          </label>
        </div>
        <div class="button-row">
          <button class="primary-button" type="button" :disabled="ttvSaving" @click="saveTtv">
            {{ ttvSaving ? 'Menyimpan...' : '💾 Simpan TTV' }}
          </button>
          <button class="small-button" type="button" @click="loadTtv">🔄 Muat TTV</button>
        </div>
      </div>

      <div class="card">
        <h3>Riwayat Tanda Vital</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Waktu</th><th>TD</th><th>Nadi</th><th>RR</th><th>Suhu</th>
                <th>SpO2</th><th>BB</th><th>TB</th><th>Nyeri</th><th>Kesadaran</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in ttvList" :key="row.id">
                <td>{{ row.ttvDate }}</td>
                <td>{{ row.systole ?? '-' }}/{{ row.diastole ?? '-' }}</td>
                <td>{{ row.pulse ?? '-' }}</td>
                <td>{{ row.respiratoryRate ?? '-' }}</td>
                <td>{{ row.temperature ?? '-' }}</td>
                <td>{{ row.spo2 ?? '-' }}</td>
                <td>{{ row.weight ?? '-' }}</td>
                <td>{{ row.height ?? '-' }}</td>
                <td>{{ row.painScale ?? '-' }}</td>
                <td>{{ row.consciousness || '-' }}</td>
              </tr>
              <tr v-if="!ttvList.length">
                <td colspan="10" class="empty-state">Belum ada data. Isi kode registrasi lalu klik "Muat TTV".</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ======================== OUTBOX ======================== -->
    <template v-else-if="activeTab === 'outbox'">
      <div class="card">
        <h3>Outbox / Retry</h3>
        <div class="form-row">
          <label>
            Kode Registrasi (tambahkan ke antrian)
            <input v-model="outboxRef.registrationCode" type="text" placeholder="cth: J-KAN-20260825-616" />
          </label>
        </div>
        <div class="button-row">
          <button class="primary-button" type="button" @click="enqueueRegistration">➕ Tambah ke Antrian</button>
          <button class="accent-button" type="button" :disabled="outboxBusy" @click="processOutbox">
            {{ outboxBusy ? 'Memproses...' : '▶ Proses Sekarang' }}
          </button>
          <button class="small-button" type="button" @click="loadOutbox">🔄 Muat</button>
        </div>
        <p class="hint">Job diproses otomatis oleh scheduler; tombol "Proses Sekarang" memaksa pemrosesan.</p>
      </div>

      <div class="card">
        <h3>Daftar Job</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th><th>Jenis</th><th>Ref</th><th>Status</th><th>Percobaan</th><th>Jadwal</th><th>Error</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="job in outbox" :key="job.id">
                <td>{{ job.id }}</td>
                <td><strong>{{ job.jobType }}</strong></td>
                <td>{{ job.localRef }}</td>
                <td>{{ job.status }}</td>
                <td>{{ job.attempts }}</td>
                <td>{{ job.nextAttempt || '-' }}</td>
                <td class="err-cell">{{ job.lastError || '-' }}</td>
              </tr>
              <tr v-if="!outbox.length">
                <td colspan="7" class="empty-state">Belum ada job.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ======================== MAPPING ======================== -->
    <template v-else>
      <div v-if="loading" class="loading">Memuat mapping...</div>
      <template v-else>
        <div class="card">
          <h3>Mapping Unit → Location ID</h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama Unit</th>
                  <th>Location ID SatuSehat</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="unit in units" :key="unit.localId">
                  <td><strong>{{ unit.localCode }}</strong></td>
                  <td>{{ unit.localName }}</td>
                  <td><input v-model="unitLocation[unit.localId]" type="text" placeholder="Location ID" /></td>
                  <td>
                    <button class="small-button" type="button"
                      :disabled="savingId === `unit-${unit.localId}`"
                      @click="saveUnitLocation(unit)">
                      {{ savingId === `unit-${unit.localId}` ? '...' : 'Simpan' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="!units.length">
                  <td colspan="4" class="empty-state">Tidak ada unit.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <h3>Mapping Bangsal (Rawat Inap) → Location ID</h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama Bangsal</th>
                  <th>Location ID SatuSehat</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="hall in halls" :key="hall.localId">
                  <td><strong>{{ hall.localCode }}</strong></td>
                  <td>{{ hall.localName }}</td>
                  <td><input v-model="hallLocation[hall.localId]" type="text" placeholder="Location ID" /></td>
                  <td>
                    <button class="small-button" type="button"
                      :disabled="savingId === `hall-${hall.localId}`"
                      @click="saveHallLocation(hall)">
                      {{ savingId === `hall-${hall.localId}` ? '...' : 'Simpan' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="!halls.length">
                  <td colspan="4" class="empty-state">Tidak ada bangsal.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <h3>Mapping Dokter → Practitioner ID</h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama Dokter</th>
                  <th>Practitioner ID SatuSehat</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="doctor in doctors" :key="doctor.localId">
                  <td><strong>{{ doctor.localCode }}</strong></td>
                  <td>{{ doctor.localName }}</td>
                  <td><input v-model="doctorPractitioner[doctor.localId]" type="text" placeholder="Practitioner ID" /></td>
                  <td>
                    <button class="small-button" type="button"
                      :disabled="savingId === `doctor-${doctor.localId}`"
                      @click="saveDoctorPractitioner(doctor)">
                      {{ savingId === `doctor-${doctor.localId}` ? '...' : 'Simpan' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="!doctors.length">
                  <td colspan="4" class="empty-state">Tidak ada dokter.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <h3>
            <span>Mapping Item Obat → KFA</span>
            <span class="search-actions">
              <input v-model="itemKeyword" type="text" placeholder="Cari kode/nama obat" @keyup.enter="loadItems" />
              <button class="small-button" type="button" @click="loadItems">Cari</button>
            </span>
          </h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Kode</th><th>Nama Obat</th><th>KFA Code</th><th>Nama KFA</th><th>UCUM</th><th>Status</th><th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in items" :key="item.localId">
                  <td>{{ item.itemCode }}</td>
                  <td>{{ item.itemName }}</td>
                  <td><input v-model="itemForm[item.localId].kfaCode" type="text" placeholder="KFA" /></td>
                  <td><input v-model="itemForm[item.localId].kfaDisplay" type="text" placeholder="Nama KFA" /></td>
                  <td><input v-model="itemForm[item.localId].ucumCode" type="text" placeholder="U" /></td>
                  <td>{{ item.synced ? '✅' : '—' }}</td>
                  <td>
                    <button class="small-button" type="button"
                      :disabled="savingId === `item-${item.localId}`"
                      @click="saveItem(item)">
                      {{ savingId === `item-${item.localId}` ? '...' : 'Simpan' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="!items.length">
                  <td colspan="7" class="empty-state">Tidak ada item obat.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <h3>
            <span>Mapping Analyte Lab → LOINC</span>
            <span class="search-actions">
              <input v-model="labKeyword" type="text" placeholder="Cari nama lab/analyte" @keyup.enter="loadLabs" />
              <button class="small-button" type="button" @click="loadLabs">Cari</button>
            </span>
          </h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Lab</th><th>Analyte</th><th>Satuan</th><th>LOINC Code</th><th>Nama LOINC</th><th>UCUM</th><th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in labs" :key="row.detailId">
                  <td>{{ row.treatmentCode }} - {{ row.treatmentName }}</td>
                  <td>{{ row.analyteName }}</td>
                  <td>{{ row.unit }}</td>
                  <td><input v-model="labForm[row.detailId].loincCode" type="text" placeholder="LOINC" /></td>
                  <td><input v-model="labForm[row.detailId].loincDisplay" type="text" placeholder="Nama LOINC" /></td>
                  <td><input v-model="labForm[row.detailId].ucumCode" type="text" placeholder="%" /></td>
                  <td>
                    <button class="small-button" type="button"
                      :disabled="savingId === `lab-${row.detailId}`"
                      @click="saveLab(row)">
                      {{ savingId === `lab-${row.detailId}` ? '...' : 'Simpan' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="!labs.length">
                  <td colspan="7" class="empty-state">Tidak ada analyte lab.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 13px; }

.loading { padding: 24px; text-align: center; color: #9ca3af; }

.alert {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px; border-radius: 12px; font-weight: 600; font-size: 14px;
  margin-bottom: 14px; border: 1px solid transparent;
}
.alert--success { background: #e9f9ef; color: #166534; border-color: #b3e2c5; }
.alert--error { background: #fdeeee; color: #9f1d2f; border-color: #f2c0c6; }
.alert-icon { font-size: 18px; }
.alert-text { flex: 1; }
.alert-close { border: 0; background: transparent; color: inherit; cursor: pointer; font-size: 14px; }

.tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.tab-button {
  padding: 10px 20px; border: 1px solid #d1d9e6; background: #fff; color: #3d4b63;
  font-weight: 700; font-size: 13px; border-radius: 8px; cursor: pointer;
}
.tab-button.active { background: #304b73; color: #fff; border-color: #304b73; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card h3 { margin: 0 0 12px; font-size: 16px; color: #304b73; }

.form-row { display: flex; align-items: flex-end; gap: 12px; flex-wrap: wrap; }
.form-row label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; min-width: 260px; flex: 1; }
input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; background: #fff; width: 100%; box-sizing: border-box; }

.button-row { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px; }

.primary-button, .accent-button, .small-button { border: 0; cursor: pointer; font-weight: 700; border-radius: 8px; }
.primary-button { background: #304b73; color: #fff; padding: 10px 18px; }
.accent-button { background: #1f9d5c; color: #fff; padding: 10px 18px; }
.primary-button:disabled, .accent-button:disabled { opacity: 0.5; cursor: not-allowed; }
.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; }
.small-button:disabled { opacity: 0.5; cursor: not-allowed; }

.hint { margin: 12px 0 0; font-size: 12px; color: #6b7280; }

.table-wrap { overflow: auto; margin-top: 8px; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.empty-state { color: #9ca3af; text-align: center; padding: 20px; }
.search-actions { display: flex; gap: 6px; align-items: center; }
.search-actions input { width: 200px; }
.err-cell { max-width: 320px; word-break: break-word; font-size: 12px; color: #9f1d2f; }

.result-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.result-list li { display: flex; gap: 10px; padding: 10px 12px; border-radius: 8px; background: #f6f8fb; }
.result-list li.error { background: #fdeeee; }
.result-badge { font-size: 16px; }
.result-time { margin-left: 8px; font-size: 11px; color: #9ca3af; }
.result-list p { margin: 4px 0 0; font-size: 12px; color: #3d4b63; word-break: break-word; }

.ttv-grid { display: grid; grid-template-columns: repeat(3, minmax(180px, 1fr)); gap: 12px; margin-top: 12px; }
.ttv-grid label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; }
.ttv-grid label.wide { grid-column: 1 / -1; }
@media (max-width: 900px) { .ttv-grid { grid-template-columns: 1fr; } }
</style>
