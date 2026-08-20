<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const doctorName = ref('');
const rows = ref([]);

// paging (pageSize 10, sesuai legacy mold=paging pageSize=10)
const pageSize = 10;
const currentPage = ref(1);
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return rows.value.slice(start, start + pageSize);
});
const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)));
function goToPage(page) {
  currentPage.value = Math.min(Math.max(1, page), totalPages.value);
}

// auto refresh tiap 30 detik (layar antrian)
let refreshTimer = null;

function fmtNomor(nomor) {
  if (nomor === null || nomor === undefined) return '-';
  return nomor < 10 ? `0${nomor}` : String(nomor);
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

async function loadQueue() {
  loading.value = true;
  error.value = '';
  try {
    const data = await request('/antrian/per-dokter');
    doctorName.value = data.doctorName || '';
    rows.value = data.rows || [];
    currentPage.value = 1;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadQueue();
  refreshTimer = setInterval(loadQueue, 30000);
});

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer);
});
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🩺 ANTRIAN DOKTER</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">⚠️ {{ error }}</p>

    <div class="queue-card">
      <div class="doctor-bar">
        <span class="doctor-name">{{ doctorName || '...' }}</span>
        <button class="small-button" type="button" :disabled="loading" @click="loadQueue">🔄 Refresh</button>
      </div>

      <div v-if="loading" class="loading">Memuat antrian...</div>
      <div v-else class="queue-list">
        <div
          v-for="(row, index) in pagedRows"
          :key="row.registrationId ?? index"
          class="queue-item"
        >
          <span class="queue-nomor">{{ fmtNomor(row.number) }}</span>
          <span class="queue-pasien">{{ row.patientName }} ({{ row.mrCode }})</span>
        </div>
        <div v-if="!rows.length" class="empty-state">Tidak ada antrian pasien saat ini.</div>
      </div>

      <div class="pagination" v-if="rows.length > pageSize">
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(1)">⏮</button>
        <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">◀</button>
        <span class="page-info">Halaman {{ currentPage }} / {{ totalPages }} ({{ rows.length }} antrian)</span>
        <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">▶</button>
        <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(totalPages)">⏭</button>
      </div>

      <div class="action-bar">
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.queue-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
  max-width: 760px;
  margin: 0 auto;
}

.doctor-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 2px solid #eef2f7;
}
.doctor-name {
  font-size: 24px;
  font-weight: 900;
  color: #1d3a6b;
  letter-spacing: 0.02em;
}

.queue-list { min-height: 120px; }
.queue-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
}
.queue-item:hover { background: #f6f8fb; }
.queue-nomor {
  min-width: 52px;
  text-align: center;
  background: linear-gradient(135deg, #2f86eb, #1d5fae);
  color: #fff;
  font-weight: 900;
  font-size: 18px;
  padding: 6px 10px;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(31, 95, 174, 0.3);
}
.queue-pasien {
  font-size: 18px;
  font-weight: 800;
  color: #22324d;
}

.empty-state { color: #9ca3af; text-align: center; padding: 24px; font-size: 15px; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 10px; margin: 14px 0 4px; flex-wrap: wrap; }
.page-btn { padding: 6px 12px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.page-btn:hover:not(:disabled) { background: #eef3fa; }
.page-btn:disabled { opacity: 0.4; cursor: default; }
.page-info { font-weight: 700; color: #304b73; font-size: 13px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 16px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button:disabled { opacity: 0.5; cursor: default; }
</style>
