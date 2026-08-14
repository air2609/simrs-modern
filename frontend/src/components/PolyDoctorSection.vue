<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const saving = ref(false);
const error = ref('');
const message = ref('');

const rows = ref([]);
const search = ref('');
const masters = ref({ polyStatusOptions: [], bookingOptions: [] });

// Form state
const showForm = ref(false);
const editingId = ref(null);
const form = ref(emptyForm());

// Doctor search
const doctorSearchCode = ref('');
const doctorSearchName = ref('');
const doctorResults = ref([]);
const doctorSearching = ref(false);

// Schedule state
const showSchedule = ref(false);
const scheduleDoctorId = ref(null);
const scheduleDoctorName = ref('');
const scheduleMonth = ref(currentMonth());
const scheduleDates = ref([]);
const scheduleLoading = ref(false);

const pageSize = 10;
const currentPage = ref(1);

const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)));
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return rows.value.slice(start, start + pageSize);
});

function emptyForm() {
  return {
    id: null,
    doctorId: null,
    doctorCode: '',
    doctorName: '',
    doctorDescription: '',
    polyStatus: 'Aktif',
    bookingFlag: 'Y',
    maxPatient: null,
    scheduleFrom: '',
    scheduleTo: '',
    unit: '',
    photo: '',
    monSchedule: '',
    tueSchedule: '',
    wedSchedule: '',
    thuSchedule: '',
    friSchedule: '',
    satSchedule: '',
    sunSchedule: ''
  };
}

