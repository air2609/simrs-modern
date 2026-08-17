<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired']);

const loading = ref(false);
const approving = ref(false);
const error = ref('');

// PERSETUJUAN OPP
const oppNo = ref('');
const issuedBy = ref('');
const approvedBy = ref('');
const statusLabel = ref('STATUS :');
const approved = ref(false);

// Bandbox NO. OPP
const oppSearchOpen = ref(false);
const oppSearchCode = ref('');
const oppResults = ref([]);
const oppSearching = ref(false);

// SUPPLIER
const supplierCode = ref('');
const supplierName = ref('');
const supplierAddress = ref('');
const supplierTelp = ref('');

// Daftar item
const items = ref([]); // {itemId, itemCode, itemName, itemGroupCode, stock, bufferLimit, maxOrder, measurementCode, qtyRequested}

// Dialog persetujuan (DISETUJUI)
const approveDialogOpen = ref(false);
const approveState = ref('confirm'); // confirm | success | error
const approveError = ref('');

const totalQty = computed(() => items.value.reduce((sum, row) => sum + (row.qtyRequested || 0), 0));

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

function resetForm() {
  oppNo.value = '';
  issuedBy.value = '';
  approvedBy.value = '';
  statusLabel.value = 'STATUS :';
  approved.value = false;
  supplierCode.value = '';
  supplierName.value = '';
  supplierAddress.value = '';
  supplierTelp.value = '';
  items.value = [];
  oppResults.value = [];
  error.value = '';
}

