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
const classOptions = ref([]);
const coaResults = ref([]);
const coaSearching = ref(false);
const searchKeyword = ref('');
const searching = ref(false);


const form = ref({
  treatmentFeeId: null,
  treatmentId: null,
  code: '',
  name: '',
  treatmentGroupId: null,
  treatmentClassId: null,
  hospitalFee: 0,
  doctorFee: 0,
  medicFee: 0,
  totalFee: 0,
  coaId: null
});

const selectedId = ref(null);
const saving = ref(false);

const pageSize = 10;
const currentPage = ref(1);

const totalFee = computed(() => {
  const hospital = Number(form.value.hospitalFee) || 0;
  const doctor = Number(form.value.doctorFee) || 0;
  const medic = Number(form.value.medicFee) || 0;
  return hospital + doctor + medic;
});

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

async function loadTreatments() {
  rows.value = await request('/master/treatment');
  currentPage.value = 1;
}


async function loadOptions() {
  const [groups, classes] = await Promise.all([
    request('/master/treatment/group-options'),
    request('/master/treatment/class-options')
  ]);
  groupOptions.value = groups;
  classOptions.value = classes;
}

async function doSearch() {
  error.value = '';
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    await loadTreatments();
    return;
  }
  searching.value = true;
  try {
    rows.value = await request(`/master/treatment/search?keyword=${encodeURIComponent(keyword)}`);
    currentPage.value = 1;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    searching.value = false;
  }

}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([loadTreatments(), loadOptions()]);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}


function resetForm() {
  form.value = {
    treatmentFeeId: null,
    treatmentId: null,
    code: '',
    name: '',
    treatmentGroupId: null,
    treatmentClassId: null,
    hospitalFee: 0,
    doctorFee: 0,
    medicFee: 0,
    totalFee: 0,
    coaId: null
  };
  selectedId.value = null;
  coaResults.value = [];
}

function selectRow(row) {
  selectedId.value = row.treatmentFeeId;
  form.value = {
    treatmentFeeId: row.treatmentFeeId,
    treatmentId: row.treatmentId,
    code: row.code,
    name: row.name,
    treatmentGroupId: row.treatmentGroupId,
    treatmentClassId: row.treatmentClassId,
    hospitalFee: row.hospitalFee || 0,
    doctorFee: row.doctorFee || 0,
    medicFee: row.medicFee || 0,
    totalFee: row.totalFee || 0,
    coaId: row.coaId
  };
  coaResults.value = [];
}

