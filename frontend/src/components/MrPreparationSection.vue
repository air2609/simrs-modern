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
const success = ref('');

const notReadyList = ref([]);
const readyList = ref([]);
const selectedRegId = ref(null);

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
  const data = await request('/mr/preparation');
  notReadyList.value = data.notReadyList || [];
  readyList.value = data.readyList || [];
  selectedRegId.value = null;
}

async function initialize() {
  loading.value = true;
  error.value = '';
  success.value = '';
  try {
    await loadData();
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function selectItem(regId) {
  selectedRegId.value = regId;
}

async function markReady() {
  error.value = '';
  success.value = '';

  if (!selectedRegId.value) {
    error.value = 'PILIH DATA BERKAS REKAM MEDIS YANG BELUM SIAP TERLEBIH DAHULU...!';
    return;
  }

  try {
    await request(`/mr/preparation/${selectedRegId.value}/mark-ready`, { method: 'POST' });
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
        <h2>🗂️ Persiapan Dokumen Rekam Medis</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-if="success" class="status-banner status-banner--success">{{ success }}</p>

    <div v-if="loading" class="loading">Memuat data berkas rekam medis...</div>

    <template v-else>
      <div class="panels">
        <div class="card panel">
          <h3 class="card-title">DAFTAR BERKAS REKAM MEDIS BELUM SIAP</h3>
          <div class="note-list">
            <button
              v-for="item in notReadyList"
              :key="item.regId"
              class="note-item"
              type="button"
              :class="{ selected: selectedRegId === item.regId }"
              @click="selectItem(item.regId)"
            >
              <span class="note-name">{{ item.mrCode }} · {{ item.patientName }}</span>
              <span class="note-meta">{{ item.unitName }}</span>
            </button>
            <p v-if="!notReadyList.length" class="empty-state">Tidak ada berkas yang belum siap.</p>
          </div>
        </div>

        <div class="move-action">
          <button class="small-button primary" type="button" @click="markReady">DOKUMEN SIAP ➡️</button>
        </div>

        <div class="card panel">
          <h3 class="card-title">DAFTAR BERKAS REKAM MEDIS SUDAH SIAP</h3>
          <div class="note-list">
            <button
              v-for="item in readyList"
              :key="item.regId"
              class="note-item"
              type="button"
              disabled
            >
              <span class="note-name">{{ item.mrCode }} · {{ item.patientName }}</span>
              <span class="note-meta">{{ item.unitName }}</span>
            </button>
            <p v-if="!readyList.length" class="empty-state">Tidak ada berkas yang sudah siap.</p>
          </div>
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
.status-banner--success { background: #e6f7ee; color: #1a7f4b; }

.panels { display: flex; align-items: stretch; gap: 16px; flex-wrap: wrap; }
.panel { flex: 1; min-width: 280px; }
.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.move-action { display: flex; align-items: center; justify-content: center; }

.note-list { display: flex; flex-direction: column; gap: 6px; max-height: 500px; overflow-y: auto; }
.note-item { display: flex; flex-direction: column; align-items: flex-start; gap: 2px; padding: 8px 12px; border: 1px solid #e5e7eb; border-radius: 8px; background: #f9fafb; cursor: pointer; text-align: left; }
.note-item:hover:not(:disabled) { background: #eef2ff; border-color: #c7d2fe; }
.note-item.selected { background: #dbeafe; border-color: #60a5fa; }
.note-item:disabled { cursor: default; opacity: 0.85; }
.note-name { font-weight: 600; color: #1f2937; font-size: 13px; }
.note-meta { color: #6b7280; font-size: 12px; }
.empty-state { color: #9ca3af; font-size: 13px; text-align: center; padding: 12px; }

.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
</style>
