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
const groupOptions = ref([]);
const measurementOptions = ref([]);
const vendorOptions = ref([]);

const pageSize = 10;
const currentPage = ref(1);
const searchQuery = ref('');

// Filter baris berdasarkan kata kunci pencarian (kode, nama, group, supplier, satuan).
const filteredRows = computed(() => {
  const query = searchQuery.value.trim().toUpperCase();
  if (!query) return rows.value;
  return rows.value.filter((row) => {
    const haystack = [
      row.itemCode,
      row.itemName,
      row.itemGroupName,
      (row.suppliers || []).join(' '),
      row.measurementName
    ].join(' ').toUpperCase();
    return haystack.includes(query);
  });
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize)));

const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredRows.value.slice(start, start + pageSize);
});

// Opsi satuan unik (tanpa pengulangan) mengikuti legacy yang tidak menampilkan duplikat.
const uniqueMeasurementOptions = computed(() => {
  const seen = new Set();
  return measurementOptions.value.filter((opt) => {
    const key = (opt.endQuantify || '').toUpperCase();
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
});

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
}

const form = ref({
  id: null,
  itemCode: '',
  itemName: '',
  barcodeNo: '',
  itemGroupId: null,
  measurementId: null,
  itemReturnable: '',
  itemType: null,
  r: null,
  bufferLimit: null,
  plafon: null,
  maxOrder: null,
  supplierIds: []
});

const selectedId = ref(null);
const saving = ref(false);

// Opsi keterangan obat (returnable) mengikuti legacy medicineDesc list.
const returnableOptions = [
  { value: 'Y', label: 'BISA DI RETUR' },
  { value: 'N', label: 'TIDAK BISA DI RETUR' }
];

// Opsi tipe obat (n_type) mengikuti legacy drugTypeList.
const drugTypeOptions = [
  { value: 1, label: 'PSIKOTROPIKA' },
  { value: 2, label: 'NARKOTIKA' },
  { value: 3, label: 'GENERIK' },
  { value: 4, label: 'PATEN' },
  { value: 5, label: 'BPJS' }
];

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

async function loadItems() {
  rows.value = await request('/master/item');
  groupOptions.value = await request('/master/item/group-options');
  measurementOptions.value = await request('/master/item/measurement-options');
  vendorOptions.value = await request('/master/item/vendor-options');
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await loadItems();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    itemCode: '',
    itemName: '',
    barcodeNo: '',
    itemGroupId: null,
    measurementId: null,
    itemReturnable: '',
    itemType: null,
    r: null,
    bufferLimit: null,
    plafon: null,
    maxOrder: null,
    supplierIds: []
  };
  selectedId.value = null;
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    itemCode: row.itemCode,
    itemName: row.itemName,
    barcodeNo: row.barcodeNo,
    itemGroupId: row.itemGroupId,
    measurementId: row.measurementId,
    itemReturnable: row.itemReturnable,
    itemType: row.itemType,
    r: row.r,
    bufferLimit: row.bufferLimit,
    plafon: row.plafon,
    maxOrder: row.maxOrder,
    supplierIds: supplierIdsFor(row)
  };
}

function supplierIdsFor(row) {
  if (!row.suppliers || !row.suppliers.length) return [];
  return vendorOptions.value
    .filter((vendor) => row.suppliers.includes(vendor.name))
    .map((vendor) => vendor.id);
}

async function doSave() {
  error.value = '';
  if (!form.value.itemCode) {
    error.value = 'Kode item harus diisi.';
    return;
  }
  if (!form.value.itemName) {
    error.value = 'Nama item harus diisi.';
    return;
  }
  if (!form.value.itemGroupId) {
    error.value = 'Group item harus dipilih.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/item/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    resetForm();
    await loadItems();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data item terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data item ini?')) {
    return;
  }
  try {
    await request(`/master/item/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadItems();
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
        <h2>📦 Item</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0038 — master item</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data item...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM MASTER ITEM</h3>
        <div class="form-grid">
          <div class="field">
            <label for="item-group">GROUP ITEM</label>
            <select id="item-group" v-model="form.itemGroupId">
              <option :value="null">-- PILIH --</option>
              <option v-for="opt in groupOptions" :key="opt.id" :value="opt.id">
                {{ opt.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="item-type">TIPE OBAT</label>
            <select id="item-type" v-model="form.itemType">
              <option :value="null">-- PILIH --</option>
              <option v-for="opt in drugTypeOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="item-code">KODE ITEM</label>
            <input
              id="item-code"
              v-model="form.itemCode"
              type="text"
              maxlength="15"
              placeholder="Kode item"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="item-name">NAMA ITEM</label>
            <input
              id="item-name"
              v-model="form.itemName"
              type="text"
              maxlength="50"
              placeholder="Nama item"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="item-measurement">SATUAN</label>
            <select id="item-measurement" v-model="form.measurementId">
              <option :value="null">-- PILIH --</option>
              <option v-for="opt in uniqueMeasurementOptions" :key="opt.id" :value="opt.id">
                {{ opt.endQuantify }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="item-barcode">KODE BARCODE</label>
            <input
              id="item-barcode"
              v-model="form.barcodeNo"
              type="text"
              maxlength="30"
              placeholder="Kode barcode"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="item-returnable">KETERANGAN OBAT</label>
            <select id="item-returnable" v-model="form.itemReturnable">
              <option value="">-- PILIH --</option>
              <option v-for="opt in returnableOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="item-plafon">BATAS PENGGUNAAN</label>
            <input
              id="item-plafon"
              v-model.number="form.plafon"
              type="number"
              placeholder="Batas penggunaan"
            />
          </div>
          <div class="field">
            <label for="item-supplier">SUPPLIER</label>
            <select id="item-supplier" v-model="form.supplierIds" multiple>
              <option v-for="opt in vendorOptions" :key="opt.id" :value="opt.id">
                {{ opt.name }}
              </option>
            </select>
            <span class="field-hint">Tekan Ctrl untuk memilih lebih dari satu supplier.</span>
          </div>
          <div class="field">
            <label for="item-buffer">BATAS STOK AKHIR</label>
            <input
              id="item-buffer"
              v-model.number="form.bufferLimit"
              type="number"
              placeholder="Batas stok akhir"
            />
          </div>
          <div class="field">
            <label for="item-r">JASA R</label>
            <input
              id="item-r"
              v-model.number="form.r"
              type="number"
              placeholder="Jasa R"
            />
          </div>
          <div class="field">
            <label for="item-max-order">MAX ORDER</label>
            <input
              id="item-max-order"
              v-model.number="form.maxOrder"
              type="number"
              placeholder="Max order"
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
        <h3 class="card-title">LIST ITEM</h3>
        <div class="search-bar">
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            placeholder="🔍 Cari kode, nama, group, supplier, atau satuan..."
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
                <th>NAMA</th>
                <th>GROUP</th>
                <th>SUPPLIER</th>
                <th>SATUAN</th>
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
                <td class="strong">{{ row.itemName }}</td>
                <td>{{ row.itemGroupName }}</td>
                <td>{{ (row.suppliers || []).join('; ') }}</td>
                <td>{{ row.measurementName }}</td>
              </tr>
              <tr v-if="!filteredRows.length">
                <td colspan="5" class="empty-state">
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

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card-title { margin: 0 0 16px; color: #304b73; font-size: 15px; }

.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field--full { grid-column: 1 / -1; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field input, .field select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input:focus, .field select:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }
.field select[multiple] { min-height: 90px; text-transform: none; }
.field-hint { font-size: 11px; color: #6b7280; }

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
