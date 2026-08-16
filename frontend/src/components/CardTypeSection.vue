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
const bankOptions = ref([]);

const pageSize = 10;
const currentPage = ref(1);
const searchQuery = ref('');

// COA search (bandbox)
const coaResults = ref([]);
const coaSearching = ref(false);
const coaSearchOpen = ref(false);
const coaKeyword = ref('');

// Opsi tipe kartu (konstanta legacy)
const cardTypeOptions = [
  { value: 1, label: '1. KARTU KREDIT' },
  { value: 2, label: '2. KARTU DEBIT' }
];

// Filter baris berdasarkan kata kunci pencarian (nama bank, tipe kartu, nama kartu, no coa).
const filteredRows = computed(() => {
  const query = searchQuery.value.trim().toUpperCase();
  if (!query) return rows.value;
  return rows.value.filter((row) => {
    const haystack = [
      row.bankName,
      row.paymentTypeName,
      row.cardName,
      row.coaNo
    ].join(' ').toUpperCase();
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
  bankId: null,
  paymentType: null,
  coaId: null,
  coaKeyword: '',
  cardName: ''
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

async function loadData() {
  rows.value = await request('/master/cardtype');
  currentPage.value = 1;
}

async function loadMasters() {
  const masters = await request('/master/cardtype/masters');
  bankOptions.value = masters.bankOptions || [];
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([loadData(), loadMasters()]);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    bankId: null,
    paymentType: null,
    coaId: null,
    coaKeyword: '',
    cardName: ''
  };
  selectedId.value = null;
  coaResults.value = [];
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    bankId: row.bankId,
    paymentType: row.paymentType,
    coaId: row.coaId,
    coaKeyword: row.coaNo ? `${row.coaNo} - ${row.coaName}` : '',
    cardName: row.cardName
  };
  coaResults.value = [];
}

// Pilih COA dari bandbox NO. COA.
function selectCoa(coa) {
  form.value.coaId = coa.id;
  form.value.coaKeyword = `${coa.coaNo} - ${coa.coaName}`;
  coaResults.value = [];
  coaSearchOpen.value = false;
}

// Cari COA berdasarkan no/nama yang diketik pada bandbox NO. COA.
async function searchCoa() {
  const keyword = coaKeyword.value;
  if (!keyword || !keyword.trim()) {
    coaResults.value = [];
    return;
  }
  coaSearching.value = true;
  try {
    coaResults.value = await request(`/master/cardtype/coa-search?keyword=${encodeURIComponent(keyword.trim())}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    coaSearching.value = false;
  }
}

async function doSave() {
  error.value = '';
  if (!form.value.bankId) {
    error.value = 'Nama bank harus diisi.';
    return;
  }
  if (!form.value.coaId) {
    error.value = 'No. COA harus diisi.';
    return;
  }
  if (!form.value.paymentType) {
    error.value = 'Tipe kartu harus diisi.';
    return;
  }
  if (!form.value.cardName.trim()) {
    error.value = 'Nama kartu harus diisi.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/cardtype/save', {
      method: 'POST',
      body: JSON.stringify({
        id: form.value.id,
        bankId: form.value.bankId,
        paymentType: form.value.paymentType,
        coaId: form.value.coaId,
        cardName: form.value.cardName
      })
    });
    resetForm();
    await loadData();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data tipe kartu terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data tipe kartu ini?')) {
    return;
  }
  try {
    await request(`/master/cardtype/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadData();
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
        <h2>💳 Tipe Kartu Bank</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0048 — master tipe kartu bank</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data tipe kartu...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM TIPE KARTU BANK</h3>
        <div class="form-grid">
          <div class="field">
            <label for="cardtype-bank">NAMA BANK</label>
            <select
              id="cardtype-bank"
              v-model="form.bankId"
              @change="error = ''"
            >
              <option :value="null" disabled>-- Pilih Bank --</option>
              <option v-for="bank in bankOptions" :key="bank.id" :value="bank.id">
                {{ bank.bankName }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="cardtype-coa">NO. COA</label>
            <div class="bandbox">
              <input
                id="cardtype-coa"
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
                    v-for="coa in coaResults"
                    :key="coa.id"
                    @click="selectCoa(coa)"
                  >
                    <td class="strong">{{ coa.coaNo }}</td>
                    <td>{{ coa.coaName }}</td>
                  </tr>
                  <tr v-if="!coaResults.length">
                    <td colspan="2" class="empty-state">Ketik kode/nama COA lalu tekan CARI.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label for="cardtype-type">TIPE KARTU</label>
            <select
              id="cardtype-type"
              v-model="form.paymentType"
              @change="error = ''"
            >
              <option :value="null" disabled>-- Pilih Tipe --</option>
              <option v-for="opt in cardTypeOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="cardtype-name">NAMA KARTU</label>
            <input
              id="cardtype-name"
              v-model="form.cardName"
              type="text"
              maxlength="50"
              placeholder="Nama kartu"
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
        <h3 class="card-title">DATA TIPE KARTU</h3>
        <div class="search-bar">
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            placeholder="🔍 Cari nama bank, tipe kartu, nama kartu, atau no. COA..."
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
                <th>NAMA BANK</th>
                <th>TIPE KARTU</th>
                <th>NAMA KARTU</th>
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
                <td class="strong">{{ row.bankName }}</td>
                <td>{{ row.paymentTypeName }}</td>
                <td class="strong">{{ row.cardName }}</td>
                <td>{{ row.coaNo }}</td>
              </tr>
              <tr v-if="!filteredRows.length">
                <td colspan="4" class="empty-state">
                  {{ searchQuery ? 'Tidak ada data yang cocok dengan pencarian.' : 'Tidak ada data tipe kartu.' }}
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

.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field input, .field select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input:focus, .field select:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }
.field input:disabled { background: #f1f5f9; color: #64748b; }
.field-hint { font-size: 11px; color: #6b7280; }

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
