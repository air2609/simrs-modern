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
const typeOptions = ref([]);
const parentOptions = ref([]);
const statusFilter = ref(0); // 0 = ALL, 1 = ACTIVE, 2 = INACTIVE
const typeFilter = ref(null); // null = ALL types
const keyword = ref('');



const form = ref({
  coaId: null,
  typeId: null,
  acctNo: '',
  acctName: '',
  active: true,
  balance: 0,
  supCoaId: null
});

const selectedId = ref(null);
const saving = ref(false);

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

async function loadCoa() {
  const params = new URLSearchParams();
  params.set('status', String(statusFilter.value));
  if (typeFilter.value != null) {
    params.set('typeId', String(typeFilter.value));
  }
  if (keyword.value.trim()) {
    params.set('keyword', keyword.value.trim());
  }
  rows.value = await request(`/accounting/coa?${params.toString()}`);
  currentPage.value = 1;
}



async function loadTypes() {
  typeOptions.value = await request('/accounting/coa/types');
}

async function loadParentOptions() {
  parentOptions.value = await request('/accounting/coa/parent-options');
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([loadCoa(), loadTypes(), loadParentOptions()]);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    coaId: null,
    typeId: null,
    acctNo: '',
    acctName: '',
    active: true,
    balance: 0,
    supCoaId: null
  };
  selectedId.value = null;
}

function selectRow(row) {
  selectedId.value = row.coaId;
  form.value = {
    coaId: row.coaId,
    typeId: row.typeId,
    acctNo: row.acctNo,
    acctName: row.acctName,
    active: row.status === 1,
    balance: row.balance || 0,
    supCoaId: row.supCoaId
  };
}