async function searchCoa() {
  const keyword = form.value.coaKeyword;
  if (!keyword || !keyword.trim()) {
    coaResults.value = [];
    return;
  }
  coaSearching.value = true;
  try {
    coaResults.value = await request(`/master/treatment/coa-search?keyword=${encodeURIComponent(keyword.trim())}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    coaSearching.value = false;
  }
}

function selectCoa(coa) {
  form.value.coaId = coa.id;
  form.value.coaKeyword = `${coa.coaNo} - ${coa.coaName}`;
  coaResults.value = [];
}

async function doSave() {
  error.value = '';
  if (!form.value.code || !form.value.name) {
    error.value = 'Kode dan Nama tindakan harus diisi.';
    return;
  }
  if (!form.value.treatmentGroupId) {
    error.value = 'Group tindakan harus dipilih.';
    return;
  }
  if (!form.value.treatmentClassId) {
    error.value = 'Kelas tarif harus dipilih.';
    return;
  }
  form.value.totalFee = totalFee.value;
  saving.value = true;
  try {
    await request('/master/treatment/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    resetForm();
    await loadTreatments();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data treatment terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data treatment ini?')) {
    return;
  }
  try {
    await request(`/master/treatment/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadTreatments();
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
        <h2>🩺 Treatment Master</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0026 — master tindakan perawatan</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data treatment...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM TREATMENT</h3>
        <div class="form-grid">
          <div class="field">
            <label for="treatment-code">KODE</label>
            <input
              id="treatment-code"
              v-model="form.code"
              type="text"
              maxlength="15"
              placeholder="Kode tindakan"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="treatment-name">NAMA TINDAKAN</label>
            <input
              id="treatment-name"
              v-model="form.name"
              type="text"
              maxlength="100"
              placeholder="Nama tindakan"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="treatment-group">GROUP TINDAKAN</label>
            <select id="treatment-group" v-model="form.treatmentGroupId">
              <option :value="null" disabled>-- Pilih Group --</option>
              <option v-for="group in groupOptions" :key="group.id" :value="group.id">
                {{ group.code }} - {{ group.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="treatment-class">KELAS TARIF</label>
            <select id="treatment-class" v-model="form.treatmentClassId">
              <option :value="null" disabled>-- Pilih Kelas --</option>
              <option v-for="tclass in classOptions" :key="tclass.id" :value="tclass.id">
                {{ tclass.code }} - {{ tclass.description }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="hospital-fee">JASA RS</label>
            <input
              id="hospital-fee"
              v-model.number="form.hospitalFee"
              type="number"
              min="0"
              step="0.01"
              placeholder="0"
            />
          </div>
          <div class="field">
            <label for="doctor-fee">JASA DOKTER</label>
            <input
              id="doctor-fee"
              v-model.number="form.doctorFee"
              type="number"
              min="0"
              step="0.01"
              placeholder="0"
            />
          </div>
          <div class="field">
            <label for="medic-fee">JASA MEDIK</label>
            <input
              id="medic-fee"
              v-model.number="form.medicFee"
              type="number"
              min="0"
              step="0.01"
              placeholder="0"
            />
          </div>
          <div class="field">
            <label for="total-fee">TOTAL BIAYA</label>
            <input id="total-fee" :value="formatCurrency(totalFee)" type="text" readonly />
          </div>
          <div class="field">
            <label for="coa-search">NO. COA</label>

            <div class="coa-search">
              <input
                id="coa-search"
                v-model="form.coaKeyword"
                type="text"
                placeholder="Ketik kode/nama COA lalu Enter"
                @keyup.enter="searchCoa"
              />
              <button class="small-button" type="button" :disabled="coaSearching" @click="searchCoa">
                🔍 Cari
              </button>
            </div>
            <div v-if="coaResults.length" class="coa-results">
              <div
                v-for="coa in coaResults"
                :key="coa.id"
                class="coa-result-item"
                @click="selectCoa(coa)"
              >
                <span class="strong">{{ coa.coaNo }}</span> - {{ coa.coaName }}
              </div>
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
        <h3 class="card-title">DATA TREATMENT</h3>
        <div class="search-bar">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="Cari kode / nama tindakan / kelas tarif..."
            @keyup.enter="doSearch"
          />
          <button class="small-button" type="button" :disabled="searching" @click="doSearch">
            🔍 CARI
          </button>
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
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in paginatedRows"
                :key="row.treatmentFeeId"
                :class="{ 'row--selected': selectedId === row.treatmentFeeId }"
                @click="selectRow(row)"
              >
                <td class="strong">{{ row.code }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.treatmentClassDesc || '-' }}</td>
                <td class="num">{{ formatCurrency(row.hospitalFee) }}</td>
                <td class="num">{{ formatCurrency(row.doctorFee) }}</td>
                <td class="num">{{ formatCurrency(row.medicFee) }}</td>
                <td class="num strong">{{ formatCurrency(row.totalFee) }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="7" class="empty-state">Tidak ada data treatment.</td>
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
.field--wide { grid-column: 1 / -1; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field input, .field select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.field input { text-transform: uppercase; }
.field input:focus, .field select:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }
.field input[readonly] { background: #f6f8fb; color: #304b73; font-weight: 700; }

.coa-search { display: flex; gap: 8px; }
.coa-search input { flex: 1; }
.coa-results { border: 1px solid #d1d9e6; border-radius: 6px; max-height: 160px; overflow-y: auto; background: #fff; }
.coa-result-item { padding: 8px 10px; cursor: pointer; font-size: 13px; border-bottom: 1px solid #eef2f7; }
.coa-result-item:hover { background: #e8f0fe; }

.form-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.btn:hover { background: #f6f8fb; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.btn--primary:hover { background: #1e40af; }
.btn--danger { background: #fff; border-color: #f0b3bd; color: #a32943; }
.btn--danger:hover { background: #fde8ea; }

.search-bar { display: flex; gap: 8px; margin-bottom: 12px; }
.search-bar input { flex: 1; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.search-bar input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 14px; }

.table th, .table td { padding: 10px 12px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr { cursor: pointer; }
.table tbody tr:hover { background: #f6f8fb; }
.row--selected { background: #e8f0fe; }

.strong { font-weight: 700; }
.num { text-align: right; white-space: nowrap; }
.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

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


