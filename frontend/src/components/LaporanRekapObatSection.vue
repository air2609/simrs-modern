<script setup>
import { computed, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');

const fromDate = ref(todayIso());
const toDate = ref(todayIso());

const rows = ref([]);
const totalNilai = ref(0);
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

const fmtNumber = (v) => Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 });

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

async function loadReport() {
  if (!fromDate.value || !toDate.value) {
    await showAlert('Kedua tanggal harus diisi....!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const data = await request(`/report/rekap-obat/report${qs({
      from: fromDate.value,
      to: toDate.value
    })}`);
    rows.value = data.rows || [];
    totalNilai.value = data.totalNilaiPenjualan || 0;
    currentPage.value = 1;
    searched.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// EXPORT TO XLS (SpreadsheetML) — judul mengikuti export legacy saveToXls
function exportXls() {
  if (!searched.value) {
    showAlert('Tekan VIEW terlebih dahulu!');
    return;
  }
  const esc = (v) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const cell = (value, type = 'String') => `<Cell><Data ss:Type="${type}">${esc(value)}</Data></Cell>`;
  const title = `REKAP OBAT ${fmtDisplayDate(fromDate.value)} - ${fmtDisplayDate(toDate.value)}`;

  const headerRow = `<Row>${['NO', 'NAMA OBAT', 'QTY', 'NILAI PENJUALAN'].map((h) => cell(h)).join('')}</Row>`;
  const dataRows = rows.value.map((r, i) =>
    `<Row>${cell(i + 1, 'Number')}${cell(r.namaObat)}${cell(fmtNumber(r.qty), 'Number')}${cell(fmtNumber(r.nilaiPenjualan), 'Number')}</Row>`
  ).join('\n');
  const totalRow = `<Row>${cell('')}${cell('')}${cell('')}${cell(fmtNumber(totalNilai.value), 'Number')}</Row>`;

  const xml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
 <Worksheet ss:Name="RekapObat">
  <Table>
   <Row>${cell(title)}</Row>
   ${headerRow}
   ${dataRows}
   ${totalRow}
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
      <h2>💊 REKAP OBAT</h2>
      <p class="page-subtitle">Migrasi screen legacy RPT0016 — rekapObat.zul</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="filter-bar">
        <span class="filter-label">FROM</span>
        <span class="filter-colon">:</span>
        <input v-model="fromDate" type="date" />
        <span class="filter-label">TO</span>
        <span class="filter-colon">:</span>
        <input v-model="toDate" type="date" />
        <button class="small-button primary" type="button" :disabled="loading" @click="loadReport">👁️ VIEW</button>
      </div>

      <div v-if="loading" class="loading">Memuat laporan...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>NO</th>
              <th>NAMA OBAT</th>
              <th class="num">QTY</th>
              <th class="num">NILAI PENJUALAN</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in pagedRows" :key="index">
              <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
              <td class="strong">{{ row.namaObat }}</td>
              <td class="num">{{ fmtNumber(row.qty) }}</td>
              <td class="num strong">{{ fmtNumber(row.nilaiPenjualan) }}</td>
            </tr>
            <tr v-if="!pagedRows.length && !loading">
              <td colspan="4" class="empty-state">Pilih periode lalu tekan VIEW.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination" v-if="searched && rows.length > pageSize">
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(1)">⏮</button>
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">◀</button>
        <span class="page-info">Halaman {{ currentPage }} / {{ totalPages }} ({{ rows.length }} item)</span>
        <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">▶</button>
        <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(totalPages)">⏭</button>
      </div>

      <div class="totals-line">
        <button class="small-button" type="button" :disabled="!searched" @click="exportXls">📥 SIMPAN KE XLS</button>
        <span class="filter-label">NILAI PENJUALAN :</span>
        <input class="total-field" :value="fmtNumber(totalNilai)" readonly />
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
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

.filter-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.filter-label { font-weight: 700; color: #304b73; font-size: 13px; }
.filter-colon { color: #6b7280; font-weight: 700; }
.filter-bar input[type="date"] { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }

.table-wrap { overflow: auto; margin: 10px 0; max-height: 460px; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; position: sticky; top: 0; }
.table tbody tr:hover { background: #f6f8fb; }
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
.dialog-buttons { display: flex; justify-content: center; gap: 12px; }
.dialog-buttons .small-button { min-width: 110px; }
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
