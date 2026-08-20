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
const searchQuery = ref('');

// COA search (bandbox)
const coaResults = ref([]);
const coaSearching = ref(false);
const coaSearchOpen = ref(false);
const coaKeyword = ref('');


// Filter baris berdasarkan kata kunci pencarian (kode, nama, alamat, contact person, telp).
const filteredRows = computed(() => {
  const query = searchQuery.value.trim().toUpperCase();
  if (!query) return rows.value;
  return rows.value.filter((row) => {
    const haystack = [
      row.code,
      row.name,
      row.address,
      row.contactPerson,
      row.contactNo
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
  code: '',
  name: '',
  address: '',
  contactNo: '',
  altContactNo: '',
  faxNo: '',
  contactPerson: '',
  coaId: null,
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

async function loadData() {
  rows.value = await request('/master/vendor');
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await loadData();
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
    address: '',
    contactNo: '',
    altContactNo: '',
    faxNo: '',
    contactPerson: '',
    coaId: null,
    coaKeyword: ''
  };
  selectedId.value = null;
  coaResults.value = [];
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    code: row.code,
    name: row.name,
    address: row.address,
    contactNo: row.contactNo,
    altContactNo: row.altContactNo,
    faxNo: row.faxNo,
    contactPerson: row.contactPerson,
    coaId: row.coaId,
    coaKeyword: row.coaNo ? `${row.coaNo} - ${row.coaName}` : ''
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
    coaResults.value = await request(`/master/vendor/coa-search?keyword=${encodeURIComponent(keyword.trim())}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    coaSearching.value = false;
  }
}


async function doSave() {
  error.value = '';
  if (!form.value.code.trim()) {
    error.value = 'Kode vendor harus diisi.';
    return;
  }
  if (!form.value.name.trim()) {
    error.value = 'Nama vendor harus diisi.';
    return;
  }
  if (!form.value.address.trim()) {
    error.value = 'Alamat vendor harus diisi.';
    return;
  }
  if (!form.value.contactPerson.trim()) {
    error.value = 'Contact person harus diisi.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/vendor/save', {
      method: 'POST',
      body: JSON.stringify({
        id: form.value.id,
        code: form.value.code,
        name: form.value.name,
        address: form.value.address,
        contactNo: form.value.contactNo,
        altContactNo: form.value.altContactNo,
        faxNo: form.value.faxNo,
        contactPerson: form.value.contactPerson,
        coaId: form.value.coaId
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
    error.value = 'Pilih data vendor terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data vendor ini?')) {
    return;
  }
  try {
    await request(`/master/vendor/delete?id=${selectedId.value}`, {
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
        <h2>🏭 Vendor / Supplier</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data vendor...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM MASTER SUPPLIER</h3>
        <div class="form-grid">
          <div class="field">
            <label for="vendor-code">KODE</label>
            <input
              id="vendor-code"
              v-model="form.code"
              type="text"
              maxlength="15"
              placeholder="Kode vendor"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="vendor-name">NAMA</label>
            <input
              id="vendor-name"
              v-model="form.name"
              type="text"
              maxlength="50"
              placeholder="Nama vendor"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="vendor-address">ALAMAT</label>
            <input
              id="vendor-address"
              v-model="form.address"
              type="text"
              placeholder="Alamat vendor"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="vendor-telp">NO. TELP</label>
            <input
              id="vendor-telp"
              v-model="form.contactNo"
              type="text"
              maxlength="20"
              placeholder="No. telp"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="vendor-alt-telp">NO. TELP ALTERNATIF</label>
            <input
              id="vendor-alt-telp"
              v-model="form.altContactNo"
              type="text"
              maxlength="20"
              placeholder="No. telp alternatif"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="vendor-fax">NO. FAX</label>
            <input
              id="vendor-fax"
              v-model="form.faxNo"
              type="text"
              maxlength="20"
              placeholder="No. fax"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="vendor-contact">CONTACT PERSON</label>
            <input
              id="vendor-contact"
              v-model="form.contactPerson"
              type="text"
              maxlength="50"
              placeholder="Contact person"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="vendor-coa">NO. COA</label>
            <div class="bandbox">
              <input
                id="vendor-coa"
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
        <h3 class="card-title">DATA SUPPLIER</h3>
        <div class="search-bar">
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            placeholder="🔍 Cari kode, nama, alamat, contact person, atau telp..."
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
                <th>ALAMAT</th>
                <th>CONTACT PERSON</th>
                <th>NO. TELP</th>
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
                <td class="strong">{{ row.name }}</td>
                <td>{{ row.address }}</td>
                <td>{{ row.contactPerson }}</td>
                <td>{{ row.contactNo }}</td>
              </tr>
              <tr v-if="!filteredRows.length">
                <td colspan="5" class="empty-state">
                  {{ searchQuery ? 'Tidak ada data yang cocok dengan pencarian.' : 'Tidak ada data vendor.' }}
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
