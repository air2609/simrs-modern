<script setup>
import { onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const error = ref('');
const rows = ref([]);

const form = ref({
  id: null,
  bankName: '',
  bankAddr: '',
  bankAccNo: '',
  coaId: null,
  coaNo: '',
  bankContactNo: '',
  bank2ndCtcNo: ''
});

const selectedId = ref(null);
const saving = ref(false);

// COA search
const coaSearchOpen = ref(false);
const coaKeyword = ref('');
const coaResults = ref([]);
const coaSearching = ref(false);

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  });
  const payload = await response.json().catch(() => null);

  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Your session has been expired. You need to login again.');
    throw new Error(payload?.message || 'Unauthorized');
  }

  if (!response.ok) {
    throw new Error(payload?.message || `HTTP ${response.status}`);
  }

  return payload.data;
}

async function loadBanks() {
  rows.value = await request('/master/bank');
}

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    await loadBanks();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    id: null,
    bankName: '',
    bankAddr: '',
    bankAccNo: '',
    coaId: null,
    coaNo: '',
    bankContactNo: '',
    bank2ndCtcNo: ''
  };
  selectedId.value = null;
  coaKeyword.value = '';
  coaResults.value = [];
}

function selectRow(row) {
  selectedId.value = row.id;
  form.value = {
    id: row.id,
    bankName: row.bankName,
    bankAddr: row.bankAddr,
    bankAccNo: row.bankAccNo,
    coaId: row.coaId,
    coaNo: row.coaNo,
    bankContactNo: row.bankContactNo,
    bank2ndCtcNo: row.bank2ndCtcNo
  };
}

