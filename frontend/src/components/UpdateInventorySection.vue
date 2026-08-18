<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const items = ref([]);
const keyword = ref('');

// ---- paging (pageSize 15, sesuai legacy mold=paging pageSize=15) ----
const pageSize = 15;
const currentPage = ref(1);

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return items.value.slice(start, start + pageSize);
});

const totalPages = computed(() => Math.max(1, Math.ceil(items.value.length / pageSize)));

const pageInfo = computed(() => {
  if (!items.value.length) return '';
  const start = (currentPage.value - 1) * pageSize + 1;
  const end = Math.min(currentPage.value * pageSize, items.value.length);
  return `${start}-${end} dari ${items.value.length} item`;
});

function goToPage(page) {
  currentPage.value = Math.min(Math.max(1, page), totalPages.value);
}

const form = ref({ itemCode: '', itemName: '', qty: null });
const editLocked = ref(false);
const selectedItemId = ref(null);

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
  await loadItems();
});

async function loadItems() {
  loading.value = true;
  error.value = '';
  try {
    items.value = await request(`/master/update-inventory/items${qs({ keyword: keyword.value })}`);
    currentPage.value = 1;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function selectItem(item) {
  selectedItemId.value = item.itemId;
}

// ================= UBAH / SIMPAN / BATAL =================

function doModify() {
  const item = items.value.find((i) => i.itemId === selectedItemId.value);
  if (!item) {
    showAlert('PILIH ITEM TERLEBIH DAHULU!');
    return;
  }
  form.value.itemCode = item.code;
  form.value.itemName = item.name;
  form.value.qty = item.jumlah;
  editLocked.value = true;
}

async function doSave() {
  if (!form.value.itemCode) {
    await showAlert('KODE ITEM HARUS DI ISI!');
    return;
  }
  if (form.value.qty === null || form.value.qty === undefined || form.value.qty === '') {
    await showAlert('JUMLAH HARUS DI ISI!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    await request('/master/update-inventory/save', {
      method: 'POST',
      body: JSON.stringify({ itemCode: form.value.itemCode, qty: Number(form.value.qty) })
    });
    showToast('DATA SUKSES DIUPDATED!', 'success');
    doCancel();
    await loadItems();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function doCancel() {
  form.value = { itemCode: '', itemName: '', qty: null };
  editLocked.value = false;
  selectedItemId.value = null;
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📦 FORM UPDATE ITEM</h2>
      <p class="page-subtitle">Migrasi screen legacy SCM0057 — updateInventory.zul (Update Stok Item di Bawah Buffer)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="form-grid">
        <div class="field">
          <label>KODE ITEM</label>
          <input v-model="form.itemCode" :readonly="editLocked" />
        </div>
        <div class="field">
          <label>NAMA ITEM</label>
          <input v-model="form.itemName" :readonly="editLocked" />
        </div>
        <div class="field">
          <label>JUMLAH</label>
          <input v-model.number="form.qty" type="number" min="0" step="0.01" :disabled="!editLocked" />
        </div>
      </div>

      <div class="action-bar">
        <button class="small-button primary" type="button" :disabled="loading" @click="doSave">💾 SIMPAN</button>
        <button class="small-button" type="button" @click="doModify">✏️ UBAH</button>
        <button class="small-button" type="button" @click="doCancel">🚫 BATAL</button>
      </div>

      <div class="section-title">DAFTAR ITEM</div>
      <div class="search-row">
        <input v-model="keyword" class="search-input" placeholder="Cari nama item..." @keyup.enter="loadItems" />
        <button class="small-button primary" type="button" :disabled="loading" @click="loadItems">🔍 CARI</button>
      </div>

      <div v-if="loading" class="loading">Memuat data...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>KODE</th>
              <th>NAMA</th>
              <th class="num">JUMLAH</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pagedItems" :key="item.itemId"
                :class="{ selected: selectedItemId === item.itemId }"
                @click="selectItem(item)">
              <td class="strong">{{ item.code }}</td>
              <td>{{ item.name }}</td>
              <td class="num">{{ item.jumlah }}</td>
            </tr>
            <tr v-if="!pagedItems.length">
              <td colspan="3" class="empty-state">
                Tidak ada item dengan stok 0 di bawah batas buffer.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(1)">⏮</button>
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">◀</button>
        <span class="page-info">Halaman {{ currentPage }} / {{ totalPages }}</span>
        <span class="page-count">{{ pageInfo }}</span>
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
            <button class="small-button primary" type="button" @click="closeDialog(true)">✔ OK</button>
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
.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }

.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px 18px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly] { background: #f3f5f8; color: #4b5563; }
.field input:disabled { background: #f3f5f8; }

.search-row { display: flex; gap: 10px; align-items: center; margin-bottom: 8px; }
.search-input { flex: 1; max-width: 320px; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
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
.page-count { color: #6b7280; font-size: 12px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.dialog-box { background: #fff; border-radius: 16px; width: 380px; max-width: 92vw; padding: 26px 28px; text-align: center; box-shadow: 0 24px 50px rgba(15,23,42,0.3); border-top: 5px solid #5f83c2; }
.dialog-box--warning { border-top-color: #e6a23c; }
.dialog-box--error { border-top-color: #d64567; }
.dialog-box--success { border-top-color: #1f9d5c; }
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

@media (max-width: 960px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
