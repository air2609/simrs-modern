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

const activeTab = ref('user');

// ===================== USER MASTER =====================
const groups = ref([]);
const branches = ref([]);
const users = ref([]);
const staffResults = ref([]);
const showStaffModal = ref(false);

const userForm = reactive({
  id: '',
  userName: '',
  userFullName: '',
  groupId: '',
  staffId: '',
  staffCode: '',
  staffName: '',
  branchId: ''
});

const userKeyword = ref('');

// ===================== USER PRIVILEGE =====================
const privileges = ref([]);
const screenResults = ref([]);
const showScreenModal = ref(false);

const privilegeForm = reactive({
  userName: '',
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
  const list = activeTab.value === 'user' ? users.value : privileges.value;
  return Math.max(1, Math.ceil(list.length / pageSize));
});

const pagedRows = computed(() => {
  const list = activeTab.value === 'user' ? users.value : privileges.value;
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

// ===================== USER MASTER =====================
function resetUserForm() {
  userForm.id = '';
  userForm.userName = '';
  userForm.userFullName = '';
  userForm.groupId = groups.value.length ? String(groups.value[0].groupId) : '';
  userForm.staffId = '';
  userForm.staffCode = '';
  userForm.staffName = '';
  userForm.branchId = branches.value.length ? String(branches.value[0].branchId) : '';
}

function applyUserRow(row) {
  userForm.id = String(row.userId);
  userForm.userName = row.userName || '';
  userForm.userFullName = row.userFullName || '';
  userForm.groupId = row.groupId != null ? String(row.groupId) : '';
  userForm.staffId = row.staffId != null ? String(row.staffId) : '';
  userForm.staffCode = row.staffCode || '';
  userForm.staffName = '';
  userForm.branchId = row.branchId != null ? String(row.branchId) : '';
}

async function selectUserAndShowPrivileges(row) {
  applyUserRow(row);
  // Aktifkan tab USER PRIVILEGE dan tampilkan seluruh akses user terpilih
  privilegeForm.userName = row.userName || '';
  privilegeKeyword.value = '';
  activeTab.value = 'privilege';
  error.value = '';
  message.value = '';
  await loadPrivileges();
}

async function loadMasters() {
  const masters = await request('/admin/users/masters');
  groups.value = masters.groups || [];
  branches.value = masters.branches || [];
}

async function loadUsers() {
  const query = userKeyword.value ? `?keyword=${encodeURIComponent(userKeyword.value)}` : '';
  users.value = await request(`/admin/users${query}`);
  currentPage.value = 1;
}

async function searchStaff() {
  error.value = '';
  message.value = '';
  try {
    const query = `?code=${encodeURIComponent(userForm.staffCode || '')}&name=${encodeURIComponent(userForm.staffName || '')}`;
    staffResults.value = await request(`/admin/users/staff${query}`);
    showStaffModal.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function chooseStaff(staff) {
  userForm.staffId = String(staff.staffId);
  userForm.staffCode = staff.staffCode || '';
  userForm.staffName = staff.staffName || '';
  showStaffModal.value = false;
}

async function submitUser() {
  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    const isUpdate = Boolean(userForm.id);
    const method = userForm.id ? 'PUT' : 'POST';
    const path = userForm.id ? `/admin/users/${userForm.id}` : '/admin/users';
    const body = {
      userName: userForm.userName,
      userFullName: userForm.userFullName,
      groupId: userForm.groupId ? Number(userForm.groupId) : null,
      staffId: userForm.staffId ? Number(userForm.staffId) : null,
      branchId: userForm.branchId ? Number(userForm.branchId) : null
    };
    const user = await request(path, {
      method,
      body: JSON.stringify(body)
    });
    await loadUsers();
    applyUserRow(user);
    message.value = isUpdate ? 'Data user berhasil diubah.' : 'Data user berhasil ditambahkan.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function removeUser() {
  if (!userForm.id || !window.confirm('Hapus data user terpilih?')) {
    return;
  }

  saving.value = true;
  error.value = '';
  message.value = '';

  try {
    await request(`/admin/users/${userForm.id}`, { method: 'DELETE' });
    await loadUsers();
    resetUserForm();
    message.value = 'Data user berhasil dihapus.';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

// ===================== USER PRIVILEGE =====================
function resetPrivilegeForm() {
  privilegeForm.userName = '';
  privilegeForm.screenId = '';
  privilegeForm.screenCode = '';
  privilegeForm.screenName = '';
  privilegeForm.accessType = 'RW';
}

function applyPrivilegeRow(row) {
  privilegeForm.userName = privilegeForm.userName || '';
  privilegeForm.screenId = String(row.screenId);
  privilegeForm.screenCode = row.screenCode || '';
  privilegeForm.screenName = row.screenName || '';
  privilegeForm.accessType = row.accessType || 'RW';
}

async function loadPrivileges() {
  const query = privilegeForm.userName
    ? `?userName=${encodeURIComponent(privilegeForm.userName)}`
    : '';
  privileges.value = await request(`/admin/users/privileges${query}`);
  currentPage.value = 1;
}

async function searchScreens() {
  error.value = '';
  message.value = '';
  try {
    const query = `?code=${encodeURIComponent(privilegeForm.screenCode || '')}&name=${encodeURIComponent(privilegeForm.screenName || '')}`;
    screenResults.value = await request(`/admin/users/screens${query}`);
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
      userName: privilegeForm.userName,
      screenId: privilegeForm.screenId ? Number(privilegeForm.screenId) : null,
      accessType: privilegeForm.accessType
    };
    await request(`/admin/users/privileges`, {
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
    const query = `?userName=${encodeURIComponent(privilegeForm.userName)}&screenId=${privilegeForm.screenId}`;
    await request(`/admin/users/privileges${query}`, { method: 'DELETE' });
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
    await loadMasters();
    await loadUsers();
    resetUserForm();
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
        <h2>👤 User Maintenance</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0001 — memelihara data user dan privilege akses</p>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-else-if="message" class="status-banner status-banner--success">{{ message }}</p>

    <div v-if="loading" class="loading">Memuat data user...</div>

    <template v-else>
      <!-- Tabs -->
      <div class="tabs">
        <button
          class="tab-button"
          :class="{ active: activeTab === 'user' }"
          type="button"
          @click="switchTab('user')"
        >
          USER MASTER
        </button>
        <button
          class="tab-button"
          :class="{ active: activeTab === 'privilege' }"
          type="button"
          @click="switchTab('privilege')"
        >
          USER PRIVILEGE
        </button>
      </div>

      <!-- ======================== USER MASTER TAB ======================== -->
      <template v-if="activeTab === 'user'">
        <div class="card">
          <h3>Form User</h3>
          <div class="form-grid">
            <label>
              User ID
              <input v-model="userForm.userName" type="text" placeholder="User ID" />
            </label>
            <label>
              User Name
              <input v-model="userForm.userFullName" type="text" placeholder="Nama lengkap user" />
            </label>
            <label>
              Group
              <select v-model="userForm.groupId">
                <option value="">Pilih group</option>
                <option v-for="item in groups" :key="item.groupId" :value="String(item.groupId)">
                  {{ item.groupName }}
                </option>
              </select>
            </label>
            <label>
              Branch
              <select v-model="userForm.branchId">
                <option value="">Pilih branch</option>
                <option v-for="item in branches" :key="item.branchId" :value="String(item.branchId)">
                  {{ item.branchName }}
                </option>
              </select>
            </label>
            <label>
              Staff Code
              <div class="inline-search">
                <input v-model="userForm.staffCode" type="text" placeholder="Kode staff" />
                <button class="small-button" type="button" @click="searchStaff">Cari</button>
              </div>
            </label>
            <label>
              Staff Name
              <input v-model="userForm.staffName" type="text" placeholder="Nama staff" />
            </label>
          </div>
          <div class="save-actions">
            <button class="primary-button" :disabled="saving" @click="submitUser">💾 Simpan</button>
            <button class="secondary-button" :disabled="saving" @click="resetUserForm">Batal</button>
            <button class="danger-button" :disabled="saving || !userForm.id" @click="removeUser">Hapus</button>
          </div>
        </div>

        <div class="card">
          <h3>
            <span>List User</span>
            <span class="search-actions">
              <input v-model="userKeyword" type="text" placeholder="Cari user / group / staff" @keyup.enter="loadUsers" />
              <button class="small-button" type="button" @click="loadUsers">Cari</button>
            </span>
          </h3>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>User Name</th>
                  <th>Group</th>
                  <th>Staff</th>
                  <th>Branch</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in pagedRows"
                  :key="row.userId"
                  :class="{ selected: String(row.userId) === String(userForm.id) }"
                  @click="selectUserAndShowPrivileges(row)"
                >
                  <td><strong>{{ row.userName }}</strong></td>
                  <td>{{ row.userFullName }}</td>
                  <td>{{ row.groupName }}</td>
                  <td>{{ row.staffCode }}</td>
                  <td>{{ row.branchName || '-' }}</td>
                </tr>
                <tr v-if="!pagedRows.length">
                  <td colspan="5" class="empty-state">Tidak ada data user.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pagination-bar">
            <span class="pagination-info">
              Menampilkan {{ pagedRows.length }} dari {{ users.length }} data
            </span>
            <div class="pagination-controls">
              <button class="small-button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹ Prev</button>
              <span class="pagination-page">Halaman {{ currentPage }} / {{ totalPages }}</span>
              <button class="small-button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">Next ›</button>
            </div>
          </div>
        </div>
      </template>

      <!-- ======================== USER PRIVILEGE TAB ======================== -->
      <template v-else>
        <div class="card">
          <h3>Form Privilege</h3>
          <div class="form-grid">
            <label>
              User ID
              <input v-model="privilegeForm.userName" type="text" placeholder="User ID" @change="loadPrivileges" />
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

      <!-- ======================== STAFF SEARCH MODAL ======================== -->
      <div v-if="showStaffModal" class="modal-overlay" @click.self="showStaffModal = false">
        <div class="modal">
          <h3>Pilih Staff</h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Kode</th>
                  <th>Nama</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="staff in staffResults" :key="staff.staffId" @click="chooseStaff(staff)">
                  <td><strong>{{ staff.staffCode }}</strong></td>
                  <td>{{ staff.staffName }}</td>
                </tr>
                <tr v-if="!staffResults.length">
                  <td colspan="2" class="empty-state">Tidak ada staff ditemukan.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="save-actions">
            <button class="secondary-button" type="button" @click="showStaffModal = false">Tutup</button>
          </div>
        </div>
      </div>

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
