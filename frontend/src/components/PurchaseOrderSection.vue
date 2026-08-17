<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(true);
const saving = ref(false);
const error = ref('');

// Master data
const units = ref([]);
const measurements = ref([]);

// Header form
const selectedUnitCode = ref('');
const dueDate = ref(todayIso());
const poNo = ref('');
const issuedBy = ref('');
const approvedBy = ref('');
const oppNo = ref('');
const statusLabel = ref('STATUS :');
const mode = ref('init'); // init | after-save | modify | error

// Supplier
const supplierSearchOpen = ref(false);
const supplierCode = ref('');
const supplierName = ref('');
const supplierAddress = ref('');
const supplierTelp = ref('');
const supplierSearchCode = ref('');
const supplierSearchName = ref('');
const supplierResults = ref([]);
const supplierSearching = ref(false);
const selectedSupplierId = ref(null);

// Bandbox NO. OP
const poSearchOpen = ref(false);
const poSearchCode = ref('');
const poSearchSupName = ref('');
const poResults = ref([]);
const poSearching = ref(false);

// Bandbox NO. OPP
const oppSearchOpen = ref(false);
const oppSearchCode = ref('');
const oppResults = ref([]);
const oppSearching = ref(false);

// Header keuangan
const discount = ref(0);
const discountType = ref('RP'); // RP | %

// Daftar item
const items = ref([]);
// {itemId, itemCode, itemName, qtyRequested, measurementCode, measurementId, qtyRemaining, cost, qtyOrder, bonus, discount, discountType}

