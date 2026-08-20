<script setup>
import { onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const saving = ref(false);
const error = ref('');

// Master data
const units = ref([]);
const measurements = ref([]);

// Form header
const selectedUnitId = ref(null);
const oppNo = ref('');
const issuedBy = ref('');
const statusLabel = ref('STATUS :');

// Supplier
const supplierSearchOpen = ref(false);
const supplierCode = ref('');
const supplierName = ref('');
const supplierAddress = ref('');
const supplierTelp = ref('');
const supplierSearchResults = ref([]);
const supplierSearching = ref(false);
const selectedSupplierId = ref(null);

// OPP bandbox
const oppSearchOpen = ref(false);
const oppSearchCode = ref('');
const oppResults = ref([]);
const oppSearching = ref(false);

// Dialog TAMBAH ITEM
const addItemOpen = ref(false);
const addItemCode = ref('');
const addItemName = ref('');
const addItemResults = ref([]);
const addItemSearching = ref(false);
const addItemSelected = ref([]);

// Daftar item
const items = ref([]); // {itemId, itemCode, itemName, itemGroupCode, stock, bufferLimit, maxOrder, measurementCode, measurementId, qtyOrder}
const mode = ref('init'); // init | after-save | modify

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

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    const masters = await request('/purchasing/purchase-request/masters');
    units.value = masters.units || [];
    measurements.value = masters.measurements || [];
    if (units.value.length) {
      selectedUnitId.value = units.value[0].unitId;
    }
    issuedBy.value = 'DIBUAT OLEH';
    statusLabel.value = 'STATUS :';
    oppNo.value = '';
    items.value = [];
    mode.value = 'init';
    await loadItems();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function loadItems() {
  error.value = '';
  try {
    const unit = units.value.find((u) => u.unitId === selectedUnitId.value);
    if (!unit || unit.warehouseId == null) {
      items.value = [];
      return;
    }
    const list = await request(`/purchasing/purchase-request/items?warehouseId=${unit.warehouseId}`);
    items.value = (list || []).map((row) => ({
      itemId: row.itemId,
      itemCode: row.itemCode,
      itemName: row.itemName,
      itemGroupCode: row.itemGroupCode,
      stock: row.stock,
      bufferLimit: row.bufferLimit,
      maxOrder: row.maxOrder,
      measurementCode: row.measurementCode,
      measurementId: row.measurementId != null ? row.measurementId : findMeasurementId(row.measurementCode),
      qtyOrder: 0,
      openOppCount: row.openOppCount || 0,
      openOppNumbers: row.openOppNumbers || ''
    }));
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function findMeasurementId(code) {
  const m = measurements.value.find((x) => x.earlyQuantify === code || x.endQuantify === code);
  return m ? m.measurementId : null;
}

function onUnitChange() {
  oppNo.value = '';
  statusLabel.value = 'STATUS :';
  items.value = [];
  loadItems();
}

// ===== OPP bandbox =====
async function searchOpp() {
  oppSearching.value = true;
  try {
    oppResults.value = await request(`/purchasing/purchase-request/opp/search?prCode=${encodeURIComponent(oppSearchCode.value)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    oppSearching.value = false;
  }
}

async function pickOpp(item) {
  oppNo.value = item.prCode;
  oppSearchOpen.value = false;
  try {
    const detail = await request(`/purchasing/purchase-request/opp/detail?prCode=${encodeURIComponent(item.prCode)}`);
    applyOppDetail(detail);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function applyOppDetail(detail) {
  issuedBy.value = detail.issuerName || '';
  statusLabel.value = 'STATUS : ' + detail.status;
  if (detail.unitId != null) {
    selectedUnitId.value = detail.unitId;
  }
  if (detail.supplierId != null) {
    selectedSupplierId.value = detail.supplierId;
    supplierCode.value = detail.supplierName || '';
    supplierAddress.value = '';
    supplierTelp.value = '';
  }
  items.value = (detail.items || []).map((row) => ({
    itemId: row.itemId,
    itemCode: row.itemCode,
    itemName: row.itemName,
    itemGroupCode: row.itemGroupCode,
    stock: row.stock || 0,
    bufferLimit: row.bufferLimit || 0,
    maxOrder: row.maxOrder || 0,
    measurementCode: row.measurementCode,
    measurementId: row.measurementId != null ? row.measurementId : findMeasurementId(row.measurementCode),
    qtyOrder: row.qtyRequested || 0,
    openOppCount: 0,
    openOppNumbers: ''
  }));
  mode.value = detail.status === 'OPEN' ? 'after-save' : 'init';
}

// ===== Supplier =====
async function searchSuppliers() {
  supplierSearching.value = true;
  try {
    supplierSearchResults.value = await request(
      `/purchasing/purchase-request/suppliers/search?code=${encodeURIComponent(supplierCode.value)}&name=${encodeURIComponent(supplierName.value)}`
    );
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    supplierSearching.value = false;
  }
}

function pickSupplier(sup) {
  selectedSupplierId.value = sup.vendorId;
  supplierCode.value = `${sup.vendorCode}-${sup.vendorName}`;
  supplierAddress.value = sup.vendorAddress || '';
  supplierTelp.value = sup.vendorContactNo || '';
  supplierSearchOpen.value = false;
  supplierSearchResults.value = [];
}

// ===== TAMBAH ITEM dialog =====
function openAddItem() {
  addItemOpen.value = true;
  addItemCode.value = '';
  addItemName.value = '';
  addItemResults.value = [];
  addItemSelected.value = [];
}

async function searchAddItems() {
  if (!addItemCode.value.trim() && !addItemName.value.trim()) {
    alert('SALAH SATU FIELD HARUS DIISI!');
    return;
  }
  addItemSearching.value = true;
  try {
    addItemResults.value = await request(
      `/purchasing/purchase-request/add-items?code=${encodeURIComponent(addItemCode.value)}&name=${encodeURIComponent(addItemName.value)}`
    );
    addItemSelected.value = [];
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    addItemSearching.value = false;
  }
}

function toggleAddItem(row) {
  const idx = addItemSelected.value.findIndex((i) => i.itemId === row.itemId);
  if (idx >= 0) {
    addItemSelected.value.splice(idx, 1);
  } else {
    addItemSelected.value.push(row);
  }
}

function addSelectedItems() {
  for (const row of addItemSelected.value) {
    if (items.value.some((i) => i.itemId === row.itemId)) continue;
    items.value.push({
      itemId: row.itemId,
      itemCode: row.itemCode,
      itemName: row.itemName,
      itemGroupCode: row.itemGroupCode,
      stock: row.stock || 0,
      bufferLimit: row.bufferLimit || 0,
      maxOrder: row.maxOrder || 0,
      measurementCode: row.measurementCode,
      measurementId: row.measurementId != null ? row.measurementId : findMeasurementId(row.measurementCode),
      qtyOrder: 0,
      openOppCount: 0,
      openOppNumbers: ''
    });
  }
  addItemOpen.value = false;
  addItemResults.value = [];
  addItemSelected.value = [];
}

// ===== Hapus item =====
function removeItem(index) {
  items.value.splice(index, 1);
}

// ===== Aksi tombol =====
function validateQty() {
  for (const row of items.value) {
    if (!row.qtyOrder || row.qtyOrder < 1) {
      alert('JUMLAH ORDER HARUS DIISI (MIN 1)!');
      return false;
    }
    if (row.qtyOrder > row.maxOrder) {
      alert('Gagal menyimpan, Pemesanan diatas Max Order');
      return false;
    }
    if (row.stock >= row.bufferLimit) {
      alert('Gagal menyimpan, Stock diatas Buffer');
      return false;
    }
  }
  return true;
}

async function save() {
  error.value = '';
  if (!selectedSupplierId.value) {
    alert('Isi Data Supplier Terlebih Dahulu...!');
    return;
  }
  if (!items.value.length) {
    alert('DAFTAR ITEM BELUM ADA.');
    return;
  }
  if (!validateQty()) return;

  if (!confirm('Anda yakin akan menyimpan data?')) return;

  saving.value = true;
  try {
    const body = {
      unitId: selectedUnitId.value,
      supplierId: selectedSupplierId.value,
      lines: items.value.map((row) => ({
        itemId: row.itemId,
        qtyRequested: row.qtyOrder,
        itemMeasurementId: row.measurementId
      }))
    };
    if (oppNo.value) {
      await request('/purchasing/purchase-request/update', {
        method: 'POST',
        body: JSON.stringify({ ...body, prCode: oppNo.value })
      });
      alert('Data Berhasil Diubah!');
    } else {
      const result = await request('/purchasing/purchase-request', {
        method: 'POST',
        body: JSON.stringify(body)
      });
      oppNo.value = result.prCode;
      alert('Data Berhasil Disimpan! NO. OPP : ' + result.prCode);
    }
    statusLabel.value = 'STATUS : OPEN';
    mode.value = 'after-save';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

function modify() {
  mode.value = 'modify';
}

function cancelModify() {
  error.value = '';
  if (oppNo.value) {
    pickOpp({ prCode: oppNo.value });
  } else {
    mode.value = 'init';
  }
}

function newForm() {
  oppNo.value = '';
  statusLabel.value = 'STATUS :';
  issuedBy.value = 'DIBUAT OLEH';
  selectedSupplierId.value = null;
  supplierCode.value = '';
  supplierAddress.value = '';
  supplierTelp.value = '';
  items.value = [];
  mode.value = 'init';
  loadItems();
}

async function revokeOpp() {
  if (!oppNo.value) {
    alert('NO. OPP WAJIB DIISI');
    return;
  }
  if (!confirm('Apakah anda yakin akan membatalkan order ini?')) return;
  try {
    await request(`/purchasing/purchase-request/revoke?prCode=${encodeURIComponent(oppNo.value)}`, {
      method: 'POST'
    });
    alert('PEMBATALAN ORDER BERHASIL');
    statusLabel.value = 'STATUS : REVOKED';
    mode.value = 'init';
    oppResults.value = [];
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function closeForm() {
  newForm();
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🛒 ORDER PERMINTAAN PEMBELIAN</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data OPP...</div>

    <template v-else>
      <!-- Header OPP + SUPPLIER -->
      <div class="header-grid">
        <div class="card">
          <h3 class="card-title">OPP</h3>
          <div class="field">
            <label>LOKASI TRANSAKSI</label>
            <select v-model="selectedUnitId" :disabled="mode === 'after-save'" @change="onUnitChange">
              <option v-for="u in units" :key="u.unitId" :value="u.unitId">{{ u.unitCode }} - {{ u.unitName }}</option>
            </select>
          </div>
          <div class="field">
            <label>NO. OPP</label>
            <div class="bandbox">
              <input v-model="oppNo" type="text" readonly :disabled="mode === 'after-save'" @focus="oppSearchOpen = true" />
              <button class="bandbox-btn" type="button" @click="oppSearchOpen = !oppSearchOpen">▾</button>
            </div>
            <div v-if="oppSearchOpen" class="opp-popup">
              <div class="popup-search">
                <input v-model="oppSearchCode" type="text" placeholder="No. OPP" />
                <button class="small-button primary" type="button" :disabled="oppSearching" @click="searchOpp">CARI</button>
              </div>
              <table class="table popup-table">
                <thead><tr><th>NO. OPP</th><th>UNIT PEMINTA</th></tr></thead>
                <tbody>
                  <tr v-for="item in oppResults" :key="item.prCode" @click="pickOpp(item)">
                    <td class="strong">{{ item.prCode }}</td>
                    <td>{{ item.unitName }}</td>
                  </tr>
                  <tr v-if="!oppResults.length"><td colspan="2" class="empty-state">Tidak ada data.</td></tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label>DI BUAT OLEH</label>
            <input :value="issuedBy" readonly />
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">SUPPLIER</h3>
          <div class="field">
            <label>SUPPLIER</label>
            <div class="bandbox">
              <input v-model="supplierCode" type="text" readonly @focus="supplierSearchOpen = true" />
              <button class="bandbox-btn" type="button" @click="supplierSearchOpen = !supplierSearchOpen">▾</button>
            </div>
            <div v-if="supplierSearchOpen" class="opp-popup">
              <div class="popup-search-grid">
                <div class="popup-field">
                  <label>SUPPLIER CODE</label>
                  <input v-model="supplierCode" type="text" placeholder="Kode supplier" />
                </div>
                <div class="popup-field">
                  <label>SUPPLIER NAME</label>
                  <input v-model="supplierName" type="text" placeholder="Nama supplier" />
                </div>
                <button class="small-button primary" type="button" :disabled="supplierSearching" @click="searchSuppliers">CARI</button>
              </div>
              <table class="table popup-table">
                <thead><tr><th>SUPPLIER CODE</th><th>SUPPLIER NAME</th></tr></thead>
                <tbody>
                  <tr v-for="sup in supplierSearchResults" :key="sup.vendorId" @click="pickSupplier(sup)">
                    <td class="strong">{{ sup.vendorCode }}</td>
                    <td>{{ sup.vendorName }}</td>
                  </tr>
                  <tr v-if="!supplierSearchResults.length"><td colspan="2" class="empty-state">Isi minimal satu field lalu tekan CARI.</td></tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label>ALAMAT</label>
            <input :value="supplierAddress" readonly />
          </div>
          <div class="field">
            <label>NO. TELP</label>
            <input :value="supplierTelp" readonly />
          </div>
        </div>
      </div>

      <!-- Status -->
      <p class="status-label">{{ statusLabel }}</p>

      <!-- DAFTAR ORDER -->
      <div class="card">
        <h3 class="card-title">DAFTAR ORDER PERMINTAAN PEMBELIAN</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>KETERANGAN</th>
                <th>JENIS</th>
                <th class="num">STOK</th>
                <th class="num">BUFFER</th>
                <th class="num">MAX ORDER</th>
                <th>SATUAN</th>
                <th class="num">JLH ORDER</th>
                <th v-if="mode !== 'after-save'"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in items" :key="row.itemId">
                <td class="strong">{{ row.itemCode }}</td>
                <td>{{ row.itemName }}</td>
                <td>{{ row.itemGroupCode }}</td>
                <td class="num">{{ row.stock }}</td>
                <td class="num">{{ row.bufferLimit }}</td>
                <td class="num">{{ row.maxOrder }}</td>
                <td>
                  <select v-model="row.measurementId" :disabled="mode === 'after-save'">
                    <option v-for="m in measurements" :key="m.measurementId" :value="m.measurementId">
                      {{ m.earlyQuantify }}
                    </option>
                  </select>
                </td>
                <td>
                  <input v-model.number="row.qtyOrder" type="number" min="0" :disabled="mode === 'after-save'" />
                </td>
                <td v-if="mode !== 'after-save'">
                  <button class="small-button" type="button" @click="removeItem(index)">🗑️</button>
                </td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="9" class="empty-state">Belum ada item. Klik TAMBAH ITEM untuk menambah.</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="item-actions" v-if="mode !== 'after-save'">
          <button class="small-button" type="button" @click="openAddItem">➕ TAMBAH ITEM</button>
        </div>
      </div>

      <!-- Aksi -->
      <div class="action-bar">
        <button class="small-button primary" type="button" :disabled="saving || mode === 'after-save'" @click="save">
          💾 SIMPAN
        </button>
        <button class="small-button" type="button" :disabled="mode === 'init' || mode === 'after-save'" @click="modify">✏️ UBAH</button>
        <button class="small-button" type="button" :disabled="mode !== 'modify'" @click="cancelModify">❌ BATAL</button>
        <button class="small-button" type="button" @click="newForm">🆕 BARU</button>
        <button class="small-button" type="button" :disabled="!oppNo || mode !== 'after-save'" @click="revokeOpp">🚫 PEMBATALAN ORDER</button>
        <button class="small-button" type="button" @click="closeForm">✅ SELESAI</button>
      </div>
    </template>

    <!-- MODAL TAMBAH ITEM -->
    <div v-if="addItemOpen" class="modal-overlay" @click.self="addItemOpen = false">
      <div class="modal-card">
        <h3 class="card-title">FORM TAMBAH ITEM</h3>
        <div class="popup-search-grid">
          <div class="popup-field">
            <label>KODE</label>
            <input v-model="addItemCode" type="text" />
          </div>
          <div class="popup-field">
            <label>NAMA</label>
            <input v-model="addItemName" type="text" />
          </div>
          <button class="small-button primary" type="button" :disabled="addItemSearching" @click="searchAddItems">CARI</button>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th></th>
                <th>KODE</th>
                <th>NAMA</th>
                <th>QTY</th>
                <th>BFR</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in addItemResults" :key="row.itemId"
                :class="{ selected: addItemSelected.some((i) => i.itemId === row.itemId) }"
                @click="toggleAddItem(row)">
                <td>{{ addItemSelected.some((i) => i.itemId === row.itemId) ? '☑' : '☐' }}</td>
                <td class="strong">{{ row.itemCode }}</td>
                <td>{{ row.itemName }}</td>
                <td class="num">{{ row.stock }}</td>
                <td class="num">{{ row.bufferLimit }}</td>
              </tr>
              <tr v-if="!addItemResults.length">
                <td colspan="5" class="empty-state">Isi KODE / NAMA lalu tekan CARI.</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="modal-actions">
          <button class="small-button primary" type="button" @click="addSelectedItems">SIMPAN</button>
          <button class="small-button" type="button" @click="addItemOpen = false">SELESAI</button>
        </div>
      </div>
    </div>
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

.header-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 16px; }
.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.field { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field select, .field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input[readonly] { background: #f6f8fb; color: #6b7280; }
.field select:disabled, .field input:disabled { background: #f6f8fb; color: #6b7280; }

.bandbox { display: flex; align-items: stretch; }
.bandbox input { flex: 1; border-top-right-radius: 0; border-bottom-right-radius: 0; }
.bandbox-btn { padding: 0 12px; border: 1px solid #d1d9e6; border-left: none; border-radius: 0 6px 6px 0; background: #f6f8fb; cursor: pointer; }

.opp-popup { border: 1px solid #d1d9e6; border-radius: 8px; padding: 10px; background: #fff; box-shadow: 0 8px 20px rgba(0,0,0,0.12); margin-top: 4px; }
.popup-search { display: flex; gap: 8px; margin-bottom: 8px; }
.popup-search input { flex: 1; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.popup-search-grid { display: flex; gap: 10px; align-items: flex-end; margin-bottom: 10px; }
.popup-field { display: flex; flex-direction: column; gap: 4px; flex: 1; }
.popup-field label { font-size: 12px; font-weight: 700; color: #304b73; }
.popup-field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }

.status-label { font-size: 13px; font-weight: 700; color: #b91c1c; margin: 0 0 8px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.selected { background: #eef3fb; }
.table input[type="number"] { width: 70px; padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }
.table select { padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }

.strong { font-weight: 700; }
.num { text-align: right; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.item-actions { display: flex; gap: 10px; }
.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal-card { background: #fff; border-radius: 12px; padding: 20px; width: 720px; max-width: 95vw; max-height: 85vh; overflow-y: auto; }
.modal-actions { display: flex; gap: 10px; margin-top: 12px; }
</style>