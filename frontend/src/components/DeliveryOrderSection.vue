<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const saving = ref(false);
const error = ref('');

// Tabs
const activeTab = ref('penerimaan'); // penerimaan | batch

// ===== Tab PENERIMAAN BARANG =====
const warehouses = ref([]);
const selectedWarehouseId = ref(null);
const poNo = ref('');
const recDate = ref(todayIso());
const recBy = ref('');
const bppNo = ref('');
const approvedBy = ref('');
const statusLabel = ref('STATUS :');
const mode = ref('init'); // init | view | modify | error

// Supplier (readonly, dari OP)
const supCode = ref('');
const supName = ref('');
const supAddress = ref('');
const supTelp = ref('');

// Keuangan header
const discount = ref(0);
const discountType = ref('RP');
const ppn = ref(10);
const ppnType = ref('%');

// Bandbox NO. OP
const poSearchOpen = ref(false);
const poSearchCode = ref('');
const poSearchSupName = ref('');
const poResults = ref([]);
const poSearching = ref(false);

// Bandbox NO. BPP
const doSearchOpen = ref(false);
const doSearchCode = ref('');
const doSearchWhouse = ref('');
const doResults = ref([]);
const doSearching = ref(false);

// Daftar item BPP
const items = ref([]);
// {poDetId, itemId, itemCode, itemName, qtyOrdered, bonus, measurementCode, cost, qtySisa, bonusSisa, qtyArrived, bonusArrived, discount, discountType}

// ===== Tab INPUT BATCH NO. =====
const batchItems = ref([]); // {itemId, itemCode, itemName, initQty, initM}
const selectedBatchItemId = ref(null);
const batchNo = ref('');
const endM = ref('');
const endMOptions = ref([]);
const batchQty = ref(0);
const expiredDate = ref(todayIso());
const batchEntries = ref([]);
// {itemId, itemCode, itemName, batchNo, qty (input), expDate, finalM, multiplier, displayQty}
const batchSelected = ref(-1);
const batchLoadedDoCode = ref('');

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function fmt(value) {
  const n = Number(value) || 0;
  return n.toLocaleString('en-US', { maximumFractionDigits: 2 });
}

function itemSubtotal(row) {
  const cost = Number(row.cost) || 0;
  const qty = Number(row.qtyArrived) || 0;
  const disc = Number(row.discount) || 0;
  const orderQty = Number(row.qtyOrdered) || 0;
  if (row.discountType === 'RP') {
    const d = orderQty !== 0 ? (disc / orderQty) * qty : 0;
    return cost * qty - d;
  }
  const res = cost * qty;
  return res - (disc / 100) * res;
}

const total = computed(() => items.value.reduce((sum, row) => sum + itemSubtotal(row), 0));

const priceAfterDiscount = computed(() => {
  const disc = Number(discount.value) || 0;
  if (discountType.value === 'RP') return total.value - disc;
  return total.value - (total.value * disc) / 100;
});

