<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const activeTab = ref('request');

const masters = ref(null);
const sourceWarehouseId = ref(null);
const targetWarehouseId = ref(null);
const requestCode = ref('');

// ---- tab 1: permintaan ----
const itemSearch = ref({ code: '', name: '' });
const itemResults = ref([]);
const requestLines = ref([]);
const requestQty = ref(null);

// ---- tab 2: persetujuan ----
const pendingGroups = ref([]);
const approvalGroups = ref([]);
const selectedRequestItems = ref([]);   // irId yang dipilih (BATAL)
const selectedApprovalItems = ref([]);  // mitemId yang dipilih

// ---- tab 3: history ----
const historyFrom = ref(todayIso());
const historyTo = ref(todayIso());
const historyGroups = ref([]);

// ---- dialog ----
const toast = ref({ visible: false, message: '', type: 'success' });
const dialog = ref({ visible: false, mode: 'alert', type: 'warning', title: '', message: '', resolve: null });
let toastTimer = null;

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

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
    masters.value = await request('/item-request/masters');
    if (masters.value.sourceWarehouses.length) {
      sourceWarehouseId.value = masters.value.sourceWarehouses[0].warehouseId;
    }
    if (masters.value.targetWarehouses.length) {
      targetWarehouseId.value = masters.value.targetWarehouses[0].warehouseId;
    }
  } catch (requestError) {
    error.value = requestError.message;
  }
});

// ================= TAB 1: PERMINTAAN =================

async function searchItems() {
  if (!targetWarehouseId.value) {
    await showAlert('PILIH GUDANG TUJUAN TERLEBIH DAHULU!');
    return;
  }
  if (!itemSearch.value.code && !itemSearch.value.name) {
    await showAlert('Salah satu field (kode/nama) harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    itemResults.value = await request(`/item-request/items${qs({
      warehouseId: targetWarehouseId.value,
      code: itemSearch.value.code,
      name: itemSearch.value.name
    })}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function addItem(result) {
  const existing = requestLines.value.find((line) => line.itemId === result.itemId);
  if (existing) {
    showAlert(`${result.name} sudah ada dalam daftar.`);
    return;
  }
  requestLines.value.push({
    itemId: result.itemId,
    code: result.code,
    name: result.name,
    unit: result.unit,
    stock: result.stock,
    qty: requestQty.value || 1
  });
  requestQty.value = null;
  itemSearch.value = { code: '', name: '' };
  itemResults.value = [];
}

function removeLine(index) {
  requestLines.value.splice(index, 1);
}

