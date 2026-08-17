<script setup>
import { ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired']);

// ===== SEBAB KEMATIAN (migrasi dari sebabKematian.zul, tab SEBAB KEMATIAN SC0083) =====
const codList = ref('');
const duration = ref(null);
const violentList = ref('');
const caraKejadian = ref('');
const bodyDestroy = ref('');
const pregnantList = ref('');
const causeOfStillbirth = ref('');
const operationType = ref('');

const diseaseList = ref([]); // [{icdId, icdCode, icdName}]

// ICD search
const showIcdDialog = ref(false);
const icdSearchCode = ref('');
const icdSearchName = ref('');
const icdResults = ref([]);
const icdSearching = ref(false);

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Session expired.');
    throw new Error(payload?.message || 'Unauthorized');
  }
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  return payload.data;
}

function clear() {
  codList.value = '';
  duration.value = null;
  violentList.value = '';
  caraKejadian.value = '';
  bodyDestroy.value = '';
  pregnantList.value = '';
  causeOfStillbirth.value = '';
  operationType.value = '';
  diseaseList.value = [];
}

async function searchIcd() {
  icdSearching.value = true;
  try {
    const params = new URLSearchParams();
    if (icdSearchCode.value) params.set('code', icdSearchCode.value);
    if (icdSearchName.value) params.set('name', icdSearchName.value);
    icdResults.value = await request(`/mr/diagnose/icd/search?${params.toString()}`);
  } catch (requestError) {
    alert(requestError.message);
  } finally {
    icdSearching.value = false;
  }
}

function addIcd(item) {
  if (!diseaseList.value.some((e) => e.icdId === item.icdId)) {
    diseaseList.value.push(item);
  }
}

function removeIcd(icdId) {
  diseaseList.value = diseaseList.value.filter((i) => i.icdId !== icdId);
}

// Data untuk disimpan bersama diagnose (dibaca parent)
function toPayload() {
  const deathType = {
    'Karena Penyakit': 13,
    'Karena Rudapaksa': 14,
    'Kelahiran Mati': 15,
    'Persalinan, Kehamilan': 16,
    'Operasi': 17
  }[codList.value];

  return {
    deathType,
    illness: deathType === 13 ? diseaseList.value.map((icd) => ({
      icdId: icd.icdId,
      duration: duration.value ? Number(duration.value) : null
    })) : null,
    violent: deathType === 14 ? {
      violentType: { 'BUNUH DIRI': 1, 'PEMBUNUHAN': 2, 'KECELAKAAN': 3 }[violentList.value] || null,
      description: caraKejadian.value,
      damageDescription: bodyDestroy.value
    } : null,
    pregnancy: deathType === 16 ? {
      type: pregnantList.value === 'Kehamilan' ? 21 : 22
    } : null,
    stillbirth: deathType === 15 ? { description: causeOfStillbirth.value } : null,
    operation: deathType === 17 ? { operationType: operationType.value } : null
  };
}

defineExpose({ toPayload, clear });
</script>