// ===== Bandbox NO. OPP =====
async function searchOpp() {
  oppSearching.value = true;
  try {
    oppResults.value = await request(`/purchasing/purchase-request-approval/opp/search?prCode=${encodeURIComponent(oppSearchCode.value)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    oppSearching.value = false;
  }
}

function openOppSearch() {
  oppSearchOpen.value = true;
  oppSearchCode.value = '';
  oppResults.value = [];
}

async function pickOpp(item) {
  oppNo.value = item.prCode;
  oppSearchOpen.value = false;
  error.value = '';
  loading.value = true;
  try {
    const detail = await request(`/purchasing/purchase-request-approval/opp/detail?prCode=${encodeURIComponent(item.prCode)}`);
    applyDetail(detail);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function applyDetail(detail) {
  issuedBy.value = detail.issuerName || '';
  approvedBy.value = detail.approvedByName || '';
  statusLabel.value = 'STATUS : ' + detail.status;
  approved.value = detail.status === 'APPROVED';

  if (detail.supplierId != null) {
    supplierCode.value = `${detail.supplierCode}-${detail.supplierName}`;
    supplierName.value = detail.supplierName || '';
    supplierAddress.value = detail.supplierAddress || '';
    supplierTelp.value = detail.supplierTelp || '';
  } else {
    supplierCode.value = '';
    supplierName.value = '';
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
    measurementCode: row.measurementCode || '-',
    qtyRequested: row.qtyRequested || 0
  }));
}

// ===== DISETUJUI =====
function openApproveDialog() {
  if (!oppNo.value) {
    alert('Pilih NO. OPP terlebih dahulu!');
    return;
  }
  approveState.value = 'confirm';
  approveError.value = '';
  approveDialogOpen.value = true;
}

function closeApproveDialog() {
  if (approving.value) return;
  approveDialogOpen.value = false;
}

async function requestApprove() {
  approving.value = true;
  approveError.value = '';
  try {
    const result = await request(
      `/purchasing/purchase-request-approval/approve?prCode=${encodeURIComponent(oppNo.value)}`,
      { method: 'POST' }
    );
    statusLabel.value = 'STATUS : ' + result.status;
    approvedBy.value = result.approvedByName || '';
    approved.value = true;
    // Hapus OPP yang sudah disetujui dari hasil pencarian bandbox
    oppResults.value = oppResults.value.filter((o) => o.prCode !== oppNo.value);
    approveState.value = 'success';
  } catch (requestError) {
    approveError.value = requestError.message;
    approveState.value = 'error';
  } finally {
    approving.value = false;
  }
}

// ===== PRINT =====
function print() {
  if (!oppNo.value) {
    alert('Pilih NO. OPP terlebih dahulu!');
    return;
  }
  // Buka PDF "SURAT PEMESANAN" (migrasi dari legacy PORApproval.print()) di tab baru.
  window.open(`${props.apiBaseUrl}/purchasing/purchase-request-approval/print?prCode=${encodeURIComponent(oppNo.value)}`, '_blank');
}

onMounted(resetForm);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>✅ FORM PERSETUJUAN ORDER PERMINTAAN PEMBELIAN</h2>
      <p class="page-subtitle">Migrasi screen legacy SC0192 — prApproval.zul (Persetujuan OPP)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="header-grid">
      <!-- PERSETUJUAN OPP -->
      <div class="card">
        <h3 class="card-title">PERSETUJUAN OPP</h3>
        <div class="field">
          <label>NO. OPP</label>
          <div class="bandbox">
            <input v-model="oppNo" type="text" readonly @focus="openOppSearch" />
            <button class="bandbox-btn" type="button" @click="oppSearchOpen = !oppSearchOpen">▾</button>
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
          <label>DIBUAT OLEH</label>
          <input :value="issuedBy" readonly />
        </div>
        <div class="field">
          <label>DISETUJUI OLEH</label>
          <input :value="approvedBy" readonly />
        </div>
      </div>

      <!-- SUPPLIER -->
      <div class="card">
        <h3 class="card-title">SUPPLIER</h3>
        <div class="field">
          <label>SUPPLIER</label>
          <input :value="supplierCode" readonly />
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
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in items" :key="row.itemId">
              <td class="strong">{{ row.itemCode }}</td>
              <td>{{ row.itemName }}</td>
              <td>{{ row.itemGroupCode }}</td>
              <td class="num">{{ row.stock }}</td>
              <td class="num">{{ row.bufferLimit }}</td>
              <td class="num">{{ row.maxOrder }}</td>
              <td>{{ row.measurementCode }}</td>
              <td class="num strong">{{ row.qtyRequested }}</td>
            </tr>
            <tr v-if="!items.length">
              <td colspan="8" class="empty-state">Pilih NO. OPP untuk melihat daftar item.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Aksi -->
    <div class="action-bar">
      <button class="small-button primary" type="button" :disabled="approving || approved || !oppNo" @click="openApproveDialog">
        ✅ DISETUJUI
      </button>
      <button class="small-button" type="button" :disabled="!oppNo" @click="print">🖨️ PRINT</button>
    </div>
  </div>

  <!-- MODAL KONFIRMASI PERSETUJUAN -->
  <div v-if="approveDialogOpen" class="modal-overlay" @click.self="closeApproveDialog">
    <div class="approve-modal" role="dialog" aria-modal="true">

      <!-- State: Konfirmasi -->
      <template v-if="approveState === 'confirm'">
        <div class="approve-icon confirm"><span>?</span></div>
        <h3 class="approve-title">Konfirmasi Persetujuan</h3>
        <p class="approve-text">Anda Menyetujui OPP Ini?</p>

        <div class="approve-summary">
          <div class="summary-row">
            <span>NO. OPP</span>
            <strong>{{ oppNo }}</strong>
          </div>
          <div class="summary-row">
            <span>DIBUAT OLEH</span>
            <strong>{{ issuedBy }}</strong>
          </div>
          <div class="summary-row">
            <span>SUPPLIER</span>
            <strong>{{ supplierName || '-' }}</strong>
          </div>
          <div class="summary-row">
            <span>JUMLAH ITEM</span>
            <strong>{{ items.length }} item &middot; total {{ totalQty }}</strong>
          </div>
        </div>

        <div class="approve-actions">
          <button class="modal-btn secondary" type="button" :disabled="approving" @click="closeApproveDialog">
            TIDAK
          </button>
          <button class="modal-btn primary" type="button" :disabled="approving" @click="requestApprove">
            <span v-if="approving" class="spinner"></span>
            {{ approving ? 'MENYETUJUI...' : 'YA, SETUJUI' }}
          </button>
        </div>
      </template>

      <!-- State: Sukses -->
      <template v-else-if="approveState === 'success'">
        <div class="approve-icon success"><span>✓</span></div>
        <h3 class="approve-title">Berhasil!</h3>
        <p class="approve-text">OPP Telah Disetujui</p>

        <div class="approve-summary">
          <div class="summary-row">
            <span>NO. OPP</span>
            <strong>{{ oppNo }}</strong>
          </div>
          <div class="summary-row">
            <span>STATUS</span>
            <strong class="status-ok">APPROVED</strong>
          </div>
          <div class="summary-row">
            <span>DISETUJUI OLEH</span>
            <strong>{{ approvedBy }}</strong>
          </div>
        </div>

        <div class="approve-actions">
          <button class="modal-btn primary" type="button" @click="approveDialogOpen = false">
            SELESAI
          </button>
        </div>
      </template>

      <!-- State: Error -->
      <template v-else>
        <div class="approve-icon error"><span>!</span></div>
        <h3 class="approve-title">Persetujuan Gagal</h3>
        <p class="approve-text">{{ approveError || 'Terjadi kesalahan saat menyetujui OPP.' }}</p>

        <div class="approve-actions">
          <button class="modal-btn secondary" type="button" @click="approveState = 'confirm'">
            COBA LAGI
          </button>
          <button class="modal-btn primary" type="button" @click="approveDialogOpen = false">
            TUTUP
          </button>
        </div>
      </template>

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

.header-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 16px; }
.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.field { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input[readonly] { background: #f6f8fb; color: #6b7280; }

.bandbox { display: flex; align-items: stretch; }
.bandbox input { flex: 1; border-top-right-radius: 0; border-bottom-right-radius: 0; }
.bandbox-btn { padding: 0 12px; border: 1px solid #d1d9e6; border-left: none; border-radius: 0 6px 6px 0; background: #f6f8fb; cursor: pointer; }

.opp-popup { border: 1px solid #d1d9e6; border-radius: 8px; padding: 10px; background: #fff; box-shadow: 0 8px 20px rgba(0,0,0,0.12); margin-top: 4px; }
.popup-search { display: flex; gap: 8px; margin-bottom: 8px; }
.popup-search input { flex: 1; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }

.status-label { font-size: 13px; font-weight: 700; color: #b91c1c; margin: 0 0 8px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr:hover { background: #f6f8fb; }

.strong { font-weight: 700; }
.num { text-align: right; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

/* ===== Modal Konfirmasi Persetujuan ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  animation: fade-in 0.18s ease;
}

.approve-modal {
  background: #fff;
  border-radius: 18px;
  padding: 28px 30px 24px;
  width: 440px;
  max-width: 94vw;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35);
  text-align: center;
  animation: pop-in 0.22s cubic-bezier(0.2, 0.9, 0.3, 1.2);
}

.approve-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 900;
  color: #fff;
}

.approve-icon.confirm {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  box-shadow: 0 6px 16px rgba(217, 119, 6, 0.4);
  animation: pulse-ring 1.6s ease infinite;
}

.approve-icon.success {
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 6px 16px rgba(5, 150, 105, 0.4);
  animation: success-pop 0.5s cubic-bezier(0.2, 0.9, 0.3, 1.3);
}

.approve-icon.error {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
  box-shadow: 0 6px 16px rgba(185, 28, 28, 0.4);
}

.approve-title {
  margin: 0 0 6px;
  font-size: 20px;
  color: #1f2937;
  font-weight: 800;
}

.approve-text {
  margin: 0 0 16px;
  font-size: 14px;
  color: #6b7280;
}

.approve-summary {
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: #f8fafc;
  padding: 4px 14px;
  margin-bottom: 20px;
  text-align: left;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px dashed #e2e8f0;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-row span {
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.summary-row strong {
  font-size: 13px;
  color: #1f2937;
  text-align: right;
  word-break: break-word;
}

.summary-row strong.status-ok {
  color: #059669;
  font-weight: 800;
}

.approve-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.modal-btn {
  min-width: 120px;
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: transform 0.1s ease, box-shadow 0.15s ease;
}

.modal-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.modal-btn.primary {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}

.modal-btn.secondary {
  color: #374151;
}

.modal-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes pop-in {
  from { opacity: 0; transform: scale(0.88) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@keyframes success-pop {
  0% { transform: scale(0.4); opacity: 0; }
  60% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(1); }
}

@keyframes pulse-ring {
  0%, 100% { box-shadow: 0 6px 16px rgba(217, 119, 6, 0.4), 0 0 0 0 rgba(245, 158, 11, 0.45); }
  50% { box-shadow: 0 6px 16px rgba(217, 119, 6, 0.4), 0 0 0 12px rgba(245, 158, 11, 0); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
