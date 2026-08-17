<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const error = ref('');
const items = ref([]);
const antrianText = ref('');

let refreshTimer = null;

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

function scheduleRefresh(delayMillis) {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
  }
  refreshTimer = setTimeout(loadData, delayMillis || 5000);
}

async function loadData() {
  try {
    const data = await request('/antrian/apotik-display');
    items.value = data.items || [];
    antrianText.value = data.antrianText || '';
    error.value = '';
    scheduleRefresh(data.delayMillis);
  } catch (requestError) {
    error.value = requestError.message;
    scheduleRefresh(5000);
  } finally {
    loading.value = false;
  }
}

function rowNumber(index) {
  const number = index + 1;
  return number < 10 ? `0${number}` : `${number}`;
}

function patientLabel(item) {
  return item.mrCode ? `${item.patientName} (${item.mrCode})` : item.patientName;
}

onMounted(loadData);

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
  }
});
</script>

<template>
  <div class="display-page">
    <h2 class="display-title">OBAT PASIEN SUDAH JADI</h2>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div v-if="loading" class="loading">Memuat data antrian apotik...</div>

    <template v-else>
      <div class="display-table">
        <table>
          <thead>
            <tr>
              <th class="col-name">NAMA PASIEN</th>
              <th class="col-type">TIPE OBAT</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in items" :key="index">
              <td class="col-name">{{ rowNumber(index) }}. {{ patientLabel(item) }}</td>
              <td class="col-type">{{ item.drugType }}</td>
            </tr>
            <tr v-if="!items.length">
              <td colspan="2" class="empty-state">Belum ada obat yang sudah jadi.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="antrianText" class="marquee-wrap">
        <div class="marquee-text">{{ antrianText }}</div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.display-page { padding: 16px; }
.display-title { text-align: center; color: #304b73; font-size: 24px; margin-bottom: 20px; }

.loading { padding: 24px; text-align: center; color: #9ca3af; }
.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.display-table {
  max-width: 950px;
  margin: 0 auto;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  max-height: 450px;
  overflow-y: auto;
}
table { width: 100%; border-collapse: collapse; }
th, td { padding: 10px 16px; text-align: left; font-weight: bold; font-size: 16px; }
thead th { background: #304b73; color: #fff; position: sticky; top: 0; }
.col-name { width: 80%; }
.col-type { width: 20%; }
tbody tr:nth-child(even) { background: #f8fafc; }
.empty-state { text-align: center; color: #9ca3af; font-weight: normal; }

.marquee-wrap {
  max-width: 70%;
  margin: 20px auto 0;
  overflow: hidden;
  white-space: nowrap;
}
.marquee-text {
  display: inline-block;
  font-weight: bold;
  font-size: 15pt;
  padding-left: 100%;
  animation: marquee 15s linear infinite;
}
@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}
</style>
