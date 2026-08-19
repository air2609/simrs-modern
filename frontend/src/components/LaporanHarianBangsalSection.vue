<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');

const units = ref([]);
const unitCode = ref('');
const fromDate = ref(todayIso());
const toDate = ref(todayIso());

const mrNo = ref('');
const namaPasien = ref('');
const bed = ref('');
const regId = ref(null);
const regNo = ref('');
const ruangan = ref('');
const kelas = ref('');

const rows = ref([]);
const totalNilai = ref(0);
const searched = ref(false);

// patient search modal
const patientDialog = ref({ visible: false });
const searchMr = ref('');
const searchNama = ref('');
const searchAlamat = ref('');
const patientResults = ref([]);
const patientLoading = ref(false);
const patientPageSize = 15;
const patientPage = ref(1);
const pagedPatients = computed(() => {
  const start = (patientPage.value - 1) * patientPageSize;
  return patientResults.value.slice(start, start + patientPageSize);
});
const patientTotalPages = computed(() => Math.max(1, Math.ceil(patientResults.value.length / patientPageSize)));
function patientGoToPage(page) {
  patientPage.value = Math.min(Math.max(1, page), patientTotalPages.value);
}

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

const fmtMoney = (v) => Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 });

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

onMounted(async () => {
  try {
    units.value = await request('/report/harian-bangsal/masters');
    if (units.value.length) unitCode.value = units.value[0].code;
  } catch (requestError) {
    error.value = requestError.message;
  }
});

async function openPatientSearch() {
  searchMr.value = '';
  searchNama.value = '';
  searchAlamat.value = '';
  patientResults.value = [];
  patientPage.value = 1;
  patientDialog.value.visible = true;
}

async function searchPatient() {
  if (!searchMr.value && !searchNama.value && !searchAlamat.value) {
    await showAlert('Salah satu field harus diisi');
    return;
  }
  patientLoading.value = true;
  try {
    patientResults.value = await request(`/report/harian-bangsal/patients${qs({
      mrCode: searchMr.value,
      patientName: searchNama.value,
      address: searchAlamat.value
    })}`);
    patientPage.value = 1;
  } catch (requestError) {
    await showAlert(requestError.message);
  } finally {
    patientLoading.value = false;
  }
}

async function choosePatient(patient) {
  patientDialog.value.visible = false;
  try {
    const reg = await request(`/report/harian-bangsal/registration${qs({
      mrCode: patient.mrCode
    })}`);
    mrNo.value = reg.mrNo;
    namaPasien.value = reg.namaPasien;
    bed.value = reg.bed;
    regId.value = reg.regId;
    regNo.value = reg.regNo;
    ruangan.value = reg.ruangan;
    kelas.value = reg.kelas;
    rows.value = [];
    totalNilai.value = 0;
    searched.value = false;
  } catch (requestError) {
    error.value = requestError.message;
  }
}

