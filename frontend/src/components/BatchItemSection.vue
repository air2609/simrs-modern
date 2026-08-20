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
const uploading = ref(false);

const pageSize = 25;
const currentPage = ref(1);
const searchQuery = ref('');

// Filter baris berdasarkan kata kunci pencarian (kode atau nama item).
const filteredRows = computed(() => {
  const query = searchQuery.value.trim().toUpperCase();
  if (!query) return rows.value;
  return rows.value.filter((row) => {
    const haystack = [row.code, row.name].join(' ').toUpperCase();
    return haystack.includes(query);
  });
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize)));

const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredRows.value.slice(start, start + pageSize);
});

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
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

async function loadData() {
  rows.value = await request('/master/batchitem');
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  success.value = '';
  try {
    await loadData();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '0';
  const num = Number(value);
  if (Number.isNaN(num)) return String(value);
  return num.toLocaleString('id-ID');
}

// SIMPAN: kirim seluruh baris untuk batch update.
async function doSave() {
  error.value = '';
  success.value = '';
  if (!rows.value.length) {
    error.value = 'Tidak ada data item untuk disimpan.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/batchitem/save', {
      method: 'POST',
      body: JSON.stringify({ items: rows.value })
    });
    success.value = 'Data berhasil disimpan.';
    await loadData();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

// DOWNLOAD: unduh file CSV batchItem.csv (delimiter ';').
function doDownload() {
  error.value = '';
  success.value = '';
  const header = 'KODE;NAMA ITEM;BUFFER;MAX ORDER;HRG BELI;HRG JUAL';
  const lines = rows.value.map((row) => [
    row.code,
    row.name,
    row.buffer ?? 0,
    row.maxOrder ?? 0,
    row.buyPrice ?? 0,
    row.sellPrice ?? 0
  ].join(';'));
  const csv = [header, ...lines].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'batchItem.csv';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// UPLOAD: baca file CSV (delimiter ';') dan isi ulang tabel.
function onFileSelected(event) {
  error.value = '';
  success.value = '';
  const file = event.target.files && event.target.files[0];
  event.target.value = '';
  if (!file) return;

  if (file.type && file.type !== 'text/csv' && !file.name.toLowerCase().endsWith('.csv')) {
    error.value = 'Format File Tidak Valid...!';
    return;
  }

  uploading.value = true;
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const text = String(reader.result || '');
      const lines = text.split(/\r?\n/).filter((line) => line.trim() !== '');
      const parsed = [];
      // Lewati baris header (baris pertama).
      for (let i = 1; i < lines.length; i++) {
        const parts = lines[i].split(';');
        if (parts.length < 6) continue;
        parsed.push({
          id: null,
          code: (parts[0] || '').trim(),
          name: (parts[1] || '').trim(),
          buffer: toNumber(parts[2]),
          maxOrder: toNumber(parts[3]),
          buyPrice: toNumber(parts[4]),
          sellPrice: toNumber(parts[5])
        });
      }
      if (!parsed.length) {
        error.value = 'File tidak berisi data yang valid.';
        return;
      }
      rows.value = parsed;
      currentPage.value = 1;
      success.value = `Berhasil memuat ${parsed.length} baris dari file.`;
    } catch (parseError) {
      error.value = 'Gagal membaca file: ' + parseError.message;
    } finally {
      uploading.value = false;
    }
  };
  reader.onerror = () => {
    uploading.value = false;
    error.value = 'Gagal membaca file.';
  };
  reader.readAsText(file);
}

function toNumber(value) {
  if (value === null || value === undefined || value === '') return null;
  const cleaned = String(value).replace(/[^\d.-]/g, '');
  const num = Number(cleaned);
  return Number.isNaN(num) ? null : num;
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>📦 Update Batch Item</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-if="success" class="status-banner status-banner--success">{{ success }}</p>

    <div v-if="loading" class="loading">Memuat data item...</div>

    <template v-else>
      <!-- Action buttons -->
      <div class="card action-card">
        <div class="action-buttons">
          <button class="btn btn--primary" type="button" :disabled="saving || !rows.length" @click="doSave">
            💾 SIMPAN
          </button>
          <button class="btn" type="button" :disabled="!rows.length" @click="doDownload">
            ⬇ DOWNLOAD
          </button>
          <label class="btn btn--upload">
            ⬆ UPLOAD
            <input type="file" accept=".csv,text/csv" :disabled="uploading" @change="onFileSelected" />
          </label>
        </div>
        <p class="field-hint">
          Format file CSV (delimiter ;): KODE;NAMA ITEM;BUFFER;MAX ORDER;HRG BELI;HRG JUAL
        </p>
      </div>

      <!-- List -->
      <div class="card">
        <h3 class="card-title">DATA ITEM</h3>
        <div class="search-bar">
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            placeholder="🔍 Cari kode atau nama item..."
            @input="currentPage = 1"
          />
          <button v-if="searchQuery" class="small-button" type="button" @click="searchQuery = ''; currentPage = 1">
            ✖ Bersihkan
          </button>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>NAMA ITEM</th>
                <th>BUFFER</th>
                <th>MAX ORDER</th>
                <th>HRG BELI</th>
                <th>HRG JUAL</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in paginatedRows" :key="row.code">
                <td class="strong">{{ row.code }}</td>
                <td>{{ row.name }}</td>
                <td>
                  <input
                    v-model.number="row.buffer"
                    type="number"
                    class="cell-input"
                    min="0"
                  />
                </td>
                <td>
                  <input
                    v-model.number="row.maxOrder"
                    type="number"
                    class="cell-input"
                    min="0"
                  />
                </td>
                <td class="num">{{ formatNumber(row.buyPrice) }}</td>
                <td>
                  <input
                    v-model.number="row.sellPrice"
                    type="number"
                    class="cell-input"
                    min="0"
                    step="0.01"
                  />
                </td>
              </tr>
              <tr v-if="!filteredRows.length">
                <td colspan="6" class="empty-state">
                  {{ searchQuery ? 'Tidak ada data yang cocok dengan pencarian.' : 'Tidak ada data item.' }}
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

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card-title { margin: 0 0 16px; color: #304b73; font-size: 15px; }

.action-card { display: flex; flex-direction: column; gap: 10px; }
.action-buttons { display: flex; gap: 10px; flex-wrap: wrap; }
.field-hint { font-size: 11px; color: #6b7280; margin: 0; }

.btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.btn:hover { background: #f6f8fb; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.btn--primary:hover { background: #1e40af; }
.btn--upload { position: relative; overflow: hidden; }
.btn--upload input[type="file"] { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }

.search-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d1d9e6;
  border-radius: 6px;
  font-size: 14px;
}
.search-input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 14px; }
.table th, .table td { padding: 10px 12px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr:hover { background: #f6f8fb; }

.cell-input {
  width: 100px;
  padding: 6px 8px;
  border: 1px solid #d1d9e6;
  border-radius: 6px;
  font-size: 14px;
  text-align: right;
}
.cell-input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.strong { font-weight: 700; }
.num { text-align: right; }
.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }

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