async function doSave() {
  error.value = '';
  if (!form.value.acctNo || !form.value.acctName) {
    error.value = 'Nomor akun dan nama akun harus diisi.';
    return;
  }
  if (!form.value.typeId) {
    error.value = 'Tipe akun harus dipilih.';
    return;
  }
  saving.value = true;
  try {
    await request('/accounting/coa/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    resetForm();
    await Promise.all([loadCoa(), loadParentOptions()]);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data COA terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data COA ini?')) {
    return;
  }
  try {
    await request(`/accounting/coa/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await Promise.all([loadCoa(), loadParentOptions()]);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function typeName(typeId) {
  const found = typeOptions.value.find((t) => t.typeId === typeId);
  return found ? found.typeName : (typeId ?? '-');
}

function parentLabel(row) {
  if (!row.supCoaId) return '-';
  const found = rows.value.find((r) => r.coaId === row.supCoaId);
  return found ? `${found.acctNo} - ${found.acctName}` : String(row.supCoaId);
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>📒 Chart of Account</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data COA...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM COA</h3>
        <div class="form-grid">
          <div class="field">
            <label for="coa-type">ACCOUNT TYPE</label>
            <select id="coa-type" v-model="form.typeId">
              <option :value="null" disabled>-- Pilih Tipe --</option>
              <option v-for="type in typeOptions" :key="type.typeId" :value="type.typeId">
                {{ type.typeName }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="coa-no">ACCOUNT NO</label>
            <input
              id="coa-no"
              v-model="form.acctNo"
              type="text"
              maxlength="20"
              placeholder="Nomor akun"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="coa-name">ACCOUNT NAME</label>
            <input
              id="coa-name"
              v-model="form.acctName"
              type="text"
              maxlength="100"
              placeholder="Nama akun"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="coa-status">STATUS</label>
            <select id="coa-status" v-model="form.active">
              <option :value="true">ACTIVE</option>
              <option :value="false">INACTIVE</option>
            </select>
          </div>
          <div class="field">
            <label for="coa-balance">BALANCE</label>
            <input
              id="coa-balance"
              v-model.number="form.balance"
              type="number"
              min="0"
              step="0.01"
              placeholder="0"
            />
          </div>
          <div class="field">
            <label for="coa-sup">SUB ACCOUNT OF</label>
            <select id="coa-sup" v-model="form.supCoaId">
              <option :value="null">-- Tidak Ada (Header) --</option>
              <option v-for="parent in parentOptions" :key="parent.coaId" :value="parent.coaId">
                {{ parent.acctNo }} - {{ parent.acctName }}
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
        <h3 class="card-title">DATA COA</h3>
        <div class="search-bar">
          <select v-model="statusFilter" class="status-filter" @change="loadCoa">
            <option :value="0">SEMUA STATUS</option>
            <option :value="1">ACTIVE</option>
            <option :value="2">INACTIVE</option>
          </select>
          <select v-model="typeFilter" class="status-filter" @change="loadCoa">
            <option :value="null">SEMUA TYPE</option>
            <option v-for="type in typeOptions" :key="type.typeId" :value="type.typeId">
              {{ type.typeName }}
            </option>
          </select>
          <input
            v-model="keyword"
            class="search-input"
            type="text"
            placeholder="Cari No. COA / Nama..."
            @keyup.enter="loadCoa"
          />
          <button class="small-button" type="button" @click="loadCoa">🔍 CARI</button>
          <button v-if="keyword" class="small-button" type="button" @click="keyword = ''; loadCoa()">✖ Bersihkan</button>
        </div>


        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>ACCOUNT NO</th>
                <th>ACCOUNT NAME</th>
                <th>TYPE</th>
                <th>SUB ACCOUNT OF</th>
                <th>BALANCE</th>
                <th>STATUS</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="row in paginatedRows" :key="row.coaId">
                <tr
                  :class="{ 'row--selected': selectedId === row.coaId }"
                  @click="selectRow(row)"
                >
                  <td class="strong">{{ row.acctNo }}</td>
                  <td>{{ row.acctName }}</td>
                  <td>{{ row.typeName || typeName(row.typeId) }}</td>
                  <td>{{ parentLabel(row) }}</td>
                  <td class="num">{{ formatCurrency(row.balance) }}</td>
                  <td>
                    <span :class="row.status === 1 ? 'badge badge--active' : 'badge badge--inactive'">
                      {{ row.statusLabel || (row.status === 1 ? 'ACTIVE' : 'INACTIVE') }}
                    </span>
                  </td>
                </tr>
                <tr
                  v-for="child in row.children"
                  :key="child.coaId"
                  class="child-row"
                  :class="{ 'row--selected': selectedId === child.coaId }"
                  @click="selectRow(child)"
                >
                  <td class="strong child-indent">↳ {{ child.acctNo }}</td>
                  <td>{{ child.acctName }}</td>
                  <td>{{ child.typeName || typeName(child.typeId) }}</td>
                  <td>{{ parentLabel(child) }}</td>
                  <td class="num">{{ formatCurrency(child.balance) }}</td>
                  <td>
                    <span :class="child.status === 1 ? 'badge badge--active' : 'badge badge--inactive'">
                      {{ child.statusLabel || (child.status === 1 ? 'ACTIVE' : 'INACTIVE') }}
                    </span>
                  </td>
                </tr>
              </template>
              <tr v-if="!rows.length">
                <td colspan="6" class="empty-state">Tidak ada data COA.</td>
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
.field input, .field select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.field input { text-transform: uppercase; }
.field input:focus, .field select:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.form-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.btn:hover { background: #f6f8fb; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.btn--primary:hover { background: #1e40af; }
.btn--danger { background: #fff; border-color: #f0b3bd; color: #a32943; }
.btn--danger:hover { background: #fde8ea; }

.search-bar { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; align-items: center; }
.status-filter { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.search-input { flex: 1; min-width: 200px; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.search-input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }


.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 14px; }

.table th, .table td { padding: 10px 12px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr { cursor: pointer; }
.table tbody tr:hover { background: #f6f8fb; }
.row--selected { background: #e8f0fe; }
.child-row { background: #fafbfd; }
.child-indent { padding-left: 28px; }

.strong { font-weight: 700; }
.num { text-align: right; white-space: nowrap; }
.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

.badge { padding: 3px 8px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.badge--active { background: #e6f7ee; color: #0f7a3d; }
.badge--inactive { background: #fde8ea; color: #a32943; }

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
