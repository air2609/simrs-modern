<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');

const tipe = ref('RAJAL'); // RAWAT JALAN / RAWAT INAP
const units = ref([]);
const unitId = ref(null);
const shift = ref('2'); // 2 PAGI (default), 1 SORE, 3 MALAM, ALL
const fromDate = ref(todayIso());
const toDate = ref(todayIso());

const rows = ref([]);
const unitName = ref('');
const shiftLabel = ref('');
const searched = ref(false);

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

const shiftOptions = [
  { value: '2', label: 'SHIFT PAGI' },
  { value: '1', label: 'SHIFT SORE' },
  { value: '3', label: 'SHIFT MALAM' },
  { value: 'ALL', label: 'ALL SHIFT' }
];

const isTotalRow = (row) => row.pasien === 'TOTAL' || row.pasien === 'TOTAL SELURUHNYA';

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
    units.value = await request('/report/penjualan/masters');
    if (units.value.length) unitId.value = units.value[0].unitId;
  } catch (requestError) {
    error.value = requestError.message;
  }
});

function onTipeChange() {
  rows.value = [];
  searched.value = false;
}

async function loadReport() {
  if (!fromDate.value || !toDate.value) {
    await showAlert('TANGGAL HARUS DI ISI!');
    return;
  }
  if (unitId.value == null) {
    await showAlert('LOKASI HARUS DIPILIH!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const data = await request(`/report/penjualan/report${qs({
      tipe: tipe.value,
      from: fromDate.value,
      to: toDate.value,
      unitId: unitId.value,
      shift: shift.value
    })}`);
    rows.value = data.rows;
    unitName.value = data.unitName;
    shiftLabel.value = data.shiftLabel;
    searched.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// EXPORT TO XLS (SpreadsheetML) — mengikuti struktur export legacy
// (judul laporan, nama unit, shift, periode, header, data)
function exportXls() {
  if (!searched.value) {
    showAlert('Tekan LIHAT LAPORAN terlebih dahulu!');
    return;
  }
  const esc = (v) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const cell = (value, type = 'String') => `<Cell><Data ss:Type="${type}">${esc(value)}</Data></Cell>`;

  const tipeLabel = tipe.value === 'RANAP' ? 'RAWAT INAP' : 'RAWAT JALAN';
  const infoRows = [
    ['', 'LAPORAN PENJUALAN', tipeLabel],
    ['', 'NAMA UNIT', unitName.value],
    ['', 'SHIFT', shiftLabel.value],
    ['', 'PERIODE', `${fmtDisplayDate(fromDate.value)} s.d ${fmtDisplayDate(toDate.value)}`]
  ].map((r) => `<Row>${r.map((c) => cell(c)).join('')}</Row>`).join('\n');

  const rajal = tipe.value !== 'RANAP';
  const headers = rajal
    ? ['NO', 'NO. NOTA', 'NAMA PASIEN', 'TOTAL']
    : ['NO', 'NO. NOTA', 'NO. REGISTRASI', 'NAMA PASIEN', 'BED', 'RUANGAN', 'R', 'TOTAL'];
  const headerRow = `<Row>${headers.map((h) => cell(h)).join('')}</Row>`;
  const dataRows = rows.value.map((r) => {
    const cells = rajal
      ? [r.no ?? '', r.nota ?? '', r.pasien ?? '', fmtMoney(r.total)]
      : [r.no ?? '', r.nota ?? '', r.reg ?? '', r.pasien ?? '', r.bed ?? '', r.ruangan ?? '', r.r ?? '', fmtMoney(r.total)];
    return `<Row>${cells.map((c, i) => cell(c, i >= 3 ? 'Number' : 'String')).join('')}</Row>`;
  }).join('\n');

  const xml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
 <Worksheet ss:Name="LaporanPenjualan">
  <Table>
   ${infoRows}
   ${headerRow}
   ${dataRows}
  </Table>
 </Worksheet>
</Workbook>`;

  const blob = new Blob([xml], { type: 'application/vnd.ms-excel;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'laporan_penjualan.xls';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// LIHAT PDF (buka di tab baru, sesuai legacy createRepport/openPdf)
function printReport() {
  if (!searched.value) {
    showAlert('Tekan LIHAT LAPORAN terlebih dahulu!');
    return;
  }
  window.open(`${props.apiBaseUrl}/report/penjualan/print${qs({
    tipe: tipe.value,
    from: fromDate.value,
    to: toDate.value,
    unitId: unitId.value,
    shift: shift.value
  })}`, '_blank');
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
      <h2>💊 LAPORAN PENJUALAN PASIEN</h2>
      <p class="page-subtitle">Migrasi screen legacy RPT0001 — laporanRajal.zul</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="filter-bar">
        <span class="filter-label">LAPORAN</span>
        <span class="filter-colon">:</span>
        <label class="radio-option">
          <input type="radio" value="RAJAL" v-model="tipe" @change="onTipeChange" /> RAWAT JALAN
        </label>
        <label class="radio-option">
          <input type="radio" value="RANAP" v-model="tipe" @change="onTipeChange" /> RAWAT INAP
        </label>
      </div>

      <div class="filter-bar">
        <span class="filter-label">LOKASI</span>
        <span class="filter-colon">:</span>
        <select v-model="unitId">
          <option v-for="u in units" :key="u.unitId" :value="u.unitId">{{ u.code }} - {{ u.name }}</option>
        </select>
        <span class="filter-label">SHIFT</span>
        <span class="filter-colon">:</span>
        <select v-model="shift">
          <option v-for="s in shiftOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
        <span class="filter-label">DARI</span>
        <span class="filter-colon">:</span>
        <input v-model="fromDate" type="date" />
        <span class="filter-label">SAMPAI</span>
        <span class="filter-colon">:</span>
        <input v-model="toDate" type="date" />
        <button class="small-button primary" type="button" :disabled="loading" @click="loadReport">👁️ LIHAT LAPORAN</button>
      </div>

      <div v-if="loading" class="loading">Memuat laporan...</div>
      <div v-else class="table-wrap">
        <table v-if="tipe === 'RANAP'" class="table">
          <thead>
            <tr>
              <th>NO</th>
              <th>NO. NOTA</th>
              <th>NO. REGISTRASI</th>
              <th>NAMA PASIEN</th>
              <th>BED</th>
              <th>RUANGAN</th>
              <th class="num">R</th>
              <th class="num">TOTAL</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in rows" :key="index" :class="{ 'total-row': isTotalRow(row) }">
              <td>{{ row.no ?? '' }}</td>
              <td class="strong">{{ row.nota ?? '' }}</td>
              <td>{{ row.reg ?? '' }}</td>
              <td :class="{ strong: isTotalRow(row) }">{{ row.pasien ?? '' }}</td>
              <td>{{ row.bed ?? '' }}</td>
              <td>{{ row.ruangan ?? '' }}</td>
              <td class="num">{{ row.r ?? '' }}</td>
              <td class="num strong">{{ fmtMoney(row.total) }}</td>
            </tr>
            <tr v-if="!rows.length && !loading">
              <td colspan="8" class="empty-state">Pilih filter lalu tekan LIHAT LAPORAN.</td>
            </tr>
          </tbody>
        </table>
        <table v-else class="table">
          <thead>
            <tr>
              <th>NO</th>
              <th>NO. NOTA</th>
              <th>NAMA PASIEN</th>
              <th class="num">TOTAL</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in rows" :key="index" :class="{ 'total-row': isTotalRow(row) }">
              <td>{{ row.no ?? '' }}</td>
              <td class="strong">{{ row.nota ?? '' }}</td>
              <td :class="{ strong: isTotalRow(row) }">{{ row.pasien ?? '' }}</td>
              <td class="num strong">{{ fmtMoney(row.total) }}</td>
            </tr>
            <tr v-if="!rows.length && !loading">
              <td colspan="4" class="empty-state">Pilih filter lalu tekan LIHAT LAPORAN.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="action-bar">
        <button class="small-button" type="button" :disabled="!searched" @click="exportXls">📥 DOWNLOAD XLS</button>
        <button class="small-button" type="button" :disabled="!searched" @click="printReport">🖨️ LIHAT PDF</button>
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
.filter-bar select, .filter-bar input[type="date"] { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }
.radio-option { font-weight: 700; font-size: 13px; color: #304b73; display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.total-row { background: #eef3fa; }
.table tbody tr.total-row td { border-top: 2px solid #dce6f2; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

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
