<script setup>
import { onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(true);
const saving = ref(false);
const error = ref('');

// Form header
const aplDate = ref(todayIso());
const description = ref('');
const voucherNo = ref('');

// Baris baru
const coaOptions = ref([]);
const selectedCoaId = ref(null);
const debitInput = ref(0);
const creditInput = ref(0);

// Daftar item
const items = ref([]);
// {coaId, acctNo, acctName, voucherNo, debit, credit}
const selectedIndex = ref(-1);

// Dialog hasil simpan
const resultDialogOpen = ref(false);
const resultState = ref('success'); // success | error
const resultMessage = ref('');

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function fmt(value) {
  const n = Number(value) || 0;
  return n.toLocaleString('en-US', { maximumFractionDigits: 0 });
}

function selectedCoa() {
  return coaOptions.value.find((o) => o.coaId === selectedCoaId.value);
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

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    const masters = await request('/accounting/journal-entry/masters');
    coaOptions.value = masters.coaOptions || [];
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function clearForm() {
  items.value = [];
  debitInput.value = 0;
  creditInput.value = 0;
  description.value = '';
  aplDate.value = todayIso();
  selectedCoaId.value = null;
  selectedIndex.value = -1;
}

function tambah() {
  const coa = selectedCoa();
  if (!coa) {
    alert('PILIH ACCOUNT TERLEBIH DAHULU!');
    return;
  }
  if (!debitInput.value && !creditInput.value) {
    alert('ISI DEBET ATAU CREDIT!');
    return;
  }
  items.value.push({
    coaId: coa.coaId,
    acctNo: coa.acctNo,
    acctName: coa.acctName,
    voucherNo: voucherNo.value.trim().toUpperCase(),
    debit: Number(debitInput.value) || 0,
    credit: Number(creditInput.value) || 0
  });
  debitInput.value = 0;
  creditInput.value = 0;
  selectedCoaId.value = null;
}

function hapus() {
  if (selectedIndex.value < 0) {
    alert('PILIH DATA DARI LIST TERLEBIH DAHULU..!');
    return;
  }
  if (!confirm('Anda Yakin akan Mengubah Data yang Dipilih..?')) return;
  items.value.splice(selectedIndex.value, 1);
  selectedIndex.value = -1;
}

async function save() {
  error.value = '';
  if (!items.value.length) {
    alert('ISILAH TRANSAKSINYA');
    return;
  }
  if (!voucherNo.value.trim()) {
    alert('VOUCHER NO. WAJIB DIISI!');
    return;
  }
  saving.value = true;
  try {
    const message = await request('/accounting/journal-entry', {
      method: 'POST',
      body: JSON.stringify({
        aplDate: aplDate.value,
        voucherNo: voucherNo.value,
        description: description.value,
        lines: items.value.map((row) => ({
          coaId: row.coaId,
          debit: row.debit,
          credit: row.credit
        }))
      })
    });
    resultState.value = 'success';
    resultMessage.value = message;
    resultDialogOpen.value = true;
    clearForm();
  } catch (requestError) {
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

function print() {
  if (!voucherNo.value.trim()) {
    alert('VOUCHER NO. WAJIB DIISI!');
    return;
  }
  window.open(`${props.apiBaseUrl}/accounting/journal-entry/print?voucherNo=${encodeURIComponent(voucherNo.value.trim())}`, '_blank');
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📝 MANUAL JOURNAL ENTRY</h2>
      <p class="page-subtitle">Migrasi screen legacy SC0199 — journalEntry.zul (Input Jurnal Manual)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <div v-if="loading" class="loading">Memuat data COA...</div>

    <template v-else>
      <div class="card header-card">
        <div class="field">
          <label>APL DATE</label>
          <input v-model="aplDate" type="date" />
        </div>
        <div class="field">
          <label>DESCRIPTION</label>
          <input v-model="description" type="text" @keyup.enter="() => {}" />
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">JOURNAL</h3>
        <div class="entry-grid">
          <div class="field">
            <label>VOUCHER NO.</label>
            <input v-model="voucherNo" type="text" @keyup.enter="tambah" />
          </div>
          <div class="field">
            <label>ACCOUNT</label>
            <select v-model="selectedCoaId">
              <option :value="null">- Pilih COA -</option>
              <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.coaId">{{ opt.acctNo }} - {{ opt.acctName }}</option>
            </select>
          </div>
          <div class="field">
            <label>DEBET</label>
            <input v-model.number="debitInput" type="number" min="0" />
          </div>
          <div class="field">
            <label>CREDIT</label>
            <input v-model.number="creditInput" type="number" min="0" />
          </div>
          <div class="entry-action">
            <button class="small-button primary" type="button" @click="tambah">➕ TAMBAH</button>
          </div>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>ACCT. NO.</th>
                <th>ACCT. NAME</th>
                <th>VOUCHER NO.</th>
                <th class="num">DEBET</th>
                <th class="num">CREDIT</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in items" :key="index" :class="{ selected: selectedIndex === index }" @click="selectedIndex = index">
                <td class="strong">{{ row.acctNo }}</td>
                <td>{{ row.acctName }}</td>
                <td>{{ row.voucherNo }}</td>
                <td class="num">{{ fmt(row.debit) }}</td>
                <td class="num">{{ fmt(row.credit) }}</td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="5" class="empty-state">Pilih ACCOUNT, isi DEBET/CREDIT lalu tekan TAMBAH.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="action-bar">
          <button class="small-button primary" type="button" :disabled="saving" @click="save">💾 SIMPAN</button>
          <button class="small-button" type="button" @click="hapus">🗑️ HAPUS</button>
          <button class="small-button" type="button" @click="clearForm">🆕 BARU</button>
          <button class="small-button" type="button" @click="print">🖨️ CETAK</button>
          <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
        </div>
      </div>
    </template>
  </div>

  <!-- MODAL HASIL SIMPAN -->
  <div v-if="resultDialogOpen" class="modal-overlay" @click.self="closeResultDialog">
    <div class="result-modal" role="dialog" aria-modal="true">

      <template v-if="resultState === 'success'">
        <div class="result-icon success"><span>✓</span></div>
        <h3 class="result-title">Berhasil!</h3>
        <p class="result-text">{{ resultMessage }}</p>
        <div class="result-summary">
          <div class="summary-row">
            <span>VOUCHER NO.</span>
            <strong>{{ voucherNo.trim().toUpperCase() || '-' }}</strong>
          </div>
          <div class="summary-row">
            <span>JUMLAH ITEM</span>
            <strong>{{ items.length }} baris jurnal</strong>
          </div>
        </div>
        <div class="result-actions">
          <button class="modal-btn primary" type="button" @click="closeResultDialog">SELESAI</button>
        </div>
      </template>

      <template v-else>
        <div class="result-icon error"><span>!</span></div>
        <h3 class="result-title">Gagal Menyimpan</h3>
        <p class="result-text">{{ resultMessage }}</p>
        <div class="result-actions">
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

.header-card { display: flex; gap: 24px; max-width: 720px; }
.header-card .field { flex: 1; }

.field { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field select, .field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }

.entry-grid { display: grid; grid-template-columns: repeat(4, 1fr) auto; gap: 12px; align-items: end; margin-bottom: 12px; }
.entry-action { padding-bottom: 10px; }

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

@media (max-width: 900px) {
  .entry-grid { grid-template-columns: repeat(2, 1fr); }
  .header-card { flex-direction: column; gap: 0; }
}

/* ===== Modal Hasil Simpan ===== */
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

.result-modal {
  background: #fff;
  border-radius: 18px;
  padding: 28px 30px 24px;
  width: 420px;
  max-width: 94vw;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35);
  text-align: center;
  animation: pop-in 0.22s cubic-bezier(0.2, 0.9, 0.3, 1.2);
}

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

.result-summary {
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

.summary-row:last-child { border-bottom: none; }

.summary-row span {
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.summary-row strong { font-size: 13px; color: #1f2937; text-align: right; word-break: break-word; }

.result-actions { display: flex; gap: 10px; justify-content: center; }

.modal-btn {
  min-width: 120px;
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