function currentMonth() {
  const now = new Date();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  return `${mm}-${now.getFullYear()}`;
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

async function loadRows() {
  const params = new URLSearchParams();
  if (search.value.trim()) {
    params.set('search', search.value.trim());
  }
  const qs = params.toString();
  rows.value = await request(`/antrian/poli-dokter${qs ? `?${qs}` : ''}`);
  currentPage.value = 1;
}

async function loadMasters() {
  masters.value = await request('/antrian/poli-dokter/masters');
}

async function searchDoctors() {
  doctorSearching.value = true;
  error.value = '';
  try {
    const params = new URLSearchParams();
    if (doctorSearchCode.value.trim()) {
      params.set('code', doctorSearchCode.value.trim());
    }
    if (doctorSearchName.value.trim()) {
      params.set('name', doctorSearchName.value.trim());
    }
    const qs = params.toString();
    doctorResults.value = await request(`/antrian/poli-dokter/doctors${qs ? `?${qs}` : ''}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    doctorSearching.value = false;
  }
}

function selectDoctor(doctor) {
  form.value.doctorId = doctor.staffId;
  form.value.doctorCode = doctor.staffCode;
  form.value.doctorName = doctor.staffName;
  form.value.unit = doctor.unit;
  doctorResults.value = [];
}

function openNew() {
  editingId.value = null;
  form.value = emptyForm();
  showForm.value = true;
  error.value = '';
}

function openEdit(row) {
  editingId.value = row.id;
  form.value = {
    id: row.id,
    doctorId: row.doctorId,
    doctorCode: row.doctorCode,
    doctorName: row.doctorName,
    doctorDescription: row.doctorDescription || '',
    polyStatus: row.polyStatus || 'Aktif',
    bookingFlag: row.bookingFlag || 'Y',
    maxPatient: row.maxPatient,
    scheduleFrom: row.scheduleFrom || '',
    scheduleTo: row.scheduleTo || '',
    unit: row.unit || '',
    photo: row.photo || '',
    monSchedule: row.monSchedule || '',
    tueSchedule: row.tueSchedule || '',
    wedSchedule: row.wedSchedule || '',
    thuSchedule: row.thuSchedule || '',
    friSchedule: row.friSchedule || '',
    satSchedule: row.satSchedule || '',
    sunSchedule: row.sunSchedule || ''
  };
  showForm.value = true;
  error.value = '';
}

function closeForm() {
  showForm.value = false;
  editingId.value = null;
  doctorResults.value = [];
}

async function saveForm() {
  saving.value = true;
  error.value = '';
  message.value = '';
  try {
    await request('/antrian/poli-dokter/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    await loadRows();
    closeForm();
    message.value = 'Data dokter poli berhasil disimpan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function deleteRow(row) {
  if (!window.confirm(`Hapus dokter poli "${row.doctorName}"?`)) {
    return;
  }
  error.value = '';
  message.value = '';
  try {
    await request(`/antrian/poli-dokter/delete?id=${row.id}`, { method: 'DELETE' });
    await loadRows();
    message.value = 'Data dokter poli berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  }
}

// Schedule management
function openSchedule(row) {
  scheduleDoctorId.value = row.doctorId;
  scheduleDoctorName.value = row.doctorName;
  scheduleMonth.value = currentMonth();
  showSchedule.value = true;
  loadSchedules();
}

function closeSchedule() {
  showSchedule.value = false;
  scheduleDoctorId.value = null;
  scheduleDoctorName.value = '';
  scheduleDates.value = [];
}

async function loadSchedules() {
  scheduleLoading.value = true;
  error.value = '';
  try {
    const params = new URLSearchParams();
    params.set('doctorId', scheduleDoctorId.value);
    params.set('month', scheduleMonth.value);
    const list = await request(`/antrian/poli-dokter/schedules?${params.toString()}`);
    scheduleDates.value = list.map((item) => item.schedule);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    scheduleLoading.value = false;
  }
}

function changeScheduleMonth() {
  loadSchedules();
}

function addScheduleDate() {
  const input = document.getElementById('schedule-date-input');
  if (!input || !input.value) {
    return;
  }
  const value = input.value;
  if (!scheduleDates.value.includes(value)) {
    scheduleDates.value.push(value);
    scheduleDates.value.sort();
  }
  input.value = '';
}

function removeScheduleDate(date) {
  scheduleDates.value = scheduleDates.value.filter((d) => d !== date);
}

async function saveSchedules() {
  saving.value = true;
  error.value = '';
  message.value = '';
  try {
    await request('/antrian/poli-dokter/schedules/save', {
      method: 'POST',
      body: JSON.stringify({
        doctorId: scheduleDoctorId.value,
        dates: scheduleDates.value
      })
    });
    await loadSchedules();
    message.value = 'Jadwal praktek berhasil disimpan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function deleteScheduleDate(date) {
  if (!window.confirm(`Hapus jadwal tanggal ${date}?`)) {
    return;
  }
  error.value = '';
  message.value = '';
  try {
    const params = new URLSearchParams();
    params.set('doctorId', scheduleDoctorId.value);
    params.set('date', date);
    await request(`/antrian/poli-dokter/schedules/delete?${params.toString()}`, { method: 'DELETE' });
    await loadSchedules();
    message.value = 'Jadwal berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) {
    return;
  }
  currentPage.value = page;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([loadRows(), loadMasters()]);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  initialize();
});
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>🩺 Poli Dokter</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0059 — mengelola dokter poli, status, dan jadwal praktek</p>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-else-if="message" class="status-banner status-banner--success">{{ message }}</p>

    <div v-if="loading" class="loading">Memuat data dokter poli...</div>

    <template v-else>
      <!-- List card -->
      <div class="card">
        <h3>
          <span>Daftar Dokter Poli</span>
          <span class="search-actions">
            <input v-model="search" class="search-input" type="text" placeholder="Cari nama dokter..." @keyup.enter="loadRows" />
            <button class="small-button" type="button" @click="loadRows">🔍 Cari</button>
            <button class="small-button" type="button" @click="openNew">➕ Tambah</button>
          </span>
        </h3>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>NO</th>
                <th>KODE</th>
                <th>NAMA DOKTER</th>
                <th>STATUS</th>
                <th>MAX PASIEN</th>
                <th>UNIT</th>
                <th>BOOKING</th>
                <th>JADWAL</th>
                <th>AKSI</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in pagedRows" :key="row.id">
                <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td>{{ row.doctorCode }}</td>
                <td><strong>{{ row.doctorName }}</strong></td>
                <td>
                  <span class="status-badge" :class="row.polyStatus === 'Aktif' ? 'badge--active' : 'badge--inactive'">
                    {{ row.polyStatus }}
                  </span>
                </td>
                <td>{{ row.maxPatient ?? '-' }}</td>
                <td>{{ row.unit || '-' }}</td>
                <td>{{ row.bookingFlag }}</td>
                <td>
                  <button class="small-button" type="button" @click="openSchedule(row)">📅 Jadwal</button>
                </td>
                <td>
                  <div class="row-actions">
                    <button class="small-button" type="button" @click="openEdit(row)">✏️ Edit</button>
                    <button class="small-button danger" type="button" @click="deleteRow(row)">🗑️</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!pagedRows.length">
                <td colspan="9" class="empty-state">Tidak ada data dokter poli.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar">
          <span class="pagination-info">Menampilkan {{ pagedRows.length }} dari {{ rows.length }} data</span>
          <div class="pagination-controls">
            <button class="small-button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹ Prev</button>
            <span class="pagination-page">Halaman {{ currentPage }} / {{ totalPages }}</span>
            <button class="small-button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">Next ›</button>
          </div>
        </div>
      </div>

      <!-- Form card -->
      <div v-if="showForm" class="card">
        <h3>{{ editingId ? '✏️ Edit Dokter Poli' : '➕ Tambah Dokter Poli' }}</h3>

        <div class="form-grid">
          <div class="form-field">
            <label>Pilih Dokter</label>
            <div class="doctor-search">
              <input v-model="doctorSearchCode" class="search-input" type="text" placeholder="Kode dokter" />
              <input v-model="doctorSearchName" class="search-input" type="text" placeholder="Nama dokter" />
              <button class="small-button" type="button" :disabled="doctorSearching" @click="searchDoctors">
                {{ doctorSearching ? '...' : '🔍 Cari' }}
              </button>
            </div>
            <div v-if="doctorResults.length" class="doctor-results">
              <div v-for="doc in doctorResults" :key="doc.staffId" class="doctor-result" @click="selectDoctor(doc)">
                <strong>{{ doc.staffName }}</strong>
                <span>{{ doc.staffCode }} — {{ doc.unit || '-' }}</span>
              </div>
            </div>
            <div v-if="form.doctorId" class="selected-doctor">
              ✅ {{ form.doctorName }} ({{ form.doctorCode }}) — {{ form.unit || '-' }}
            </div>
          </div>

          <div class="form-field">
            <label>Status Poli</label>
            <select v-model="form.polyStatus">
              <option v-for="opt in masters.polyStatusOptions" :key="opt" :value="opt">{{ opt }}</option>
            </select>
          </div>

          <div class="form-field">
            <label>Booking</label>
            <select v-model="form.bookingFlag">
              <option v-for="opt in masters.bookingOptions" :key="opt" :value="opt">{{ opt }}</option>
            </select>
          </div>

          <div class="form-field">
            <label>Max Pasien</label>
            <input v-model.number="form.maxPatient" class="search-input" type="number" min="0" />
          </div>

          <div class="form-field">
            <label>Jam Praktek Dari</label>
            <input v-model="form.scheduleFrom" class="search-input" type="time" />
          </div>

          <div class="form-field">
            <label>Jam Praktek Sampai</label>
            <input v-model="form.scheduleTo" class="search-input" type="time" />
          </div>

          <div class="form-field">
            <label>Unit</label>
            <input v-model="form.unit" class="search-input" type="text" />
          </div>

          <div class="form-field">
            <label>Foto (URL)</label>
            <input v-model="form.photo" class="search-input" type="text" />
          </div>

          <div class="form-field form-field--full">
            <label>Deskripsi Dokter</label>
            <textarea v-model="form.doctorDescription" class="search-input" rows="2"></textarea>
          </div>

          <div class="form-field">
            <label>Senin</label>
            <input v-model="form.monSchedule" class="search-input" type="text" placeholder="cth: 08:00-12:00" />
          </div>
          <div class="form-field">
            <label>Selasa</label>
            <input v-model="form.tueSchedule" class="search-input" type="text" placeholder="cth: 08:00-12:00" />
          </div>
          <div class="form-field">
            <label>Rabu</label>
            <input v-model="form.wedSchedule" class="search-input" type="text" placeholder="cth: 08:00-12:00" />
          </div>
          <div class="form-field">
            <label>Kamis</label>
            <input v-model="form.thuSchedule" class="search-input" type="text" placeholder="cth: 08:00-12:00" />
          </div>
          <div class="form-field">
            <label>Jumat</label>
            <input v-model="form.friSchedule" class="search-input" type="text" placeholder="cth: 08:00-12:00" />
          </div>
          <div class="form-field">
            <label>Sabtu</label>
            <input v-model="form.satSchedule" class="search-input" type="text" placeholder="cth: 08:00-12:00" />
          </div>
          <div class="form-field">
            <label>Minggu</label>
            <input v-model="form.sunSchedule" class="search-input" type="text" placeholder="cth: 08:00-12:00" />
          </div>
        </div>

        <div class="save-actions">
          <button class="primary-button" :disabled="saving" @click="saveForm">💾 Simpan</button>
          <button class="small-button" type="button" @click="closeForm">Batal</button>
        </div>
      </div>

      <!-- Schedule card -->
      <div v-if="showSchedule" class="card">
        <h3>
          <span>📅 Jadwal Praktek — {{ scheduleDoctorName }}</span>
          <span class="search-actions">
            <input v-model="scheduleMonth" class="search-input month-input" type="month" @change="changeScheduleMonth" />
            <button class="small-button" type="button" @click="closeSchedule">Tutup</button>
          </span>
        </h3>

        <div v-if="scheduleLoading" class="loading">Memuat jadwal...</div>
        <template v-else>
          <div class="schedule-add">
            <input id="schedule-date-input" class="search-input" type="date" />
            <button class="small-button" type="button" @click="addScheduleDate">➕ Tambah Tanggal</button>
          </div>

          <div class="schedule-list">
            <div v-for="date in scheduleDates" :key="date" class="schedule-item">
              <span>{{ date }}</span>
              <button class="small-button danger" type="button" @click="deleteScheduleDate(date)">🗑️</button>
            </div>
            <p v-if="!scheduleDates.length" class="empty-state">Belum ada jadwal untuk bulan ini.</p>
          </div>

          <div class="save-actions">
            <button class="primary-button" :disabled="saving" @click="saveSchedules">💾 Simpan Jadwal</button>
          </div>
        </template>
      </div>
    </template>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.loading { padding: 24px; text-align: center; color: #9ca3af; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--success { background: #e6f5ea; color: #1d6b3a; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card h3 { margin: 0 0 12px; font-size: 16px; color: #304b73; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }

.search-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.search-input { padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; font: inherit; }
.month-input { width: 150px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr:hover { background: #f6f8fb; }

.status-badge { padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.badge--active { background: #e6f5ea; color: #1d6b3a; }
.badge--inactive { background: #fde8ea; color: #a32943; }

.row-actions { display: flex; gap: 6px; }
.danger { color: #a32943; }

.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

.pagination-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; margin-top: 12px; }
.pagination-info { font-size: 13px; color: #6b7280; }
.pagination-controls { display: flex; align-items: center; gap: 8px; }
.pagination-page { font-size: 13px; color: #3d4b63; font-weight: 600; }

.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
.small-button:disabled { opacity: 0.5; cursor: not-allowed; }

.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-field--full { grid-column: 1 / -1; }
.form-field label { font-size: 12px; font-weight: 700; color: #3d4b63; }
.form-field select, .form-field textarea { padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; font: inherit; }

.doctor-search { display: flex; gap: 6px; flex-wrap: wrap; }
.doctor-results { border: 1px solid #e2e8f0; border-radius: 8px; max-height: 180px; overflow: auto; }
.doctor-result { padding: 8px 10px; cursor: pointer; display: flex; flex-direction: column; border-bottom: 1px solid #eef2f7; }
.doctor-result:hover { background: #f6f8fb; }
.doctor-result span { font-size: 12px; color: #6b7280; }
.selected-doctor { padding: 8px 10px; background: #e6f5ea; color: #1d6b3a; border-radius: 8px; font-size: 13px; font-weight: 600; }

.save-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-top: 16px; }
.primary-button { border: 0; cursor: pointer; padding: 8px 20px; font-weight: 700; border-radius: 8px; background: #304b73; color: #fff; }
.primary-button:disabled { opacity: 0.5; cursor: not-allowed; }

.schedule-add { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; }
.schedule-list { display: flex; flex-direction: column; gap: 8px; }
.schedule-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 8px; }

@media (max-width: 960px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