async function searchCoa() {
  coaSearching.value = true;
  try {
    coaResults.value = await request(`/master/bank/coa-search?keyword=${encodeURIComponent(coaKeyword.value)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    coaSearching.value = false;
  }
}

function selectCoa(coa) {
  form.value.coaId = coa.id;
  form.value.coaNo = coa.coaNo;
  coaSearchOpen.value = false;
}

async function doSave() {
  error.value = '';
  if (!form.value.bankName || !form.value.bankAccNo) {
    error.value = 'Nama dan No. Account harus diisi.';
    return;
  }
  saving.value = true;
  try {
    await request('/master/bank/save', {
      method: 'POST',
      body: JSON.stringify(form.value)
    });
    resetForm();
    await loadBanks();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

async function doDelete() {
  error.value = '';
  if (!selectedId.value) {
    error.value = 'Pilih data bank terlebih dahulu.';
    return;
  }
  if (!window.confirm('Yakin ingin menghapus data bank ini?')) {
    return;
  }
  try {
    await request(`/master/bank/delete?id=${selectedId.value}`, {
      method: 'DELETE'
    });
    resetForm();
    await loadBanks();
  } catch (requestError) {
    error.value = requestError.message;
  }
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>🏦 Bank</h2>
        <p class="page-subtitle">Migrasi form legacy SCM0033 — master bank</p>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data bank...</div>

    <template v-else>
      <!-- Form -->
      <div class="card">
        <h3 class="card-title">FORM MASTER BANK</h3>
        <div class="form-grid">
          <div class="field">
            <label for="bank-name">NAMA</label>
            <input
              id="bank-name"
              v-model="form.bankName"
              type="text"
              maxlength="50"
              placeholder="Nama bank"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="bank-address">ALAMAT</label>
            <input
              id="bank-address"
              v-model="form.bankAddr"
              type="text"
              placeholder="Alamat bank"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="bank-acct-no">ACCOUNT NO.</label>
            <input
              id="bank-acct-no"
              v-model="form.bankAccNo"
              type="text"
              maxlength="35"
              placeholder="No. account"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="bank-coa">NO. COA</label>
            <div class="coa-field">
              <input
                id="bank-coa"
                v-model="form.coaNo"
                type="text"
                readonly
                placeholder="Pilih COA"
                @focus="coaSearchOpen = true"
              />
              <button class="coa-button" type="button" @click="coaSearchOpen = !coaSearchOpen">🔍</button>
            </div>
          </div>
          <div class="field">
            <label for="bank-telp">NO. TELP</label>
            <input
              id="bank-telp"
              v-model="form.bankContactNo"
              type="text"
              maxlength="25"
              placeholder="No. telp"
              @keyup.enter="doSave"
            />
          </div>
          <div class="field">
            <label for="bank-alt-telp">NO. TELP ALTERNATIF</label>
            <input
              id="bank-alt-telp"
              v-model="form.bank2ndCtcNo"
              type="text"
              maxlength="25"
              placeholder="No. telp alternatif"
              @keyup.enter="doSave"
            />
          </div>
        </div>

        <!-- COA search popup -->
        <div v-if="coaSearchOpen" class="coa-popup">
          <div class="coa-popup-header">PENCARIAN DATA COA</div>
          <div class="coa-search-row">
            <input
              v-model="coaKeyword"
              type="text"
              placeholder="No. COA / Nama"
              @keyup.enter="searchCoa"
            />
            <button class="btn" type="button" :disabled="coaSearching" @click="searchCoa">
              {{ coaSearching ? '...' : 'CARI' }}
            </button>
          </div>
          <div class="coa-results">
            <table class="table">
              <thead>
                <tr>
                  <th>NO. COA</th>
                  <th>NAMA</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="coa in coaResults"
                  :key="coa.id"
                  @click="selectCoa(coa)"
                >
                  <td class="strong">{{ coa.coaNo }}</td>
                  <td>{{ coa.coaName }}</td>
                </tr>
                <tr v-if="!coaResults.length">
                  <td colspan="2" class="empty-state">Ketik kata kunci lalu tekan CARI.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="form-actions">
          <button class="btn btn--primary" type="button" :disabled="saving" @click="doSave">
            💾 SIMPAN
          </button>
          <button class="btn" type="button" @click="resetForm">✖ BATAL</button>
          <button class="btn btn--danger" type="button" @click="doDelete">🗑 HAPUS</button>
        </div>
      </div>

      <!-- List -->
      <div class="card">
        <h3 class="card-title">DATA BANK</h3>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>NAMA</th>
                <th>NO. ACCT</th>
                <th>NO. COA</th>
                <th>NO. TELP</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in rows"
                :key="row.id"
                :class="{ 'row--selected': selectedId === row.id }"
                @click="selectRow(row)"
              >
                <td class="strong">{{ row.bankName }}</td>
                <td>{{ row.bankAccNo }}</td>
                <td>{{ row.coaNo }}</td>
                <td>{{ row.bankContactNo }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="4" class="empty-state">Tidak ada data bank.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }

.header-actions { display: flex; align-items: center; gap: 10px; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card-title { margin: 0 0 16px; color: #304b73; font-size: 15px; }

.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }

.coa-field { display: flex; gap: 6px; }
.coa-field input { flex: 1; }
.coa-button { padding: 8px 12px; border: 1px solid #d1d9e6; border-radius: 6px; background: #eef3fb; cursor: pointer; }

.coa-popup { border: 1px solid #d1d9e6; border-radius: 8px; padding: 12px; margin-bottom: 16px; background: #f8fafc; }
.coa-popup-header { font-weight: 700; color: #304b73; margin-bottom: 10px; font-size: 13px; }
.coa-search-row { display: flex; gap: 8px; margin-bottom: 10px; }
.coa-search-row input { flex: 1; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }
.coa-results { max-height: 220px; overflow: auto; }

.form-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: 1px solid #d1d9e6; border-radius: 6px; background: #fff; color: #304b73; cursor: pointer; }
.btn:hover { background: #f6f8fb; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.btn--primary:hover { background: #1e40af; }
.btn--danger { background: #fff; border-color: #f0b3bd; color: #a32943; }
.btn--danger:hover { background: #fde8ea; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 14px; }
.table th, .table td { padding: 10px 12px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr { cursor: pointer; }
.table tbody tr:hover { background: #f6f8fb; }
.row--selected { background: #e8f0fe; }

.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
</style>
