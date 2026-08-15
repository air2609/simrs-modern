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
const rows = ref([]);

const pageSize = 18;
const currentPage = ref(1);
const searchQuery = ref('');

// Filter baris berdasarkan kata kunci pencarian (satuan awal, satuan akhir).
const filteredRows = computed(() => {
  const query = searchQuery.value.trim().toUpperCase();
  if (!query) return rows.value;
  return rows.value.filter((row) => {
    const haystack = [row.earlyQuantify, row.endQuantify].join(' ').toUpperCase();
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

const form = ref({
  id: null,
  earlyQuantify: '',
  endQuantify: '',
  endQty: null
});

const selectedId = ref(null);
const saving = ref(false);

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

async function loadMeasurements() {
  rows.value = await request('/master/item-measurement');
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await loadMeasurements();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    earlyQuantify: '',
    endQuantify: '',
    endQty: null
  };
  selectedId.value = null;
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    earlyQuantify: row.earlyQuantify,
    endQuantify: row.endQuantify,
    endQty: row.endQty
  };
}

async function doSave() {
  error.value = '';
  if (!form.value.earlyQuantify) {
    error.value = 'Satuan awal harus diisi.';
    return;
  }
  if (!form.value.endQuantify) {
    error.value = 'Satuan akhir harus diisi.';
    return;
  }
  if (form.value.endQty === null || form.value.endQty === '') {
    error.value = 'Jumlah pembagi harus diisi.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/item-measurement/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    resetForm();
    await loadMeasurements();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data satuan item terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data satuan item ini?')) {
    return;
  }
  try {
    await request(`/master/item-measurement/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadMeasurements();
  } catch (requestError) {
    error.value = requestError.message;
  }
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>📏 Satuan Item</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0040 — master satuan item</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data satuan item...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM MASTER SATUAN ITEM</h3>
        <div class="form-grid">
          <div class="field">
            <label for="early-quantify">SATUAN AWAL</label>
            <input
              id="early-quantify"
              v-model="form.earlyQuantify"
              type="text"
              maxlength="20"
              placeholder="Satuan awal"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="end-quantify">SATUAN AKHIR</label>
            <input
              id="end-quantify"
              v-model="form.endQuantify"
              type="text"
              maxlength="20"
              placeholder="Satuan akhir"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="end-qty">JUMLAH PEMBAGI</label>
            <input
              id="end-qty"
              v-model.number="form.endQty"
              type="number"
              placeholder="Jumlah pembagi"
              @keyup.enter="doSave"
            />
          </div>
        </div>

        <div class="form-actions">
          <button class="btn btn--primary" type="button" :disabled="saving" @click="doSave">
            💾 SIMPAN
          </button>
          <button class="btn" type="button" @click="resetForm">✖ BATAL</button>
          <button class="btn btn--danger" type="button" @click="doDelete">🗑 HAPUS</button>
        </div>
      </div>

      <!-- List -->
      <div class="card">
        <h3 class="card-title">LIST SATUAN ITEM</h3>
        <div class="search-bar">
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            placeholder="🔍 Cari satuan awal atau satuan akhir..."
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
                <th>SATUAN AWAL</th>
                <th>SATUAN AKHIR</th>
                <th>JUMLAH PEMBAGI</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in paginatedRows"
                :key="row.id"
                :class="{ 'row--selected': selectedId === row.id }"
                @click="selectRow(row)"
              >
                <td class="strong">{{ row.earlyQuantify }}</td>
                <td class="strong">{{ row.endQuantify }}</td>
                <td>{{ row.endQty }}</td>
              </tr>
              <tr v-if="!filteredRows.length">
                <td colspan="3" class="empty-state">
                  {{ searchQuery ? 'Tidak ada data yang cocok dengan pencarian.' : 'Tidak ada data satuan item.' }}
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

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card-title { margin: 0 0 16px; color: #304b73; font-size: 15px; }

.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.form-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.btn:hover { background: #f6f8fb; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.btn--primary:hover { background: #1e40af; }
.btn--danger { background: #fff; border-color: #f0b3bd; color: #a32943; }
.btn--danger:hover { background: #fde8ea; }

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
.table tbody tr { cursor: pointer; }
.table tbody tr:hover { background: #f6f8fb; }
.row--selected { background: #e8f0fe; }

.strong { font-weight: 700; }
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
