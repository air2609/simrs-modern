<script setup>
import { computed, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');

const tipe = ref('PD'); // PD / OBAT / ALL
const selectedDoctor = ref(null); // { staffId, code, name }
const doctorLabel = ref('');
const fromDate = ref(todayIso());
const toDate = ref(todayIso());
const patientType = ref('BPJS');

const rows = ref([]);       // PD & OBAT
const allRows = ref([]);    // ALL
const total = ref(0);
const searched = ref(false);

// paging (pageSize 20, sesuai legacy mold=paging pageSize=20)
const pageSize = 20;
const currentPage = ref(1);
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return rows.value.slice(start, start + pageSize);
});
const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)));
function goToPage(page) {
  currentPage.value = Math.min(Math.max(1, page), totalPages.value);
}

// doctor search modal
const doctorDialog = ref({ visible: false });
const doctorCode = ref('');
const doctorName = ref('');
const doctorResults = ref([]);
const doctorLoading = ref(false);
const doctorPageSize = 15;
const doctorPage = ref(1);
const pagedDoctors = computed(() => {
  const start = (doctorPage.value - 1) * doctorPageSize;
  return doctorResults.value.slice(start, start + doctorPageSize);
});
const doctorTotalPages = computed(() => Math.max(1, Math.ceil(doctorResults.value.length / doctorPageSize)));
function doctorGoToPage(page) {
  doctorPage.value = Math.min(Math.max(1, page), doctorTotalPages.value);
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

async function searchDoctor() {
  doctorLoading.value = true;
  try {
    doctorResults.value = await request(`/report/pendapatan-dokter/doctors${qs({
      code: doctorCode.value,
      name: doctorName.value
    })}`);
    doctorPage.value = 1;
    doctorDialog.value.visible = true;
  } catch (requestError) {
    await showAlert(requestError.message);
  } finally {
    doctorLoading.value = false;
  }
}

function chooseDoctor(doc) {
  selectedDoctor.value = doc;
  doctorLabel.value = `${doc.code} - ${doc.name}`;
  doctorDialog.value.visible = false;
}

async function loadReport() {
  if (!fromDate.value || !toDate.value) {
    await showAlert('Kedua tanggal harus diisi....!');
    return;
  }
  if (tipe.value !== 'ALL' && !selectedDoctor.value) {
    await showAlert('Pilih data dokter terlebih dahulu');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const data = await request(`/report/pendapatan-dokter/report${qs({
      tipe: tipe.value,
      staffId: selectedDoctor.value?.staffId,
      from: fromDate.value,
      to: toDate.value,
      patientType: patientType.value
    })}`);
    rows.value = data.rows || [];
    allRows.value = data.allRows || [];
    total.value = data.total || 0;
    currentPage.value = 1;
    searched.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function onTipeChange() {
  rows.value = [];
  allRows.value = [];
  total.value = 0;
  searched.value = false;
  currentPage.value = 1;
}

// EXPORT TO XLS (SpreadsheetML) — judul mengikuti export legacy saveToXLS
function exportXls() {
  if (!searched.value) {
    showAlert('Tekan LIHAT LAPORAN terlebih dahulu!');
    return;
  }
  const esc = (v) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const cell = (value, type = 'String') => `<Cell><Data ss:Type="${type}">${esc(value)}</Data></Cell>`;
  const title = fmtDisplayDate(fromDate.value);
  const title2 = fmtDisplayDate(toDate.value);
  const pasLabel = patientType.value === 'NONBPJS' ? 'NON BPJS' : patientType.value;

  let titleRow;
  let headerRow;
  let dataRows;
  if (tipe.value === 'PD') {
    titleRow = `LAPORAN PENDAPATAN ${doctorLabel.value} ${title} - ${title2} TIPE PASIEN : ${pasLabel}`;
    headerRow = ['NO NOTA', 'KODE', 'NAMA TINDAKAN', 'VALIDATE BY', 'NAMA PASIEN', 'TIPE PASIEN', 'KLS TARIF', 'TANGGAL', 'JASA DOKTER'].map((h) => cell(h)).join('');
    dataRows = rows.value.map((r) => `<Row>${cell(r.nota)}${cell(r.kode)}${cell(r.tindakan)}${cell(r.validasi)}${cell(r.pasien)}${cell(r.tipe)}${cell(r.kelas)}${cell(r.tanggal)}${cell(fmtMoney(r.jumlah), 'Number')}</Row>`).join('\n');
  } else if (tipe.value === 'OBAT') {
    titleRow = `LAPORAN SUMBANGSIH PENJUALAN OBAT ${doctorLabel.value} ${title} - ${title2} TIPE PASIEN : ${pasLabel}`;
    headerRow = ['NO NOTA', 'NAMA PASIEN', 'TGL TRANSAKSI', 'NILAI TRANSAKSI'].map((h) => cell(h)).join('');
    dataRows = rows.value.map((r) => `<Row>${cell(r.nota)}${cell(r.pasien)}${cell(r.tanggal)}${cell(fmtMoney(r.jumlah), 'Number')}</Row>`).join('\n');
  } else {
    titleRow = `LAPORAN PENDAPATAN DOKTER ALL ${title} - ${title2} TIPE PASIEN : ${pasLabel}`;
    headerRow = ['NAMA DOKTER', 'PENDAPATAN JASA', 'SUMBANGSIH OBAT'].map((h) => cell(h)).join('');
    dataRows = allRows.value.map((r) => `<Row>${cell(r.namaDokter)}${cell(fmtMoney(r.pendapatanJasa), 'Number')}${cell(fmtMoney(r.sumbangsihObat), 'Number')}</Row>`).join('\n');
  }

  const xml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
 <Worksheet ss:Name="PendapatanDokter">
  <Table>
   <Row>${cell(titleRow)}</Row>
   <Row>${headerRow}</Row>
   ${dataRows}
   ${tipe.value !== 'ALL' ? `<Row>${cell('')}${cell('')}${cell('TOTAL')}${cell(fmtMoney(total.value), 'Number')}</Row>` : ''}
  </Table>
 </Worksheet>
</Workbook>`;

  const blob = new Blob([xml], { type: 'application/vnd.ms-excel;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'laporan.xls';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function fmtDisplayDate(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🩺 LAPORAN PENDAPATAN DOKTER</h2>
      <p class="page-subtitle">Migrasi screen legacy RPT0013 — laporanPendapatanDokter.zul</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="filter-bar">
        <span class="filter-label">DOKTER</span>
        <span class="filter-colon">:</span>
        <input class="doctor-field" :value="doctorLabel" placeholder="Cari dokter..." readonly @click="searchDoctor" />
        <button class="small-button" type="button" :disabled="doctorLoading" @click="searchDoctor">🔍 CARI</button>
        <span class="filter-label">LAPORAN</span>
        <span class="filter-colon">:</span>
        <select v-model="tipe" @change="onTipeChange">
          <option value="PD">PENDAPATAN TINDAKAN</option>
          <option value="OBAT">SUMBANGSIH PENJUALAN OBAT</option>
          <option value="ALL">ALL</option>
        </select>
      </div>

      <div class="filter-bar">
        <span class="filter-label">PERIODE</span>
        <span class="filter-colon">:</span>
        <input v-model="fromDate" type="date" />
        <span class="filter-label">s.d</span>
        <input v-model="toDate" type="date" />
        <span class="filter-label">TIPE PASIEN</span>
        <span class="filter-colon">:</span>
        <select v-model="patientType">
          <option value="BPJS">BPJS</option>
          <option value="NONBPJS">NON BPJS</option>
          <option value="ALL">ALL</option>
        </select>
        <button class="small-button primary" type="button" :disabled="loading" @click="loadReport">👁️ LIHAT LAPORAN</button>
      </div>

      <div v-if="loading" class="loading">Memuat laporan...</div>
      <div v-else class="table-wrap">
        <!-- PD -->
        <table v-if="tipe === 'PD'" class="table">
          <thead>
            <tr>
              <th>NO NOTA</th>
              <th>KODE</th>
              <th>NAMA TINDAKAN</th>
              <th>VALIDATE BY</th>
              <th>NAMA PASIEN</th>
              <th>TIPE PASIEN</th>
              <th>KLS TARIF</th>
              <th>TANGGAL</th>
              <th class="num">JASA DOKTER</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in pagedRows" :key="index">
              <td class="strong">{{ row.nota }}</td>
              <td>{{ row.kode }}</td>
              <td>{{ row.tindakan }}</td>
              <td>{{ row.validasi }}</td>
              <td>{{ row.pasien }}</td>
              <td>{{ row.tipe }}</td>
              <td>{{ row.kelas }}</td>
              <td>{{ row.tanggal }}</td>
              <td class="num strong">{{ fmtMoney(row.jumlah) }}</td>
            </tr>
            <tr v-if="!pagedRows.length && !loading">
              <td colspan="9" class="empty-state">Pilih dokter, periode &amp; tipe pasien lalu tekan LIHAT LAPORAN.</td>
            </tr>
          </tbody>
        </table>
        <!-- OBAT -->
        <table v-else-if="tipe === 'OBAT'" class="table">
          <thead>
            <tr>
              <th>NO NOTA</th>
              <th>NAMA PASIEN</th>
              <th>TGL TRANSAKSI</th>
              <th class="num">NILAI TRANSAKSI</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in pagedRows" :key="index">
              <td class="strong">{{ row.nota }}</td>
              <td>{{ row.pasien }}</td>
              <td>{{ row.tanggal }}</td>
              <td class="num strong">{{ fmtMoney(row.jumlah) }}</td>
            </tr>
            <tr v-if="!pagedRows.length && !loading">
              <td colspan="4" class="empty-state">Pilih dokter, periode &amp; tipe pasien lalu tekan LIHAT LAPORAN.</td>
            </tr>
          </tbody>
        </table>
        <!-- ALL -->
        <table v-else class="table">
          <thead>
            <tr>
              <th>NAMA DOKTER</th>
              <th class="num">PENDAPATAN JASA</th>
              <th class="num">SUMBANGSIH OBAT</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in allRows" :key="index">
              <td class="strong">{{ row.namaDokter }}</td>
              <td class="num">{{ fmtMoney(row.pendapatanJasa) }}</td>
              <td class="num">{{ fmtMoney(row.sumbangsihObat) }}</td>
            </tr>
            <tr v-if="!allRows.length && !loading">
              <td colspan="3" class="empty-state">Pilih periode &amp; tipe pasien lalu tekan LIHAT LAPORAN.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination" v-if="searched && tipe !== 'ALL' && rows.length > pageSize">
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(1)">⏮</button>
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">◀</button>
        <span class="page-info">Halaman {{ currentPage }} / {{ totalPages }} ({{ rows.length }} item)</span>
        <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">▶</button>
        <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(totalPages)">⏭</button>
      </div>

      <div class="totals-line">
        <button class="small-button" type="button" :disabled="!searched" @click="exportXls">📥 SIMPAN KE XLS</button>
        <template v-if="tipe !== 'ALL'">
          <span class="filter-label">TOTAL</span>
          <input class="total-field" :value="fmtMoney(total)" readonly />
        </template>
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>

    <!-- ==================== MODAL CARI DOKTER ==================== -->
    <transition name="dialog-fade">
      <div v-if="doctorDialog.visible" class="modal-overlay" @click.self="doctorDialog.visible = false">
        <div class="dialog-box doctor-box">
          <div class="dialog-title">CARI DATA DOKTER</div>
          <div class="doctor-search">
            <span class="filter-label">KODE</span>
            <input v-model="doctorCode" type="text" />
            <span class="filter-label">NAMA</span>
            <input v-model="doctorName" type="text" />
            <button class="small-button primary" type="button" :disabled="doctorLoading" @click="searchDoctor">🔍 CARI</button>
          </div>
          <div class="table-wrap doctor-table">
            <table class="table">
              <thead>
                <tr>
                  <th>KODE</th>
                  <th>NAMA</th>
                  <th>UNIT</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(doc, index) in pagedDoctors" :key="index" class="clickable" @click="chooseDoctor(doc)">
                  <td class="strong">{{ doc.code }}</td>
                  <td>{{ doc.name }}</td>
                  <td>{{ doc.units }}</td>
                </tr>
                <tr v-if="!pagedDoctors.length">
                  <td colspan="3" class="empty-state">Tidak ada dokter. Tekan CARI.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination" v-if="doctorResults.length > doctorPageSize">
            <button class="page-btn" type="button" :disabled="doctorPage <= 1" @click="doctorGoToPage(1)">⏮</button>
            <button class="page-btn" type="button" :disabled="doctorPage <= 1" @click="doctorGoToPage(doctorPage - 1)">◀</button>
            <span class="page-info">Halaman {{ doctorPage }} / {{ doctorTotalPages }}</span>
            <button class="page-btn" type="button" :disabled="doctorPage >= doctorTotalPages" @click="doctorGoToPage(doctorPage + 1)">▶</button>
            <button class="page-btn" type="button" :disabled="doctorPage >= doctorTotalPages" @click="doctorGoToPage(doctorTotalPages)">⏭</button>
          </div>
          <div class="dialog-buttons">
            <button class="small-button" type="button" @click="doctorDialog.visible = false">TUTUP</button>
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
.doctor-field { width: 260px; background: #f3f5f8; cursor: pointer; }

.table-wrap { overflow: auto; margin: 10px 0; max-height: 460px; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; position: sticky; top: 0; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.clickable { cursor: pointer; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 10px; margin: 12px 0 4px; flex-wrap: wrap; }
.page-btn { padding: 6px 12px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.page-btn:hover:not(:disabled) { background: #eef3fa; }
.page-btn:disabled { opacity: 0.4; cursor: default; }
.page-info { font-weight: 700; color: #304b73; font-size: 13px; }

.totals-line { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-top: 12px; justify-content: flex-end; }
.total-field { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 160px; box-sizing: border-box; background: #f3f5f8; font-weight: 800; text-align: right; }

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
.doctor-box { width: 640px; max-width: 94vw; }
.doctor-search { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: center; margin-bottom: 10px; }
.doctor-search input { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }
.doctor-table { max-height: 320px; }
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
