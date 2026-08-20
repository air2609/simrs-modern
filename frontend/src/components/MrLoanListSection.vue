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
const messages = ref([]);

const loanList = ref([]);
const selectedMrCodes = ref([]);

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

async function loadData() {
  loanList.value = await request('/mr/loan-list');
  selectedMrCodes.value = [];
}

async function initialize() {
  loading.value = true;
  error.value = '';
  messages.value = [];
  try {
    await loadData();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function toggleSelected(mrCode) {
  const index = selectedMrCodes.value.indexOf(mrCode);
  if (index >= 0) {
    selectedMrCodes.value.splice(index, 1);
  } else {
    selectedMrCodes.value.push(mrCode);
  }
}

async function updateStatus(action) {
  error.value = '';
  messages.value = [];

  if (!selectedMrCodes.value.length) {
    error.value = 'Pilih Data Rekam Medis Terlebih Dahulu..!';
    return;
  }

  try {
    const result = await request('/mr/loan-list/status', {
      method: 'POST',
      body: JSON.stringify({
        mrCodes: selectedMrCodes.value,
        action
      })
    });
    messages.value = result.results.map((item) => ({
      text: `${item.mrCode} ${item.message}`,
      success: item.success
    }));
    await loadData();
  } catch (requestError) {
    error.value = requestError.message;
  }
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <div>
        <h2>🔁 Daftar Peminjaman Berkas Rekam Medis</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-for="(msg, index) in messages" :key="index"
      :class="['status-banner', msg.success ? 'status-banner--success' : 'status-banner--error']">
      {{ msg.text }}
    </p>

    <div v-if="loading" class="loading">Memuat data...</div>

    <template v-else>
      <div class="card">
        <h3 class="card-title">BERKAS REKAM MEDIS</h3>
        <table class="file-table">
          <thead>
            <tr>
              <th></th>
              <th>NO. MR</th>
              <th>NAMA PASIEN</th>
              <th>STATUS MR</th>
              <th>DI PINJAM OLEH</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in loanList" :key="item.mrCode">
              <td>
                <input
                  type="checkbox"
                  :checked="selectedMrCodes.includes(item.mrCode)"
                  @change="toggleSelected(item.mrCode)"
                />
              </td>
              <td>{{ item.mrCode }}</td>
              <td>{{ item.patientName }}</td>
              <td>{{ item.statusLabel }}</td>
              <td>{{ item.unitName }}</td>
            </tr>
            <tr v-if="!loanList.length">
              <td colspan="5" class="empty-state">Tidak ada berkas yang sedang/akan dipinjam.</td>
            </tr>
          </tbody>
        </table>

        <div class="file-actions">
          <button class="small-button primary" type="button" @click="updateStatus('MR_OUT')">📤 MR KELUAR</button>
          <button class="small-button primary" type="button" @click="updateStatus('MR_BACK')">📥 MR KEMBALI</button>
          <button class="small-button" type="button" @click="updateStatus('MR_CANCEL')">❌ BATAL PINJAM</button>
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

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 8px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.status-banner--success { background: #e6f7ee; color: #1a7f4b; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th { text-align: left; padding: 8px; border-bottom: 2px solid #e5e7eb; color: #304b73; }
.file-table td { padding: 8px; border-bottom: 1px solid #f3f4f6; }
.empty-state { color: #9ca3af; text-align: center; padding: 12px; }

.file-actions { display: flex; gap: 10px; margin-top: 12px; }

.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
</style>
