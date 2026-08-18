<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');

const delayAntrian = ref(null);
const textAntrian = ref('');
const editMode = ref(false); // false = readonly (setelah load/simpan), true = mode UBAH

const doctors = ref([]);
const doctorId = ref(null);
const queue = ref([]);
const selectedRegId = ref(null);

// paging queue (pageSize 10, sesuai legacy mold=paging pageSize=10)
const pageSize = 10;
const currentPage = ref(1);

const pagedQueue = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return queue.value.slice(start, start + pageSize);
});

const totalPages = computed(() => Math.max(1, Math.ceil(queue.value.length / pageSize)));

function goToPage(page) {
  currentPage.value = Math.min(Math.max(1, page), totalPages.value);
}

// dialog/toast
const toast = ref({ visible: false, message: '', type: 'success' });
const dialog = ref({ visible: false, mode: 'alert', type: 'warning', title: '', message: '', resolve: null });
let toastTimer = null;

function showToast(message, type = 'success') {
  toast.value = { visible: true, message, type };
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.value.visible = false; }, 3500);
}

function showAlert(message, options = {}) {
  return new Promise((resolve) => {
    dialog.value = { visible: true, mode: 'alert', type: options.type || 'warning',
      title: options.title || 'PERHATIAN', message, resolve };
  });
}

function showConfirm(message, options = {}) {
  return new Promise((resolve) => {
    dialog.value = { visible: true, mode: 'confirm', type: options.type || 'confirm',
      title: options.title || 'KONFIRMASI', message, resolve };
  });
}

function closeDialog(result) {
  const resolve = dialog.value.resolve;
  dialog.value.visible = false;
  if (resolve) resolve(result);
}

const dialogIcon = computed(() => ({
  warning: '⚠️', info: 'ℹ️', error: '❌', success: '✅', confirm: '❓'
}[dialog.value.type] || 'ℹ️'));

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Your session has been expired. You need to login again.');
    throw new Error(payload?.message || 'Unauthorized');
  }
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  return payload.data;
}

function qs(params) {
  const parts = [];
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      parts.push(`${key}=${encodeURIComponent(value)}`);
    }
  });
  return parts.length ? `?${parts.join('&')}` : '';
}

onMounted(async () => {
  await loadMasters();
});

