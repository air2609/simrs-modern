<script setup>
import { onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(true);
const saving = ref(false);
const error = ref('');

const coaOptions = ref([]);

// Nilai default (acctNo) per field
const inAr = ref('');
const outAr = ref('');
const ap = ref('');
const apPatient = ref('');
const pph21 = ref('');
const miscTrx = ref('');
const apStaff = ref('');

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

async function initialize() {
  loading.value = true;
  error.value = '';
  try {
    const masters = await request('/accounting/acct-default/masters');
    coaOptions.value = masters.coaOptions || [];
    inAr.value = masters.inAr || '';
    outAr.value = masters.outAr || '';
    ap.value = masters.ap || '';
    apPatient.value = masters.apPatient || '';
    pph21.value = masters.pph21 || '';
    miscTrx.value = masters.miscTrx || '';
    apStaff.value = masters.apStaff || '';
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function save() {
  error.value = '';
  saving.value = true;
  try {
    await request('/accounting/acct-default', {
      method: 'POST',
      body: JSON.stringify({
        inAr: inAr.value,
        outAr: outAr.value,
        ap: ap.value,
        apPatient: apPatient.value,
        pph21: pph21.value,
        miscTrx: miscTrx.value,
        apStaff: apStaff.value
      })
    });
    alert('Data Berhasil Disimpan');
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    saving.value = false;
  }
}

onMounted(initialize);
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🧾 FORM ACCT DEFAULT</h2>
      <p class="page-subtitle">Migrasi screen legacy SCM0050 — acctDefaultDataInput.zul (Akun Default COA)</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>
    <div v-if="loading" class="loading">Memuat data COA...</div>

    <template v-else>
      <div class="card">
        <h3 class="card-title">FORM ACCT DEFAULT</h3>
        <div class="field">
          <label>AR PASIEN RANAP</label>
          <select v-model="inAr">
            <option value="">- Pilih COA -</option>
            <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.acctNo">{{ opt.acctNo }} - {{ opt.acctName }}</option>
          </select>
        </div>
        <div class="field">
          <label>AR PASIEN RAJAL</label>
          <select v-model="outAr">
            <option value="">- Pilih COA -</option>
            <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.acctNo">{{ opt.acctNo }} - {{ opt.acctName }}</option>
          </select>
        </div>
        <div class="field">
          <label>AP</label>
          <select v-model="ap">
            <option value="">- Pilih COA -</option>
            <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.acctNo">{{ opt.acctNo }} - {{ opt.acctName }}</option>
          </select>
        </div>
        <div class="field">
          <label>AP PASIEN</label>
          <select v-model="apPatient">
            <option value="">- Pilih COA -</option>
            <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.acctNo">{{ opt.acctNo }} - {{ opt.acctName }}</option>
          </select>
        </div>
        <div class="field">
          <label>PPH 21</label>
          <select v-model="pph21">
            <option value="">- Pilih COA -</option>
            <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.acctNo">{{ opt.acctNo }} - {{ opt.acctName }}</option>
          </select>
        </div>
        <div class="field">
          <label>BIAYA LAIN-LAIN</label>
          <select v-model="miscTrx">
            <option value="">- Pilih COA -</option>
            <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.acctNo">{{ opt.acctNo }} - {{ opt.acctName }}</option>
          </select>
        </div>
        <div class="field">
          <label>AP STAFF</label>
          <select v-model="apStaff">
            <option value="">- Pilih COA -</option>
            <option v-for="opt in coaOptions" :key="opt.coaId" :value="opt.acctNo">{{ opt.acctNo }} - {{ opt.acctName }}</option>
          </select>
        </div>
      </div>

      <div class="action-bar">
        <button class="small-button primary" type="button" :disabled="saving" @click="save">💾 SIMPAN</button>
        <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; max-width: 720px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.field { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button:disabled { opacity: 0.5; cursor: default; }
</style>
