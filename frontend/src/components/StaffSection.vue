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

const unitOptions = ref([]);
const coaOptions = ref([]);

const coaKeyword = ref('');
const coaSearchOpen = ref(false);
const coaSearching = ref(false);

const form = ref({
  staffId: null,
  code: '',
  name: '',
  address: '',
  phone: '',
  coaId: null,
  salary: null,
  hiredDate: '',
  firedDate: '',
  unitIds: [],
  coaKeyword: ''
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

async function loadStaffs() {
  rows.value = await request('/master/staff');
  currentPage.value = 1;
}

async function loadMasters() {
  const masters = await request('/master/staff/masters');
  unitOptions.value = masters.unitOptions || [];
  coaOptions.value = masters.coaOptions || [];
}

async function searchCoa() {
  const keyword = coaKeyword.value;
  if (!keyword || !keyword.trim()) {
    coaOptions.value = [];
    return;
  }
  coaSearching.value = true;
  try {
    coaOptions.value = await request(`/master/staff/coa-search?keyword=${encodeURIComponent(keyword.trim())}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    coaSearching.value = false;
  }
}

function selectCoa(coa) {
  form.value.coaId = coa.id;
  form.value.coaKeyword = `${coa.no} - ${coa.name}`;
  coaOptions.value = [];
  coaSearchOpen.value = false;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([loadStaffs(), loadMasters()]);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    staffId: null,
    code: '',
    name: '',
    address: '',
    phone: '',
    coaId: null,
    salary: null,
    hiredDate: '',
    firedDate: '',
    unitIds: [],
    coaKeyword: ''
  };
  selectedId.value = null;
  coaKeyword.value = '';
  coaOptions.value = [];
  coaSearchOpen.value = false;
}

function selectRow(row) {
  selectedId.value = row.staffId;
  form.value = {
    staffId: row.staffId,
    code: row.code,
    name: row.name,
    address: row.address,
    phone: row.phone,
    coaId: row.coaId,
    salary: row.salary,
    hiredDate: row.hiredDate || '',
    firedDate: row.firedDate || '',
    unitIds: row.unitIds || [],
    coaKeyword: row.coaNo || ''
  };
  coaOptions.value = [];
}

function unitName(unitId) {
  const found = unitOptions.value.find((option) => option.id === unitId);
  return found ? found.name : '';
}

function toggleUnit(unitId) {
  const index = form.value.unitIds.indexOf(unitId);
  if (index >= 0) {
    form.value.unitIds.splice(index, 1);
  } else {
    form.value.unitIds.push(unitId);
  }
}

async function doSave() {
  error.value = '';
  if (!form.value.code || !form.value.name) {
    error.value = 'Kode dan Nama staff harus diisi.';
    return;
  }
  if (!form.value.unitIds.length) {
    error.value = 'Sub divisi harus dipilih.';
    return;
  }
  saving.value = true;
  try {
    const payload = { ...form.value };
    delete payload.coaKeyword;
    await request('/master/staff/save', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    resetForm();
    await loadStaffs();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data staff terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data staff ini?')) {
    return;
  }
  try {
    await request(`/master/staff/delete?staffId=${form.value.staffId}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadStaffs();
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
        <h2>👥 Master Staff</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0031 — master staff</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data staff...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM DATA STAFF</h3>
        <div class="form-grid">
          <div class="field">
            <label for="staff-code">KODE STAFF</label>
            <input
              id="staff-code"
              v-model="form.code"
              type="text"
              maxlength="15"
              placeholder="Kode staff"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="staff-name">NAMA</label>
            <input
              id="staff-name"
              v-model="form.name"
              type="text"
              maxlength="50"
              placeholder="Nama staff"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="coa-search">NO. COA</label>
            <div class="bandbox">
              <input
                id="coa-search"
                v-model="form.coaKeyword"
                type="text"
                placeholder="Pilih COA"
                readonly
                @focus="coaSearchOpen = true"
              />
              <button class="bandbox-btn" type="button" @click="coaSearchOpen = !coaSearchOpen">▾</button>
            </div>
            <div v-if="coaSearchOpen" class="bandbox-popup">
              <div class="bandbox-search">
                <input
                  v-model="coaKeyword"
                  type="text"
                  placeholder="Cari kode/nama COA"
                  @keyup.enter="searchCoa"
                />
                <button class="small-button" type="button" :disabled="coaSearching" @click="searchCoa">
                  CARI
                </button>
              </div>
              <table class="table bandbox-table">
                <thead>
                  <tr>
                    <th>NO. COA</th>
                    <th>NAMA COA</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="coa in coaOptions"
                    :key="coa.id"
                    @click="selectCoa(coa)"
                  >
                    <td class="strong">{{ coa.no }}</td>
                    <td>{{ coa.name }}</td>
                  </tr>

                  <tr v-if="!coaOptions.length">
                    <td colspan="2" class="empty-state">Ketik kode/nama COA lalu tekan CARI.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label for="staff-address">ALAMAT</label>
            <input
              id="staff-address"
              v-model="form.address"
              type="text"
              maxlength="100"
              placeholder="Alamat"
            />
          </div>
          <div class="field">
            <label for="staff-phone">NO. TELP / NO. HP</label>
            <input
              id="staff-phone"
              v-model="form.phone"
              type="text"
              maxlength="30"
              placeholder="No. telp / HP"
            />
          </div>
          <div class="field">
            <label for="staff-salary">GAJI</label>
            <input
              id="staff-salary"
              v-model.number="form.salary"
              type="number"
              placeholder="Gaji"
            />
          </div>
          <div class="field">
            <label for="staff-hired">TANGGAL MASUK</label>
            <input id="staff-hired" v-model="form.hiredDate" type="date" />
          </div>
          <div class="field">
            <label for="staff-fired">TANGGAL KELUAR</label>
            <input id="staff-fired" v-model="form.firedDate" type="date" />
          </div>
          <div class="field field--full">
            <label>SUB DIVISI</label>
            <div class="unit-checkbox-grid">
              <label
                v-for="option in unitOptions"
                :key="option.id"
                class="unit-checkbox"
                :class="{ 'unit-checkbox--checked': form.unitIds.includes(option.id) }"
              >
                <input
                  type="checkbox"
                  :checked="form.unitIds.includes(option.id)"
                  @change="toggleUnit(option.id)"
                />
                <span>{{ option.code }} - {{ option.name }}</span>
              </label>
            </div>
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
        <h3 class="card-title">DAFTAR STAFF</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>NAMA</th>
                <th>SUB DIVISI</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in paginatedRows"
                :key="row.staffId"
                :class="{ 'row--selected': selectedId === row.staffId }"
                @click="selectRow(row)"
              >
                <td class="strong">{{ row.code }}</td>
                <td>{{ row.name }}</td>
                <td>{{ (row.unitNames || []).join(', ') }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="3" class="empty-state">Tidak ada data staff.</td>
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
.field--full { grid-column: 1 / -1; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field input, .field select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.field input { text-transform: uppercase; }
.field input:focus, .field select:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }
.field input[readonly] { background: #f6f8fb; color: #6b7280; }

.bandbox { display: flex; align-items: stretch; }
.bandbox input { flex: 1; border-top-right-radius: 0; border-bottom-right-radius: 0; }
.bandbox-btn { padding: 0 12px; border: 1px solid #d1d9e6; border-left: none; border-radius: 0 6px 6px 0; background: #f6f8fb; cursor: pointer; }

.bandbox-popup { border: 1px solid #d1d9e6; border-radius: 8px; padding: 10px; background: #fff; box-shadow: 0 8px 20px rgba(0,0,0,0.12); margin-top: 4px; }
.bandbox-search { display: flex; gap: 8px; margin-bottom: 8px; }
.bandbox-search input { flex: 1; text-transform: uppercase; }
.bandbox-table { font-size: 13px; }
.bandbox-table tbody tr { cursor: pointer; }
.bandbox-table tbody tr:hover { background: #f6f8fb; }

.unit-checkbox-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.unit-checkbox { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; font-size: 13px; }
.unit-checkbox:hover { background: #f6f8fb; }
.unit-checkbox--checked { background: #e8f0fe; border-color: #1d4ed8; }

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
