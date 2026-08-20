<script setup>
import { computed, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const results = ref([]);
const searched = ref(false);

const filters = ref({ mrCode: '', name: '', address: '', hall: '', doctor: '' });

// ---- modal ruangan ----
const showHallModal = ref(false);
const hallSearch = ref({ code: '', name: '' });
const hallResults = ref([]);

// ---- dialog ----
const toast = ref({ visible: false, message: '', type: 'success' });
const dialog = ref({ visible: false, mode: 'alert', type: 'warning', title: '', message: '', resolve: null });
let toastTimer = null;

function showToast(message, type = 'success') {
  toast.value = { visible: true, message, type };
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.value.visible = false; }, 3500);
}

function showAlert(message, options = {}) {
  return new Promise((resolve) => {
    dialog.value = { visible: true, mode: 'alert', type: options.type || 'warning',
      title: options.title || 'PERHATIAN', message, resolve };
  });
}

function closeDialog(result) {
  const resolve = dialog.value.resolve;
  dialog.value.visible = false;
  if (resolve) resolve(result);
}

const dialogIcon = computed(() => ({
  warning: '⚠️', info: 'ℹ️', error: '❌', success: '✅', confirm: '❓'
}[dialog.value.type] || 'ℹ️'));

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Your session has been expired. You need to login again.');
    throw new Error(payload?.message || 'Unauthorized');
  }
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  return payload.data;
}

function qs(params) {
  const parts = [];
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      parts.push(`${key}=${encodeURIComponent(value)}`);
    }
  });
  return parts.length ? `?${parts.join('&')}` : '';
}

// ================= RUANGAN =================

