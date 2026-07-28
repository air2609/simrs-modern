<script setup>
import { ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired']);

const loading = ref(false);
const error = ref('');

const voucherNo = ref('');
const dateFrom = ref('');
const dateTo = ref('');
const journals = ref([]);

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Sesi habis.');
    throw new Error(payload?.message || 'Unauthorized');
  }
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  return payload;
}

async function openJournal() {
  if (!dateFrom.value || !dateTo.value) {
    error.value = 'Isi Date From dan Date To terlebih dahulu.';
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    const params = new URLSearchParams();
    params.set('voucherNo', voucherNo.value);
    params.set('dateFrom', dateFrom.value);
    params.set('dateTo', dateTo.value);

    const res = await request(`/accounting/journals?${params.toString()}`);
    journals.value = res.data || [];
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

function resetSearch() {
  voucherNo.value = '';
  dateFrom.value = '';
  dateTo.value = '';
  journals.value = [];
  error.value = '';
}

function formatCurrency(value) {
  if (value == null || isNaN(value)) return '0';
  return Number(value).toLocaleString('id-ID');
}

function getTodayDate() {
  const d = new Date();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${month}-${day}`;
}

function getFirstDateOfMonth() {
  const d = new Date();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  return `${d.getFullYear()}-${month}-01`;
}
</script>

<template>
  <div class="journal-section">
    <div class="page-header">
      <h2>📒 Open Journal</h2>
      <p class="page-subtitle">Cari dan lihat jurnal yang sudah dibuat di sistem</p>
    </div>

    <!-- Search Form -->
    <div class="card">
      <h3>Pencarian Jurnal</h3>
      <div class="form-row">
        <label>
          VOUCHER NO.
          <input v-model="voucherNo" placeholder="Ketik No. Voucher" @keyup.enter="openJournal" />
        </label>
        <label>
          FROM
          <input v-model="dateFrom" type="date" @keyup.enter="openJournal" />
        </label>
        <label>
          TO
          <input v-model="dateTo" type="date" @keyup.enter="openJournal" />
        </label>
      </div>
      <div class="search-actions">
        <button class="primary-button" :disabled="loading" @click="openJournal">
          {{ loading ? 'Mencari...' : '🔍 OPEN' }}
        </button>
        <button class="secondary-button" @click="resetSearch">Reset</button>
      </div>

      <!-- Quick Date Buttons -->
      <div class="quick-dates">
        <button class="quick-date-button" type="button" @click="dateFrom = getFirstDateOfMonth(); dateTo = getTodayDate(); openJournal()">
          📅 Bulan Ini
        </button>
        <button class="quick-date-button" type="button" @click="dateFrom = getTodayDate(); dateTo = getTodayDate(); openJournal()">
          📅 Hari Ini
        </button>
      </div>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="status-banner status-banner--error">{{ error }}</div>

    <!-- Journal List -->
    <div class="card" v-if="journals.length > 0">
      <h3>
        <span>JOURNAL LIST</span>
        <span class="record-count">{{ journals.length }} record(s)</span>
      </h3>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>VOUCHER NO.</th>
              <th>BATCH NO.</th>
              <th>REKENING</th>
              <th>KETERANGAN</th>
              <th>APL DATE</th>
              <th>DEBIT</th>
              <th>KREDIT</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="j in journals" :key="j.journalId">
              <td><strong>{{ j.voucherNo || '-' }}</strong></td>
              <td>{{ j.journalBatchId || '-' }}</td>
              <td>
                <span class="acct-code">{{ j.accountNo || '' }}</span>
                {{ j.accountName || '-' }}
              </td>
              <td>{{ j.description || '-' }}</td>
              <td>{{ j.aplDate }}</td>
              <td class="amount">{{ j.debit > 0 ? formatCurrency(j.debit) : '' }}</td>
              <td class="amount">{{ j.credit > 0 ? formatCurrency(j.credit) : '' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && journals.length === 0 && !error" class="card">
      <p class="empty-state">
        Masukkan rentang tanggal dan klik <strong>OPEN</strong> untuk menampilkan jurnal.
      </p>
    </div>
  </div>
</template>

<style scoped>
.journal-section { padding: 16px; }

.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}
.card h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #304b73;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.record-count { font-size: 12px; color: #6b7280; }

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}
.form-row label {
  display: grid;
  gap: 4px;
  font-size: 13px;
  color: #3d4b63;
}

input, select {
  padding: 8px 10px;
  border: 1px solid #d1d9e6;
  border-radius: 8px;
  font: inherit;
  background: #fff;
}

.search-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.primary-button {
  padding: 8px 20px;
  background: #5f83c2;
  border: 0;
  border-radius: 8px;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}
.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.secondary-button {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d1d9e6;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  color: #3d4b63;
}
.secondary-button:hover {
  background: #f6f8fb;
}

.quick-dates {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.quick-date-button {
  padding: 7px 16px;
  background: #fff;
  border: 1px solid #d1d9e6;
  border-radius: 8px;
  font-weight: 700;
  font-size: 13px;
  color: #3d4b63;
  cursor: pointer;
}
.quick-date-button:hover {
  background: #f6f8fb;
  border-color: #5f83c2;
  color: #304b73;
}

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }

.amount {
  text-align: right;
  font-family: 'Courier New', monospace;
  white-space: nowrap;
}

.acct-code {
  color: #5f83c2;
  font-weight: 700;
  margin-right: 4px;
}

.empty-state {
  text-align: center;
  color: #9ca3af;
  padding: 32px 0;
  margin: 0;
  font-size: 14px;
}
</style>
