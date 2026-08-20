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
const medicStaffGroupOptions = ref([]);
const levelOfExpertiseOptions = ref([]);
const statusOptions = ref([]);

const coaKeyword = ref('');
const coaSearchOpen = ref(false);
const coaSearching = ref(false);

const form = ref({
  id: null,
  staffId: null,
  code: '',
  name: '',
  address: '',
  phone: '',
  coaId: null,
  staffGroup: 4,
  levelOfExpertise: '',
  status: '',
  outPatientEarnings: null,
  bankAccNo: '',
  assistenOf: null,
  assistenOfName: '',
  percentageInPatientWage: null,
  docType: null,
  flagAntrian: null,
  hiredDate: '',
  firedDate: '',
  salary: null,
  unitId: null,
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

async function loadDoctors() {
  rows.value = await request('/master/doctor');
  currentPage.value = 1;
}

async function loadMasters() {
  const masters = await request('/master/doctor/masters');
  unitOptions.value = masters.unitOptions || [];
  coaOptions.value = masters.coaOptions || [];
  medicStaffGroupOptions.value = masters.medicStaffGroupOptions || [];
  levelOfExpertiseOptions.value = masters.levelOfExpertiseOptions || [];
  statusOptions.value = masters.statusOptions || [];
}

async function searchCoa() {
  const keyword = coaKeyword.value;
  if (!keyword || !keyword.trim()) {
    coaOptions.value = [];
    return;
  }
  coaSearching.value = true;
  try {
    coaOptions.value = await request(`/master/doctor/coa-search?keyword=${encodeURIComponent(keyword.trim())}`);
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
    await Promise.all([loadDoctors(), loadMasters()]);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    staffId: null,
    code: '',
    name: '',
    address: '',
    phone: '',
    coaId: null,
    staffGroup: 4,
    levelOfExpertise: '',
    status: '',
    outPatientEarnings: null,
    bankAccNo: '',
    assistenOf: null,
    assistenOfName: '',
    percentageInPatientWage: null,
    docType: null,
    flagAntrian: null,
    hiredDate: '',
    firedDate: '',
    salary: null,
    unitId: null,
    unitIds: [],
    coaKeyword: ''
  };
  selectedId.value = null;
  coaKeyword.value = '';
  coaOptions.value = [];
  coaSearchOpen.value = false;
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    staffId: row.staffId,
    code: row.code,
    name: row.name,
    address: row.address,
    phone: row.phone,
    coaId: row.coaId,
    staffGroup: row.staffGroup ?? 4,
    levelOfExpertise: row.levelOfExpertise || '',
    status: row.status || '',
    outPatientEarnings: row.outPatientEarnings,
    bankAccNo: row.bankAccNo || '',
    assistenOf: row.assistenOf,
    assistenOfName: row.assistenOfName || '',
    percentageInPatientWage: row.percentageInPatientWage,
    docType: row.docType,
    flagAntrian: row.flagAntrian,
    hiredDate: row.hiredDate || '',
    firedDate: row.firedDate || '',
    salary: null,
    unitId: (row.unitIds && row.unitIds.length) ? row.unitIds[0] : null,
    unitIds: row.unitIds || [],
    coaKeyword: row.coaNo || ''
  };
  coaOptions.value = [];
}

function unitName(unitId) {
  const found = unitOptions.value.find((option) => option.id === unitId);
  return found ? found.name : '';
}

function staffGroupName(groupId) {
  const found = medicStaffGroupOptions.value.find((option) => option.id === groupId);
  return found ? found.name : '';
}

async function doSave() {
  error.value = '';
  if (!form.value.code || !form.value.name) {
    error.value = 'Kode dan Nama Dokter harus diisi.';
    return;
  }
  if (!form.value.unitId) {
    error.value = 'Unit harus dipilih.';
    return;
  }
  saving.value = true;
  try {
    const payload = { ...form.value };
    delete payload.coaKeyword;
    delete payload.assistenOfName;
    await request('/master/doctor/save', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    resetForm();
    await loadDoctors();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data dokter terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data dokter ini?')) {
    return;
  }
  try {
    await request(`/master/doctor/delete?staffId=${form.value.staffId}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadDoctors();
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
        <h2>🩺 Master Dokter</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data dokter...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM DOKTER</h3>
        <div class="form-grid">
          <div class="field">
            <label for="doctor-code">KODE STAFF</label>
            <input
              id="doctor-code"
              v-model="form.code"
              type="text"
              maxlength="15"
              placeholder="Kode staff"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="doctor-name">NAMA</label>
            <input
              id="doctor-name"
              v-model="form.name"
              type="text"
              maxlength="50"
              placeholder="Nama dokter"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="doctor-unit">UNIT</label>
            <select id="doctor-unit" v-model="form.unitId">
              <option :value="null" disabled>-- Pilih Unit --</option>
              <option v-for="option in unitOptions" :key="option.id" :value="option.id">
                {{ option.code }} - {{ option.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="doctor-group">GROUP STAFF MEDIS</label>
            <select id="doctor-group" v-model.number="form.staffGroup">
              <option v-for="option in medicStaffGroupOptions" :key="option.id" :value="option.id">
                {{ option.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="doctor-level">TINGKAT KEAHLIAN</label>
            <select id="doctor-level" v-model="form.levelOfExpertise">
              <option value="" disabled>-- Pilih --</option>
              <option v-for="option in levelOfExpertiseOptions" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="doctor-status">STATUS</label>
            <select id="doctor-status" v-model="form.status">
              <option v-for="option in statusOptions" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="doctor-address">ALAMAT</label>
            <input
              id="doctor-address"
              v-model="form.address"
              type="text"
              maxlength="100"
              placeholder="Alamat"
            />
          </div>
          <div class="field">
            <label for="doctor-phone">NO. TELP</label>
            <input
              id="doctor-phone"
              v-model="form.phone"
              type="text"
              maxlength="30"
              placeholder="No. telp"
            />
          </div>
          <div class="field">
            <label for="doctor-salary">GAJI</label>
            <input
              id="doctor-salary"
              v-model.number="form.salary"
              type="number"
              placeholder="Gaji"
            />
          </div>
          <div class="field">
            <label for="doctor-hired">TGL. MASUK</label>
            <input id="doctor-hired" v-model="form.hiredDate" type="date" />
          </div>
          <div class="field">
            <label for="doctor-fired">TGL. KELUAR</label>
            <input id="doctor-fired" v-model="form.firedDate" type="date" />
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
            <label for="doctor-outpatient">PENDAPATAN RAWAT JALAN</label>
            <input
              id="doctor-outpatient"
              v-model.number="form.outPatientEarnings"
              type="number"
              placeholder="Pendapatan rawat jalan"
            />
          </div>
          <div class="field">
            <label for="doctor-percentage">% GAJI RAWAT INAP</label>
            <input
              id="doctor-percentage"
              v-model.number="form.percentageInPatientWage"
              type="number"
              placeholder="Persentase"
            />
          </div>
          <div class="field">
            <label for="doctor-bank">NO. REKENING</label>
            <input
              id="doctor-bank"
              v-model="form.bankAccNo"
              type="text"
              maxlength="30"
              placeholder="No. rekening"
            />
          </div>
          <div class="field">
            <label for="doctor-assisten">ASISTEN DARI</label>
            <input
              id="doctor-assisten"
              :value="form.assistenOfName"
              type="text"
              placeholder="Asisten dari"
              readonly
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
        <h3 class="card-title">DATA DOKTER</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE STAFF</th>
                <th>NAMA</th>
                <th>BIDANG</th>
                <th>STATUS</th>
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
                <td>{{ row.name }}</td>
                <td>{{ unitName(row.unitIds && row.unitIds[0]) }}</td>
                <td>{{ row.status }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="4" class="empty-state">Tidak ada data dokter.</td>
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
