<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const warehouses = ref([]);
const warehouseId = ref(null);

const requestGroups = ref([]);
const selectedRequestItem = ref(null); // {group, item} dari LIST PERMINTAAN

const mutationLines = ref([]); // DETAIL ITEM: {batchId, code, name, unit, qty, irId}

// modal detail
const showDetail = ref(false);
const detailBatches = ref([]); // batch dari gudang
const detailQty = ref({});

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
  try {
    warehouses.value = await request('/item-mutation/masters');
    if (warehouses.value.length) {
      warehouseId.value = warehouses.value[0].warehouseId;
      await loadRequests();
    }
  } catch (requestError) {
    error.value = requestError.message;
  }
});

async function loadRequests() {
  if (!warehouseId.value) return;
  loading.value = true;
  error.value = '';
  try {
    requestGroups.value = await request(`/item-mutation/requests${qs({ warehouseId: warehouseId.value })}`);
    selectedRequestItem.value = null;
    mutationLines.value = [];
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function selectRequestItem(group, item) {
  selectedRequestItem.value = { group, item };
}

// ================= DETAIL =================

async function openDetail() {
  if (!selectedRequestItem.value) {
    await showAlert('PILIH ITEM PERMINTAAN TERLEBIH DAHULU!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const item = selectedRequestItem.value.item;
    detailBatches.value = await request(`/item-mutation/requests/${item.irId}/batches${qs({ warehouseId: warehouseId.value, irId: item.irId })}`);
    detailQty.value = {};
    // alokasi awal per batch (FIFO): min(sisa, stok) urut batch
    let remaining = item.sisa;
    for (const batch of detailBatches.value) {
      if (remaining <= 0) {
        detailQty.value[batch.batchId] = 0;
        continue;
      }
      const take = Math.min(remaining, batch.stock);
      detailQty.value[batch.batchId] = take;
      remaining -= take;
    }
    showDetail.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function detailTotal() {
  return detailBatches.value.reduce((sum, b) => sum + (Number(detailQty.value[b.batchId]) || 0), 0);
}

function saveDetail() {
  const item = selectedRequestItem.value.item;
  const sisa = item.sisa;
  let total = 0;
  detailBatches.value.forEach((batch) => {
    const qty = Number(detailQty.value[batch.batchId]) || 0;
    if (qty < 0) {
      showAlert('INPUT NEGATIF TIDAK DIPERBOLEHKAN!');
      return;
    }
    if (qty > batch.stock) {
      showAlert(`JUMLAH BATCH ${batch.batchId} MELEBIHI STOK (${batch.stock})!`);
      return;
    }
    total += qty;
  });
  if (total > sisa) {
    showAlert(`TOTAL MELEBIHI SISA PERMINTAAN (${sisa})!`);
    return;
  }
  if (!total) {
    showAlert('PILIH ITEM TERLEBIH DAHULU..!');
    return;
  }
  // tambahkan ke DETAIL ITEM (merge batch yang sama)
  detailBatches.value.forEach((batch) => {
    const qty = Number(detailQty.value[batch.batchId]) || 0;
    if (qty <= 0) return;
    const existing = mutationLines.value.find((l) => l.batchId === batch.batchId && l.irId === item.irId);
    if (existing) {
      existing.qty += qty;
    } else {
      mutationLines.value.push({
        irId: item.irId,
        batchId: batch.batchId,
        code: batch.code,
        name: batch.name,
        unit: batch.unit,
        qty
      });
    }
  });
  showDetail.value = false;
}

// ================= KIRIM / HAPUS =================

function deleteLine(index) {
  mutationLines.value.splice(index, 1);
}

function mutationTotal() {
  return mutationLines.value.reduce((sum, l) => sum + (Number(l.qty) || 0), 0);
}

async function send() {
  if (!mutationLines.value.length) {
    await showAlert('Isi Data Item Terlebih Dahulu..!');
    return;
  }
  const ok = await showConfirm(`Kirim ${mutationLines.value.length} baris mutasi item?`);
  if (!ok) return;
  loading.value = true;
  error.value = '';
  try {
    const result = await request('/item-mutation/send', {
      method: 'POST',
      body: JSON.stringify({
        warehouseId: warehouseId.value,
        lines: mutationLines.value.map((l) => ({ irId: l.irId, batchId: l.batchId, qty: l.qty }))
      })
    });
    showToast(result.message || 'Permintaan Telah Di Kirim..!', 'success');
    await loadRequests();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📦 FORM MUTASI ITEM</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="location-bar">
        <span class="period-label">LOKASI TRANSAKSI</span>
        <span class="period-colon">:</span>
        <select v-model="warehouseId" @change="loadRequests">
          <option v-for="w in warehouses" :key="w.warehouseId" :value="w.warehouseId">{{ w.warehouseId }}. {{ w.name }}</option>
        </select>
      </div>

      <div class="section-title">LIST PERMINTAAN ITEM</div>
      <div v-if="loading" class="loading">Memuat data...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>NO. PERMINTAAN</th>
              <th>NAMA GUDANG PEMINTA</th>
              <th>SATUAN</th>
              <th class="num">JLH ORDER</th>
              <th class="num">JLH TERIMA</th>
              <th class="num">SISA</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="(group, gi) in requestGroups" :key="'g' + gi">
              <tr class="group-row">
                <td class="strong" colspan="6">{{ group.requestCode }} · {{ group.sourceWarehouseName }}</td>
              </tr>
              <tr v-for="(item, ii) in group.items" :key="'i' + gi + '-' + ii"
                  :class="{ selected: selectedRequestItem && selectedRequestItem.item.irId === item.irId }"
                  @click="selectRequestItem(group, item)">
                <td>{{ item.name }}</td>
                <td></td>
                <td>{{ item.unit }}</td>
                <td class="num">{{ item.qtyReq }}</td>
                <td class="num">{{ item.qtySent }}</td>
                <td class="num strong">{{ item.sisa }}</td>
              </tr>
            </template>
            <tr v-if="!requestGroups.length">
              <td colspan="6" class="empty-state">Tidak ada permintaan yang harus dipenuhi.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="action-bar">
        <button class="small-button" type="button" @click="loadRequests">🔄 REFRESH</button>
        <button class="small-button primary" type="button" :disabled="loading" @click="openDetail">🔍 DETAIL</button>
      </div>

      <div class="section-title">DETAIL ITEM</div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>NO. BATCH</th>
              <th>NAMA</th>
              <th>SATUAN</th>
              <th class="num">JLH</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(line, index) in mutationLines" :key="index">
              <td class="strong">{{ line.batchId }}</td>
              <td>{{ line.name }}</td>
              <td>{{ line.unit }}</td>
              <td class="num">{{ line.qty }}</td>
              <td><button class="mini danger" type="button" @click="deleteLine(index)">✖</button></td>
            </tr>
            <tr v-if="!mutationLines.length">
              <td colspan="5" class="empty-state">Pilih item permintaan lalu tekan DETAIL untuk memilih batch.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="total-line">
        <span class="total-label">TOTAL ({{ mutationLines.length }} baris)</span>
        <span class="total-colon">:</span>
        <input class="total-input" :value="mutationTotal()" readonly />
      </div>

      <div class="action-bar">
        <button class="small-button primary" type="button" :disabled="loading" @click="send">📤 KIRIM</button>
        <button class="small-button danger" type="button" @click="mutationLines.length && deleteLine(mutationLines.length - 1)">🗑️ HAPUS</button>
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>

    <!-- ==================== MODAL: DETAIL ITEM (pilih batch) ==================== -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
      <div class="modal">
        <div class="modal-header">
          DETAIL ITEM — {{ selectedRequestItem ? selectedRequestItem.item.name : '' }}
        </div>
        <div class="modal-body">
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>NO. BATCH</th><th>NAMA</th><th>SATUAN</th><th class="num">STOK</th><th class="num">JLH</th></tr></thead>
              <tbody>
                <tr v-for="b in detailBatches" :key="b.batchId">
                  <td class="strong">{{ b.batchId }}</td>
                  <td>{{ b.name }}</td>
                  <td>{{ b.unit }}</td>
                  <td class="num">{{ b.stock }}</td>
                  <td class="num"><input v-model.number="detailQty[b.batchId]" class="num-input" type="number" min="0" /></td>
                </tr>
                <tr v-if="!detailBatches.length">
                  <td colspan="5" class="empty-state">Tidak ada stok batch untuk item ini.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="total-line">
            <span class="total-label">TOTAL DIPILIH</span>
            <span class="total-colon">:</span>
            <input class="total-input" :value="detailTotal()" readonly />
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button primary" type="button" @click="saveDetail">💾 SIMPAN</button>
          <button class="small-button" type="button" @click="showDetail = false">BATAL</button>
        </div>
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
.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }

.location-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.period-label { font-weight: 700; color: #304b73; font-size: 13px; }
.period-colon { color: #6b7280; }
.location-bar select { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; min-width: 220px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.selected { background: #e8eef8; }
.table tbody tr.group-row { background: #eef3fa; }
.table tbody tr.group-row td { border-bottom: 1px solid #dce6f2; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }
.num-input { width: 70px; padding: 4px 6px; border: 1px solid #d1d9e6; border-radius: 4px; text-align: right; }
.mini { padding: 4px 8px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 700; }
.mini.danger { color: #a32943; border-color: #a32943; }

.total-line { display: flex; align-items: center; justify-content: flex-end; gap: 8px; margin: 8px 0; }
.total-label { font-weight: 800; color: #304b73; font-size: 13px; }
.total-colon { font-weight: 800; }
.total-input { width: 140px; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-weight: 800; text-align: right; background: #f3f5f8; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button.danger { background: #fde8ea; color: #a32943; border-color: #a32943; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal { background: #fff; border-radius: 12px; width: 640px; max-width: 94vw; max-height: 88vh; display: flex; flex-direction: column; box-shadow: 0 20px 40px rgba(0,0,0,0.2); }
.modal-header { padding: 14px 18px; background: #304b73; color: #fff; font-weight: 800; border-radius: 12px 12px 0 0; }
.modal-body { padding: 14px 18px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
.modal-list { max-height: 300px; }
.modal-footer { padding: 12px 18px; border-top: 1px solid #eef2f7; display: flex; justify-content: flex-end; gap: 10px; border-radius: 0 0 12px 12px; }

.toast { position: fixed; bottom: 24px; right: 24px; z-index: 100; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-radius: 12px; font-weight: 700; font-size: 14px; color: #fff; box-shadow: 0 10px 30px rgba(15,23,42,0.25); max-width: 420px; }
.toast--success { background: linear-gradient(135deg, #177245, #1f9d5c); }
.toast--error { background: linear-gradient(135deg, #a32943, #d64567); }
.toast--info { background: linear-gradient(135deg, #304b73, #5f83c2); }
.toast-icon { font-size: 18px; }
.toast-message { flex: 1; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateY(16px); }

.dialog-fade-enter-active, .dialog-fade-leave-active { transition: all 0.25s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.92); }
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
</style>
