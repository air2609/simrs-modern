<script setup>
import { onMounted, reactive, ref } from 'vue';

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

const units = ref([]);
const selectedUnitId = ref('');

const mrCodeInput = ref('');
const fileList = ref([]);
const selectedMrCodes = ref([]);

const searching = ref(false);
const showSearchDialog = ref(false);
const searchResults = ref([]);
const searchForm = reactive({
  mrCode: '',
  patientName: '',
  nik: '',
  birthDate: '',
  address: ''
});

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
  const data = await request('/mr/borrow-request/units');
  units.value = data || [];
  if (units.value.length) {
    selectedUnitId.value = units.value[0].unitId;
  }
}

async function initialize() {
  loading.value = true;
  error.value = '';
  success.value = '';
  try {
    await loadUnits();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function addToFileList(item) {
  if (fileList.value.some((existing) => existing.mrCode === item.mrCode)) {
    return;
  }
  fileList.value.push(item);
}

async function getMrByCode() {
  error.value = '';
  success.value = '';
  if (!mrCodeInput.value || mrCodeInput.value.trim().length < 2) {
    return;
  }
  try {
    const item = await request(`/mr/borrow-request/lookup?code=${encodeURIComponent(mrCodeInput.value.trim())}`);
    addToFileList(item);
    mrCodeInput.value = '';
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function openSearchDialog() {
  showSearchDialog.value = true;
  searchResults.value = [];
}

function closeSearchDialog() {
  showSearchDialog.value = false;
}

async function runSearch() {
  error.value = '';
  searching.value = true;
  try {
    const params = new URLSearchParams();
    if (searchForm.mrCode) params.set('mrCode', searchForm.mrCode);
    if (searchForm.patientName) params.set('patientName', searchForm.patientName);
    if (searchForm.nik) params.set('nik', searchForm.nik);
    if (searchForm.birthDate) params.set('birthDate', searchForm.birthDate);
    if (searchForm.address) params.set('address', searchForm.address);
    searchResults.value = await request(`/mr/borrow-request/search?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    searching.value = false;
  }
}

function pickSearchResult(item) {
  addToFileList(item);
  closeSearchDialog();
}

function toggleSelected(mrCode) {
  const index = selectedMrCodes.value.indexOf(mrCode);
  if (index >= 0) {
    selectedMrCodes.value.splice(index, 1);
  } else {
    selectedMrCodes.value.push(mrCode);
  }
}

function clearFileList() {
  fileList.value = [];
  selectedMrCodes.value = [];
}

async function requestBorrow() {
  error.value = '';
  success.value = '';

  if (!selectedUnitId.value) {
    error.value = 'LOKASI harus dipilih.';
    return;
  }
  if (!selectedMrCodes.value.length) {
    return;
  }

  try {
    const result = await request('/mr/borrow-request', {
      method: 'POST',
      body: JSON.stringify({
        unitId: selectedUnitId.value,
        mrCodes: selectedMrCodes.value
      })
    });

    if (result.failedCodes && result.failedCodes.length) {
      error.value = `Berkas Rekam Medis dengan Kode ${result.failedCodes.join(', ')} Tidak Bisa Dipinjam...!`;
    }
    if (result.requestedCount > 0) {
      success.value = 'Data Sudah Terkirim...!';
    }
    selectedMrCodes.value = [];
  } catch (requestError) {
    error.value = requestError.message;
  }
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <div>
        <h2>📋 Form Peminjaman Berkas Rekam Medis</h2>
        <p class="page-subtitle">Migrasi screen legacy SC0175 — permintaan peminjaman berkas rekam medis</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-if="success" class="status-banner status-banner--success">{{ success }}</p>

    <div v-if="loading" class="loading">Memuat data...</div>

    <template v-else>
      <div class="card">
        <div class="search-row">
          <label class="field">
            <span class="field-label">LOKASI</span>
            <select v-model="selectedUnitId" class="field-input">
              <option v-for="unit in units" :key="unit.unitId" :value="unit.unitId">
                {{ unit.unitCode }} - {{ unit.unitName }}
              </option>
            </select>
          </label>

          <label class="field">
            <span class="field-label">NO. MR</span>
            <input
              v-model="mrCodeInput"
              class="field-input"
              placeholder="Ketik No. MR lalu Enter"
              @keyup.enter="getMrByCode"
            />
          </label>

          <button class="small-button" type="button" @click="openSearchDialog">🔍 Cari Pasien</button>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">DAFTAR BERKAS REKAM MEDIS</h3>
        <table class="file-table">
          <thead>
            <tr>
              <th></th>
              <th>NO. MR</th>
              <th>NAMA PASIEN</th>
              <th>STATUS MR</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in fileList" :key="item.mrCode">
              <td>
                <input
                  type="checkbox"
                  :checked="selectedMrCodes.includes(item.mrCode)"
                  @change="toggleSelected(item.mrCode)"
                />
              </td>
              <td>{{ item.mrCode }}</td>
              <td>{{ item.patientName }}</td>
              <td>{{ item.mrStatusLabel }}</td>
            </tr>
            <tr v-if="!fileList.length">
              <td colspan="4" class="empty-state">Belum ada berkas yang ditambahkan.</td>
            </tr>
          </tbody>
        </table>

        <div class="file-actions">
          <button class="small-button primary" type="button" @click="requestBorrow">📄 PINJAM BERKAS</button>
          <button class="small-button" type="button" @click="clearFileList">🧹 BERSIHKAN</button>
        </div>
      </div>
    </template>

    <div v-if="showSearchDialog" class="modal-overlay" @click.self="closeSearchDialog">
      <div class="modal-card">
        <h3 class="card-title">PENCARIAN DATA PASIEN</h3>

        <div class="search-form-grid">
          <label class="field">
            <span class="field-label">NO. MR</span>
            <input v-model="searchForm.mrCode" class="field-input" />
          </label>
          <label class="field">
            <span class="field-label">NAMA</span>
            <input v-model="searchForm.patientName" class="field-input" />
          </label>
          <label class="field">
            <span class="field-label">NIK</span>
            <input v-model="searchForm.nik" class="field-input" />
          </label>
          <label class="field">
            <span class="field-label">TGL. LAHIR</span>
            <input v-model="searchForm.birthDate" type="date" class="field-input" />
          </label>
          <label class="field">
            <span class="field-label">ALAMAT</span>
            <input v-model="searchForm.address" class="field-input" />
          </label>
        </div>

        <div class="modal-actions">
          <button class="small-button primary" type="button" :disabled="searching" @click="runSearch">CARI</button>
          <button class="small-button" type="button" @click="closeSearchDialog">TUTUP</button>
        </div>

        <table class="file-table">
          <thead>
            <tr>
              <th>NO. MR</th>
              <th>NAMA</th>
              <th>TGL. LAHIR</th>
              <th>ALAMAT</th>
              <th>STATUS MR</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in searchResults" :key="item.mrCode" class="clickable-row" @click="pickSearchResult(item)">
              <td>{{ item.mrCode }}</td>
              <td>{{ item.patientName }}</td>
              <td>{{ item.birthDate }}</td>
              <td>{{ item.address }}</td>
              <td>{{ item.mrStatusLabel }}</td>
            </tr>
            <tr v-if="!searchResults.length">
              <td colspan="5" class="empty-state">Tidak ada data. Silakan cari terlebih dahulu.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
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

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.search-row { display: flex; align-items: flex-end; gap: 16px; flex-wrap: wrap; }
.search-form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 12px; }
.field { display: flex; flex-direction: column; gap: 4px; min-width: 180px; }
.field-label { font-size: 12px; font-weight: 600; color: #6b7280; }
.field-input { padding: 8px 10px; border-radius: 8px; border: 1px solid #d1d5db; font-size: 13px; }

.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th { text-align: left; padding: 8px; border-bottom: 2px solid #e5e7eb; color: #304b73; }
.file-table td { padding: 8px; border-bottom: 1px solid #f3f4f6; }
.clickable-row { cursor: pointer; }
.clickable-row:hover { background: #eef2ff; }
.empty-state { color: #9ca3af; text-align: center; padding: 12px; }

.file-actions { display: flex; gap: 10px; margin-top: 12px; }

.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.6; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal-card { background: #fff; border-radius: 12px; padding: 20px; width: 720px; max-width: 95vw; max-height: 85vh; overflow-y: auto; }
.modal-actions { display: flex; gap: 10px; margin-bottom: 12px; }
</style>
