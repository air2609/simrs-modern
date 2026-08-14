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
const saving = ref(false);
const error = ref('');
const message = ref('');

const beds = ref([]);
const allShown = ref(false);

const pageSize = 10;
const currentPage = ref(1);

const totalPages = computed(() => Math.max(1, Math.ceil(beds.value.length / pageSize)));

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return beds.value.slice(start, start + pageSize);
});

function goToPage(page) {
  if (page < 1 || page > totalPages.value) {
    return;
  }
  currentPage.value = page;
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
  beds.value = await request('/ward/bed-display');
  currentPage.value = 1;
}

function toggleAll() {
  beds.value.forEach((bed) => {
    bed.shown = allShown.value;
  });
}

function onRowShownChange() {
  // Jika ada bed yang tidak dicentang, matikan checkbox "TAMPIL semua"
  const anyUnchecked = beds.value.some((bed) => !bed.shown);
  if (anyUnchecked) {
    allShown.value = false;
  }
}

async function saveAll() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const payload = beds.value.map((bed) => ({
      bedId: bed.bedId,
      shown: bed.shown,
      availableStatus: bed.availableStatus
    }));
    await request('/ward/bed-display/save', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    await loadBeds();
    message.value = 'Data bed berhasil disimpan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function initialize() {
  loading.value = true;
  error.value = '';

  try {
    await loadBeds();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  initialize();
});
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>🛏️ Master Kamar Ranap</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0058 — mengatur bed yang ditampilkan dan status ketersediaannya</p>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-else-if="message" class="status-banner status-banner--success">{{ message }}</p>

    <div v-if="loading" class="loading">Memuat data bed...</div>

    <template v-else>
      <div class="card">
        <h3>
          <span>Daftar Bed Yang Akan Ditampilkan</span>
          <span class="search-actions">
            <button class="small-button" type="button" @click="loadBeds">🔄 Refresh</button>
          </span>
        </h3>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>NO</th>
                <th>KELAS TARIF</th>
                <th>RUANGAN</th>
                <th>NO KAMAR</th>
                <th>NO BED</th>
                <th>DESKRIPSI</th>
                <th>KONDISI</th>
                <th>
                  <label class="check-label">
                    <input v-model="allShown" type="checkbox" @change="toggleAll" />
                    TAMPIL
                  </label>
                </th>
                <th>STATUS</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in pagedRows" :key="row.bedId">
                <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td>{{ row.tariffClass }}</td>
                <td>{{ row.roomName }}</td>
                <td>{{ row.roomNumber }}</td>
                <td><strong>{{ row.bedCode }}</strong></td>
                <td>{{ row.bedDesc }}</td>
                <td>
                  <span class="condition-badge" :class="row.condition === 'Terisi' ? 'badge--occupied' : 'badge--empty'">
                    {{ row.condition }}
                  </span>
                </td>
                <td>
                  <input
                    v-model="row.shown"
                    type="checkbox"
                    @change="onRowShownChange"
                  />
                </td>
                <td>
                  <select v-model="row.availableStatus" :disabled="row.condition === 'Terisi'">
                    <option value="A">Available</option>
                    <option value="B">Dipesan</option>
                    <option value="C">Perbaikan</option>
                  </select>
                </td>
              </tr>
              <tr v-if="!pagedRows.length">
                <td colspan="9" class="empty-state">Tidak ada data bed aktif.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar">
          <span class="pagination-info">
            Menampilkan {{ pagedRows.length }} dari {{ beds.length }} data
          </span>
          <div class="pagination-controls">
            <button class="small-button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹ Prev</button>
            <span class="pagination-page">Halaman {{ currentPage }} / {{ totalPages }}</span>
            <button class="small-button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">Next ›</button>
          </div>
        </div>

        <div class="save-actions">
          <button class="primary-button" :disabled="saving" @click="saveAll">💾 Simpan</button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.loading { padding: 24px; text-align: center; color: #9ca3af; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--success { background: #e6f5ea; color: #1d6b3a; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card h3 { margin: 0 0 12px; font-size: 16px; color: #304b73; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }

.search-actions { display: flex; gap: 6px; align-items: center; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr:hover { background: #f6f8fb; }

.check-label { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }

.condition-badge { padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.badge--occupied { background: #fde8ea; color: #a32943; }
.badge--empty { background: #e6f5ea; color: #1d6b3a; }

select { padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; font: inherit; background: #fff; }
select:disabled { background: #f7f7f9; color: #6b7280; }

.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

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

.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
.small-button:disabled { opacity: 0.5; cursor: not-allowed; }

.save-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-top: 16px; }
.primary-button { border: 0; cursor: pointer; padding: 8px 20px; font-weight: 700; border-radius: 8px; background: #304b73; color: #fff; }
.primary-button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
