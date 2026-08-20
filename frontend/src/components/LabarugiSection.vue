<script setup>
import { ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const startDate = ref(todayIso());
const endDate = ref(todayIso());
const groups = ref([]);
const searched = ref(false);

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function fmt(value) {
  const n = Number(value) || 0;
  return n.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

function groupTotal(items) {
  return items.reduce((sum, item) => sum + (Number(item.balance) || 0), 0);
}

function formatDateForHeader(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
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

async function cari() {
  error.value = '';
  if (!startDate.value || !endDate.value) {
    alert('TANGGAL HARUS DI ISI!');
    return;
  }
  if (startDate.value > endDate.value) {
    alert('TANGGAL AWAL TIDAK BOLEH MELEBIHI TANGGAL AKHIR!');
    return;
  }
  loading.value = true;
  try {
    groups.value = await request(
      `/accounting/laba-rugi?from=${encodeURIComponent(startDate.value)}&to=${encodeURIComponent(endDate.value)}`
    );
    searched.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// CETAK BY DATE: PDF rentang tanggal (migrasi LabarugiController.printLabarugi)
function printByDate() {
  if (!startDate.value || !endDate.value) {
    alert('TANGGAL HARUS DI ISI!');
    return;
  }
  window.open(
    `${props.apiBaseUrl}/accounting/laba-rugi/print-by-date?from=${encodeURIComponent(startDate.value)}&to=${encodeURIComponent(endDate.value)}`,
    '_blank'
  );
}

// CETAK: PDF seluruh periode (migrasi AccountingReport.openCurrentLabarugi / v_profit_loss)
function printAll() {
  window.open(`${props.apiBaseUrl}/accounting/laba-rugi/print`, '_blank');
}

// EXPORT TO XLS: buat file Excel (SpreadsheetML) kompatibel .xls di sisi klien.
function exportXls() {
  error.value = '';
  if (!searched.value) {
    alert('Tekan tombol CARI terlebih dahulu!');
    return;
  }
  const esc = (v) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const cell = (value, type = 'String') => `<Cell><Data ss:Type="${type}">${esc(value)}</Data></Cell>`;

  const headerRow = `<Row>${['NO. REKENING', 'NAMA', 'JUMLAH'].map((h) => cell(h)).join('')}</Row>`;
  const dataRows = groups.value.flatMap((group) => {
    const groupRow = `<Row>${cell(group.caption)}</Row>`;
    const itemRows = group.items.map((item) =>
      `<Row>${cell(item.acctNo)}${cell(item.acctName)}${cell(fmt(item.balance), 'Number')}</Row>`
    ).join('\n');
    return groupRow + '\n' + itemRows;
  }).join('\n');

  const xml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
 <Worksheet ss:Name="LabaRugi">
  <Table>
   <Row><Cell ss:MergeAcross="2"><Data ss:Type="String">LAPORAN LABA RUGI PERIODE : ${formatDateForHeader(startDate.value)} - ${formatDateForHeader(endDate.value)}</Data></Cell></Row>
   ${headerRow}
   ${dataRows}
  </Table>
 </Worksheet>
</Workbook>`;

  const blob = new Blob([xml], { type: 'application/vnd.ms-excel;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'labarugi.xls';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📈 LABA RUGI</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="period-bar">
        <span class="period-label">TANGGAL</span>
        <span class="period-colon">:</span>
        <input v-model="startDate" type="date" />
        <span class="period-label">S.D.</span>
        <input v-model="endDate" type="date" />
        <button class="small-button primary" type="button" :disabled="loading" @click="cari">
          🔍 CARI
        </button>
      </div>

      <div v-if="loading" class="loading">Menghitung laba rugi...</div>

      <template v-else>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>NO. REKENING</th>
                <th>NAMA</th>
                <th class="num">JUMLAH</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="(group, groupIndex) in groups" :key="groupIndex">
                <tr class="group-row">
                  <td class="strong" colspan="2">{{ group.caption }}</td>
                  <td class="num strong">{{ fmt(groupTotal(group.items)) }}</td>
                </tr>
                <tr v-for="(item, itemIndex) in group.items" :key="`${groupIndex}-${itemIndex}`">
                  <td class="acct-no">{{ item.acctNo }}</td>
                  <td>{{ item.acctName }}</td>
                  <td class="num">{{ fmt(item.balance) }}</td>
                </tr>
              </template>
              <tr v-if="!groups.length">
                <td colspan="3" class="empty-state">Pilih rentang tanggal lalu tekan tombol CARI.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="action-bar">
          <button class="small-button" type="button" :disabled="!groups.length" @click="exportXls">📥 EXPORT TO XLS</button>
          <button class="small-button" type="button" :disabled="!groups.length" @click="printByDate">🖨️ CETAK BY DATE</button>
          <button class="small-button" type="button" @click="printAll">🖨️ CETAK</button>
          <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
        </div>
      </template>
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

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }

.period-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.period-label { font-weight: 700; color: #304b73; font-size: 14px; }
.period-colon { color: #6b7280; }
.period-bar input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }

.group-row { background: #eef3fa; }
.group-row td { border-bottom: 1px solid #dce6f2; }
.strong { font-weight: 700; }
.acct-no { font-family: monospace; color: #5f83c2; }
.num { text-align: right; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }
</style>
