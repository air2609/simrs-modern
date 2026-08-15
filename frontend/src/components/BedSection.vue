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
const classOptions = ref([]);
const coaResults = ref([]);
const coaSearching = ref(false);
const searchKeyword = ref('');
const searching = ref(false);

// Room search (bandbox)
const roomSearchOpen = ref(false);
const roomKeyword = ref('');
const roomResults = ref([]);

// COA search (bandbox)
const coaSearchOpen = ref(false);
const coaKeyword = ref('');


const form = ref({
  id: null,
  roomId: null,
  roomName: '',
  treatmentClassId: null,
  bedCode: '',
  bedDesc: '',
  bedPrice: 0,
  coaId: null,
  coaKeyword: '',
  activeStatus: 'A'
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

async function loadBeds() {
  rows.value = await request('/master/bed');
  currentPage.value = 1;
}

async function loadOptions() {
  classOptions.value = await request('/master/bed/class-options');
}

async function searchRooms() {
  const name = roomKeyword.value.trim();
  roomResults.value = await request(`/master/bed/rooms?name=${encodeURIComponent(name)}`);
}

function selectRoom(room) {
  form.value.roomId = room.roomId;
  form.value.roomName = room.roomName;
  roomSearchOpen.value = false;
}

async function searchCoa() {
  const keyword = coaKeyword.value;
  if (!keyword || !keyword.trim()) {
    coaResults.value = [];
    return;
  }
  coaSearching.value = true;
  try {
    coaResults.value = await request(`/master/bed/coa-search?keyword=${encodeURIComponent(keyword.trim())}`);
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
  coaSearchOpen.value = false;
}

async function doSearch() {
  error.value = '';
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    await loadBeds();
    return;
  }
  searching.value = true;
  try {
    rows.value = await request(`/master/bed/search?keyword=${encodeURIComponent(keyword)}`);
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
    await Promise.all([loadBeds(), loadOptions()]);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    roomId: null,
    roomName: '',
    treatmentClassId: null,
    bedCode: '',
    bedDesc: '',
    bedPrice: 0,
    coaId: null,
    coaKeyword: '',
    activeStatus: 'A'
  };
  selectedId.value = null;
  roomKeyword.value = '';
  roomResults.value = [];
  coaResults.value = [];
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    roomId: row.roomId,
    roomName: row.roomName,
    treatmentClassId: row.treatmentClassId,
    bedCode: row.bedCode,
    bedDesc: row.bedDesc,
    bedPrice: row.bedPrice || 0,
    coaId: row.coaId,
    coaKeyword: row.coaNo ? `${row.coaNo} - ${row.coaName}` : '',
    activeStatus: row.activeStatus || 'A'
  };
  coaResults.value = [];
}

async function doSave() {
  error.value = '';
  if (!form.value.roomId) {
    error.value = 'NAMA KAMAR harus dipilih.';
    return;
  }
  if (!form.value.treatmentClassId) {
    error.value = 'KELAS TARIF harus dipilih.';
    return;
  }
  if (!form.value.bedCode) {
    error.value = 'KODE BED harus diisi.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/bed/save', {
      method: 'POST',
      body: JSON.stringify({
        id: form.value.id,
        roomId: form.value.roomId,
        treatmentClassId: form.value.treatmentClassId,
        bedCode: form.value.bedCode,
        bedDesc: form.value.bedDesc,
        bedPrice: form.value.bedPrice,
        coaId: form.value.coaId,
        activeStatus: form.value.activeStatus
      })
    });
    resetForm();
    await loadBeds();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data bed terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data bed ini?')) {
    return;
  }
  try {
    await request(`/master/bed/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadBeds();
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
        <h2>🛏️ Form Bed</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0020 — master bed perawatan</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data bed...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM BED</h3>
        <div class="form-grid">
          <div class="field">
            <label for="bed-room">NAMA KAMAR</label>
            <div class="bandbox">
              <input
                id="bed-room"
                v-model="form.roomName"
                type="text"
                placeholder="Pilih kamar"
                readonly
                @focus="roomSearchOpen = true"
              />
              <button class="bandbox-btn" type="button" @click="roomSearchOpen = !roomSearchOpen">▾</button>
            </div>
            <div v-if="roomSearchOpen" class="bandbox-popup">
              <div class="bandbox-search">
                <input
                  v-model="roomKeyword"
                  type="text"
                  placeholder="Cari nama kamar"
                  @keyup.enter="searchRooms"
                />
                <button class="small-button" type="button" @click="searchRooms">CARI</button>
              </div>
              <table class="table bandbox-table">
                <thead>
                  <tr>
                    <th>NAMA KAMAR</th>
                    <th>KELAS TARIF</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="room in roomResults"
                    :key="room.roomId"
                    @click="selectRoom(room)"
                  >
                    <td class="strong">{{ room.roomName }}</td>
                    <td>{{ room.tariffClass }}</td>
                  </tr>
                  <tr v-if="!roomResults.length">
                    <td colspan="2" class="empty-state">Ketik nama kamar lalu tekan CARI.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="field">
            <label for="bed-class">KELAS TARIF</label>
            <select id="bed-class" v-model="form.treatmentClassId">
              <option :value="null" disabled>-- Pilih Kelas --</option>
              <option v-for="tclass in classOptions" :key="tclass.id" :value="tclass.id">
                {{ tclass.code }} - {{ tclass.description }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="bed-code">KODE BED</label>
            <input
              id="bed-code"
              v-model="form.bedCode"
              type="text"
              maxlength="15"
              placeholder="Kode bed"
              @keyup.enter="doSave"
            />
          </div>

          <div class="field">
            <label for="bed-desc">NAMA BED</label>
            <input
              id="bed-desc"
              v-model="form.bedDesc"
              type="text"
              readonly
              placeholder="Otomatis dari hall-kelas-kamar-kode"
            />
          </div>

          <div class="field">
            <label for="bed-price">HARGA</label>
            <input
              id="bed-price"
              v-model.number="form.bedPrice"
              type="number"
              min="0"
              step="0.01"
              placeholder="0"
            />
          </div>

          <div class="field">
            <label for="bed-status">STATUS</label>
            <select id="bed-status" v-model="form.activeStatus">
              <option value="A">ACTIVE</option>
              <option value="I">INACTIVE</option>
            </select>
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
        <h3 class="card-title">DATA BED</h3>
        <div class="search-bar">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="Cari kelas tarif / kode bed / nama bed / kamar..."
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
                <th>RUANGAN</th>
                <th>NAMA BED</th>
                <th>KELAS TARIF</th>
                <th>KODE BED</th>
                <th>HARGA</th>
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
                <td class="strong">{{ row.roomName }}</td>
                <td>{{ row.bedDesc }}</td>
                <td>{{ row.tariffClass }}</td>
                <td>{{ row.bedCode }}</td>
                <td class="num">{{ formatCurrency(row.bedPrice) }}</td>
                <td>{{ row.activeStatus === 'A' ? 'ACTIVE' : 'INACTIVE' }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="6" class="empty-state">Tidak ada data bed.</td>
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
