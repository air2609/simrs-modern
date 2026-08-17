<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  },
  mrCode: {
    type: String,
    default: null
  }
});

const emit = defineEmits(['session-expired']);

const loading = ref(false);
const error = ref('');
const items = ref([]);

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

async function loadHistory() {
  if (!props.mrCode) {
    items.value = [];
    return;
  }
  error.value = '';
  loading.value = true;
  try {
    items.value = await request(`/mr/diagnose/history?mrCode=${encodeURIComponent(props.mrCode)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

watch(() => props.mrCode, loadHistory, { immediate: true });
</script>

<template>
  <div class="card">
    <h3 class="card-title">HISTORY DIAGNOSA PASIEN</h3>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <div v-if="loading" class="loading">Memuat history diagnosa...</div>
    <p v-else-if="!mrCode" class="empty-state">Cari pasien pada tab DIAGNOSA PASIEN terlebih dahulu.</p>

    <table v-else class="file-table">
      <thead>
        <tr>
          <th>TANGGAL</th>
          <th>UNIT</th>
          <th>DOKTER</th>
          <th>CATATAN DOKTER</th>
          <th>DIAGNOSA</th>
          <th>HASIL LAB</th>
          <th>RESEP</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, index) in items" :key="index">
          <td>{{ item.date }}</td>
          <td>{{ item.unitName }}</td>
          <td>{{ item.doctorName }}</td>
          <td>{{ item.notes }}</td>
          <td>{{ item.diagnosisNames }}</td>
          <td>{{ item.labResultLabel }}</td>
          <td>{{ item.receiptText }}</td>
        </tr>
        <tr v-if="!items.length">
          <td colspan="7" class="empty-state">Tidak ada history diagnosa.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }
.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }
.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th { text-align: left; padding: 8px; border-bottom: 2px solid #e5e7eb; color: #304b73; }
.file-table td { padding: 8px; border-bottom: 1px solid #f3f4f6; }
.empty-state { color: #9ca3af; text-align: center; padding: 12px; }
</style>
