<script setup>
import { computed, onMounted, reactive, ref } from 'vue';

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

const subsystems = ref([]);
const units = ref([]);
const screens = ref([]);

const form = reactive({
  id: '',
  screenCode: '',
  screenName: '',
  subsystemId: '',
  unitIds: []
});

const keyword = ref('');

// Pagination
const pageSize = 10;
const currentPage = ref(1);

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(screens.value.length / pageSize));
});

const pagedScreens = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return screens.value.slice(start, start + pageSize);
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

function resetForm() {
  form.id = '';
  form.screenCode = '';
  form.screenName = '';
  form.subsystemId = subsystems.value.length ? String(subsystems.value[0].subsystemId) : '';
  form.unitIds = [];
}

function applyRow(row) {
  form.id = String(row.screenId);
  form.screenCode = row.screenCode || '';
  form.screenName = row.screenName || '';
  form.subsystemId = row.subsystemId != null ? String(row.subsystemId) : '';
  form.unitIds = (row.unitIds || []).map((id) => String(id));
}

async function loadMasters() {
  const masters = await request('/master/screens/masters');
  subsystems.value = masters.subsystems || [];
  units.value = masters.units || [];
}

async function loadScreens() {
  const query = keyword.value ? `?keyword=${encodeURIComponent(keyword.value)}` : '';
  screens.value = await request(`/master/screens${query}`);
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';

  try {
    await loadMasters();
    await loadScreens();
    resetForm();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function searchScreens() {
  error.value = '';
  message.value = '';
  try {
    await loadScreens();
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function chooseRow(row) {
  error.value = '';
  message.value = '';
  applyRow(row);
}

async function submit() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const isUpdate = Boolean(form.id);
    const method = form.id ? 'PUT' : 'POST';
    const path = form.id ? `/master/screens/${form.id}` : '/master/screens';
    const body = {
      screenCode: form.screenCode,
      screenName: form.screenName,
      subsystemId: form.subsystemId ? Number(form.subsystemId) : null,
      unitIds: form.unitIds.map((id) => Number(id))
    };
    const screen = await request(path, {
      method,
      body: JSON.stringify(body)
    });
    await loadScreens();
    applyRow(screen);
    message.value = isUpdate ? 'Data screen berhasil diubah.' : 'Data screen berhasil ditambahkan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function remove() {
  if (!form.id || !window.confirm('Hapus data screen terpilih?')) {
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    await request(`/master/screens/${form.id}`, { method: 'DELETE' });
    await loadScreens();
    resetForm();
    message.value = 'Data screen berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

function toggleUnit(unitId) {
  const value = String(unitId);
  const index = form.unitIds.indexOf(value);
  if (index >= 0) {
    form.unitIds.splice(index, 1);
  } else {
    form.unitIds.push(value);
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
        <h2>🖥️ Screen Master</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0003 — memelihara data screen, subsystem, dan sub divisi (unit)</p>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-else-if="message" class="status-banner status-banner--success">{{ message }}</p>

    <div v-if="loading" class="loading">Memuat master screen...</div>

    <template v-else>
      <!-- ======================== FORM (TOP) ======================== -->
      <div class="card">
        <h3>Form Screen</h3>
        <div class="form-row">
          <label>
            Kode
            <input v-model="form.screenCode" type="text" placeholder="Kode screen" />
          </label>
        </div>
        <div class="form-row">
          <label>
            Nama Screen
            <input v-model="form.screenName" type="text" placeholder="Nama screen" />
          </label>
        </div>
        <div class="form-row">
          <label>
            Subsystem
            <select v-model="form.subsystemId">
              <option value="">Pilih subsystem</option>
              <option v-for="item in subsystems" :key="item.subsystemId" :value="String(item.subsystemId)">
                {{ item.subsystemCode }} - {{ item.subsystemName }}
              </option>
            </select>
          </label>
        </div>
        <div class="form-row">
          <label>
            Sub Divisi
            <div class="unit-list">
              <label v-for="unit in units" :key="unit.unitId" class="unit-check">
                <input
                  type="checkbox"
                  :value="String(unit.unitId)"
                  :checked="form.unitIds.includes(String(unit.unitId))"
                  @change="toggleUnit(unit.unitId)"
                />
                <span>{{ unit.unitCode }} - {{ unit.unitName }}</span>
              </label>
            </div>
          </label>
        </div>
        <div class="save-actions">
          <button class="primary-button" :disabled="saving" @click="submit">💾 Simpan</button>
          <button class="secondary-button" :disabled="saving" @click="resetForm">Batal</button>
          <button class="danger-button" :disabled="saving || !form.id" @click="remove">Hapus</button>
        </div>
      </div>

      <!-- ======================== LIST (BOTTOM) ======================== -->
      <div class="card">
        <h3>
          <span>List Screen</span>
          <span class="search-actions">
            <input v-model="keyword" type="text" placeholder="Cari kode / nama screen" @keyup.enter="searchScreens" />
            <button class="small-button" type="button" @click="searchScreens">Cari</button>
          </span>
        </h3>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Kode</th>
                <th>Nama Screen</th>
                <th>Subsystem</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in pagedScreens"
                :key="row.screenId"
                :class="{ selected: String(row.screenId) === String(form.id) }"
                @click="chooseRow(row)"
              >
                <td><strong>{{ row.screenCode }}</strong></td>
                <td>{{ row.screenName }}</td>
                <td>{{ row.subsystemName }}</td>
              </tr>
              <tr v-if="!pagedScreens.length">
                <td colspan="3" class="empty-state">Tidak ada data screen.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="pagination-bar">
          <span class="pagination-info">
            Menampilkan {{ pagedScreens.length }} dari {{ screens.length }} data
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
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.loading { padding: 24px; text-align: center; color: #9ca3af; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--success { background: #e6f5ea; color: #1d6b3a; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card h3 { margin: 0 0 12px; font-size: 16px; color: #304b73; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }

.form-row { display: grid; grid-template-columns: 1fr; gap: 10px; margin-bottom: 10px; }
.form-row label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; }

input, select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; background: #fff; }
input:disabled, select:disabled { background: #f7f7f9; color: #6b7280; }

.unit-list {
  max-height: 140px;
  overflow-y: auto;
  border: 1px solid #d1d9e6;
  border-radius: 8px;
  padding: 8px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  background: #fff;
}

.unit-check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 400;
  color: #3d4b63;
}

.unit-check input {
  width: auto;
  height: auto;
  padding: 0;
}

.primary-button, .secondary-button, .danger-button { border: 0; cursor: pointer; padding: 8px 20px; font-weight: 700; border-radius: 8px; }
.primary-button { background: #304b73; color: #fff; }
.primary-button:disabled { opacity: 0.5; cursor: not-allowed; }
.secondary-button { background: #fff; border: 1px solid #d1d9e6; color: #3d4b63; }
.secondary-button:hover { background: #f6f8fb; }
.danger-button { background: #b84747; color: #fff; }
.danger-button:disabled { opacity: 0.5; cursor: not-allowed; }
.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
.small-button:disabled { opacity: 0.5; cursor: not-allowed; }

.save-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-top: 4px; }

.search-actions { display: flex; gap: 6px; align-items: center; }
.search-actions input { width: 220px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr { cursor: pointer; }
.table tbody tr.selected { background: #eef3fb; }
.table tbody tr:hover { background: #f6f8fb; }

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
</style>
