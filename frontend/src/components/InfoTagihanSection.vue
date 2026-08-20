<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');

const patient = ref(null);
const form = ref({ mrCode: '', regNo: '', patientName: '', address: '', tClass: '', bed: '', patientTypeName: '' });

const startDate = ref(todayIso());
const endDate = ref(todayIso());

const rows = ref([]);
const total = ref(0);
const deposit = ref(0);
const retur = ref(0);
const sisa = ref(0);

const showPatientModal = ref(false);
const patientSearch = ref({ mrCode: '', name: '', address: '' });
const patientResults = ref([]);

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

// dialog/toast
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

const fmtMoney = (v) => Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });

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

// ================= PASIEN =================

async function searchPatient() {
  const s = patientSearch.value;
  if (!s.mrCode && !s.name && !s.address) {
    await showAlert('Salah satu field pencarian pasien harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    patientResults.value = await request(`/cashier/info-tagihan/patients/ranap${qs(s)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function selectPatient(result) {
  showPatientModal.value = false;
  loading.value = true;
  error.value = '';
  try {
    patient.value = await request(`/cashier/info-tagihan/patients/${encodeURIComponent(result.mrCode)}`);
    form.value.mrCode = patient.value.mrCode;
    form.value.regNo = patient.value.registrationNumber || '';
    form.value.patientName = patient.value.patientName || '';
    form.value.address = patient.value.address || '';
    form.value.tClass = patient.value.bed || '';
    form.value.bed = patient.value.bed || '';
    form.value.patientTypeName = patient.value.patientTypeName || '';
    await loadTransactions();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// ================= TRANSAKSI =================

async function loadTransactions() {
  if (!patient.value) {
    await showAlert('PILIH PASIEN TERLEBIH DAHULU!');
    return;
  }
  if (!startDate.value || !endDate.value) {
    await showAlert('TANGGAL HARUS DI ISI!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const data = await request(`/cashier/info-tagihan/transactions${qs({
      patientId: patient.value.patientId,
      registrationId: patient.value.registrationId,
      from: startDate.value,
      to: endDate.value
    })}`);
    rows.value = data.rows;
    total.value = data.total;
    deposit.value = data.deposit;
    retur.value = data.retur;
    sisa.value = data.sisa;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function statusClass(status) {
  return status === 'LUNAS' ? 'badge badge--lunas' : 'badge badge--belum';
}

onMounted(() => {
  // tidak memuat otomatis; pilih pasien dahulu
});
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🧾 FORM INFORMASI TAGIHAN PASIEN</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="section-title">DATA PASIEN</div>
      <div class="patient-grid">
        <div class="field">
          <label>NO. MR</label>
          <div class="input-row">
            <input v-model="form.mrCode" readonly placeholder="-" />
            <button class="mini primary" type="button" @click="showPatientModal = true">CARI PASIEN</button>
          </div>
        </div>
        <div class="field">
          <label>NO. REGISTRASI</label>
          <input v-model="form.regNo" readonly />
        </div>
        <div class="field">
          <label>NAMA</label>
          <input v-model="form.patientName" readonly />
        </div>
        <div class="field field--wide">
          <label>ALAMAT</label>
          <input v-model="form.address" readonly />
        </div>
        <div class="field">
          <label>KELAS TARIF</label>
          <input v-model="form.tClass" readonly />
        </div>
        <div class="field">
          <label>BED</label>
          <input v-model="form.bed" readonly />
        </div>
        <div class="field">
          <label>TIPE PASIEN</label>
          <input v-model="form.patientTypeName" readonly />
        </div>
      </div>

      <div class="section-title">DAFTAR TRANSAKSI PASIEN</div>
      <div class="filter-row">
        <span class="filter-label">TANGGAL :</span>
        <input v-model="startDate" type="date" />
        <span class="filter-label">S.D.</span>
        <input v-model="endDate" type="date" />
        <button class="small-button primary" type="button" :disabled="loading" @click="loadTransactions">🔍 CARI</button>
      </div>

      <div v-if="loading" class="loading">Memuat transaksi...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>TANGGAL</th>
              <th>KETERANGAN</th>
              <th>NO. NOTA</th>
              <th>STAFF</th>
              <th>STATUS</th>
              <th>KWITANSI</th>
              <th class="num">JUMLAH</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in rows" :key="index">
              <td>{{ row.tanggal }}</td>
              <td>{{ row.keterangan }}</td>
              <td class="strong">{{ row.noteNo }}</td>
              <td>{{ row.staff }}</td>
              <td><span :class="statusClass(row.status)">{{ row.status }}</span></td>
              <td>{{ row.kwitansi }}</td>
              <td class="num">{{ fmtMoney(row.jumlah) }}</td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="7" class="empty-state">Pilih pasien + rentang tanggal lalu tekan CARI.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="totals-line">
        <div class="field">
          <label>DEPOSIT</label>
          <input :value="fmtMoney(deposit)" readonly />
        </div>
        <div class="field">
          <label>RETUR</label>
          <input :value="fmtMoney(retur)" readonly />
        </div>
        <div class="field">
          <label>TOTAL</label>
          <input :value="fmtMoney(total)" readonly />
        </div>
        <div class="field">
          <label>SISA TAGIHAN</label>
          <input :value="fmtMoney(sisa)" readonly class="highlight" />
        </div>
      </div>

      <div class="action-bar">
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>

    <!-- ==================== MODAL: CARI PASIEN ==================== -->
    <div v-if="showPatientModal" class="modal-overlay" @click.self="showPatientModal = false">
      <div class="modal">
        <div class="modal-header">CARI DATA PASIEN RAWAT INAP</div>
        <div class="modal-body">
          <div class="field"><label>NO. MR</label><input v-model="patientSearch.mrCode" /></div>
          <div class="field"><label>NAMA</label><input v-model="patientSearch.name" /></div>
          <div class="field"><label>ALAMAT</label><input v-model="patientSearch.address" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchPatient">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>NO. MR</th><th>NAMA</th><th>TIPE PASIEN</th><th>ALAMAT</th></tr></thead>
              <tbody>
                <tr v-for="r in patientResults" :key="r.mrId" @click="selectPatient(r)">
                  <td class="strong">{{ r.mrCode }}</td>
                  <td>{{ r.patientName }}</td>
                  <td>{{ r.patientType }}</td>
                  <td>{{ r.address }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showPatientModal = false">TUTUP</button>
        </div>
      </div>
    </div>

    <!-- ==================== DIALOG / TOAST ==================== -->
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
.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }

.patient-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px 18px; }
.field--wide { grid-column: 1 / -1; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input, .field select { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly] { background: #f3f5f8; color: #4b5563; }
.highlight { font-weight: 800; color: #a32943; }
.input-row { display: flex; gap: 6px; align-items: center; }
.input-row input { flex: 1; }
.mini { padding: 6px 10px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 12px; white-space: nowrap; }
.mini.primary { background: #304b73; color: #fff; border-color: #304b73; }

.filter-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 8px; }
.filter-label { font-weight: 700; color: #304b73; font-size: 13px; }
.filter-row input[type="date"] { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.badge { padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.badge--lunas { background: #e7f6ec; color: #177245; }
.badge--belum { background: #fef3d6; color: #b7791f; }

.totals-line { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 10px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal { background: #fff; border-radius: 12px; width: 680px; max-width: 94vw; max-height: 88vh; display: flex; flex-direction: column; box-shadow: 0 20px 40px rgba(0,0,0,0.2); }
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
  .patient-grid, .totals-line { grid-template-columns: 1fr; }
}
</style>