async function searchHall() {
  loading.value = true;
  error.value = '';
  try {
    hallResults.value = await request(`/admission/cari-pasien/halls${qs(hallSearch.value)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function selectHall(hall) {
  filters.value.hall = hall.name;
  showHallModal.value = false;
}

// ================= CARI PASIEN =================

async function searchPatients() {
  loading.value = true;
  error.value = '';
  try {
    results.value = await request(`/admission/cari-pasien/patients${qs(filters.value)}`);
    searched.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function clearForm() {
  filters.value = { mrCode: '', name: '', address: '', hall: '', doctor: '' };
  hallSearch.value = { code: '', name: '' };
  hallResults.value = [];
  results.value = [];
  searched.value = false;
}

function durationLabel(days) {
  if (days === null || days === undefined) return '-';
  return `${days} hari`;
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🏥 PENCARIAN PASIEN RAWAT INAP</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="filter-grid">
        <div class="field">
          <label>NO. MR</label>
          <input v-model="filters.mrCode" />
        </div>
        <div class="field">
          <label>NAMA</label>
          <input v-model="filters.name" />
        </div>
        <div class="field">
          <label>ALAMAT</label>
          <input v-model="filters.address" />
        </div>
        <div class="field">
          <label>RUANGAN</label>
          <div class="input-row">
            <input v-model="filters.hall" readonly placeholder="-" />
            <button class="mini primary" type="button" @click="showHallModal = true">CARI</button>
          </div>
        </div>
        <div class="field">
          <label>DOKTER</label>
          <input v-model="filters.doctor" />
        </div>
        <div class="field search-btn-field">
          <button class="small-button primary" type="button" :disabled="loading" @click="searchPatients">🔍 CARI</button>
        </div>
      </div>

      <div class="section-title">DAFTAR PASIEN RAWAT INAP</div>
      <div v-if="loading" class="loading">Mencari pasien...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>NO. MR</th>
              <th>NAMA</th>
              <th>TIPE PASIEN</th>
              <th>ALAMAT</th>
              <th>RUANGAN</th>
              <th>BED</th>
              <th>DOKTER</th>
              <th class="num">DURASI</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in results" :key="index">
              <td class="strong">{{ row.mrCode }}</td>
              <td>{{ row.patientName }}</td>
              <td><span class="badge" :class="row.patientType === 'BPJS' ? 'badge--bpjs' : 'badge--non'">{{ row.patientType }}</span></td>
              <td>{{ row.address }}</td>
              <td>{{ row.hall }}</td>
              <td>{{ row.bed }}</td>
              <td>{{ row.doctor }}</td>
              <td class="num">{{ durationLabel(row.duration) }}</td>
            </tr>
            <tr v-if="!results.length">
              <td colspan="8" class="empty-state">
                {{ searched ? 'Tidak ada pasien rawat inap yang cocok.' : 'Isi kriteria pencarian lalu tekan CARI.' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="action-bar">
        <button class="small-button" type="button" @click="clearForm">🆕 BARU</button>
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>

    <!-- ==================== MODAL: CARI RUANGAN ==================== -->
    <div v-if="showHallModal" class="modal-overlay" @click.self="showHallModal = false">
      <div class="modal">
        <div class="modal-header">CARI DATA RUANGAN</div>
        <div class="modal-body">
          <div class="field"><label>KODE</label><input v-model="hallSearch.code" /></div>
          <div class="field"><label>NAMA</label><input v-model="hallSearch.name" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchHall">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>KODE</th><th>NAMA</th></tr></thead>
              <tbody>
                <tr v-for="h in hallResults" :key="h.hallId" @click="selectHall(h)">
                  <td class="strong">{{ h.code }}</td>
                  <td>{{ h.name }}</td>
                </tr>
                <tr v-if="!hallResults.length">
                  <td colspan="2" class="empty-state">Ketik kode/nama lalu tekan CARI.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showHallModal = false">TUTUP</button>
        </div>
      </div>
    </div>

    <!-- ==================== DIALOG ==================== -->
    <transition name="dialog-fade">
      <div v-if="dialog.visible" class="modal-overlay" @click.self="closeDialog(false)">
        <div class="dialog-box" :class="'dialog-box--' + dialog.type">
          <div class="dialog-icon">{{ dialogIcon }}</div>
          <div class="dialog-title">{{ dialog.title }}</div>
          <div class="dialog-message">{{ dialog.message }}</div>
          <div class="dialog-buttons">
            <button class="small-button primary" type="button" @click="closeDialog(true)">✔ OK</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ==================== TOAST ==================== -->
    <transition name="toast-fade">
      <div v-if="toast.visible" class="toast" :class="'toast--' + toast.type">
        <span class="toast-icon">{{ toast.type === 'success' ? '✅' : toast.type === 'error' ? '❌' : 'ℹ️' }}</span>
        <span class="toast-message">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }

.filter-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px 18px; margin-bottom: 14px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly] { background: #f3f5f8; color: #4b5563; }
.search-btn-field { justify-content: flex-end; }
.search-btn-field .small-button { align-self: flex-end; }
.input-row { display: flex; gap: 6px; align-items: center; }
.input-row input { flex: 1; }
.mini { padding: 6px 10px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 12px; white-space: nowrap; }
.mini.primary { background: #304b73; color: #fff; border-color: #304b73; }

.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.badge { padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.badge--bpjs { background: #e7f6ec; color: #177245; }
.badge--non { background: #f3f4f6; color: #6b7280; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal { background: #fff; border-radius: 12px; width: 560px; max-width: 94vw; max-height: 88vh; display: flex; flex-direction: column; box-shadow: 0 20px 40px rgba(0,0,0,0.2); }
.modal-header { padding: 14px 18px; background: #304b73; color: #fff; font-weight: 800; border-radius: 12px 12px 0 0; }
.modal-body { padding: 14px 18px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
.modal-list { max-height: 300px; }
.modal-footer { padding: 12px 18px; border-top: 1px solid #eef2f7; display: flex; justify-content: flex-end; gap: 10px; border-radius: 0 0 12px 12px; }

.toast { position: fixed; bottom: 24px; right: 24px; z-index: 100; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-radius: 12px; font-weight: 700; font-size: 14px; color: #fff; box-shadow: 0 10px 30px rgba(15,23,42,0.25); max-width: 420px; }
.toast--success { background: linear-gradient(135deg, #177245, #1f9d5c); }
.toast--error { background: linear-gradient(135deg, #a32943, #d64567); }
.toast--info { background: linear-gradient(135deg, #304b73, #5f83c2); }
.toast-icon { font-size: 18px; }
.toast-message { flex: 1; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateY(16px); }

.dialog-fade-enter-active, .dialog-fade-leave-active { transition: all 0.25s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.92); }
.dialog-box { background: #fff; border-radius: 16px; width: 380px; max-width: 92vw; padding: 26px 28px; text-align: center; box-shadow: 0 24px 50px rgba(15,23,42,0.3); border-top: 5px solid #5f83c2; }
.dialog-box--warning { border-top-color: #e6a23c; }
.dialog-box--error { border-top-color: #d64567; }
.dialog-box--success { border-top-color: #1f9d5c; }
.dialog-box--confirm { border-top-color: #5f83c2; }
.dialog-icon { font-size: 44px; margin-bottom: 8px; }
.dialog-title { font-size: 17px; font-weight: 800; color: #304b73; margin-bottom: 8px; }
.dialog-message { font-size: 14px; color: #4b5563; line-height: 1.5; margin-bottom: 18px; white-space: pre-line; }
.dialog-buttons { display: flex; justify-content: center; gap: 12px; }
.dialog-buttons .small-button { min-width: 110px; }

@media (max-width: 960px) {
  .filter-grid { grid-template-columns: 1fr; }
}
</style>