<template>
  <div class="cod-section">
    <h3 class="card-title">FORM SEBAB KEMATIAN</h3>

    <div class="field">
      <label for="cod-list">SEBAB KEMATIAN</label>
      <select id="cod-list" v-model="codList">
        <option value="" disabled>-- Pilih --</option>
        <option value="Karena Penyakit">1. KARENA PENYAKIT</option>
        <option value="Karena Rudapaksa">2. KARENA RUDAPAKSA</option>
        <option value="Kelahiran Mati">3. KELAHIRAN MATI</option>
        <option value="Persalinan, Kehamilan">4. PERSALINAN, KEHAMILAN</option>
        <option value="Operasi">5. OPERASI</option>
      </select>
    </div>

    <!-- MATI KARENA PENYAKIT -->
    <div v-if="codList === 'Karena Penyakit'" class="cod-group">
      <h4 class="cod-group-title">MATI KARENA PENYAKIT</h4>
      <table class="file-table">
        <thead>
          <tr>
            <th>KODE</th>
            <th>KETERANGAN</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="icd in diseaseList" :key="icd.icdId">
            <td>{{ icd.icdCode }}</td>
            <td>{{ icd.icdName }}</td>
            <td><button class="small-button" type="button" @click="removeIcd(icd.icdId)">🗑️</button></td>
          </tr>
          <tr v-if="!diseaseList.length">
            <td colspan="3" class="empty-state">Belum ada diagnosa penyebab kematian.</td>
          </tr>
        </tbody>
      </table>
      <div class="cod-actions">
        <div class="field duration-field">
          <label for="cod-duration">DURASI KEMATIAN (JAM SETELAH SAKIT)</label>
          <input id="cod-duration" v-model.number="duration" type="number" min="0" />
        </div>
        <button class="small-button" type="button" @click="showIcdDialog = true">TAMBAH ICD</button>
      </div>
    </div>

    <!-- MATI KARENA RUDAPAKSA -->
    <div v-if="codList === 'Karena Rudapaksa'" class="cod-group">
      <h4 class="cod-group-title">MATI KARENA RUDAPAKSA</h4>
      <div class="cod-grid">
        <div class="field">
          <label for="cod-violent">MACAM RUDAPAKSA</label>
          <select id="cod-violent" v-model="violentList">
            <option value="" disabled>-- Pilih --</option>
            <option value="BUNUH DIRI">BUNUH DIRI</option>
            <option value="PEMBUNUHAN">PEMBUNUHAN</option>
            <option value="KECELAKAAN">KECELAKAAN</option>
          </select>
        </div>
        <div class="field">
          <label for="cod-cara">CARA KEJADIAN</label>
          <input id="cod-cara" v-model="caraKejadian" type="text" />
        </div>
        <div class="field">
          <label for="cod-jejas">SIFAT JEJAS</label>
          <input id="cod-jejas" v-model="bodyDestroy" type="text" />
        </div>
      </div>
    </div>

    <!-- MATI KARENA KEHAMILAN/PERSALINAN -->
    <div v-if="codList === 'Persalinan, Kehamilan'" class="cod-group">
      <h4 class="cod-group-title">MATI KARENA KEHAMILAN/PERSALINAN</h4>
      <div class="field">
        <label for="cod-pregnancy">PERISTIWA KEMATIAN</label>
        <select id="cod-pregnancy" v-model="pregnantList">
          <option value="" disabled>-- Pilih --</option>
          <option value="Kehamilan">1. KARENA KEHAMILAN</option>
          <option value="Persalinan">2. KARENA PERSALINAN</option>
        </select>
      </div>
    </div>

    <!-- MATI KARENA KELAHIRAN MATI -->
    <div v-if="codList === 'Kelahiran Mati'" class="cod-group">
      <h4 class="cod-group-title">MATI KARENA KELAHIRAN MATI</h4>
      <div class="field">
        <label for="cod-stillbirth">SEBAB KELAHIRAN MATI</label>
        <textarea id="cod-stillbirth" v-model="causeOfStillbirth" rows="4"></textarea>
      </div>
    </div>

    <!-- MATI KARENA OPERASI -->
    <div v-if="codList === 'Operasi'" class="cod-group">
      <h4 class="cod-group-title">MATI KARENA OPERASI</h4>
      <div class="field">
        <label for="cod-operation">JENIS OPERASI</label>
        <textarea id="cod-operation" v-model="operationType" rows="2"></textarea>
      </div>
    </div>

    <!-- ICD SEARCH DIALOG -->
    <div v-if="showIcdDialog" class="modal-overlay" @click.self="showIcdDialog = false">
      <div class="modal-card">
        <h3 class="card-title">PENCARIAN DATA DIAGNOSA (ICD)</h3>
        <div class="search-form-grid">
          <div class="field">
            <label>KODE</label>
            <input v-model="icdSearchCode" />
          </div>
          <div class="field">
            <label>NAMA</label>
            <input v-model="icdSearchName" />
          </div>
        </div>
        <div class="modal-actions">
          <button class="small-button primary" type="button" :disabled="icdSearching" @click="searchIcd">CARI</button>
          <button class="small-button" type="button" @click="showIcdDialog = false">TUTUP</button>
        </div>
        <table class="file-table">
          <thead><tr><th>KODE</th><th>NAMA</th><th></th></tr></thead>
          <tbody>
            <tr v-for="item in icdResults" :key="item.icdId">
              <td>{{ item.icdCode }}</td>
              <td>{{ item.icdName }}</td>
              <td><button class="small-button" type="button" @click="addIcd(item)">+ Pilih</button></td>
            </tr>
            <tr v-if="!icdResults.length"><td colspan="3" class="empty-state">Tidak ada data.</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cod-section { padding: 8px; }
.cod-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.cod-group { margin-top: 16px; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px; background: #fafbfc; }
.cod-group-title { margin: 0 0 12px; color: #304b73; font-size: 14px; }
.cod-actions { display: flex; align-items: flex-end; gap: 12px; margin-top: 12px; }
.duration-field { flex: 1; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field select, .field input, .field textarea { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input:focus, .field select:focus, .field textarea:focus { outline: none; border-color: #1d4ed8; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }
.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th { text-align: left; padding: 8px; border-bottom: 2px solid #e5e7eb; color: #304b73; }
.file-table td { padding: 8px; border-bottom: 1px solid #f3f4f6; }
.empty-state { color: #9ca3af; text-align: center; padding: 12px; }
.search-form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 12px; }
.modal-actions { display: flex; gap: 10px; margin-bottom: 12px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal-card { background: #fff; border-radius: 12px; padding: 20px; width: 820px; max-width: 95vw; max-height: 85vh; overflow-y: auto; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.6; cursor: default; }
</style>