// LIHAT LAPORAN — hanya menampilkan data di tabel. PDF lewat tombol LIHAT PDF.
async function loadReport() {
  if (!regId.value) {
    await showAlert('Pilih pasien terlebih dahulu');
    return;
  }
  if (!fromDate.value || !toDate.value) {
    await showAlert('TANGGAL HARUS DI ISI!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const data = await request(`/report/harian-bangsal/report${qs({
      regId: regId.value,
      unitCode: unitCode.value,
      from: fromDate.value,
      to: toDate.value
    })}`);
    rows.value = data.rows || [];
    totalNilai.value = data.totalNilai || 0;
    searched.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// LIHAT PDF
function printReport() {
  if (!searched.value) {
    showAlert('Tekan LIHAT LAPORAN terlebih dahulu!');
    return;
  }
  window.open(`${props.apiBaseUrl}/report/harian-bangsal/print${qs({
    regId: regId.value,
    unitCode: unitCode.value,
    from: fromDate.value,
    to: toDate.value
  })}`, '_blank');
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🏥 LAPORAN HARIAN BANGSAL</h2>
      <p class="page-subtitle">Migrasi screen legacy RPT0006 — laporanHarianBangsal.zul</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="filter-bar">
        <span class="filter-label">LOKASI</span>
        <span class="filter-colon">:</span>
        <select v-model="unitCode">
          <option v-for="u in units" :key="u.unitId" :value="u.code">{{ u.code }} - {{ u.name }}</option>
        </select>
        <span class="filter-label">TANGGAL</span>
        <span class="filter-colon">:</span>
        <input v-model="fromDate" type="date" />
        <span class="filter-label">S.D.</span>
        <input v-model="toDate" type="date" />
      </div>

      <div class="filter-bar">
        <span class="filter-label">NO. MR</span>
        <span class="filter-colon">:</span>
        <input class="mr-field" :value="mrNo" placeholder="Cari pasien rawat inap..." readonly @click="openPatientSearch" />
        <button class="small-button" type="button" @click="openPatientSearch">🔍 CARI</button>
        <span class="filter-label">NAMA</span>
        <span class="filter-colon">:</span>
        <input class="ro-field" :value="namaPasien" readonly />
        <span class="filter-label">BED</span>
        <span class="filter-colon">:</span>
        <input class="ro-field" :value="bed" readonly />
        <button class="small-button primary" type="button" :disabled="loading" @click="loadReport">👁️ LIHAT LAPORAN</button>
        <button class="small-button" type="button" :disabled="!searched" @click="printReport">🖨️ LIHAT PDF</button>
      </div>

      <div v-if="searched" class="periode-line">
        <span class="periode-label">NO. REGISTRASI</span>
        <span class="periode-value">{{ regNo }}</span>
        <span class="periode-label">RUANGAN</span>
        <span class="periode-value">{{ ruangan }}</span>
        <span class="periode-label">KELAS</span>
        <span class="periode-value">{{ kelas }}</span>
      </div>

      <div v-if="loading" class="loading">Memuat laporan...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>NO.</th>
              <th>KODE</th>
              <th>KETERANGAN</th>
              <th class="num">JLH</th>
              <th class="num">HARGA</th>
              <th>NO. NOTA</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in rows" :key="index" :class="{ 'total-row': row.keterangan === 'T  O  T  A  L' }">
              <td>{{ row.nomor ?? '' }}</td>
              <td>{{ row.kodeTransaksi ?? '' }}</td>
              <td :class="{ strong: row.keterangan === 'T  O  T  A  L' }">{{ row.keterangan ?? '' }}</td>
              <td class="num">{{ row.jumlah ?? '' }}</td>
              <td class="num strong">{{ fmtMoney(row.nilai) }}</td>
              <td>{{ row.nomorTransaksi ?? '' }}</td>
            </tr>
            <tr v-if="!rows.length && !loading">
              <td colspan="6" class="empty-state">Pilih pasien &amp; periode lalu tekan LIHAT LAPORAN.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="action-bar">
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>

    <!-- ==================== MODAL CARI PASIEN ==================== -->
    <transition name="dialog-fade">
      <div v-if="patientDialog.visible" class="modal-overlay" @click.self="patientDialog.visible = false">
        <div class="dialog-box patient-box">
          <div class="dialog-title">CARI PASIEN <span class="note">*) KHUSUS PASIEN RAWAT INAP</span></div>
          <div class="patient-search">
            <div class="search-row">
              <span class="filter-label">NO. MR</span>
              <input v-model="searchMr" type="text" placeholder="No. MR" />
            </div>
            <div class="search-row">
              <span class="filter-label">NAMA</span>
              <input v-model="searchNama" type="text" placeholder="Nama pasien" />
            </div>
            <div class="search-row">
              <span class="filter-label">ALAMAT</span>
              <input v-model="searchAlamat" type="text" placeholder="Alamat" />
            </div>
            <button class="small-button primary" type="button" :disabled="patientLoading" @click="searchPatient">🔍 CARI</button>
          </div>
          <div class="table-wrap patient-table">
            <table class="table">
              <thead>
                <tr>
                  <th>NO. MR</th>
                  <th>NAMA</th>
                  <th>ALAMAT</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(p, index) in pagedPatients" :key="index" class="clickable" @click="choosePatient(p)">
                  <td class="strong">{{ p.mrCode }}</td>
                  <td>{{ p.patientName }}</td>
                  <td>{{ p.address }}</td>
                </tr>
                <tr v-if="!pagedPatients.length">
                  <td colspan="3" class="empty-state">Tidak ada pasien. Tekan CARI.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination" v-if="patientResults.length > patientPageSize">
            <button class="page-btn" type="button" :disabled="patientPage <= 1" @click="patientGoToPage(1)">⏮</button>
            <button class="page-btn" type="button" :disabled="patientPage <= 1" @click="patientGoToPage(patientPage - 1)">◀</button>
            <span class="page-info">Halaman {{ patientPage }} / {{ patientTotalPages }}</span>
            <button class="page-btn" type="button" :disabled="patientPage >= patientTotalPages" @click="patientGoToPage(patientPage + 1)">▶</button>
            <button class="page-btn" type="button" :disabled="patientPage >= patientTotalPages" @click="patientGoToPage(patientTotalPages)">⏭</button>
          </div>
          <div class="dialog-buttons">
            <button class="small-button" type="button" @click="patientDialog.visible = false">TUTUP</button>
          </div>
        </div>
      </div>
    </transition>

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

.filter-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.filter-label { font-weight: 700; color: #304b73; font-size: 13px; }
.filter-colon { color: #6b7280; font-weight: 700; }
.filter-bar select, .filter-bar input[type="date"], .filter-bar input[type="text"] { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }
.mr-field { width: 180px; background: #f3f5f8; cursor: pointer; }
.ro-field { width: 200px; background: #f3f5f8; }

.periode-line { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 10px; padding: 8px 12px; background: #f6f8fb; border-radius: 8px; }
.periode-label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.periode-value { font-weight: 800; color: #304b73; font-size: 13px; }

.table-wrap { overflow: auto; margin: 10px 0; max-height: 460px; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; position: sticky; top: 0; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.clickable { cursor: pointer; }
.table tbody tr.total-row { background: #eef3fa; }
.table tbody tr.total-row td { border-top: 2px solid #dce6f2; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 10px; margin: 12px 0 4px; flex-wrap: wrap; }
.page-btn { padding: 6px 12px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.page-btn:hover:not(:disabled) { background: #eef3fa; }
.page-btn:disabled { opacity: 0.4; cursor: default; }
.page-info { font-weight: 700; color: #304b73; font-size: 13px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.dialog-box { background: #fff; border-radius: 16px; width: 380px; max-width: 92vw; padding: 26px 28px; text-align: center; box-shadow: 0 24px 50px rgba(15,23,42,0.3); border-top: 5px solid #5f83c2; }
.dialog-box--warning { border-top-color: #e6a23c; }
.dialog-box--error { border-top-color: #d64567; }
.dialog-box--success { border-top-color: #1f9d5c; }
.dialog-box--confirm { border-top-color: #5f83c2; }
.dialog-icon { font-size: 44px; margin-bottom: 8px; }
.dialog-title { font-size: 17px; font-weight: 800; color: #304b73; margin-bottom: 8px; }
.dialog-message { font-size: 14px; color: #4b5563; line-height: 1.5; margin-bottom: 18px; white-space: pre-line; }
.dialog-buttons { display: flex; justify-content: center; gap: 12px; margin-top: 12px; }
.dialog-buttons .small-button { min-width: 110px; }
.patient-box { width: 720px; max-width: 95vw; }
.patient-search { display: flex; flex-direction: column; align-items: center; gap: 8px; margin-bottom: 10px; }
.search-row { display: flex; align-items: center; gap: 8px; width: 100%; max-width: 460px; }
.search-row .filter-label { width: 70px; text-align: right; }
.search-row input { flex: 1; padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }
.patient-search .small-button { min-width: 120px; }
.patient-table { max-height: 320px; }
.note { font-size: 11px; color: #d64567; font-weight: 700; }
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: all 0.25s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.92); }

.toast { position: fixed; bottom: 24px; right: 24px; z-index: 100; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-radius: 12px; font-weight: 700; font-size: 14px; color: #fff; box-shadow: 0 10px 30px rgba(15,23,42,0.25); max-width: 420px; }
.toast--success { background: linear-gradient(135deg, #177245, #1f9d5c); }
.toast--error { background: linear-gradient(135deg, #a32943, #d64567); }
.toast--info { background: linear-gradient(135deg, #304b73, #5f83c2); }
.toast-icon { font-size: 18px; }
.toast-message { flex: 1; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateY(16px); }
</style>
