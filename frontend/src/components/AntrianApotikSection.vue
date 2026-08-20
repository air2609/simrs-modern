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

const validatedNotes = ref([]);
const readyNotes = ref([]);

const antrianText = ref('');
const hasAntrianText = ref(false);
const isEditing = ref(false);

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
  const data = await request('/antrian/apotik');
  validatedNotes.value = data.validatedNotes || [];
  readyNotes.value = data.readyNotes || [];
  antrianText.value = data.antrianText || '';
  hasAntrianText.value = data.hasAntrianText;
  isEditing.value = false;
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

async function moveToReady(note) {
  error.value = '';
  success.value = '';
  try {
    await request(`/antrian/apotik/notes/${note.noteId}/move-to-ready`, { method: 'POST' });
    await loadData();
  } catch (requestError) {
    error.value = requestError.message;
  }
}

async function takeOut(note) {
  error.value = '';
  success.value = '';
  try {
    await request(`/antrian/apotik/notes/${note.noteId}/take-out`, { method: 'POST' });
    await loadData();
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function startEdit() {
  isEditing.value = true;
}

async function saveText() {
  error.value = '';
  success.value = '';
  try {
    await request('/antrian/apotik/text', {
      method: 'POST',
      body: JSON.stringify({ antrianText: antrianText.value })
    });
    success.value = 'Data Sukses Disimpan...!';
    isEditing.value = false;
    hasAntrianText.value = true;
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
        <h2>💊 Kontrol Antrian Apotik</h2>
      </div>
      <div class="header-actions">
        <button class="small-button" type="button" @click="initialize">🔄 Refresh</button>
      </div>
    </div>

    <!-- Notifications -->
    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <p v-if="success" class="status-banner status-banner--success">{{ success }}</p>

    <div v-if="loading" class="loading">Memuat data antrian apotik...</div>

    <template v-else>
      <!-- Dua panel: nota divalidasi & obat jadi -->
      <div class="panels">
        <div class="card panel">
          <h3 class="card-title">NOTA PASIEN YANG SUDAH DIVALIDASI</h3>
          <div class="note-list">
            <button
              v-for="note in validatedNotes"
              :key="note.noteId"
              class="note-item"
              type="button"
              title="Klik untuk pindah ke daftar obat jadi"
              @click="moveToReady(note)"
            >
              <span class="note-name">{{ note.patientName }}</span>
              <span class="note-meta">{{ note.noteNumber }} · {{ note.createdTime }}</span>
            </button>
            <p v-if="!validatedNotes.length" class="empty-state">Tidak ada nota yang sudah divalidasi.</p>
          </div>
        </div>

        <div class="card panel">
          <h3 class="card-title">OBAT PASIEN YANG SUDAH JADI</h3>
          <div class="note-list">
            <button
              v-for="note in readyNotes"
              :key="note.noteId"
              class="note-item"
              type="button"
              title="Klik untuk mengeluarkan dari antrian"
              @click="takeOut(note)"
            >
              <span class="note-name">{{ note.patientName }}</span>
              <span class="note-meta">{{ note.noteNumber }} · {{ note.createdTime }}</span>
            </button>
            <p v-if="!readyNotes.length" class="empty-state">Tidak ada obat yang sudah jadi.</p>
          </div>
        </div>
      </div>

      <!-- Teks antrian -->
      <div class="card">
        <h3 class="card-title">TEXT ANTRIAN</h3>
        <div class="antrian-row">
          <textarea
            v-model="antrianText"
            class="antrian-text"
            rows="3"
            :readonly="!isEditing"
            placeholder="Masukkan teks antrian apotik..."
          ></textarea>
          <div class="antrian-actions">
            <button
              class="small-button primary"
              type="button"
              :disabled="isEditing"
              @click="saveText"
            >
              💾 SIMPAN
            </button>
            <button
              class="small-button"
              type="button"
              :disabled="!hasAntrianText || isEditing"
              @click="startEdit"
            >
              ✏️ UBAH
            </button>
            <button class="small-button" type="button" @click="initialize">🔄 REFRESH</button>
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

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card-title { margin: 0 0 16px; color: #304b73; font-size: 15px; }

.panels { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.panel { min-height: 320px; }

.note-list { display: flex; flex-direction: column; gap: 8px; max-height: 320px; overflow-y: auto; }
.note-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s, border-color 0.15s;
}
.note-item:hover { background: #eef3fb; border-color: #5f83c2; }
.note-name { font-weight: 700; color: #1f2937; }
.note-meta { font-size: 12px; color: #6b7280; }

.empty-state { color: #9ca3af; text-align: center; padding: 20px; }

.antrian-row { display: flex; align-items: flex-start; gap: 12px; }
.antrian-text {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #d1d9e6;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}
.antrian-text:focus { outline: none; border-color: #1d4ed8; box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15); }
.antrian-text[readonly] { background: #f1f5f9; color: #475569; }

.antrian-actions { display: flex; flex-direction: column; gap: 8px; }

.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
.small-button:disabled { opacity: 0.5; cursor: not-allowed; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }

@media (max-width: 960px) {
  .panels { grid-template-columns: 1fr; }
  .antrian-row { flex-direction: column; }
}
</style>
