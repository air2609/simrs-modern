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

// Pagination
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
  hallId: null,
  hallName: '',
  roomCode: '',
  roomName: ''
});

const selectedId = ref(null);
const saving = ref(false);

// Hall search (bandbox)
const hallSearchOpen = ref(false);
const hallKeyword = ref('');
const hallResults = ref([]);

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

async function loadRooms() {
  rows.value = await request('/master/room');
}

async function searchHalls() {
  const name = hallKeyword.value.trim();
  hallResults.value = await request(`/master/room/halls?name=${encodeURIComponent(name)}`);
}

function selectHall(hall) {
  form.value.hallId = hall.hallId;
  form.value.hallName = hall.hallName;
  hallSearchOpen.value = false;
  updateRoomName();
}

function updateRoomName() {
  if (form.value.hallName && form.value.roomCode) {
    form.value.roomName = `${form.value.hallName}-${form.value.roomCode}`;
  } else {
    form.value.roomName = '';
  }
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await loadRooms();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = { id: null, hallId: null, hallName: '', roomCode: '', roomName: '' };
  selectedId.value = null;
  hallKeyword.value = '';
  hallResults.value = [];
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    hallId: null,
    hallName: row.hallName,
    roomCode: row.roomCode,
    roomName: row.roomName
  };
}

async function doSave() {
  error.value = '';
  if (!form.value.hallId) {
    error.value = 'NAMA RUANGAN harus dipilih.';
    return;
  }
  if (!form.value.roomCode) {
    error.value = 'NOMOR KAMAR harus diisi.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/room/save', {
      method: 'POST',
      body: JSON.stringify({
        id: form.value.id,
        hallId: form.value.hallId,
        roomCode: form.value.roomCode
      })
    });
    resetForm();
    await loadRooms();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data kamar terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data kamar ini?')) {
    return;
  }
  try {
    await request(`/master/room/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadRooms();
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
        <h2>🛏️ Form Kamar</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data kamar...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM KAMAR</h3>
        <div class="form-grid">
          <div class="field">
            <label for="room-hall">NAMA RUANGAN</label>
            <div class="bandbox">
              <input
                id="room-hall"
                v-model="form.hallName"
                type="text"
                placeholder="Pilih ruangan"
                readonly
                @focus="hallSearchOpen = true"
              />
              <button class="bandbox-btn" type="button" @click="hallSearchOpen = !hallSearchOpen">▾</button>
            </div>
            <div v-if="hallSearchOpen" class="bandbox-popup">
              <div class="bandbox-search">
                <input
                  v-model="hallKeyword"
                  type="text"
                  placeholder="Cari nama ruangan"
                  @keyup.enter="searchHalls"
                />
                <button class="small-button" type="button" @click="searchHalls">CARI</button>
              </div>
              <table class="table bandbox-table">
                <thead>
                  <tr>
                    <th>RUANGAN</th>
                    <th>KELAS TARIF</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="hall in hallResults"
                    :key="hall.hallId"
                    @click="selectHall(hall)"
                  >
                    <td class="strong">{{ hall.hallName }}</td>
                    <td>{{ hall.tariffClass }}</td>
                  </tr>
                  <tr v-if="!hallResults.length">
                    <td colspan="2" class="empty-state">Ketik nama ruangan lalu tekan CARI.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="field">
            <label for="room-code">NOMOR KAMAR</label>
            <input
              id="room-code"
              v-model="form.roomCode"
              type="text"
              maxlength="20"
              placeholder="Nomor kamar"
              @input="updateRoomName"
              @keyup.enter="doSave"
            />
          </div>

          <div class="field">
            <label for="room-name">NAMA KAMAR</label>
            <input
              id="room-name"
              v-model="form.roomName"
              type="text"
              readonly
              placeholder="Otomatis dari ruangan + nomor"
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
        <h3 class="card-title">DATA KAMAR</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>RUANGAN</th>
                <th>KELAS TARIF</th>
                <th>NO. KAMAR</th>
                <th>NAMA KAMAR</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in paginatedRows"
                :key="row.id"
                :class="{ 'row--selected': selectedId === row.id }"
                @click="selectRow(row)"
              >
                <td class="strong">{{ row.hallName }}</td>
                <td>{{ row.tariffClass }}</td>
                <td>{{ row.roomCode }}</td>
                <td>{{ row.roomName }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="4" class="empty-state">Tidak ada data kamar.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="rows.length > pageSize" class="pagination">
          <button
            class="page-btn"
            type="button"
            :disabled="currentPage === 1"
            @click="goToPage(currentPage - 1)"
          >‹ Prev</button>
          <span class="page-info">
            Halaman {{ currentPage }} dari {{ totalPages }}
            <span class="page-total">({{ rows.length }} data)</span>
          </span>
          <button
            class="page-btn"
            type="button"
            :disabled="currentPage === totalPages"
            @click="goToPage(currentPage + 1)"
          >Next ›</button>
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
.field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }
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

.pagination { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 12px; }
.page-btn { padding: 6px 14px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.page-btn:hover:not(:disabled) { background: #f6f8fb; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 13px; color: #304b73; font-weight: 600; }
.page-total { color: #9ca3af; font-weight: 400; }
</style>
