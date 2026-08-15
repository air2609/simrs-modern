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
const warehouseOptions = ref([]);

const pageSize = 10;
const currentPage = ref(1);

const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)));

const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return rows.value.slice(start, start + pageSize);
});

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
}

const form = ref({
  id: null,
  whouseId: null,
  itemId: null,
  itemCode: '',
  batchNo: '',
  qty: null,
  cogsPrice: null
});

const selectedId = ref(null);
const saving = ref(false);
const searchKeyword = ref('');

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

async function loadWarehouseOptions() {
  warehouseOptions.value = await request('/master/item-inventory/warehouse-options');
}

async function loadInventory() {
  const params = new URLSearchParams();
  if (form.value.whouseId) params.set('whouseId', form.value.whouseId);
  if (searchKeyword.value) params.set('keyword', searchKeyword.value);
  const query = params.toString();
  rows.value = await request(`/master/item-inventory${query ? `?${query}` : ''}`);
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await loadWarehouseOptions();
    await loadInventory();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    whouseId: form.value.whouseId,
    itemId: null,
    itemCode: '',
    batchNo: '',
    qty: null,
    cogsPrice: null
  };
  selectedId.value = null;
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    whouseId: row.whouseId,
    itemId: row.itemId,
    itemCode: row.itemCode,
    batchNo: row.batchNo,
    qty: row.qty,
    cogsPrice: row.cogsPrice
  };
}

async function doSave() {
  error.value = '';
  if (!form.value.whouseId) {
    error.value = 'Lokasi gudang harus dipilih.';
    return;
  }
  if (!form.value.itemCode) {
    error.value = 'Kode item harus diisi.';
    return;
  }
  if (!form.value.batchNo) {
    error.value = 'Batch no harus diisi.';
    return;
  }
  if (form.value.qty == null || form.value.qty === '') {
    error.value = 'Jumlah harus diisi.';
    return;
  }
  if (form.value.cogsPrice == null || form.value.cogsPrice === '') {
    error.value = 'Harga beli harus diisi.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/item-inventory/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    resetForm();
    await loadInventory();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data alokasi terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data alokasi item ini?')) {
    return;
  }
  try {
    await request(`/master/item-inventory/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadInventory();
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function formatNumber(value) {
  if (value == null) return '-';
  return Number(value).toLocaleString('id-ID');
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>📦 Alokasi Item</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0032 — form alokasi item</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data alokasi item...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM ALOKASI ITEM</h3>
        <div class="form-grid">
          <div class="field">
            <label for="whouse">LOKASI</label>
            <select id="whouse" v-model="form.whouseId" @change="loadInventory">
              <option :value="null">-- PILIH --</option>
              <option v-for="opt in warehouseOptions" :key="opt.id" :value="opt.id">
                {{ opt.whouseCode }} - {{ opt.whouseName }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="item-code">KODE ITEM</label>
            <input
              id="item-code"
              v-model="form.itemCode"
              type="text"
              maxlength="20"
              placeholder="Kode item"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="batch-no">BATCH NO</label>
            <input
              id="batch-no"
              v-model="form.batchNo"
              type="text"
              maxlength="30"
              placeholder="No. batch"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="qty">JUMLAH</label>
            <input
              id="qty"
              v-model="form.qty"
              type="number"
              min="0"
              step="any"
              placeholder="Jumlah"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="cogs-price">HARGA BELI</label>
            <input
              id="cogs-price"
              v-model="form.cogsPrice"
              type="number"
              min="0"
              step="any"
              placeholder="Harga beli"
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
        <h3 class="card-title">LIST ALOKASI ITEM</h3>
        <div class="search-row">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="Cari kode item / nama / batch no..."
            @keyup.enter="loadInventory"
          />
          <button class="btn" type="button" @click="loadInventory">🔍 CARI</button>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>NO. BATCH</th>
                <th>NAMA</th>
                <th>JUMLAH</th>
                <th>HRG BELI</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in paginatedRows"
                :key="row.id"
                :class="{ 'row--selected': selectedId === row.id }"
                @click="selectRow(row)"
              >
                <td class="strong">{{ row.itemCode }}</td>
                <td>{{ row.batchNo }}</td>
                <td>{{ row.itemName }}</td>
                <td class="strong">{{ formatNumber(row.qty) }}</td>
                <td>{{ formatNumber(row.cogsPrice) }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="5" class="empty-state">Tidak ada data alokasi item.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="rows.length" class="pagination-bar">
          <span class="pagination-info">
            Menampilkan {{ paginatedRows.length }} dari {{ rows.length }} data
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
.field input, .field select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input:focus, .field select:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.form-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.btn:hover { background: #f6f8fb; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.btn--primary:hover { background: #1e40af; }
.btn--danger { background: #fff; border-color: #f0b3bd; color: #a32943; }
.btn--danger:hover { background: #fde8ea; }

.search-row { display: flex; gap: 8px; margin-bottom: 12px; }
.search-row input { flex: 1; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }

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
