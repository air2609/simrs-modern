<script setup>
import { onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const saving = ref(false);
const error = ref('');
const rows = ref([]);
const selectedIndex = ref(-1);

// Pencarian + paging
const searchKeyword = ref('');
const page = ref(1);
const pageSize = 20;
const total = ref(0);
const totalPages = ref(0);

// Dialog pembayaran
const payOpen = ref(false);
const coaOptions = ref([]);
const viaCoaId = ref(null);
const payTotal = ref(0);
const payMemo = ref('');

// Dialog LIHAT JOURNAL
const journalOpen = ref(false);
const journalRows = ref([]);

// Dialog LIHAT HISTORY PEMBAYARAN
const historyOpen = ref(false);
const historyRows = ref([]);

// Dialog hasil pembayaran
const resultDialogOpen = ref(false);
const resultState = ref('success'); // success | error
const resultMessage = ref('');

function fmt(value) {
  const n = Number(value) || 0;
  return n.toLocaleString('en-US', { maximumFractionDigits: 0 });
}

function selectedAp() {
  return selectedIndex.value >= 0 ? rows.value[selectedIndex.value] : null;
}

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

async function loadList() {
  loading.value = true;
  error.value = '';
  try {
    const params = new URLSearchParams({
      keyword: searchKeyword.value.trim(),
      page: String(page.value),
      pageSize: String(pageSize)
    });
    const data = await request(`/accounting/account-payable?${params.toString()}`);
    rows.value = data.rows || [];
    total.value = data.total || 0;
    totalPages.value = data.totalPages || 0;
    selectedIndex.value = -1;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function search() {
  page.value = 1;
  loadList();
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1;
    loadList();
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1;
    loadList();
  }
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    const [list, masters] = await Promise.all([
      request(`/accounting/account-payable?keyword=&page=1&pageSize=${pageSize}`),
      request('/accounting/account-payable/masters')
    ]);
    rows.value = (list && list.rows) || [];
    total.value = (list && list.total) || 0;
    totalPages.value = (list && list.totalPages) || 0;
    coaOptions.value = masters.coaOptions || [];
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function selectRow(index) {
  selectedIndex.value = index;
}

// ===== LIHAT JOURNAL =====
async function lihatJurnal() {
  const ap = selectedAp();
  if (!ap) {
    alert('PILIH SALAH SATU ITEM DULU!!!');
    return;
  }
  if (ap.journalBatchId === '-') {
    alert('JURNAL AP TIDAK DITEMUKAN.');
    return;
  }
  error.value = '';
  try {
    journalRows.value = await request(`/accounting/account-payable/journal?batchId=${encodeURIComponent(ap.journalBatchId)}`);
    journalOpen.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  }
}

// ===== LIHAT HISTORY PEMBAYARAN =====
async function lihatHistory() {
  const ap = selectedAp();
  if (!ap) {
    alert('PILIH SALAH SATU ITEM DULU!!!');
    return;
  }
  error.value = '';
  try {
    historyRows.value = await request(`/accounting/account-payable/history?apId=${ap.apId}`);
    historyOpen.value = true;
  } catch (requestError) {
    error.value = requestError.message;
  }
}

// ===== PEMBAYARAN =====
function openPayment() {
  const ap = selectedAp();
  if (!ap) {
    alert('PILIH SALAH SATU ITEM DULU!!!');
    return;
  }
  payTotal.value = ap.totalRemaining;
  viaCoaId.value = null;
  payMemo.value = '';
  payOpen.value = true;
}

async function submitPayment() {
  const ap = selectedAp();
  if (!ap) return;
  error.value = '';
  saving.value = true;
  try {
    const message = await request('/accounting/account-payable/pay', {
      method: 'POST',
      body: JSON.stringify({
        apId: ap.apId,
        viaCoaId: viaCoaId.value,
        total: payTotal.value,
        memo: payMemo.value
      })
    });
    payOpen.value = false;
    resultState.value = 'success';
    resultMessage.value = message;
    resultDialogOpen.value = true;
    await loadList();
  } catch (requestError) {
    payOpen.value = false;
    resultState.value = 'error';
    resultMessage.value = requestError.message || 'TRANSAKSI GAGAL DISIMPAN';
    resultDialogOpen.value = true;
  } finally {
    saving.value = false;
  }
}

function closeResultDialog() {
  resultDialogOpen.value = false;
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>💳 ACCOUNT PAYABLE</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <div v-if="loading" class="loading">Memuat data AP...</div>

    <template v-else>
      <div class="card">
        <div class="search-bar">
          <input v-model="searchKeyword" type="text" placeholder="Cari Nama Supplier / Journal Batch ID..." @keyup.enter="search" />
          <button class="small-button primary" type="button" @click="search">🔍 CARI</button>
          <span class="search-info">Menampilkan {{ rows.length }} dari {{ total }} data</span>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>NAMA SUPPLIER</th>
                <th>JOURNAL BATCH ID</th>
                <th class="num">TOTAL TERHUTANG</th>
                <th>TANGGAL JATUH TEMPO</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in rows" :key="row.apId" :class="{ selected: selectedIndex === index }" @click="selectRow(index)">
                <td class="strong">{{ row.supplierName }}</td>
                <td>{{ row.journalBatchId }}</td>
                <td class="num">{{ fmt(row.totalRemaining) }}</td>
                <td>{{ row.dueDate }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="4" class="empty-state">Tidak ada data account payable.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar">
          <button class="small-button" type="button" :disabled="page <= 1" @click="prevPage">⬅ PREV</button>
          <span class="page-info">Halaman {{ page }} / {{ totalPages || 1 }}</span>
          <button class="small-button" type="button" :disabled="page >= totalPages" @click="nextPage">NEXT ➡</button>
        </div>

        <div class="action-bar">
          <button class="small-button" type="button" @click="lihatJurnal">📄 LIHAT JOURNAL</button>
          <button class="small-button primary" type="button" @click="openPayment">💸 PEMBAYARAN</button>
          <button class="small-button" type="button" @click="lihatHistory">🕓 LIHAT HISTORY PEMBAYARAN</button>
          <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
        </div>
      </div>
    </template>
  </div>

  <!-- MODAL PEMBAYARAN AP -->
  <div v-if="payOpen" class="modal-overlay" @click.self="payOpen = false">
    <div class="modal-card payment-card">
      <h3 class="card-title">PEMBAYARAN AP</h3>
      <div class="payment-info">
        <span>SUPPLIER :</span>
        <strong>{{ selectedAp() ? selectedAp().supplierName : '-' }}</strong>
      </div>
      <div class="payment-info">
        <span>SISA HUTANG :</span>
        <strong>{{ fmt(selectedAp() ? selectedAp().totalRemaining : 0) }}</strong>
      </div>
      <div class="field">
        <label>VIA ACCT</label>
        <select v-model="viaCoaId">
          <option :value="null">- Pilih Rekening -</option>
          <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.coaId">{{ opt.acctNo }} - {{ opt.acctName }}</option>
        </select>
      </div>
      <div class="field">
        <label>TOTAL</label>
        <input v-model.number="payTotal" type="number" min="0" step="any" />
      </div>
      <div class="field">
        <label>MEMO</label>
        <textarea v-model="payMemo" rows="4"></textarea>
      </div>
      <div class="modal-actions">
        <button class="modal-btn primary" type="button" :disabled="saving" @click="submitPayment">OK</button>
        <button class="modal-btn secondary" type="button" @click="payOpen = false">BATAL</button>
      </div>
    </div>
  </div>

  <!-- MODAL LIHAT JOURNAL -->
  <div v-if="journalOpen" class="modal-overlay" @click.self="journalOpen = false">
    <div class="modal-card wide-card">
      <h3 class="card-title">JOURNAL DETAIL</h3>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>BATCH ID</th>
              <th>VOUCHER NO</th>
              <th>REKENING</th>
              <th>KETERANGAN</th>
              <th class="num">DEBET</th>
              <th class="num">KREDIT</th>
              <th>APL DATE</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in journalRows" :key="index">
              <td>{{ row.batchId }}</td>
              <td>{{ row.voucherNo }}</td>
              <td>{{ row.acctName }}</td>
              <td>{{ row.description }}</td>
              <td class="num">{{ fmt(row.debit) }}</td>
              <td class="num">{{ fmt(row.credit) }}</td>
              <td>{{ row.aplDate }}</td>
            </tr>
            <tr v-if="!journalRows.length"><td colspan="7" class="empty-state">Tidak ada data.</td></tr>
          </tbody>
        </table>
      </div>
      <div class="modal-actions">
        <button class="modal-btn secondary" type="button" @click="journalOpen = false">SELESAI</button>
      </div>
    </div>
  </div>

  <!-- MODAL LIHAT HISTORY PEMBAYARAN -->
  <div v-if="historyOpen" class="modal-overlay" @click.self="historyOpen = false">
    <div class="modal-card wide-card">
      <h3 class="card-title">HISTORY PEMBAYARAN</h3>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>BATCH ID</th>
              <th>VOUCHER NO.</th>
              <th>KETERANGAN</th>
              <th class="num">DEBET</th>
              <th class="num">KREDIT</th>
              <th>APL DATE</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in historyRows" :key="index">
              <td>{{ row.batchId }}</td>
              <td>{{ row.voucherNo }}</td>
              <td>{{ row.description }}</td>
              <td class="num">{{ fmt(row.debit) }}</td>
              <td class="num">{{ fmt(row.credit) }}</td>
              <td>{{ row.aplDate }}</td>
            </tr>
            <tr v-if="!historyRows.length"><td colspan="6" class="empty-state">Tidak ada data pembayaran.</td></tr>
          </tbody>
        </table>
      </div>
      <div class="modal-actions">
        <button class="modal-btn secondary" type="button" @click="historyOpen = false">SELESAI</button>
      </div>
    </div>
  </div>

  <!-- MODAL HASIL PEMBAYARAN -->
  <div v-if="resultDialogOpen" class="modal-overlay" @click.self="closeResultDialog">
    <div class="modal-card result-card">
      <template v-if="resultState === 'success'">
        <div class="result-icon success"><span>✓</span></div>
        <h3 class="result-title">Berhasil!</h3>
        <p class="result-text">{{ resultMessage }}</p>
        <div class="modal-actions">
          <button class="modal-btn primary" type="button" @click="closeResultDialog">SELESAI</button>
        </div>
      </template>
      <template v-else>
        <div class="result-icon error"><span>!</span></div>
        <h3 class="result-title">Gagal</h3>
        <p class="result-text">{{ resultMessage }}</p>
        <div class="modal-actions">
          <button class="modal-btn secondary" type="button" @click="closeResultDialog">TUTUP</button>
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
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.search-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.search-bar input { flex: 1; min-width: 240px; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.search-info { font-size: 12px; color: #6b7280; font-weight: 700; }

.pagination-bar { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 12px 4px 0; }
.page-info { font-weight: 700; color: #304b73; font-size: 13px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.selected { background: #eef3fb; }

.strong { font-weight: 700; }
.num { text-align: right; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }

/* ===== Modal ===== */
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

.modal-card {
  background: #fff;
  border-radius: 18px;
  padding: 24px 28px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35);
  animation: pop-in 0.22s cubic-bezier(0.2, 0.9, 0.3, 1.2);
  max-height: 88vh;
  overflow-y: auto;
}

.payment-card { width: 420px; max-width: 94vw; }
.wide-card { width: 900px; max-width: 96vw; }
.result-card { width: 400px; max-width: 94vw; text-align: center; }

.payment-info { display: flex; justify-content: space-between; gap: 12px; padding: 8px 0; border-bottom: 1px dashed #e2e8f0; margin-bottom: 10px; }
.payment-info span { font-size: 12px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.04em; }
.payment-info strong { font-size: 13px; color: #1f2937; text-align: right; }

.field { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field select, .field input, .field textarea { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.field textarea { resize: vertical; text-transform: uppercase; }

.modal-actions { display: flex; gap: 10px; justify-content: center; margin-top: 14px; }

.modal-btn {
  min-width: 110px;
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.15s ease;
}

.modal-btn:hover { transform: translateY(-1px); }

.modal-btn.primary {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}

.modal-btn.secondary { color: #374151; }

.result-icon {
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

.result-icon.success {
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 6px 16px rgba(5, 150, 105, 0.4);
  animation: success-pop 0.5s cubic-bezier(0.2, 0.9, 0.3, 1.3);
}

.result-icon.error {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
  box-shadow: 0 6px 16px rgba(185, 28, 28, 0.4);
}

.result-title { margin: 0 0 6px; font-size: 20px; color: #1f2937; font-weight: 800; }
.result-text { margin: 0 0 16px; font-size: 14px; color: #6b7280; }

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
</style>