async function loadMasters() {
  loading.value = true;
  error.value = '';
  try {
    const data = await request('/antrian/delay');
    delayAntrian.value = data.delayAntrian ?? null;
    textAntrian.value = data.textAntrian || '';
    doctors.value = data.doctors || [];
    editMode.value = false;
    if (doctors.value.length) {
      doctorId.value = doctors.value[0].staffId;
      await loadQueue();
    } else {
      queue.value = [];
    }
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (delayAntrian.value === null || delayAntrian.value === undefined || delayAntrian.value === '') {
    await showAlert('DELAY ANTRIAN HARUS DI ISI!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const result = await request('/antrian/delay', {
      method: 'POST',
      body: JSON.stringify({ delayAntrian: Number(delayAntrian.value), textAntrian: textAntrian.value })
    });
    showToast(result.message || 'Data Sukses Disimpan...!', 'success');
    editMode.value = false;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function edit() {
  editMode.value = true;
}

async function changeDoctor() {
  currentPage.value = 1;
  await loadQueue();
}

async function loadQueue() {
  if (!doctorId.value) return;
  loading.value = true;
  error.value = '';
  try {
    queue.value = await request(`/antrian/delay/queue${qs({ doctorId: doctorId.value })}`);
    selectedRegId.value = null;
    currentPage.value = 1;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function takeOut() {
  if (!selectedRegId.value) {
    await showAlert('PILIH DATA YANG AKAN DIKELUARKAN DARI ANTRIAN...', { title: 'INFORMASI' });
    return;
  }
  const ok = await showConfirm('Keluarkan pasien terpilih dari antrian?', { title: 'KELUARKAN DARI ANTRIAN' });
  if (!ok) return;
  loading.value = true;
  error.value = '';
  try {
    const result = await request('/antrian/delay/queue/take-out', {
      method: 'POST',
      body: JSON.stringify({ registrationId: selectedRegId.value })
    });
    showToast(result.message || 'Pasien dikeluarkan dari antrian.', 'success');
    await loadQueue();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function selectQueueRow(regId) {
  selectedRegId.value = regId;
}

function numberLabel(number) {
  if (number === null || number === undefined) return '';
  return number < 10 ? `0${number}` : `${number}`;
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📢 MASTER ANTRIAN DOKTER</h2>
      <p class="page-subtitle">Migrasi screen legacy SCM0053 — delayAntrian.zul (Delay &amp; Text Antrian, Keluarkan dari Antrian)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card card--center">
      <div class="master-box">
        <div class="field">
          <label>DELAY ANTRIAN</label>
          <div class="input-with-suffix">
            <input v-model.number="delayAntrian" type="number" min="0" :readonly="!editMode" />
            <span class="suffix">(DETIK)</span>
          </div>
        </div>
        <div class="field">
          <label>TEXT ANTRIAN</label>
          <textarea v-model="textAntrian" rows="4" :readonly="!editMode"></textarea>
        </div>
        <div class="action-bar">
          <button class="small-button primary" type="button" :disabled="loading || !editMode" @click="save">💾 SIMPAN</button>
          <button class="small-button" type="button" :disabled="editMode" @click="edit">✏️ UBAH</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="queue-layout">
        <div class="field">
          <label>DOKTER</label>
          <select v-model="doctorId" @change="changeDoctor">
            <option v-for="d in doctors" :key="d.staffId" :value="d.staffId">{{ d.name }}</option>
          </select>
        </div>
        <button class="small-button danger" type="button" @click="takeOut">🚫 KELUARKAN DARI ANTRIAN</button>
      </div>

      <div v-if="loading" class="loading">Memuat antrian...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr><th class="num">NO. ANTRIAN</th><th>NAMA PASIEN</th><th>NO. MR</th><th>TGL. REGISTRASI</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedQueue" :key="row.registrationId"
                :class="{ selected: selectedRegId === row.registrationId }"
                @click="selectQueueRow(row.registrationId)">
              <td class="num strong">{{ numberLabel(row.number) }}</td>
              <td class="strong">{{ row.patientName }}</td>
              <td>{{ row.mrCode }}</td>
              <td>{{ row.registrationDate }}</td>
            </tr>
            <tr v-if="!pagedQueue.length">
              <td colspan="4" class="empty-state">Belum ada pasien dalam antrian dokter ini.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(1)">⏮</button>
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">◀</button>
        <span class="page-info">Halaman {{ currentPage }} / {{ totalPages }}</span>
        <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">▶</button>
        <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(totalPages)">⏭</button>
      </div>

      <div class="action-bar">
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>

    <!-- ==================== DIALOG / TOAST ==================== -->
    <transition name="dialog-fade">
      <div v-if="dialog.visible" class="modal-overlay" @click.self="closeDialog(false)">
        <div class="dialog-box" :class="'dialog-box--' + dialog.type">
          <div class="dialog-icon">{{ dialogIcon }}</div>
          <div class="dialog-title">{{ dialog.title }}</div>
          <div class="dialog-message">{{ dialog.message }}</div>
          <div class="dialog-buttons">
            <template v-if="dialog.mode === 'confirm'">
              <button class="small-button danger" type="button" @click="closeDialog(false)">✖ TIDAK</button>
              <button class="small-button primary" type="button" @click="closeDialog(true)">✔ YA</button>
            </template>
            <template v-else>
              <button class="small-button primary" type="button" @click="closeDialog(true)">✔ OK</button>
            </template>
          </div>
        </div>
      </div>
    </transition>

    <transition name="toast-fade">
      <div v-if="toast.visible" class="toast" :class="'toast--' + toast.type">
        <span class="toast-icon">{{ toast.type === 'success' ? '✅' : toast.type === 'error' ? '❌' : 'ℹ️' }}</span>
        <span class="toast-message">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.card--center { display: flex; justify-content: center; }
.master-box { width: 100%; max-width: 560px; display: flex; flex-direction: column; gap: 14px; }

.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input, .field select, .field textarea { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly], .field textarea[readonly] { background: #f3f5f8; color: #4b5563; }
.field textarea { resize: vertical; font-family: inherit; }
.input-with-suffix { display: flex; align-items: center; gap: 8px; }
.input-with-suffix input { flex: 1; max-width: 160px; }
.suffix { font-weight: 700; color: #6b7280; font-size: 12px; }

.queue-layout { display: flex; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.queue-layout .field { flex: 1; min-width: 220px; }
.queue-layout .small-button { margin-bottom: 0; }

.table-wrap { overflow: auto; margin: 12px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 14px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.selected { background: #e8eef8; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 10px; margin: 12px 0 4px; flex-wrap: wrap; }
.page-btn { padding: 6px 12px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.page-btn:hover:not(:disabled) { background: #eef3fa; }
.page-btn:disabled { opacity: 0.4; cursor: default; }
.page-info { font-weight: 700; color: #304b73; font-size: 13px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button.danger { background: #fde8ea; color: #a32943; border-color: #a32943; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.dialog-box { background: #fff; border-radius: 16px; width: 380px; max-width: 92vw; padding: 26px 28px; text-align: center; box-shadow: 0 24px 50px rgba(15,23,42,0.3); border-top: 5px solid #5f83c2; }
.dialog-box--warning { border-top-color: #e6a23c; }
.dialog-box--error { border-top-color: #d64567; }
.dialog-box--success { border-top-color: #1f9d5c; }
.dialog-box--confirm { border-top-color: #5f83c2; }
.dialog-icon { font-size: 44px; margin-bottom: 8px; }
.dialog-title { font-size: 17px; font-weight: 800; color: #304b73; margin-bottom: 8px; }
.dialog-message { font-size: 14px; color: #4b5563; line-height: 1.5; margin-bottom: 18px; white-space: pre-line; }
.dialog-buttons { display: flex; justify-content: center; gap: 12px; }
.dialog-buttons .small-button { min-width: 110px; }
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: all 0.25s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.92); }

.toast { position: fixed; bottom: 24px; right: 24px; z-index: 100; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-radius: 12px; font-weight: 700; font-size: 14px; color: #fff; box-shadow: 0 10px 30px rgba(15,23,42,0.25); max-width: 420px; }
.toast--success { background: linear-gradient(135deg, #177245, #1f9d5c); }
.toast--error { background: linear-gradient(135deg, #a32943, #d64567); }
.toast--info { background: linear-gradient(135deg, #304b73, #5f83c2); }
.toast-icon { font-size: 18px; }
.toast-message { flex: 1; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateY(16px); }
</style>
