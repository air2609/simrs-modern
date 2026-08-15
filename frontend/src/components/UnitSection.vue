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

const divisionOptions = ref([]);
const warehouseOptions = ref([]);
const coaOptions = ref([]);
const coaKeyword = ref('');

const form = ref({
  id: null,
  code: '',
  name: '',
  divisionId: null,
  unitType: 1,
  warehouseId: null,
  coaId: null
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

async function loadUnits() {
  rows.value = await request('/master/unit');
  currentPage.value = 1;
}

async function loadDivisionOptions() {
  divisionOptions.value = await request('/master/unit/division-options');
}

async function loadWarehouseOptions() {
  warehouseOptions.value = await request('/master/unit/warehouse-options');
}

async function searchCoa() {
  const keyword = coaKeyword.value.trim();
  coaOptions.value = await request(`/master/unit/coa-search?keyword=${encodeURIComponent(keyword)}`);
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([loadUnits(), loadDivisionOptions(), loadWarehouseOptions()]);
    await searchCoa();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    code: '',
    name: '',
    divisionId: null,
    unitType: 1,
    warehouseId: null,
    coaId: null
  };
  selectedId.value = null;
  coaKeyword.value = '';
  coaOptions.value = [];
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    code: row.code,
    name: row.name,
    divisionId: row.divisionId,
    unitType: row.unitType ?? 1,
    warehouseId: row.warehouseId,
    coaId: row.coaId
  };
  coaKeyword.value = row.coaNo || '';
}

function unitTypeLabel(value) {
  return value === 0 ? 'NON TRANSAKSIONAL' : 'TRANSAKSIONAL';
}

function divisionName(divisionId) {
  const found = divisionOptions.value.find((option) => option.id === divisionId);
  return found ? found.name : '';
}

function warehouseName(warehouseId) {
  const found = warehouseOptions.value.find((option) => option.id === warehouseId);
  return found ? found.whouseName : '';
}

async function doSave() {
  error.value = '';
  if (!form.value.code || !form.value.name) {
    error.value = 'Kode dan Nama Unit harus diisi.';
    return;
  }
  if (!form.value.divisionId) {
    error.value = 'Divisi harus dipilih.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/unit/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    resetForm();
    await loadUnits();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data unit terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data unit ini?')) {
    return;
  }
  try {
    await request(`/master/unit/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadUnits();
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
        <h2>🏢 Unit</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0024 — master unit divisi</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data unit...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM UNIT DIVISI</h3>
        <div class="form-grid">
          <div class="field">
            <label for="unit-code">KODE</label>
            <input
              id="unit-code"
              v-model="form.code"
              type="text"
              maxlength="15"
              placeholder="Kode unit"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="unit-division">DIVISI</label>
            <select id="unit-division" v-model="form.divisionId">
              <option :value="null" disabled>-- Pilih Divisi --</option>
              <option v-for="option in divisionOptions" :key="option.id" :value="option.id">
                {{ option.code }} - {{ option.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="unit-name">NAMA UNIT</label>
            <input
              id="unit-name"
              v-model="form.name"
              type="text"
              maxlength="50"
              placeholder="Nama unit"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="unit-type">TYPE UNIT</label>
            <select id="unit-type" v-model.number="form.unitType">
              <option :value="1">TRANSAKSIONAL</option>
              <option :value="0">NON TRANSAKSIONAL</option>
            </select>
          </div>
          <div class="field">
            <label for="unit-warehouse">GUDANG UNIT</label>
            <select id="unit-warehouse" v-model="form.warehouseId">
              <option :value="null">-- Pilih Gudang --</option>
              <option v-for="option in warehouseOptions" :key="option.id" :value="option.id">
                {{ option.whouseCode }} - {{ option.whouseName }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="unit-coa">NO. COA</label>
            <div class="coa-search">
              <input
                id="unit-coa"
                v-model="coaKeyword"
                type="text"
                placeholder="Cari no. coa"
                @keyup.enter="searchCoa"
              />
              <button class="small-button" type="button" @click="searchCoa">CARI</button>
            </div>
            <select v-if="coaOptions.length" v-model="form.coaId" class="coa-select">
              <option :value="null">-- Pilih COA --</option>
              <option v-for="option in coaOptions" :key="option.coaId" :value="option.coaId">
                {{ option.acctNo }} - {{ option.acctName }}
              </option>
            </select>
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
        <h3 class="card-title">DATA UNIT DIVISI</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>DIVISI</th>
                <th>NAMA UNIT</th>
                <th>TYPE UNIT</th>
                <th>GUDANG</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in paginatedRows"
                :key="row.id"
                :class="{ 'row--selected': selectedId === row.id }"
                @click="selectRow(row)"
              >
                <td class="strong">{{ row.code }}</td>
                <td>{{ divisionName(row.divisionId) }}</td>
                <td>{{ row.name }}</td>
                <td>{{ unitTypeLabel(row.unitType) }}</td>
                <td>{{ warehouseName(row.warehouseId) }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="5" class="empty-state">Tidak ada data unit.</td>
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

.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin-bottom: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field input, .field select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input:focus, .field select:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.coa-search { display: flex; gap: 6px; }
.coa-search input { flex: 1; }
.coa-select { margin-top: 6px; }

.form-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.btn:hover { background: #f6f8fb; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.btn--primary:hover { background: #1e40af; }
.btn--danger { background: #fff; border-color: #f0b3bd; color: #a32943; }
.btn--danger:hover { background: #fde8ea; }

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
