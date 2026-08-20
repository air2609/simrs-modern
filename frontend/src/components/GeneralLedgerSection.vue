<script setup>
import { onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const dfrom = ref(todayIso());
const dto = ref(todayIso());
const coaOptions = ref([]);
const selectedCoaId = ref(null);
const rows = ref([]);
const selectedIndex = ref(-1);
const searched = ref(false);

// Dialog LIHAT JOURNAL ENTRY
const journalOpen = ref(false);
const journalRows = ref([]);

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function fmt(value) {
  const n = Number(value) || 0;
  return n.toLocaleString('en-US', { maximumFractionDigits: 3 });
}

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

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    const masters = await request('/accounting/journal-entry/masters');
    coaOptions.value = masters.coaOptions || [];
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function requireDate() {
  if (!dfrom.value || !dto.value) {
    alert('TANGGAL HARUS DI ISI!');
    return false;
  }
  return true;
}

async function loadReport(coaId) {
  if (!requireDate()) return;
  error.value = '';
  loading.value = true;
  try {
    const params = new URLSearchParams({ from: dfrom.value, to: dto.value });
    if (coaId != null) params.set('coaId', String(coaId));
    rows.value = await request(`/accounting/general-ledger/report?${params.toString()}`);
    searched.value = true;
    selectedIndex.value = -1;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function viewAll() {
  selectedCoaId.value = null;
  loadReport(null);
}

function onCoaSelect() {
  if (selectedCoaId.value == null) return;
  loadReport(selectedCoaId.value);
}

// ===== LIHAT JOURNAL ENTRY =====
async function lihatJournal() {
  const row = selectedIndex.value >= 0 ? rows.value[selectedIndex.value] : null;
  if (!row || !row.batchId) {
    alert('PILIHLAH ITEM JOURNAL DULU!!!');
    return;
  }
  error.value = '';
  try {
    journalRows.value = await request(`/accounting/account-payable/journal?batchId=${encodeURIComponent(row.batchId)}`);
    journalOpen.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  }
}

// ===== PRINT / PRINT ALL =====
function print() {
  if (selectedCoaId.value == null) {
    alert('PILIH ACCOUNT TERLEBIH DAHULU!');
    return;
  }
  if (!requireDate()) return;
  window.open(`${props.apiBaseUrl}/accounting/general-ledger/print?coaId=${selectedCoaId.value}&from=${encodeURIComponent(dfrom.value)}&to=${encodeURIComponent(dto.value)}`, '_blank');
}

function printAll() {
  window.open(`${props.apiBaseUrl}/accounting/general-ledger/print-all`, '_blank');
}

// ===== EXPORT TO XLS =====
function exportXls() {
  error.value = '';
  if (!searched.value) {
    alert('Tampilkan data terlebih dahulu!');
    return;
  }
  const esc = (v) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const cell = (value, type = 'String') => `<Cell><Data ss:Type="${type}">${esc(value)}</Data></Cell>`;
  const headerRow = `<Row>${['ACT NO', 'ACT NAME', 'VOUCHER NO.', 'KETERANGAN', 'APL DATE', 'DEBET', 'KREDIT', 'BALANCE']
    .map((h) => cell(h)).join('')}</Row>`;
  const dataRows = rows.value.map((row) => `<Row>${cell(row.acctNo)}${cell(row.acctName)}${cell(row.voucherNo)}${cell(row.description)}${cell(row.aplDate)}${cell(fmt(row.debit), 'Number')}${cell(fmt(row.credit), 'Number')}${cell(fmt(row.balance), 'Number')}</Row>`).join('\n');
  const xml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
 <Worksheet ss:Name="General Ledger">
  <Table>
   <Row><Cell ss:MergeAcross="7"><Data ss:Type="String">GENERAL LEDGER : ${selectedCoaId.value != null ? 'ACCOUNT TERPILIH' : 'SEMUA AKUN'} PERIODE ${dfrom.value} - ${dto.value}</Data></Cell></Row>
   ${headerRow}
   ${dataRows}
  </Table>
 </Worksheet>
</Workbook>`;
  const blob = new Blob([xml], { type: 'application/vnd.ms-excel;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'general_ledger.xls';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📒 GENERAL LEDGER</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="filter-bar">
      <span class="filter-label">DARI :</span>
      <input v-model="dfrom" type="date" />
      <span class="filter-label">SAMPAI :</span>
      <input v-model="dto" type="date" />
      <button class="small-button primary" type="button" @click="viewAll">👁️ VIEW ALL</button>
      <span class="filter-label">ACCOUNT :</span>
      <select v-model="selectedCoaId" @change="onCoaSelect">
        <option :value="null">- Pilih COA -</option>
        <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.coaId">{{ opt.acctNo }} - {{ opt.acctName }}</option>
      </select>
    </div>

    <div class="card">
      <div v-if="loading" class="loading">Memuat general ledger...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ACT NO</th>
              <th>ACT NAME</th>
              <th>VOUCHER NO.</th>
              <th>KETERANGAN</th>
              <th>APL DATE</th>
              <th class="num">DEBET</th>
              <th class="num">KREDIT</th>
              <th class="num">BALANCE</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in rows" :key="index" :class="{ selected: selectedIndex === index }" @click="selectedIndex = index">
              <td class="strong">{{ row.acctNo }}</td>
              <td>{{ row.acctName }}</td>
              <td>{{ row.voucherNo }}</td>
              <td>{{ row.description }}</td>
              <td>{{ row.aplDate }}</td>
              <td class="num">{{ fmt(row.debit) }}</td>
              <td class="num">{{ fmt(row.credit) }}</td>
              <td class="num">{{ fmt(row.balance) }}</td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="8" class="empty-state">Pilih periode lalu tekan VIEW ALL / pilih ACCOUNT.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="action-bar">
        <button class="small-button" type="button" @click="print">🖨️ PRINT</button>
        <button class="small-button" type="button" @click="printAll">🖨️ PRINT ALL</button>
        <button class="small-button" type="button" @click="lihatJournal">📄 LIHAT JOURNAL ENTRY</button>
        <button class="small-button" type="button" @click="exportXls">📥 EXPORT TO XLS</button>
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>
  </div>

  <!-- MODAL LIHAT JOURNAL ENTRY -->
  <div v-if="journalOpen" class="modal-overlay" @click.self="journalOpen = false">
    <div class="modal-card wide-card">
      <h3 class="card-title">JOURNAL DETAIL</h3>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>BATCH ID</th>
              <th>VOUCHER NO</th>
              <th>REKENING</th>
              <th>KETERANGAN</th>
              <th class="num">DEBET</th>
              <th class="num">KREDIT</th>
              <th>APL DATE</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in journalRows" :key="index">
              <td>{{ row.batchId }}</td>
              <td>{{ row.voucherNo }}</td>
              <td>{{ row.acctName }}</td>
              <td>{{ row.description }}</td>
              <td class="num">{{ fmt(row.debit) }}</td>
              <td class="num">{{ fmt(row.credit) }}</td>
              <td>{{ row.aplDate }}</td>
            </tr>
            <tr v-if="!journalRows.length"><td colspan="7" class="empty-state">Tidak ada data.</td></tr>
          </tbody>
        </table>
      </div>
      <div class="modal-actions">
        <button class="modal-btn secondary" type="button" @click="journalOpen = false">SELESAI</button>
      </div>
    </div>
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

.filter-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; padding: 12px 16px; background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.filter-label { font-weight: 700; color: #304b73; font-size: 13px; }
.filter-bar input, .filter-bar select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.filter-bar select { min-width: 260px; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
.table th, .table td { padding: 6px 8px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.selected { background: #eef3fb; }

.strong { font-weight: 700; }
.num { text-align: right; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55); backdrop-filter: blur(3px); display: flex; align-items: center; justify-content: center; z-index: 50; animation: fade-in 0.18s ease; }
.modal-card { background: #fff; border-radius: 18px; padding: 24px 28px; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35); animation: pop-in 0.22s cubic-bezier(0.2, 0.9, 0.3, 1.2); max-height: 88vh; overflow-y: auto; }
.wide-card { width: 900px; max-width: 96vw; }
.modal-actions { display: flex; gap: 10px; justify-content: center; margin-top: 14px; }
.modal-btn { min-width: 110px; padding: 10px 18px; border-radius: 10px; border: 1px solid #d1d5db; background: #fff; font-weight: 700; font-size: 13px; cursor: pointer; }
.modal-btn.secondary { color: #374151; }

@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes pop-in { from { opacity: 0; transform: scale(0.88) translateY(10px); } to { opacity: 1; transform: scale(1) translateY(0); } }
</style>
