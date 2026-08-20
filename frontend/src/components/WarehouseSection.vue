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
  whouseCode: '',
  whouseName: '',
  whouseLoc: '',
  superiorId: null,
  coaId: null,
  coaNo: ''
});

const selectedId = ref(null);
const saving = ref(false);

// COA search
const coaSearchOpen = ref(false);
const coaKeyword = ref('');
const coaResults = ref([]);
const coaSearching = ref(false);

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

async function loadWarehouses() {
  rows.value = await request('/master/warehouse');
  warehouseOptions.value = await request('/master/warehouse/options');
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await loadWarehouses();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    whouseCode: '',
    whouseName: '',
    whouseLoc: '',
    superiorId: null,
    coaId: null,
    coaNo: ''
  };
  selectedId.value = null;
  coaKeyword.value = '';
  coaResults.value = [];
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    whouseCode: row.whouseCode,
    whouseName: row.whouseName,
    whouseLoc: row.whouseLoc,
    superiorId: row.superiorId,
    coaId: row.coaId,
    coaNo: row.coaNo
  };
}

async function searchCoa() {
  coaSearching.value = true;
  try {
    coaResults.value = await request(`/master/warehouse/coa-search?keyword=${encodeURIComponent(coaKeyword.value)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    coaSearching.value = false;
  }
}

function selectCoa(coa) {
  form.value.coaId = coa.id;
  form.value.coaNo = coa.coaNo;
  coaSearchOpen.value = false;
}

async function doSave() {
  error.value = '';
  if (!form.value.whouseCode) {
    error.value = 'Kode gudang harus diisi.';
    return;
  }
  if (!form.value.whouseName) {
    error.value = 'Nama gudang harus diisi.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/warehouse/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    resetForm();
    await loadWarehouses();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data gudang terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data gudang ini?')) {
    return;
  }
  try {
    await request(`/master/warehouse/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadWarehouses();
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
        <h2>🏬 Gudang</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data gudang...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM MASTER GUDANG</h3>
        <div class="form-grid">
          <div class="field">
            <label for="whouse-code">KODE</label>
            <input
              id="whouse-code"
              v-model="form.whouseCode"
              type="text"
              maxlength="15"
              placeholder="Kode gudang"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="whouse-name">NAMA</label>
            <input
              id="whouse-name"
              v-model="form.whouseName"
              type="text"
              maxlength="50"
              placeholder="Nama gudang"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="whouse-loc">LOKASI</label>
            <input
              id="whouse-loc"
              v-model="form.whouseLoc"
              type="text"
              maxlength="50"
              placeholder="Lokasi gudang"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="whouse-superior">GUDANG UTAMA</label>
            <select id="whouse-superior" v-model="form.superiorId">
              <option :value="null">-- PILIH --</option>
              <option v-for="opt in warehouseOptions" :key="opt.id" :value="opt.id">
                {{ opt.whouseCode }} - {{ opt.whouseName }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="whouse-coa">NO. COA</label>
            <div class="coa-field">
              <input
                id="whouse-coa"
                v-model="form.coaNo"
                type="text"
                readonly
                placeholder="Pilih COA"
                @focus="coaSearchOpen = true"
              />
              <button class="coa-button" type="button" @click="coaSearchOpen = !coaSearchOpen">🔍</button>
            </div>
          </div>
        </div>

        <!-- COA search popup -->
        <div v-if="coaSearchOpen" class="coa-popup">
          <div class="coa-popup-header">PENCARIAN DATA COA</div>
          <div class="coa-search-row">
            <input
              v-model="coaKeyword"
              type="text"
              placeholder="No. COA / Nama"
              @keyup.enter="searchCoa"
            />
            <button class="btn" type="button" :disabled="coaSearching" @click="searchCoa">
              {{ coaSearching ? '...' : 'CARI' }}
            </button>
          </div>
          <div class="coa-results">
            <table class="table">
              <thead>
                <tr>
                  <th>NO. COA</th>
                  <th>NAMA</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="coa in coaResults"
                  :key="coa.id"
                  @click="selectCoa(coa)"
                >
                  <td class="strong">{{ coa.coaNo }}</td>
                  <td>{{ coa.coaName }}</td>
                </tr>
                <tr v-if="!coaResults.length">
                  <td colspan="2" class="empty-state">Ketik kata kunci lalu tekan CARI.</td>
                </tr>
              </tbody>
            </table>
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
        <h3 class="card-title">LIST GUDANG</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>NAMA</th>
                <th>LOKASI</th>
                <th>GUDANG UTAMA</th>
                <th>NO. COA</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in paginatedRows"
                :key="row.id"
                :class="{ 'row--selected': selectedId === row.id }"
                @click="selectRow(row)"
              >
                <td class="strong">{{ row.whouseCode }}</td>
                <td class="strong">{{ row.whouseName }}</td>
                <td>{{ row.whouseLoc }}</td>
                <td>{{ row.superiorName }}</td>
                <td>{{ row.coaNo }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="5" class="empty-state">Tidak ada data gudang.</td>
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

.coa-field { display: flex; gap: 6px; }
.coa-field input { flex: 1; }
.coa-button { padding: 8px 12px; border: 1px solid #d1d9e6; border-radius: 6px; background: #eef3fb; cursor: pointer; }

.coa-popup { border: 1px solid #d1d9e6; border-radius: 8px; padding: 12px; margin-bottom: 16px; background: #f8fafc; }
.coa-popup-header { font-weight: 700; color: #304b73; margin-bottom: 10px; font-size: 13px; }
.coa-search-row { display: flex; gap: 8px; margin-bottom: 10px; }
.coa-search-row input { flex: 1; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.coa-results { max-height: 220px; overflow: auto; }

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