async function sendRequest() {
  if (!requestLines.value.length) {
    await showAlert('ISILAH ITEM PERMINTAAN DULU!!');
    return;
  }
  for (const line of requestLines.value) {
    if (!line.qty || line.qty <= 0) {
      await showAlert(`JUMLAH ITEM ${line.name} HARUS DIISI (POSITIF)!`);
      return;
    }
    if (line.qty > line.stock) {
      await showAlert(`PERMINTAAN ${line.name} TDK BOLEH MELEBIHI STOK (${line.stock})!`);
      return;
    }
  }
  const ok = await showConfirm(`Kirim permintaan O-BM (${requestLines.value.length} item)?`);
  if (!ok) return;
  loading.value = true;
  error.value = '';
  try {
    const result = await request('/item-request/requests', {
      method: 'POST',
      body: JSON.stringify({
        sourceWarehouseId: sourceWarehouseId.value,
        targetWarehouseId: targetWarehouseId.value,
        lines: requestLines.value.map((line) => ({ itemId: line.itemId, qty: line.qty }))
      })
    });
    showToast(result.message || 'PERMINTAAN TELAH DIKIRIM', 'success');
    requestCode.value = result.requestCode;
    requestLines.value = [];
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function newRequest() {
  requestLines.value = [];
  requestCode.value = '';
  itemSearch.value = { code: '', name: '' };
  itemResults.value = [];
  requestQty.value = null;
}

// ================= TAB 2: PERSETUJUAN =================

async function loadApprovalData() {
  if (!sourceWarehouseId.value) {
    await showAlert('LOKASI TRANSAKSI HARUS DIISI!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    pendingGroups.value = await request(`/item-request/requests${qs({ sourceWarehouseId: sourceWarehouseId.value })}`);
    approvalGroups.value = await request(`/item-request/approvals${qs({ sourceWarehouseId: sourceWarehouseId.value })}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function toggleRequestSelection(irId) {
  const index = selectedRequestItems.value.indexOf(irId);
  if (index >= 0) selectedRequestItems.value.splice(index, 1);
  else selectedRequestItems.value.push(irId);
}

function toggleApprovalSelection(mutationId) {
  const index = selectedApprovalItems.value.indexOf(mutationId);
  if (index >= 0) selectedApprovalItems.value.splice(index, 1);
  else selectedApprovalItems.value.push(mutationId);
}

async function cancelRequest() {
  if (!selectedRequestItems.value.length) {
    await showAlert('PILIH ITEM PERMINTAAN TERLEBIH DAHULU!');
    return;
  }
  const ok = await showConfirm(`Batalkan ${selectedRequestItems.value.length} item permintaan?`);
  if (!ok) return;
  loading.value = true;
  error.value = '';
  try {
    for (const irId of selectedRequestItems.value) {
      await request(`/item-request/requests/${irId}/cancel`, { method: 'POST' });
    }
    selectedRequestItems.value = [];
    showToast('PEMBATALAN BERHASIL', 'success');
    await loadApprovalData();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function approveSelected() {
  if (!selectedApprovalItems.value.length) {
    await showAlert('PILIH ITEM PERSETUJUAN TERLEBIH DAHULU!');
    return;
  }
  const ok = await showConfirm(`Setujui penerimaan ${selectedApprovalItems.value.length} item?`);
  if (!ok) return;
  loading.value = true;
  error.value = '';
  try {
    for (const mutationId of selectedApprovalItems.value) {
      await request(`/item-request/approvals/${mutationId}/approve`, { method: 'POST' });
    }
    selectedApprovalItems.value = [];
    showToast('PERSETUJUAN BERHASIL DISIMPAN', 'success');
    await loadApprovalData();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function cancelApproval() {
  if (!selectedApprovalItems.value.length) {
    await showAlert('PILIH ITEM PERSETUJUAN TERLEBIH DAHULU!');
    return;
  }
  const ok = await showConfirm(`Batalkan ${selectedApprovalItems.value.length} item persetujuan?`);
  if (!ok) return;
  loading.value = true;
  error.value = '';
  try {
    for (const mutationId of selectedApprovalItems.value) {
      await request(`/item-request/approvals/${mutationId}/cancel`, { method: 'POST' });
    }
    selectedApprovalItems.value = [];
    showToast('PEMBATALAN BERHASIL', 'success');
    await loadApprovalData();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// ================= TAB 3: HISTORY =================

async function loadHistory() {
  if (!sourceWarehouseId.value) {
    await showAlert('LOKASI TRANSAKSI HARUS DIISI!');
    return;
  }
  if (!historyFrom.value || !historyTo.value) {
    await showAlert('TANGGAL HARUS DI ISI!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    historyGroups.value = await request(`/item-request/history${qs({
      sourceWarehouseId: sourceWarehouseId.value,
      from: historyFrom.value,
      to: historyTo.value
    })}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function printHistory() {
  window.open(`${props.apiBaseUrl}/item-request/history/print${qs({
    sourceWarehouseId: sourceWarehouseId.value,
    from: historyFrom.value,
    to: historyTo.value
  })}`, '_blank');
}

// ================= helpers =================

function sourceWarehouseName() {
  if (!masters.value || !sourceWarehouseId.value) return '-';
  const found = masters.value.sourceWarehouses.find((w) => w.warehouseId === sourceWarehouseId.value);
  return found ? `${found.name}` : '-';
}

function targetWarehouseName(id) {
  if (!masters.value) return '-';
  const found = masters.value.targetWarehouses.find((w) => w.warehouseId === id);
  return found ? found.name : '-';
}

function statusLabel(status) {
  return status === 2 ? 'SELESAI' : status === 0 ? 'BARU' : '';
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📦 PERMINTAAN O-BM</h2>
      <p class="page-subtitle">Migrasi screen legacy SC0174 — itemRequest.zul (Permintaan / Persetujuan / History O-BM)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="tabs">
        <button class="tab" :class="{ active: activeTab === 'request' }" type="button" @click="activeTab = 'request'">📝 PERMINTAAN O-BM</button>
        <button class="tab" :class="{ active: activeTab === 'approved' }" type="button" @click="activeTab = 'approved'; loadApprovalData()">✅ PERSETUJUAN PERMINTAAN O-BM</button>
        <button class="tab" :class="{ active: activeTab === 'history' }" type="button" @click="activeTab = 'history'; loadHistory()">🕘 HISTORY PERMINTAAN</button>
      </div>

      <!-- ==================== TAB 1: PERMINTAAN ==================== -->
      <div v-if="activeTab === 'request'" class="section-body">
        <div class="form-grid">
          <div class="field">
            <label>LOKASI TRANSAKSI</label>
            <select v-model="sourceWarehouseId">
              <option v-for="w in (masters?.sourceWarehouses || [])" :key="w.warehouseId" :value="w.warehouseId">
                {{ w.warehouseId }}. {{ w.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>NAMA GUDANG TUJUAN</label>
            <select v-model="targetWarehouseId" @change="itemResults = []; requestLines = []">
              <option v-for="w in (masters?.targetWarehouses || [])" :key="w.warehouseId" :value="w.warehouseId">
                {{ w.warehouseId }}. {{ w.name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>NO. PERMINTAAN</label>
            <input :value="requestCode" readonly placeholder="-" />
          </div>
        </div>

        <div class="section-title">DATA OBAT - BAHAN MEDIS</div>
        <div class="search-row">
          <div class="field"><label>KODE</label><input v-model="itemSearch.code" /></div>
          <div class="field"><label>NAMA</label><input v-model="itemSearch.name" /></div>
          <div class="field"><label>JUMLAH</label><input v-model.number="requestQty" type="number" min="1" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchItems">🔍 CARI</button>
        </div>

        <div v-if="itemResults.length" class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th><th>NAMA</th><th class="num">JML STOK</th><th>SATUAN</th>
                <th class="num">JUMLAH</th><th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in itemResults" :key="r.itemId" @click="addItem(r)">
                <td class="strong">{{ r.code }}</td>
                <td>{{ r.name }}</td>
                <td class="num">{{ r.stock }}</td>
                <td>{{ r.unit }}</td>
                <td class="num">{{ requestQty || 1 }}</td>
                <td>➕</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th><th>NAMA</th><th class="num">JML STOK</th><th>SATUAN</th>
                <th class="num">JUMLAH</th><th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(line, index) in requestLines" :key="index">
                <td class="strong">{{ line.code }}</td>
                <td>{{ line.name }}</td>
                <td class="num">{{ line.stock }}</td>
                <td>{{ line.unit }}</td>
                <td class="num"><input v-model.number="line.qty" class="num-input" type="number" min="1" /></td>
                <td><button class="mini danger" type="button" @click="removeLine(index)">✖</button></td>
              </tr>
              <tr v-if="!requestLines.length">
                <td colspan="6" class="empty-state">Belum ada item. Cari item lalu klik baris hasil untuk menambahkan.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="action-bar">
          <button class="small-button primary" type="button" :disabled="loading" @click="sendRequest">📤 KIRIM</button>
          <button class="small-button" type="button" @click="newRequest">🆕 BARU</button>
          <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
        </div>
      </div>

      <!-- ==================== TAB 2: PERSETUJUAN ==================== -->
      <div v-else-if="activeTab === 'approved'" class="section-body">
        <div class="approve-layout">
          <div class="approve-col">
            <div class="section-title">LIST PERMINTAAN OBAT-BAHAN MEDIS</div>
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>NO. PERMINTAAN</th><th>JLH TERIMA</th><th>SATUAN</th>
                    <th class="num">JLH ORDER</th><th class="num">SISA</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="(group, gi) in pendingGroups" :key="'g' + gi">
                    <tr class="group-row">
                      <td class="strong" colspan="6">{{ group.requestCode }} [{{ group.targetWarehouseName }}]</td>
                    </tr>
                    <tr v-for="(item, ii) in group.items" :key="'i' + gi + '-' + ii" :class="{ selected: selectedRequestItems.includes(item.irId) }">
                      <td>{{ item.name }}</td>
                      <td>{{ item.qtySent }}</td>
                      <td>{{ item.unit }}</td>
                      <td class="num">{{ item.qtyReq }}</td>
                      <td class="num">{{ item.sisa }}</td>
                      <td><input type="checkbox" :checked="selectedRequestItems.includes(item.irId)" @change="toggleRequestSelection(item.irId)" /></td>
                    </tr>
                  </template>
                  <tr v-if="!pendingGroups.length">
                    <td colspan="6" class="empty-state">Tidak ada permintaan yang belum terkirim penuh.</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="action-bar">
              <button class="small-button danger" type="button" @click="cancelRequest">🚫 BATAL</button>
            </div>
          </div>

          <div class="approve-col">
            <div class="section-title">LIST PERSETUJUAN OBAT-BAHAN MEDIS</div>
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>NO. PERMINTAAN</th><th>JLH TERIMA</th><th>SATUAN</th>
                    <th class="num">JLH ORDER</th><th class="num">SISA</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="(group, gi) in approvalGroups" :key="'g' + gi">
                    <tr class="group-row">
                      <td class="strong" colspan="6">{{ group.requestCode }} [{{ group.targetWarehouseName }}]</td>
                    </tr>
                    <tr v-for="(item, ii) in group.items" :key="'i' + gi + '-' + ii" :class="{ selected: selectedApprovalItems.includes(item.mutationId) }">
                      <td>{{ item.name }}</td>
                      <td>{{ item.qty }}</td>
                      <td>{{ item.unit }}</td>
                      <td class="num">{{ item.qty }}</td>
                      <td class="num">0</td>
                      <td><input type="checkbox" :checked="selectedApprovalItems.includes(item.mutationId)" @change="toggleApprovalSelection(item.mutationId)" /></td>
                    </tr>
                  </template>
                  <tr v-if="!approvalGroups.length">
                    <td colspan="6" class="empty-state">Tidak ada item menunggu persetujuan.</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="action-bar">
              <button class="small-button success" type="button" @click="approveSelected">✔ DI SETUJUI</button>
              <button class="small-button" type="button" @click="loadApprovalData">🔄 REFRESH</button>
              <button class="small-button danger" type="button" @click="cancelApproval">🚫 BATAL</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== TAB 3: HISTORY ==================== -->
      <div v-else class="section-body">
        <div class="search-row">
          <div class="field"><label>TANGGAL</label><input v-model="historyFrom" type="date" /></div>
          <span class="sdot">S.D.</span>
          <div class="field"><label>&nbsp;</label><input v-model="historyTo" type="date" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="loadHistory">🔍 CARI</button>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>NO. PERMINTAAN</th><th>NAMA GUDANG TUJUAN</th><th>SATUAN</th>
                <th class="num">JLH ORDER</th><th class="num">JLH TERIMA</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="(group, gi) in historyGroups" :key="'g' + gi">
                <tr class="group-row">
                  <td class="strong" colspan="5">{{ group.requestCode }} [{{ group.sourceWarehouseName }}] · {{ group.date }}</td>
                </tr>
                <tr v-for="(item, ii) in group.items" :key="'i' + gi + '-' + ii">
                  <td>{{ item.name }}</td>
                  <td>{{ group.targetWarehouseName }}</td>
                  <td>{{ item.unit }}</td>
                  <td class="num">{{ item.qtyReq }}</td>
                  <td class="num">{{ item.qtySent }}</td>
                </tr>
              </template>
              <tr v-if="!historyGroups.length">
                <td colspan="5" class="empty-state">Pilih rentang tanggal lalu tekan CARI.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="action-bar">
          <button class="small-button" type="button" :disabled="!historyGroups.length" @click="printHistory">🖨️ CETAK</button>
        </div>
      </div>
    </div>

    <!-- ==================== DIALOG ==================== -->
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

    <!-- ==================== TOAST ==================== -->
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

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.section-body { margin-top: 8px; }

.tabs { display: flex; gap: 8px; margin-bottom: 12px; border-bottom: 2px solid #eef2f7; padding-bottom: 8px; flex-wrap: wrap; }
.tab { padding: 8px 16px; border: 1px solid #d1d9e6; border-radius: 8px; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.tab.active { background: #304b73; color: #fff; border-color: #304b73; }

.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }

.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px 18px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input, .field select { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly] { background: #f3f5f8; color: #4b5563; }

.search-row { display: flex; align-items: flex-end; gap: 12px; flex-wrap: wrap; margin-bottom: 10px; }
.search-row .field { min-width: 160px; }
.sdot { font-weight: 800; color: #304b73; padding-bottom: 8px; }

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
.num-input { width: 80px; padding: 4px 6px; border: 1px solid #d1d9e6; border-radius: 4px; text-align: right; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button.success { background: #e7f6ec; color: #177245; border-color: #177245; }
.small-button.danger { background: #fde8ea; color: #a32943; border-color: #a32943; }
.small-button:disabled { opacity: 0.5; cursor: default; }
.mini { padding: 4px 8px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 700; }
.mini.danger { color: #a32943; border-color: #a32943; }

.approve-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

/* dialog & toast */
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

@media (max-width: 960px) {
  .form-grid { grid-template-columns: 1fr; }
  .approve-layout { grid-template-columns: 1fr; }
}
</style>