const gtotal = computed(() => {
  const tax = Number(ppn.value) || 0;
  if (ppnType.value === 'RP') return priceAfterDiscount.value + tax;
  return priceAfterDiscount.value + (priceAfterDiscount.value * tax) / 100;
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
    const masters = await request('/purchasing/delivery-order/masters');
    warehouses.value = masters.warehouses || [];
    recBy.value = masters.recBy || '';
    if (warehouses.value.length) {
      selectedWarehouseId.value = warehouses.value[0].warehouseId;
    }
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  poNo.value = '';
  recDate.value = todayIso();
  bppNo.value = '';
  approvedBy.value = '';
  statusLabel.value = 'STATUS :';
  supCode.value = '';
  supName.value = '';
  supAddress.value = '';
  supTelp.value = '';
  discount.value = 0;
  discountType.value = 'RP';
  ppn.value = 10;
  ppnType.value = '%';
  items.value = [];
  poResults.value = [];
  doResults.value = [];
  batchEntries.value = [];
  batchItems.value = [];
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
      `/purchasing/delivery-order/po/search?poCode=${encodeURIComponent(poSearchCode.value)}&supName=${encodeURIComponent(poSearchSupName.value)}`
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
    const detail = await request(`/purchasing/delivery-order/po/detail?poCode=${encodeURIComponent(item.poCode)}`);
    applyPoDetail(detail);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function applyPoDetail(detail) {
  supCode.value = `${detail.supplierCode}-${detail.supplierName}`;
  supName.value = detail.supplierName || '';
  supAddress.value = detail.supplierAddress || '';
  supTelp.value = detail.supplierTelp || '';
  discount.value = detail.discount || 0;
  discountType.value = detail.discountType === '%' ? '%' : 'RP';
  ppn.value = 10;
  ppnType.value = '%';
  items.value = (detail.items || []).map((row) => ({
    poDetId: row.poDetId,
    itemId: row.itemId,
    itemCode: row.itemCode,
    itemName: row.itemName,
    qtyOrdered: row.qtyOrdered || 0,
    bonus: row.bonus || 0,
    measurementCode: row.measurementCode || '-',
    cost: row.cost || 0,
    qtySisa: row.qtySisa || 0,
    bonusSisa: row.bonusSisa || 0,
    qtyArrived: row.qtySisa || 0,
    bonusArrived: 0,
    discount: row.discount || 0,
    discountType: row.discountType === '%' ? '%' : 'RP'
  }));
  mode.value = 'init';
}

// ===== Bandbox NO. BPP =====
function openDoSearch() {
  doSearchOpen.value = true;
  doSearchCode.value = '';
  doSearchWhouse.value = '';
  doResults.value = [];
}

async function searchDo() {
  doSearching.value = true;
  try {
    doResults.value = await request(
      `/purchasing/delivery-order/do/search?doCode=${encodeURIComponent(doSearchCode.value)}&whouseCode=${encodeURIComponent(doSearchWhouse.value)}`
    );
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    doSearching.value = false;
  }
}

async function pickDo(item) {
  bppNo.value = item.doCode;
  doSearchOpen.value = false;
  await loadDoDetail(item.doCode);
}

async function loadDoDetail(doCode) {
  error.value = '';
  loading.value = true;
  try {
    const detail = await request(`/purchasing/delivery-order/do/detail?doCode=${encodeURIComponent(doCode)}`);
    applyDoDetail(detail);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function applyDoDetail(detail) {
  poNo.value = detail.poCode || '';
  recDate.value = detail.recDate || todayIso();
  recBy.value = detail.recBy || '';
  approvedBy.value = detail.approvedByName || '';
  statusLabel.value = 'STATUS : ' + detail.status;
  selectedWarehouseId.value = detail.warehouseId;
  supCode.value = `${detail.supplierCode}-${detail.supplierName}`;
  supName.value = detail.supplierName || '';
  supAddress.value = detail.supplierAddress || '';
  supTelp.value = detail.supplierTelp || '';
  discount.value = detail.discount || 0;
  discountType.value = detail.discountType === '%' ? '%' : 'RP';
  ppn.value = detail.ppn || 0;
  ppnType.value = detail.ppnType === '%' ? '%' : 'RP';
  items.value = (detail.items || []).map((row) => ({
    poDetId: row.poDetId,
    itemId: row.itemId,
    itemCode: row.itemCode,
    itemName: row.itemName,
    qtyOrdered: row.qtyOrdered || 0,
    bonus: row.bonus || 0,
    measurementCode: row.measurementCode || '-',
    cost: row.cost || 0,
    qtySisa: row.qtySisa || 0,
    bonusSisa: row.bonusSisa || 0,
    qtyArrived: row.qtyArrived || 0,
    bonusArrived: row.bonusArrived || 0,
    discount: detail.discount || 0,
    discountType: detail.discountType === '%' ? '%' : 'RP'
  }));
  mode.value = 'view';
  loadBatchMasters(detail.doCode);
}

// ===== Batch tab =====
async function loadBatchMasters(doCode) {
  if (batchLoadedDoCode.value === doCode) return;
  batchLoadedDoCode.value = doCode;
  try {
    const masters = await request(`/purchasing/delivery-order/batch/masters?doCode=${encodeURIComponent(doCode)}`);
    batchItems.value = masters.items || [];
    if (batchItems.value.length) {
      selectedBatchItemId.value = batchItems.value[0].itemId;
      selectBatchItem(batchItems.value[0]);
    }
  } catch (requestError) {
    error.value = requestError.message;
  }
}

async function selectBatchItem(item) {
  selectedBatchItemId.value = item.itemId;
  batchEntries.value = batchEntries.value.filter((e) => e.itemId !== item.itemId || true);
  const options = await request(`/purchasing/delivery-order/batch/measurements?code=${encodeURIComponent(item.initM)}`);
  if (options && options.length) {
    endMOptions.value = options;
  } else {
    endMOptions.value = [{ endQuantify: item.initM, multiplier: 1 }];
  }
  endM.value = endMOptions.value[0].endQuantify;
  batchQty.value = 0;
  batchNo.value = '';
  expiredDate.value = todayIso();
}

function currentBatchItem() {
  return batchItems.value.find((i) => i.itemId === selectedBatchItemId.value);
}

async function saveBatchEntry() {
  const item = currentBatchItem();
  if (!item) {
    alert('Pilih NAMA ITEM terlebih dahulu!');
    return;
  }
  if (!batchNo.value.trim()) {
    alert('NO. BATCH WAJIB DIISI!');
    return;
  }
  if (!batchQty.value || batchQty.value < 1) {
    alert('QUANTITY BARANG TIDAK BOLEH KURANG DARI 1!');
    return;
  }
  if (!expiredDate.value) {
    alert('TGL KADALUWARSA WAJIB DIISI!');
    return;
  }
  const option = endMOptions.value.find((o) => o.endQuantify === endM.value);
  const multiplier = option ? option.multiplier : 1;

  // total batch qty untuk item ini tidak boleh melebihi JUMLAH AWAL
  const totalBatchQty = batchEntries.value
    .filter((e) => e.itemId === item.itemId)
    .reduce((sum, e) => sum + e.qty, 0);
  if (totalBatchQty + batchQty.value > item.initQty) {
    alert('QUANTITY BARANG MELEBIHI JUMLAH QUANTITY DALAM DO!');
    return;
  }
  if (batchEntries.value.some((e) => e.batchNo === batchNo.value.trim().toUpperCase())) {
    alert('BATCH NO. YANG ANDA MASUKKAN SUDAH ADA DI SCREEN!');
    return;
  }
  const duplicate = await request(`/purchasing/delivery-order/batch/check-duplicate?batchNo=${encodeURIComponent(batchNo.value.trim())}`);
  if (duplicate) {
    alert('BATCH NO. YANG ANDA MASUKKAN SUDAH ADA DI DALAM SYSTEM!');
    return;
  }

  batchEntries.value.push({
    itemId: item.itemId,
    itemCode: item.itemCode,
    itemName: item.itemName,
    batchNo: batchNo.value.trim().toUpperCase(),
    qty: batchQty.value,
    expDate: expiredDate.value,
    finalM: endM.value,
    multiplier,
    displayQty: batchQty.value * multiplier
  });
  batchNo.value = '';
  batchQty.value = 0;
  expiredDate.value = todayIso();
}

function deleteBatchEntry(index) {
  batchEntries.value.splice(index, 1);
}

function allItemsRegistered() {
  for (const item of batchItems.value) {
    const registered = batchEntries.value
      .filter((e) => e.itemId === item.itemId)
      .reduce((sum, e) => sum + e.qty, 0);
    if (registered !== item.initQty) return false;
  }
  return true;
}

// ===== Validasi input BPP =====
function validateInput() {
  if (!items.value.length) {
    alert('LIST ITEM KOSONG.');
    return false;
  }
  for (const row of items.value) {
    if (row.qtyArrived == null || row.qtyArrived < 0 || row.qtyArrived > row.qtySisa) {
      alert('NILAI QTY YANG DIPESAN TIDAK BOLEH MELEBIHI QTY SISA!');
      return false;
    }
    if (row.bonusArrived == null || row.bonusArrived < 0 || row.bonusArrived > row.bonusSisa) {
      alert('NILAI QTY BONUS TIDAK BOLEH MELEBIHI BONUS AWAL!');
      return false;
    }
  }
  return true;
}

function buildLines() {
  return items.value.map((row) => ({
    poDetId: row.poDetId,
    itemId: row.itemId,
    qtyArrived: Number(row.qtyArrived) || 0,
    bonusArrived: Number(row.bonusArrived) || 0,
    subtotal: itemSubtotal(row)
  }));
}

function buildTotalRequest() {
  return {
    discount: Number(discount.value) || 0,
    discountType: discountType.value,
    total: total.value,
    gtotal: gtotal.value
  };
}

// ===== SIMPAN =====
async function save() {
  error.value = '';
  if (!selectedWarehouseId.value) {
    alert('LOKASI GUDANG WAJIB DIISI!');
    return;
  }
  if (!poNo.value) {
    alert('NO. OP WAJIB DIISI!');
    return;
  }
  if (!bppNo.value.trim()) {
    alert('NO. BPP WAJIB DIISI!');
    return;
  }
  if (!validateInput()) return;
  if (!confirm(mode.value === 'modify' ? 'Data BPP akan diubah. Lanjutkan?' : 'Data BPP akan disimpan. Lanjutkan?')) return;

  saving.value = true;
  try {
    if (mode.value === 'modify') {
      await request('/purchasing/delivery-order/update', {
        method: 'POST',
        body: JSON.stringify({
          doCode: bppNo.value.trim(),
          warehouseId: selectedWarehouseId.value,
          recDate: recDate.value,
          ppn: Number(ppn.value) || 0,
          ppnType: ppnType.value,
          lines: buildLines(),
          ...buildTotalRequest()
        })
      });
      alert(`PERUBAHAN BPP BERHASIL! NO BPP : ${bppNo.value.trim()}`);
    } else {
      const result = await request('/purchasing/delivery-order', {
        method: 'POST',
        body: JSON.stringify({
          warehouseId: selectedWarehouseId.value,
          poCode: poNo.value,
          doCode: bppNo.value.trim(),
          recDate: recDate.value,
          ppn: Number(ppn.value) || 0,
          ppnType: ppnType.value,
          lines: buildLines(),
          ...buildTotalRequest()
        })
      });
      alert(`PEMBUATAN BPP BERHASIL! NO BPP : ${result.doCode}`);
    }
    statusLabel.value = 'STATUS : OPEN';
    mode.value = 'view';
    batchLoadedDoCode.value = '';
    await loadBatchMasters(bppNo.value.trim());
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
  if (bppNo.value) {
    loadDoDetail(bppNo.value);
  } else {
    mode.value = 'init';
  }
}

function newForm() {
  resetForm();
  activeTab.value = 'penerimaan';
}

async function revokeDo() {
  if (!bppNo.value.trim()) {
    alert('BPP TIDAK TERDAPAT DI DATABASE!');
    return;
  }
  if (!confirm('BPP akan di Batalkan, Anda Yakin?')) return;
  try {
    await request(`/purchasing/delivery-order/revoke?doCode=${encodeURIComponent(bppNo.value.trim())}`, {
      method: 'POST'
    });
    alert('BPP Telah Dibatalkan!');
    statusLabel.value = 'STATUS : REVOKED';
    mode.value = 'error';
    batchEntries.value = [];
    batchItems.value = [];
    batchLoadedDoCode.value = '';
  } catch (requestError) {
    error.value = requestError.message;
  }
}

// ===== DISETUJUI (approval BPP) =====
async function approve() {
  error.value = '';
  if (!bppNo.value.trim()) {
    alert('BPP TIDAK TERDAPAT DI DATABASE!');
    return;
  }
  if (!validateInput()) return;
  if (!batchItems.value.length) {
    await loadBatchMasters(bppNo.value.trim());
  }
  if (!allItemsRegistered()) {
    alert('DATA BATCH BELUM DIMASUKKAN SEMUA ATAU TIDAK SAMA DENGAN DATA ORDER/BONUS DITERIMA. MOHON DIPERIKSA LAGI!');
    activeTab.value = 'batch';
    return;
  }
  if (!confirm('Anda Yakin Untuk Validasi BPP Ini?')) return;

  saving.value = true;
  try {
    const result = await request('/purchasing/delivery-order/approve', {
      method: 'POST',
      body: JSON.stringify({
        doCode: bppNo.value.trim(),
        entries: batchEntries.value.map((e) => ({
          itemId: e.itemId,
          batchNo: e.batchNo,
          qty: e.qty,
          expDate: e.expDate,
          finalM: e.finalM,
          multiplier: e.multiplier
        }))
      })
    });
    alert('Approval Sukses! Inventory Sudah Ter-Update!');
    statusLabel.value = 'STATUS : ' + result.status;
    mode.value = 'error';
    batchLoadedDoCode.value = '';
    batchItems.value = [];
    batchEntries.value = [];
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🚚 BUKTI PENERIMAAN BARANG</h2>
      <p class="page-subtitle">Migrasi screen legacy SC0195 — doHead.zul (Penerimaan Barang + Input Batch No.)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <div v-if="loading" class="loading">Memuat data BPP...</div>

    <template v-else>
      <div class="tabs">
        <button class="tab" :class="{ active: activeTab === 'penerimaan' }" type="button" @click="activeTab = 'penerimaan'">
          📦 PENERIMAAN BARANG
        </button>
        <button class="tab" :class="{ active: activeTab === 'batch' }" type="button" @click="activeTab = 'batch'">
          🔢 INPUT BATCH NO.
        </button>
      </div>

      <!-- ============ TAB 1: PENERIMAAN BARANG ============ -->
      <template v-if="activeTab === 'penerimaan'">
        <div class="header-grid">
          <div class="card">
            <h3 class="card-title">PENERIMAAN BARANG</h3>
            <div class="field">
              <label>LOKASI GUDANG</label>
              <select v-model="selectedWarehouseId" :disabled="mode === 'view' || mode === 'error'">
                <option v-for="wh in warehouses" :key="wh.warehouseId" :value="wh.warehouseId">{{ wh.warehouseCode }} - {{ wh.warehouseName }}</option>
              </select>
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
                    <label>NAMA SUPPLIER</label>
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
                    <tr v-if="!poResults.length"><td colspan="3" class="empty-state">Masukkan No. OP / Nama Supplier lalu tekan CARI.</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div class="field">
              <label>TANGGAL TERIMA</label>
              <input v-model="recDate" type="date" :disabled="mode === 'view' || mode === 'error'" />
            </div>
            <div class="field">
              <label>DITERIMA OLEH</label>
              <input :value="recBy" readonly />
            </div>
            <div class="field">
              <label>NO. BPP</label>
              <div class="bandbox">
                <input v-model="bppNo" type="text" :disabled="mode === 'modify' || mode === 'error'" @focus="openDoSearch" />
                <button class="bandbox-btn" type="button" :disabled="mode === 'modify' || mode === 'error'" @click="doSearchOpen = !doSearchOpen">▾</button>
              </div>
              <div v-if="doSearchOpen" class="opp-popup">
                <div class="popup-search-grid">
                  <div class="popup-field">
                    <label>NO. BPP</label>
                    <input v-model="doSearchCode" type="text" @keyup.enter="searchDo" />
                  </div>
                  <div class="popup-field">
                    <label>LOKASI GUDANG</label>
                    <input v-model="doSearchWhouse" type="text" @keyup.enter="searchDo" />
                  </div>
                  <button class="small-button primary" type="button" :disabled="doSearching" @click="searchDo">CARI</button>
                </div>
                <table class="table popup-table">
                  <thead><tr><th>NO. DO</th><th>NAMA GUDANG</th><th>TANGGAL</th></tr></thead>
                  <tbody>
                    <tr v-for="item in doResults" :key="item.doCode" @click="pickDo(item)">
                      <td class="strong">{{ item.doCode }}</td>
                      <td>{{ item.warehouseName }}</td>
                      <td>{{ item.createdDate }}</td>
                    </tr>
                    <tr v-if="!doResults.length"><td colspan="3" class="empty-state">Masukkan No. BPP / Lokasi Gudang lalu tekan CARI.</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div class="field">
              <label>DISETUJUI OLEH</label>
              <input :value="approvedBy" readonly />
            </div>
          </div>

          <div class="card">
            <h3 class="card-title">SUPPLIER</h3>
            <div class="field">
              <label>KODE SUPPLIER</label>
              <input :value="supCode" readonly />
            </div>
            <div class="field">
              <label>NAMA SUPPLIER</label>
              <input :value="supName" readonly />
            </div>
            <div class="field">
              <label>ALAMAT</label>
              <input :value="supAddress" readonly />
            </div>
            <div class="field">
              <label>NO. TELP</label>
              <input :value="supTelp" readonly />
            </div>
          </div>
        </div>

        <p class="status-label">{{ statusLabel }}</p>

        <div class="card">
          <h3 class="card-title">DATA ORDER PEMBELIAN</h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>KODE</th>
                  <th>KETERANGAN</th>
                  <th class="num">ORD/A</th>
                  <th class="num">BONUS</th>
                  <th>SATUAN</th>
                  <th class="num">HRG SAT</th>
                  <th class="num">ORD/S</th>
                  <th class="num">ORD/T</th>
                  <th class="num">BNS/S</th>
                  <th class="num">BNS/T</th>
                  <th class="num">DISKON</th>
                  <th class="num">SUBTOTAL</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in items" :key="row.poDetId">
                  <td class="strong">{{ row.itemCode }}</td>
                  <td>{{ row.itemName }}</td>
                  <td class="num">{{ row.qtyOrdered }}</td>
                  <td class="num">{{ row.bonus }}</td>
                  <td>{{ row.measurementCode }}</td>
                  <td class="num">{{ fmt(row.cost) }}</td>
                  <td class="num">{{ row.qtySisa }}</td>
                  <td>
                    <input v-model.number="row.qtyArrived" type="number" min="0" :disabled="mode === 'view' || mode === 'error'" />
                  </td>
                  <td class="num">{{ row.bonusSisa }}</td>
                  <td>
                    <input v-model.number="row.bonusArrived" type="number" min="0" :disabled="mode === 'view' || mode === 'error'" />
                  </td>
                  <td class="num">{{ row.discount }} {{ row.discountType }}</td>
                  <td class="num strong">{{ fmt(itemSubtotal(row)) }}</td>
                </tr>
                <tr v-if="!items.length">
                  <td colspan="12" class="empty-state">Pilih NO. OP untuk mengisi data penerimaan barang.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="totals-bar">
            <span class="totals-label">TOTAL :</span>
            <span class="totals-value">{{ fmt(total) }}</span>
            <span class="totals-op">-</span>
            <span class="totals-label">DISKON :</span>
            <input v-model.number="discount" type="number" min="0" step="any" :disabled="mode === 'view' || mode === 'error'" />
            <select v-model="discountType" :disabled="mode === 'view' || mode === 'error'">
              <option value="RP">1. RP</option>
              <option value="%">2. %</option>
            </select>
            <span class="totals-op">+</span>
            <span class="totals-label">PPN :</span>
            <input v-model.number="ppn" type="number" min="0" step="any" :disabled="mode === 'view' || mode === 'error'" />
            <select v-model="ppnType" :disabled="mode === 'view' || mode === 'error'">
              <option value="RP">1. RP</option>
              <option value="%">2. %</option>
            </select>
            <span class="totals-op">=</span>
            <span class="totals-label">GRAND TOTAL :</span>
            <span class="totals-value">{{ fmt(gtotal) }}</span>
          </div>
        </div>

        <div class="action-bar">
          <button class="small-button primary" type="button" :disabled="saving || mode === 'view' || mode === 'error'" @click="save">💾 SIMPAN</button>
          <button class="small-button" type="button" :disabled="mode !== 'view'" @click="modify">✏️ UBAH</button>
          <button class="small-button" type="button" :disabled="mode !== 'modify'" @click="cancelModify">❌ BATAL</button>
          <button class="small-button" type="button" @click="newForm">🆕 BARU</button>
          <button class="small-button" type="button" :disabled="mode !== 'view'" @click="revokeDo">🚫 PEMBATALAN BPP</button>
          <button class="small-button" type="button" :disabled="saving || mode !== 'view'" @click="approve">✅ DISETUJUI</button>
          <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
        </div>
      </template>

      <!-- ============ TAB 2: INPUT BATCH NO. ============ -->
      <template v-else>
        <div class="card">
          <h3 class="card-title">FORM INPUT NO. BATCH ITEM</h3>
          <div class="batch-grid">
            <div class="field">
              <label>NO. OP</label>
              <input :value="bppNo" readonly />
            </div>
            <div class="field">
              <label>NAMA ITEM</label>
              <select v-model="selectedBatchItemId" @change="selectBatchItem(currentBatchItem())">
                <option v-for="item in batchItems" :key="item.itemId" :value="item.itemId">{{ item.itemCode }} - {{ item.itemName }}</option>
              </select>
            </div>
            <div class="field">
              <label>JUMLAH AWAL</label>
              <input :value="currentBatchItem() ? currentBatchItem().initQty : ''" readonly />
            </div>
            <div class="field">
              <label>SATUAN AWAL</label>
              <input :value="currentBatchItem() ? currentBatchItem().initM : ''" readonly />
            </div>
            <div class="field">
              <label>NO. BATCH</label>
              <input v-model="batchNo" type="text" @keyup.enter="saveBatchEntry" />
            </div>
            <div class="field">
              <label>SATUAN AKHIR</label>
              <select v-model="endM">
                <option v-for="opt in endMOptions" :key="opt.endQuantify" :value="opt.endQuantify">{{ opt.endQuantify }} ({{ opt.multiplier }}x)</option>
              </select>
            </div>
            <div class="field">
              <label>JUMLAH INPUT</label>
              <input v-model.number="batchQty" type="number" min="0" />
            </div>
            <div class="field">
              <label>TGL KADALUWARSA</label>
              <input v-model="expiredDate" type="date" />
            </div>
          </div>
          <div class="action-bar">
            <button class="small-button primary" type="button" @click="saveBatchEntry">💾 SIMPAN</button>
            <button class="small-button" type="button" :disabled="batchSelected < 0" @click="deleteBatchEntry(batchSelected)">🗑️ HAPUS</button>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">SATUAN ITEM</h3>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th></th>
                  <th>KODE</th>
                  <th>NAMA</th>
                  <th>NO. BATCH</th>
                  <th>SATUAN AKHIR</th>
                  <th class="num">JUMLAH</th>
                  <th>TGL KADALUWARSA</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(entry, index) in batchEntries" :key="index" :class="{ selected: batchSelected === index }" @click="batchSelected = index">
                  <td>{{ batchSelected === index ? '☑' : '☐' }}</td>
                  <td class="strong">{{ entry.itemCode }}</td>
                  <td>{{ entry.itemName }}</td>
                  <td>{{ entry.batchNo }}</td>
                  <td>{{ entry.finalM }}</td>
                  <td class="num">{{ entry.displayQty }}</td>
                  <td>{{ entry.expDate }}</td>
                </tr>
                <tr v-if="!batchEntries.length">
                  <td colspan="7" class="empty-state">Belum ada entry batch. Simpan BPP terlebih dahulu lalu input batch per item.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
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

.tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.tab { padding: 10px 18px; border: 1px solid #d1d9e6; background: #fff; border-radius: 10px 10px 0 0; font-weight: 700; cursor: pointer; color: #6b7280; }
.tab.active { background: #304b73; color: #fff; border-color: #304b73; }

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
.table tbody tr.selected { background: #eef3fb; }
.table input[type="number"] { width: 70px; padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }

.strong { font-weight: 700; }
.num { text-align: right; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.totals-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 12px 4px; border-top: 1px solid #eef2f7; }
.totals-bar input { width: 80px; padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }
.totals-bar select { padding: 6px 8px; border: 1px solid #d1d9e6; border-radius: 6px; }
.totals-label { font-weight: 700; color: #304b73; font-size: 13px; }
.totals-value { font-weight: 800; color: #1f2937; font-size: 14px; min-width: 90px; }
.totals-op { font-weight: 700; color: #6b7280; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.batch-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }

@media (max-width: 1100px) {
  .header-grid { grid-template-columns: 1fr; }
  .batch-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
