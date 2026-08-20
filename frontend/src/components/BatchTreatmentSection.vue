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
const error = ref('');
const success = ref('');
const rows = ref([]);
const saving = ref(false);
const fileInput = ref(null);
const searchKeyword = ref('');

const pageSize = 25;
const currentPage = ref(1);

// Filter rows berdasarkan kata kunci pencarian (kode, nama, kelas tarif)
const filteredRows = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) return rows.value;
  return rows.value.filter((row) => {
    const code = String(row.code || '').toLowerCase();
    const name = String(row.name || '').toLowerCase();
    const className = String(row.treatmentClassDesc || '').toLowerCase();
    return code.includes(keyword) || name.includes(keyword) || className.includes(keyword);
  });
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize)));

const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredRows.value.slice(start, start + pageSize);
});

function onSearch() {
  currentPage.value = 1;
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
}

function formatCurrency(value) {
  const num = Number(value) || 0;
  return num.toLocaleString('id-ID', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
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

async function loadTreatments() {
  rows.value = await request('/master/treatment-batch');
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  success.value = '';
  try {
    await loadTreatments();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// DOWNLOAD: buat file CSV dari daftar treatment
function downloadCsv() {
  const header = 'KODE;NAMA TINDAKAN;KELAS TARIF;JASA RS; JASA DOKTER; JASA MEDIS; TOTAL BIAYA; NO.COA';
  const lines = rows.value.map((row) => [
    row.code,
    row.name,
    row.treatmentClassDesc || '',
    row.hospitalFee ?? 0,
    row.doctorFee ?? 0,
    row.medicFee ?? 0,
    row.totalFee ?? 0,
    row.coaNo || ''
  ].join(';'));

  const content = [header, ...lines].join('\n');
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'treatmentBatch.csv';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// UPLOAD: baca file CSV dan isi daftar
function triggerUpload() {
  fileInput.value?.click();
}

function onFileSelected(event) {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;

  const reader = new FileReader();
  reader.onload = () => {
    const text = String(reader.result || '');
    const lines = text.split(/\r?\n/).filter((line) => line.trim().length > 0);
    const parsed = [];
    // Lewati baris header (baris pertama)
    for (let i = 1; i < lines.length; i++) {
      const parts = lines[i].split(';');
      if (parts.length < 8) continue;
      parsed.push({
        code: parts[0]?.trim() || '',
        name: parts[1]?.trim() || '',
        treatmentClassDesc: parts[2]?.trim() || '',
        hospitalFee: Number(parts[3]) || 0,
        doctorFee: Number(parts[4]) || 0,
        medicFee: Number(parts[5]) || 0,
        totalFee: Number(parts[6]) || 0,
        coaNo: parts[7]?.trim() || ''
      });
    }
    rows.value = parsed;
    currentPage.value = 1;
    success.value = `Berhasil memuat ${parsed.length} baris dari file.`;
    error.value = '';
  };
  reader.readAsText(file);
}

// SIMPAN: kirim batch update
async function doSave() {
  error.value = '';
  success.value = '';
  if (!rows.value.length) {
    error.value = 'Tidak ada data untuk disimpan.';
    return;
  }
  saving.value = true;
  try {
    const result = await request('/master/treatment-batch/save', {
      method: 'POST',
      body: JSON.stringify({ items: rows.value })
    });
    success.value = result?.message || 'Sukses Mengupdate Data';
    await loadTreatments();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>🩺 Update Master Tindakan</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-if="success" class="status-banner status-banner--success">{{ success }}</p>

    <div v-if="loading" class="loading">Memuat data treatment...</div>

    <template v-else>
      <!-- Action buttons -->
      <div class="action-bar">
        <button class="btn btn--primary" type="button" :disabled="saving" @click="doSave">
          💾 SIMPAN
        </button>
        <button class="btn" type="button" @click="downloadCsv">⬇ DOWNLOAD</button>
        <button class="btn" type="button" @click="triggerUpload">⬆ UPLOAD</button>
        <input ref="fileInput" type="file" accept=".csv,text/csv" style="display: none" @change="onFileSelected" />
      </div>

      <!-- List -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">DATA TINDAKAN</h3>
          <div class="search-box">
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="🔍 Cari kode / nama / kelas tarif..."
              @input="onSearch"
            />
          </div>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>NAMA TINDAKAN</th>
                <th>KELAS TARIF</th>
                <th>JASA RS</th>
                <th>JASA DOKTER</th>
                <th>JASA MEDIK</th>
                <th>TOTAL BIAYA</th>
                <th>NO. COA</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in paginatedRows" :key="index">
                <td class="strong">{{ row.code }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.treatmentClassDesc || '-' }}</td>
                <td>
                  <input
                    v-model.number="row.hospitalFee"
                    type="number"
                    min="0"
                    step="0.01"
                    class="cell-input"
                  />
                </td>
                <td>
                  <input
                    v-model.number="row.doctorFee"
                    type="number"
                    min="0"
                    step="0.01"
                    class="cell-input"
                  />
                </td>
                <td>
                  <input
                    v-model.number="row.medicFee"
                    type="number"
                    min="0"
                    step="0.01"
                    class="cell-input"
                  />
                </td>
                <td class="num strong">{{ formatCurrency((row.hospitalFee || 0) + (row.doctorFee || 0) + (row.medicFee || 0)) }}</td>
                <td>
                  <input v-model="row.coaNo" type="text" class="cell-input" />
                </td>
              </tr>
              <tr v-if="!filteredRows.length">
                <td colspan="8" class="empty-state">
                  {{ rows.length ? 'Tidak ada hasil untuk pencarian.' : 'Tidak ada data treatment.' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="filteredRows.length" class="pagination-bar">
          <span class="pagination-info">
            Menampilkan {{ paginatedRows.length }} dari {{ filteredRows.length }} data
          </span>
          <div class="pagination-controls">
            <button class="small-button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹ Prev</button>
            <span class="pagination-page">Halaman {{ currentPage }} / {{ totalPages }}</span>
            <button class="small-button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">Next ›</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.header-actions { display: flex; align-items: center; gap: 10px; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.status-banner--success { background: #e6f7ee; color: #1a7f4b; }

.action-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
.card-title { margin: 0; color: #304b73; font-size: 15px; }

.search-box { display: flex; align-items: center; }
.search-input {
  width: 280px;
  max-width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d9e6;
  border-radius: 8px;
  font-size: 13px;
  background: #f8fafc;
}
.search-input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); background: #fff; }

.btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.btn:hover { background: #f6f8fb; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.btn--primary:hover { background: #1e40af; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 14px; }

.table th, .table td { padding: 10px 12px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }

.strong { font-weight: 700; }
.num { text-align: right; white-space: nowrap; }
.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

.cell-input { width: 100%; min-width: 90px; padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }
.cell-input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
.small-button:disabled { opacity: 0.5; cursor: not-allowed; }

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
.pagination-info { font-size: 13px; color: #6b7280; }
.pagination-controls { display: flex; align-items: center; gap: 8px; }
.pagination-page { font-size: 13px; color: #3d4b63; font-weight: 600; }
</style>
