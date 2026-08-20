<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

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
const lastUpdated = ref(null);

// Auto-refresh interval (10 detik, mengikuti timer legacy)
const REFRESH_INTERVAL_MS = 10000;
let refreshTimer = null;

const totals = computed(() => {
  return rows.value.reduce(
    (acc, row) => {
      acc.total += row.totalBeds;
      acc.occupied += row.occupiedBeds;
      acc.booked += row.bookedBeds;
      acc.inService += row.inServiceBeds;
      acc.empty += row.emptyBeds;
      return acc;
    },
    { total: 0, occupied: 0, booked: 0, inService: 0, empty: 0 }
  );
});

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

async function loadBedInfo() {
  rows.value = await request('/ward/bed-info');
  lastUpdated.value = new Date();
}

async function initialize() {
  loading.value = true;
  error.value = '';

  try {
    await loadBedInfo();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  initialize();
  // Auto-refresh berkala seperti timer legacy (10 detik)
  refreshTimer = setInterval(() => {
    loadBedInfo().catch((requestError) => {
      error.value = requestError.message;
    });
  }, REFRESH_INTERVAL_MS);
});

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
});
</script>

<template>
  <div class="screen-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>🛏️ Informasi Kamar Ranap</h2>
      </div>
      <div class="header-actions">
        <span v-if="lastUpdated" class="updated-label">
          Terakhir diperbarui: {{ lastUpdated.toLocaleTimeString('id-ID') }}
        </span>
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat informasi kamar...</div>

    <template v-else>
      <div class="card">
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>KELAS</th>
                <th>RUANGAN</th>
                <th class="num">TOTAL BED</th>
                <th class="num">BED TERISI</th>
                <th class="num">BED DIPESAN</th>
                <th class="num">BED PERBAIKAN</th>
                <th class="num">BED KOSONG</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in rows" :key="`${row.tariffClass}-${row.hallName}-${index}`">
                <td class="strong">{{ row.tariffClass }}</td>
                <td class="strong">{{ row.hallName }}</td>
                <td class="num">{{ row.totalBeds }}</td>
                <td class="num">{{ row.occupiedBeds }}</td>
                <td class="num">{{ row.bookedBeds }}</td>
                <td class="num">{{ row.inServiceBeds }}</td>
                <td class="num">{{ row.emptyBeds }}</td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="7" class="empty-state">Tidak ada data bed yang ditampilkan.</td>
              </tr>
            </tbody>
            <tfoot v-if="rows.length">
              <tr>
                <td></td>
                <td class="total-label">TOTAL</td>
                <td class="num total-value">{{ totals.total }}</td>
                <td class="num total-value">{{ totals.occupied }}</td>
                <td class="num total-value">{{ totals.booked }}</td>
                <td class="num total-value">{{ totals.inService }}</td>
                <td class="num total-value">{{ totals.empty }}</td>
              </tr>
            </tfoot>
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
.updated-label { font-size: 12px; color: #6b7280; }

.loading { padding: 24px; text-align: center; color: #9ca3af; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 14px; }
.table th, .table td { padding: 10px 12px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.table tbody tr:hover { background: #f6f8fb; }

.num { text-align: center; }
.strong { font-weight: 700; }

.total-label { font-weight: 700; color: #1d4ed8; }
.total-value { font-weight: 700; color: #1d4ed8; }

tfoot td { border-top: 2px solid #d1d9e6; border-bottom: none; background: #f6f8fb; }

.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
.small-button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
