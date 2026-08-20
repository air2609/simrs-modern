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

const activeTab = ref('group');

// ===================== GROUP MASTER =====================
const groups = ref([]);

const groupForm = reactive({
  id: '',
  groupCode: '',
  groupName: ''
});

const groupKeyword = ref('');

// ===================== GROUP PRIVILEGE =====================
const privileges = ref([]);
const screenResults = ref([]);
const showScreenModal = ref(false);

const privilegeForm = reactive({
  groupCode: '',
  screenId: '',
  screenCode: '',
  screenName: '',
  accessType: 'RW'
});

const privilegeKeyword = ref('');

// Pagination (shared)
const pageSize = 10;
const currentPage = ref(1);

const totalPages = computed(() => {
  const list = activeTab.value === 'group' ? groups.value : privileges.value;
  return Math.max(1, Math.ceil(list.length / pageSize));
});

const pagedRows = computed(() => {
  const list = activeTab.value === 'group' ? groups.value : privileges.value;
  const start = (currentPage.value - 1) * pageSize;
  return list.slice(start, start + pageSize);
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

// ===================== GROUP MASTER =====================
function resetGroupForm() {
  groupForm.id = '';
  groupForm.groupCode = '';
  groupForm.groupName = '';
}

function applyGroupRow(row) {
  groupForm.id = String(row.groupId);
  groupForm.groupCode = row.groupCode || '';
  groupForm.groupName = row.groupName || '';
}

async function selectGroupAndShowPrivileges(row) {
  applyGroupRow(row);
  // Aktifkan tab GROUP PRIVILEGE dan tampilkan seluruh akses group terpilih
  privilegeForm.groupCode = row.groupCode || '';
  privilegeKeyword.value = '';
  activeTab.value = 'privilege';
  error.value = '';
  message.value = '';
  await loadPrivileges();
}

async function loadGroups() {
  const query = groupKeyword.value ? `?keyword=${encodeURIComponent(groupKeyword.value)}` : '';
  groups.value = await request(`/admin/groups${query}`);
  currentPage.value = 1;
}

async function submitGroup() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const isUpdate = Boolean(groupForm.id);
    const method = groupForm.id ? 'PUT' : 'POST';
    const path = groupForm.id ? `/admin/groups/${groupForm.id}` : '/admin/groups';
    const body = {
      groupCode: groupForm.groupCode,
      groupName: groupForm.groupName
    };
    const group = await request(path, {
      method,
      body: JSON.stringify(body)
    });
    await loadGroups();
    applyGroupRow(group);
    message.value = isUpdate ? 'Data group berhasil diubah.' : 'Data group berhasil ditambahkan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function removeGroup() {
  if (!groupForm.id || !window.confirm('Hapus data group terpilih?')) {
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    await request(`/admin/groups/${groupForm.id}`, { method: 'DELETE' });
    await loadGroups();
    resetGroupForm();
    message.value = 'Data group berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

// ===================== GROUP PRIVILEGE =====================
function resetPrivilegeForm() {
  privilegeForm.groupCode = '';
  privilegeForm.screenId = '';
  privilegeForm.screenCode = '';
  privilegeForm.screenName = '';
  privilegeForm.accessType = 'RW';
}

function applyPrivilegeRow(row) {
  privilegeForm.groupCode = privilegeForm.groupCode || '';
  privilegeForm.screenId = String(row.screenId);
  privilegeForm.screenCode = row.screenCode || '';
  privilegeForm.screenName = row.screenName || '';
  privilegeForm.accessType = row.accessType || 'RW';
}

async function loadPrivileges() {
  const query = privilegeForm.groupCode
    ? `?groupCode=${encodeURIComponent(privilegeForm.groupCode)}`
    : '';
  privileges.value = await request(`/admin/groups/privileges${query}`);
  currentPage.value = 1;
}

async function searchScreens() {
  error.value = '';
  message.value = '';
  try {
    const query = `?code=${encodeURIComponent(privilegeForm.screenCode || '')}&name=${encodeURIComponent(privilegeForm.screenName || '')}`;
    screenResults.value = await request(`/admin/groups/screens${query}`);
    showScreenModal.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function chooseScreen(screen) {
  privilegeForm.screenId = String(screen.screenId);
  privilegeForm.screenCode = screen.screenCode || '';
  privilegeForm.screenName = screen.screenName || '';
  showScreenModal.value = false;
}

async function submitPrivilege() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const isUpdate = Boolean(privilegeForm.screenId);
    const method = privilegeForm.screenId ? 'PUT' : 'POST';
    const body = {
      groupCode: privilegeForm.groupCode,
      screenId: privilegeForm.screenId ? Number(privilegeForm.screenId) : null,
      accessType: privilegeForm.accessType
    };
    await request(`/admin/groups/privileges`, {
      method,
      body: JSON.stringify(body)
    });
    await loadPrivileges();
    message.value = isUpdate ? 'Data privilege berhasil diubah.' : 'Data privilege berhasil ditambahkan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function removePrivilege() {
  if (!privilegeForm.screenId || !window.confirm('Hapus data privilege terpilih?')) {
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const query = `?groupCode=${encodeURIComponent(privilegeForm.groupCode)}&screenId=${privilegeForm.screenId}`;
    await request(`/admin/groups/privileges${query}`, { method: 'DELETE' });
    await loadPrivileges();
    resetPrivilegeForm();
    message.value = 'Data privilege berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

function switchTab(tab) {
  activeTab.value = tab;
  error.value = '';
  message.value = '';
  currentPage.value = 1;
}

async function initialize() {
  loading.value = true;
  error.value = '';

  try {
    await loadGroups();
    resetGroupForm();
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
        <h2>👥 Group Master</h2>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-else-if="message" class="status-banner status-banner--success">{{ message }}</p>

    <div v-if="loading" class="loading">Memuat data group...</div>

    <template v-else>
      <!-- Tabs -->
      <div class="tabs">
        <button
          class="tab-button"
          :class="{ active: activeTab === 'group' }"
          type="button"
          @click="switchTab('group')"
        >
          GROUP MASTER
        </button>
        <button
          class="tab-button"
          :class="{ active: activeTab === 'privilege' }"
          type="button"
          @click="switchTab('privilege')"
        >
          GROUP PRIVILEGE
        </button>
      </div>

      <!-- ======================== GROUP MASTER TAB ======================== -->
      <template v-if="activeTab === 'group'">
        <div class="card">
          <h3>Form Group</h3>
          <div class="form-grid">
            <label>
              Group ID
              <input v-model="groupForm.groupCode" type="text" placeholder="Group ID" />
            </label>
            <label>
              Group Name
              <input v-model="groupForm.groupName" type="text" placeholder="Nama group" />
            </label>
          </div>
          <div class="save-actions">
            <button class="primary-button" :disabled="saving" @click="submitGroup">💾 Simpan</button>
            <button class="secondary-button" :disabled="saving" @click="resetGroupForm">Batal</button>
            <button class="danger-button" :disabled="saving || !groupForm.id" @click="removeGroup">Hapus</button>
          </div>
        </div>

        <div class="card">
          <h3>
            <span>List Group</span>
            <span class="search-actions">
              <input v-model="groupKeyword" type="text" placeholder="Cari group" @keyup.enter="loadGroups" />
              <button class="small-button" type="button" @click="loadGroups">Cari</button>
            </span>
          </h3>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Group ID</th>
                  <th>Group Name</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in pagedRows"
                  :key="row.groupId"
                  :class="{ selected: String(row.groupId) === String(groupForm.id) }"
                  @click="selectGroupAndShowPrivileges(row)"
                >
                  <td><strong>{{ row.groupCode }}</strong></td>
                  <td>{{ row.groupName }}</td>
                </tr>
                <tr v-if="!pagedRows.length">
                  <td colspan="2" class="empty-state">Tidak ada data group.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pagination-bar">
            <span class="pagination-info">
              Menampilkan {{ pagedRows.length }} dari {{ groups.length }} data
            </span>
            <div class="pagination-controls">
              <button class="small-button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹ Prev</button>
              <span class="pagination-page">Halaman {{ currentPage }} / {{ totalPages }}</span>
              <button class="small-button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">Next ›</button>
            </div>
          </div>
        </div>
      </template>

      <!-- ======================== GROUP PRIVILEGE TAB ======================== -->
      <template v-else>
        <div class="card">
          <h3>Form Privilege</h3>
          <div class="form-grid">
            <label>
              Group ID
              <input v-model="privilegeForm.groupCode" type="text" placeholder="Group ID" @change="loadPrivileges" />
            </label>
            <label>
              Screen Code
              <div class="inline-search">
                <input v-model="privilegeForm.screenCode" type="text" placeholder="Kode screen" />
                <button class="small-button" type="button" @click="searchScreens">Cari</button>
              </div>
            </label>
            <label>
              Screen Name
              <input v-model="privilegeForm.screenName" type="text" placeholder="Nama screen" />
            </label>
            <label>
              Access Type
              <select v-model="privilegeForm.accessType">
                <option value="RW">RW</option>
                <option value="R">R</option>
                <option value="SPV">SPV</option>
              </select>
            </label>
          </div>
          <div class="save-actions">
            <button class="primary-button" :disabled="saving" @click="submitPrivilege">💾 Simpan</button>
            <button class="secondary-button" :disabled="saving" @click="resetPrivilegeForm">Batal</button>
            <button class="danger-button" :disabled="saving || !privilegeForm.screenId" @click="removePrivilege">Hapus</button>
          </div>
        </div>

        <div class="card">
          <h3>
            <span>List Privilege</span>
            <span class="search-actions">
              <input v-model="privilegeKeyword" type="text" placeholder="Cari screen" @keyup.enter="loadPrivileges" />
              <button class="small-button" type="button" @click="loadPrivileges">Cari</button>
            </span>
          </h3>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Screen Code</th>
                  <th>Screen Name</th>
                  <th>Access Type</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in pagedRows"
                  :key="row.screenId"
                  :class="{ selected: String(row.screenId) === String(privilegeForm.screenId) }"
                  @click="applyPrivilegeRow(row)"
                >
                  <td><strong>{{ row.screenCode }}</strong></td>
                  <td>{{ row.screenName }}</td>
                  <td>{{ row.accessType }}</td>
                </tr>
                <tr v-if="!pagedRows.length">
                  <td colspan="3" class="empty-state">Tidak ada data privilege.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pagination-bar">
            <span class="pagination-info">
              Menampilkan {{ pagedRows.length }} dari {{ privileges.length }} data
            </span>
            <div class="pagination-controls">
              <button class="small-button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹ Prev</button>
              <span class="pagination-page">Halaman {{ currentPage }} / {{ totalPages }}</span>
              <button class="small-button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">Next ›</button>
            </div>
          </div>
        </div>
      </template>

      <!-- ======================== SCREEN SEARCH MODAL ======================== -->
      <div v-if="showScreenModal" class="modal-overlay" @click.self="showScreenModal = false">
        <div class="modal">
          <h3>Pilih Screen</h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="screen in screenResults" :key="screen.screenId" @click="chooseScreen(screen)">
                  <td><strong>{{ screen.screenCode }}</strong></td>
                  <td>{{ screen.screenName }}</td>
                </tr>
                <tr v-if="!screenResults.length">
                  <td colspan="2" class="empty-state">Tidak ada screen ditemukan.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="save-actions">
            <button class="secondary-button" type="button" @click="showScreenModal = false">Tutup</button>
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

/* Tabs */
.tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.tab-button {
  padding: 10px 20px;
  border: 1px solid #d1d9e6;
  background: #fff;
  color: #3d4b63;
  font-weight: 700;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
}
.tab-button.active { background: #304b73; color: #fff; border-color: #304b73; }

.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 12px; }
.form-grid label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; }

input, select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; background: #fff; }
input:disabled, select:disabled { background: #f7f7f9; color: #6b7280; }

.inline-search { display: flex; gap: 6px; align-items: center; }
.inline-search input { flex: 1; }

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

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}
.modal {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  width: 520px;
  max-width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
}
.modal h3 { margin: 0 0 12px; color: #304b73; }
</style>