// Pelanggan (statis seperti legacy purchaseOrder.zul)
const client = { name: 'KLINIK PANCURAN MAS', address: 'JL. RAYA SERANG - PANDEGLANG', telp: '0254-205676' };

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function fmt(value) {
  const n = Number(value) || 0;
  return n.toLocaleString('id-ID', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function itemSubtotal(row) {
  const cost = Number(row.cost) || 0;
  const qty = Number(row.qtyOrder) || 0;
  const disc = Number(row.discount) || 0;
  if (row.discountType === 'RP') return cost * qty - disc;
  return cost * qty - (disc / 100) * cost * qty;
}

const subTotal = computed(() => items.value.reduce((sum, row) => sum + itemSubtotal(row), 0));

const total = computed(() => {
  const sub = subTotal.value;
  const disc = Number(discount.value) || 0;
  if (discountType.value === 'RP') return sub - disc;
  return sub - (disc / 100) * sub;
});

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
    const masters = await request('/purchasing/purchase-order/masters');
    units.value = masters.units || [];
    measurements.value = masters.measurements || [];
    if (units.value.length) {
      selectedUnitCode.value = units.value[0].unitCode;
    }
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  poNo.value = '';
  oppNo.value = '';
  issuedBy.value = '';
  approvedBy.value = '';
  statusLabel.value = 'STATUS :';
  selectedSupplierId.value = null;
  supplierCode.value = '';
  supplierName.value = '';
  supplierAddress.value = '';
  supplierTelp.value = '';
  discount.value = 0;
  discountType.value = 'RP';
  items.value = [];
  dueDate.value = todayIso();
  poResults.value = [];
  oppResults.value = [];
  mode.value = 'init';
}

// ===== Bandbox NO. OP =====
function openPoSearch() {
  poSearchOpen.value = true;
  poSearchCode.value = '';
  poSearchSupName.value = '';
  poResults.value = [];
}

async function searchPo() {
  poSearching.value = true;
  try {
    poResults.value = await request(
      `/purchasing/purchase-order/po/search?poCode=${encodeURIComponent(poSearchCode.value)}&supName=${encodeURIComponent(poSearchSupName.value)}`
    );
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    poSearching.value = false;
  }
}

async function pickPo(item) {
  poNo.value = item.poCode;
  poSearchOpen.value = false;
  error.value = '';
  loading.value = true;
  try {
    const detail = await request(`/purchasing/purchase-order/po/detail?poCode=${encodeURIComponent(item.poCode)}`);
    applyPoDetail(detail);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function applyPoDetail(detail) {
  oppNo.value = detail.oppNo || '';
  issuedBy.value = detail.issuerName || '';
  approvedBy.value = detail.approvedByName || '';
  statusLabel.value = 'STATUS : ' + detail.status;
  selectedSupplierId.value = detail.supplierId;
  supplierCode.value = `${detail.supplierCode}-${detail.supplierName}`;
  supplierName.value = detail.supplierName || '';
  supplierAddress.value = detail.supplierAddress || '';
  supplierTelp.value = detail.supplierTelp || '';
  if (detail.dueDate) dueDate.value = detail.dueDate;
  discount.value = detail.discount || 0;
  discountType.value = detail.discountType === '%' ? '%' : 'RP';
  items.value = (detail.items || []).map((row) => ({
    itemId: row.itemId,
    itemCode: row.itemCode,
    itemName: row.itemName,
    qtyRequested: null,
    measurementCode: row.measurementCode || '-',
    measurementId: row.measurementId != null ? row.measurementId : findMeasurementId(row.measurementCode),
    qtyRemaining: null,
    cost: row.cost || 0,
    qtyOrder: row.qtyOrdered || 0,
    bonus: row.bonus || 0,
    discount: row.discount || 0,
    discountType: row.discountType === '%' ? '%' : 'RP'
  }));
  mode.value = 'after-save';
}

// ===== Bandbox NO. OPP =====
function openOppSearch() {
  oppSearchOpen.value = true;
  oppSearchCode.value = '';
  oppResults.value = [];
}

async function searchOpp() {
  oppSearching.value = true;
  try {
    oppResults.value = await request(`/purchasing/purchase-order/opp/search?prCode=${encodeURIComponent(oppSearchCode.value)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    oppSearching.value = false;
  }
}

async function pickOpp(item) {
  oppNo.value = item.prCode;
  oppSearchOpen.value = false;
  error.value = '';
  loading.value = true;
  try {
    const detail = await request(`/purchasing/purchase-order/opp/detail?prCode=${encodeURIComponent(item.prCode)}`);
    applyOppDetail(detail);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function applyOppDetail(detail) {
  selectedSupplierId.value = detail.supplierId;
  supplierCode.value = `${detail.supplierCode}-${detail.supplierName}`;
  supplierName.value = detail.supplierName || '';
  supplierAddress.value = detail.supplierAddress || '';
  supplierTelp.value = detail.supplierTelp || '';
  items.value = (detail.lines || []).map((row) => ({
    itemId: row.itemId,
    itemCode: row.itemCode,
    itemName: row.itemName,
    qtyRequested: row.qtyRequested || 0,
    measurementCode: row.measurementCode || '-',
    measurementId: row.measurementId != null ? row.measurementId : findMeasurementId(row.measurementCode),
    qtyRemaining: row.qtyRemaining != null ? row.qtyRemaining : (row.qtyRequested || 0),
    cost: row.lastPrice || 0,
    qtyOrder: 0,
    bonus: 0,
    discount: 0,
    discountType: 'RP'
  }));
  mode.value = 'init';
}

function findMeasurementId(code) {
  const m = measurements.value.find((x) => x.earlyQuantify === code || x.endQuantify === code);
  return m ? m.measurementId : null;
}

// ===== Bandbox SUPPLIER =====
function openSupplierSearch() {
  supplierSearchOpen.value = true;
  supplierSearchCode.value = '';
  supplierSearchName.value = '';
  supplierResults.value = [];
}

async function searchSuppliers() {
  supplierSearching.value = true;
  try {
    supplierResults.value = await request(
      `/purchasing/purchase-order/suppliers/search?code=${encodeURIComponent(supplierSearchCode.value)}&name=${encodeURIComponent(supplierSearchName.value)}`
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
  supplierName.value = sup.vendorName || '';
  supplierAddress.value = sup.vendorAddress || '';
  supplierTelp.value = sup.vendorContactNo || '';
  supplierSearchOpen.value = false;
  supplierResults.value = [];
}

// ===== Hapus item =====
function removeItem(index) {
  items.value.splice(index, 1);
}

// ===== Aksi =====
function validateInput() {
  if (!items.value.length) {
    alert('LIST ITEM KOSONG, TIDAK DAPAT DISIMPAN!');
    return false;
  }
  for (const row of items.value) {
    const cost = Number(row.cost);
    const qty = Number(row.qtyOrder) || 0;
    const bonus = Number(row.bonus) || 0;
    const disc = Number(row.discount);
    if (!(cost >= 1)) {
      alert('INPUT DATA TIDAK VALID! MOHON DI-CEK ULANG');
      return false;
    }
    if (qty + bonus < 1) {
      alert('INPUT DATA TIDAK VALID! MOHON DI-CEK ULANG');
      return false;
    }
    if (disc == null || isNaN(disc) || disc < 0) {
      alert('INPUT DATA TIDAK VALID! MOHON DI-CEK ULANG');
      return false;
    }
    if (mode.value === 'init' && row.qtyRequested != null && qty > row.qtyRequested) {
      alert('OP TIDAK DAPAT DISIMPAN, NILAI ORDER LEBIH BESAR DARI YANG TELAH DIAPPROVED...!');
      return false;
    }
  }
  if (Number(discount.value) < 0) {
    alert('INPUT DATA TIDAK VALID! MOHON DI-CEK ULANG');
    return false;
  }
  return true;
}

function buildLines() {
  return items.value.map((row) => ({
    itemId: row.itemId,
    cost: Number(row.cost) || 0,
    qtyOrdered: Number(row.qtyOrder) || 0,
    measurementId: row.measurementId,
    bonus: Number(row.bonus) || 0,
    discount: Number(row.discount) || 0,
    discountType: row.discountType
  }));
}

async function save() {
  error.value = '';
  if (!oppNo.value) {
    alert('DATA PURCHASE REQUEST TIDAK VALID! MOHON DI-CEK ULANG');
    return;
  }
  if (!selectedSupplierId.value) {
    alert('DATA SUPPLIER TIDAK VALID! MOHON DI-CEK ULANG');
    return;
  }
  if (!validateInput()) return;
  if (!confirm('Data akan disimpan. Lanjutkan?')) return;

  saving.value = true;
  try {
    if (poNo.value) {
      await request('/purchasing/purchase-order/update', {
        method: 'POST',
        body: JSON.stringify({
          poCode: poNo.value,
          dueDate: dueDate.value,
          subtotal: subTotal.value,
          discount: Number(discount.value) || 0,
          discountType: discountType.value,
          total: total.value,
          lines: buildLines()
        })
      });
      alert('Data Berhasil Diubah!');
      statusLabel.value = 'STATUS : OPEN';
    } else {
      const result = await request('/purchasing/purchase-order', {
        method: 'POST',
        body: JSON.stringify({
          unitCode: selectedUnitCode.value,
          prCode: oppNo.value,
          supplierId: selectedSupplierId.value,
          dueDate: dueDate.value,
          subtotal: subTotal.value,
          discount: Number(discount.value) || 0,
          discountType: discountType.value,
          total: total.value,
          lines: buildLines()
        })
      });
      poNo.value = result.poCode;
      issuedBy.value = result.issuerName || '';
      statusLabel.value = 'STATUS : ' + result.status;
      alert(`PEMBUATAN ORDER PEMBELIAN BERHASIL..!\nNO OP : ${result.poCode}`);
    }
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
  if (poNo.value) {
    pickPo({ poCode: poNo.value });
  }
}

function newForm() {
  resetForm();
}

async function revokePo() {
  if (!poNo.value) {
    alert('Internal Error! OP No Tidak Ada!');
    return;
  }
  if (!confirm('OP akan di Batalkan, Anda Yakin?')) return;
  try {
    await request(`/purchasing/purchase-order/revoke?poCode=${encodeURIComponent(poNo.value)}`, {
      method: 'POST'
    });
    alert('OP Telah Dibatalkan!');
    statusLabel.value = 'STATUS : REVOKED';
    mode.value = 'error';
  } catch (requestError) {
    error.value = requestError.message;
  }
}

async function closeOpp() {
  if (!oppNo.value) {
    alert('Internal Error! OPP No Tidak Ada!');
    return;
  }
  if (!confirm('Anda Yakin Akan Menutup OPP Ini?')) return;
  try {
    await request(`/purchasing/purchase-order/close-opp?prCode=${encodeURIComponent(oppNo.value)}`, {
      method: 'POST'
    });
    alert('OPP Telah Ditutup!');
    oppNo.value = '';
    oppResults.value = [];
    mode.value = 'error';
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function print() {
  if (!poNo.value) {
    alert('Pilih NO. OP terlebih dahulu!');
    return;
  }
  window.open(`${props.apiBaseUrl}/purchasing/purchase-order/print?poCode=${encodeURIComponent(poNo.value)}`, '_blank');
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🛍️ ORDER PEMBELIAN</h2>
      <p class="page-subtitle">Migrasi screen legacy SC0193 — purchaseOrder.zul (Order Pembelian / OP)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data OP...</div>

    <template v-else>
      <div class="header-grid">
        <!-- ORDER PEMBELIAN -->
        <div class="card">
          <h3 class="card-title">ORDER PEMBELIAN</h3>
          <div class="field">
            <label>LOKASI TRANSAKSI</label>
            <select v-model="selectedUnitCode" :disabled="mode !== 'init'">
              <option v-for="u in units" :key="u.unitId" :value="u.unitCode">{{ u.unitCode }} - {{ u.unitName }}</option>
            </select>
          </div>
          <div class="field">
            <label>TGL. JATUH TEMPO</label>
            <input v-model="dueDate" type="date" :disabled="mode === 'after-save' || mode === 'error'" />
          </div>
          <div class="field">
            <label>NO. OP</label>
            <div class="bandbox">
              <input v-model="poNo" type="text" readonly :disabled="mode !== 'init'" @focus="openPoSearch" />
              <button class="bandbox-btn" type="button" :disabled="mode !== 'init'" @click="poSearchOpen = !poSearchOpen">▾</button>
            </div>
            <div v-if="poSearchOpen" class="opp-popup">
              <div class="popup-search-grid">
                <div class="popup-field">
                  <label>NO. OP</label>
                  <input v-model="poSearchCode" type="text" @keyup.enter="searchPo" />
                </div>
                <div class="popup-field">
                  <label>KODE SUPPLIER</label>
                  <input v-model="poSearchSupName" type="text" @keyup.enter="searchPo" />
                </div>
                <button class="small-button primary" type="button" :disabled="poSearching" @click="searchPo">CARI</button>
              </div>
              <table class="table popup-table">
                <thead><tr><th>NO. PO</th><th>NAMA SUPPLIER</th><th>TANGGAL</th></tr></thead>
                <tbody>
                  <tr v-for="item in poResults" :key="item.poCode" @click="pickPo(item)">
                    <td class="strong">{{ item.poCode }}</td>
                    <td>{{ item.supplierName }}</td>
                    <td>{{ item.createdDate }}</td>
                  </tr>
                  <tr v-if="!poResults.length"><td colspan="3" class="empty-state">Masukkan No. OP / Kode Supplier lalu tekan CARI.</td></tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label>DIBUAT OLEH</label>
            <input :value="issuedBy" readonly />
          </div>
          <div class="field">
            <label>NO. OPP</label>
            <div class="bandbox">
              <input v-model="oppNo" type="text" readonly :disabled="mode !== 'init'" @focus="openOppSearch" />
              <button class="bandbox-btn" type="button" :disabled="mode !== 'init'" @click="oppSearchOpen = !oppSearchOpen">▾</button>
            </div>
            <div v-if="oppSearchOpen" class="opp-popup">
              <div class="popup-search">
                <input v-model="oppSearchCode" type="text" placeholder="No. OPP" @keyup.enter="searchOpp" />
                <button class="small-button primary" type="button" :disabled="oppSearching" @click="searchOpp">CARI</button>
              </div>
              <table class="table popup-table">
                <thead><tr><th>NO. OPP</th><th>UNIT PEMINTA</th></tr></thead>
                <tbody>
                  <tr v-for="item in oppResults" :key="item.prCode" @click="pickOpp(item)">
                    <td class="strong">{{ item.prCode }}</td>
                    <td>{{ item.unitName }}</td>
                  </tr>
                  <tr v-if="!oppResults.length"><td colspan="2" class="empty-state">Masukkan No. OPP lalu tekan CARI.</td></tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label>DI SETUJUI OLEH</label>
            <input :value="approvedBy" readonly />
          </div>
        </div>

        <!-- SUPPLIER -->
        <div class="card">
          <h3 class="card-title">SUPPLIER</h3>
          <div class="field">
            <label>KODE SUPPLIER</label>
            <div class="bandbox">
              <input v-model="supplierCode" type="text" readonly :disabled="mode !== 'init'" @focus="openSupplierSearch" />
              <button class="bandbox-btn" type="button" :disabled="mode !== 'init'" @click="supplierSearchOpen = !supplierSearchOpen">▾</button>
            </div>
            <div v-if="supplierSearchOpen" class="opp-popup">
              <div class="popup-search-grid">
                <div class="popup-field">
                  <label>SUPPLIER CODE</label>
                  <input v-model="supplierSearchCode" type="text" @keyup.enter="searchSuppliers" />
                </div>
                <div class="popup-field">
                  <label>SUPPLIER NAME</label>
                  <input v-model="supplierSearchName" type="text" @keyup.enter="searchSuppliers" />
                </div>
                <button class="small-button primary" type="button" :disabled="supplierSearching" @click="searchSuppliers">CARI</button>
              </div>
              <table class="table popup-table">
                <thead><tr><th>SUPPLIER CODE</th><th>SUPPLIER NAME</th></tr></thead>
                <tbody>
                  <tr v-for="sup in supplierResults" :key="sup.vendorId" @click="pickSupplier(sup)">
                    <td class="strong">{{ sup.vendorCode }}</td>
                    <td>{{ sup.vendorName }}</td>
                  </tr>
                  <tr v-if="!supplierResults.length"><td colspan="2" class="empty-state">Isi minimal satu field lalu tekan CARI.</td></tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label>NAMA SUPPLIER</label>
            <input :value="supplierName" readonly />
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

        <!-- PELANGGAN -->
        <div class="card">
          <h3 class="card-title">PELANGGAN</h3>
          <div class="field">
            <label>NAMA</label>
            <input :value="client.name" readonly />
          </div>
          <div class="field">
            <label>ALAMAT</label>
            <input :value="client.address" readonly />
          </div>
          <div class="field">
            <label>NO. TELP</label>
            <input :value="client.telp" readonly />
          </div>
        </div>
      </div>

      <!-- Status -->
      <p class="status-label">{{ statusLabel }}</p>

      <!-- DAFTAR ORDER -->
      <div class="card">
        <h3 class="card-title">DAFTAR ORDER PEMBELIAN</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KODE</th>
                <th>KETERANGAN</th>
                <th class="num">ORD/A</th>
                <th>SATUAN</th>
                <th class="num">ORD/S</th>
                <th class="num">HRG SAT</th>
                <th class="num">JLH ORD.</th>
                <th>SAT ORD.</th>
                <th class="num">BONUS</th>
                <th>DISKON</th>
                <th class="num">SUBTOTAL</th>
                <th v-if="mode === 'init' || mode === 'modify'"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in items" :key="row.itemId">
                <td class="strong">{{ row.itemCode }}</td>
                <td>{{ row.itemName }}</td>
                <td class="num">{{ row.qtyRequested != null ? row.qtyRequested : '-' }}</td>
                <td>{{ row.measurementCode }}</td>
                <td class="num">{{ row.qtyRemaining != null ? row.qtyRemaining : '-' }}</td>
                <td>
                  <input v-model.number="row.cost" type="number" min="0" step="any" :disabled="mode === 'after-save' || mode === 'error'" />
                </td>
                <td>
                  <input v-model.number="row.qtyOrder" type="number" min="0" :disabled="mode === 'after-save' || mode === 'error'" />
                </td>
                <td>
                  <select v-model="row.measurementId" :disabled="mode === 'after-save' || mode === 'error'">
                    <option v-for="m in measurements" :key="m.measurementId" :value="m.measurementId">{{ m.earlyQuantify }}</option>
                  </select>
                </td>
                <td>
                  <input v-model.number="row.bonus" type="number" min="0" :disabled="mode === 'after-save' || mode === 'error'" />
                </td>
                <td>
                  <div class="disc-group">
                    <input v-model.number="row.discount" type="number" min="0" step="any" :disabled="mode === 'after-save' || mode === 'error'" />
                    <select v-model="row.discountType" :disabled="mode === 'after-save' || mode === 'error'">
                      <option value="RP">1.RP</option>
                      <option value="%">2.%</option>
                    </select>
                  </div>
                </td>
                <td class="num strong">{{ fmt(itemSubtotal(row)) }}</td>
                <td v-if="mode === 'init' || mode === 'modify'">
                  <button class="small-button" type="button" @click="removeItem(index)">🗑️</button>
                </td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="12" class="empty-state">Pilih NO. OPP untuk mengisi daftar order, atau pilih NO. OP untuk mengubah.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Aksi item + total -->
        <div class="totals-bar">
          <button class="small-button" type="button" :disabled="mode !== 'init' && mode !== 'modify'" @click="removeItem(items.length - 1)">🗑️ HAPUS</button>
          <button class="small-button" type="button" :disabled="mode !== 'init' && mode !== 'modify'" @click="() => {}">🧮 HITUNG</button>
          <span class="totals-label">SUB-TOTAL :</span>
          <span class="totals-value">{{ fmt(subTotal) }}</span>
          <span class="totals-op">-</span>
          <span class="totals-label">DISKON :</span>
          <input v-model.number="discount" type="number" min="0" step="any" :disabled="mode === 'after-save' || mode === 'error'" />
          <select v-model="discountType" :disabled="mode === 'after-save' || mode === 'error'">
            <option value="RP">1. RP</option>
            <option value="%">2. %</option>
          </select>
          <span class="totals-op">=</span>
          <span class="totals-label">TOTAL :</span>
          <span class="totals-value">{{ fmt(total) }}</span>
        </div>
      </div>

      <!-- Aksi utama -->
      <div class="action-bar">
        <button class="small-button primary" type="button" :disabled="saving || mode === 'after-save' || mode === 'error'" @click="save">💾 SIMPAN</button>
        <button class="small-button" type="button" :disabled="mode !== 'after-save'" @click="modify">✏️ UBAH</button>
        <button class="small-button" type="button" :disabled="mode !== 'modify'" @click="cancelModify">❌ BATAL</button>
        <button class="small-button" type="button" @click="newForm">🆕 BARU</button>
        <button class="small-button" type="button" :disabled="mode !== 'after-save'" @click="revokePo">🚫 PEMBATALAN ORDER</button>
        <button class="small-button" type="button" @click="closeOpp">🔒 TUTUP OPP</button>
        <button class="small-button" type="button" :disabled="!poNo" @click="print">🖨️ CETAK</button>
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </template>
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

.header-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
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
.bandbox-btn:disabled { opacity: 0.5; cursor: default; }

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
.table th, .table td { padding: 6px 8px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.table input[type="number"] { width: 70px; padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }
.table select { padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }

.disc-group { display: flex; gap: 4px; align-items: center; }
.disc-group input { width: 56px; }
.disc-group select { width: 64px; }

.strong { font-weight: 700; }
.num { text-align: right; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.totals-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 12px 4px; border-top: 1px solid #eef2f7; }
.totals-bar input { width: 90px; padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }
.totals-bar select { padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }
.totals-label { font-weight: 700; color: #304b73; font-size: 13px; }
.totals-value { font-weight: 800; color: #1f2937; font-size: 14px; min-width: 90px; }
.totals-op { font-weight: 700; color: #6b7280; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

@media (max-width: 1200px) {
  .header-grid { grid-template-columns: 1fr; }
}
</style>
