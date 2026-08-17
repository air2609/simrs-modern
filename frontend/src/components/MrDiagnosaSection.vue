<script setup>
import { ref } from 'vue';
import CauseOfDeathSection from './CauseOfDeathSection.vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired']);

const activeTab = ref('diagnose');
const error = ref('');
const loading = ref(false);

// NO. MR bandbox (pola SCM0020)
const mrSearchOpen = ref(false);
const mrCodeInput = ref('');
const mrSearchCode = ref('');
const mrSearchName = ref('');
const mrSearchBirthDate = ref('');
const mrSearchAddress = ref('');
const mrSearchResults = ref([]);
const mrSearching = ref(false);

const registration = ref(null);
const diagnoseType = ref('out');
const conditionList = ref('');
const caraKeluarList = ref('');
const selectedDiagnoses = ref([]);
const causeOfDeathRef = ref(null);

// ICD search
const showIcdDialog = ref(false);
const icdSearchCode = ref('');
const icdSearchName = ref('');
const icdResults = ref([]);
const icdSearching = ref(false);

const KEADAAN_KELUAR = [
  { value: '', label: '' },
  { value: 'Sembuh', label: '1. SEMBUH' },
  { value: 'Membaik', label: '2. MEMBAIK' },
  { value: 'Belum Sembuh', label: '3. BELUM SEMBUH' },
  { value: 'Mati kurang dari 48 jam', label: '4. MATI KURANG DARI 48 JAM' },
  { value: 'Mati lebih dari 48 jam', label: '5. MATI LEBIH DARI 48 JAM' }
];

const CARA_KELUAR = [
  { value: '', label: '' },
  { value: 'Diizinkan Pulang', label: '1. DIIZINKAN PULANG' },
  { value: 'Melarikan Diri', label: '2. MELARIKAN DIRI' },
  { value: 'Pindah RS Lain', label: '3. PINDAH RS LAIN' },
  { value: 'Meninggal', label: '4. MENINGGAL' },
  { value: 'Atas Kemauan Sendiri', label: '5. ATAS KEMAUAN SENDIRI' },
  { value: 'Di Rujuk', label: '6. DI RUJUK' }
];

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

function hasText(value) {
  return value != null && String(value).trim().length > 0;
}

async function lookupMr() {
  error.value = '';
  if (!hasText(mrCodeInput.value)) return;
  loading.value = true;
  try {
    const reg = await request(`/mr/diagnose/registration?mrCode=${encodeURIComponent(mrCodeInput.value.trim())}`);
    registration.value = reg;
    mrCodeInput.value = reg.mrCode;
    diagnoseType.value = reg.ranap ? 'in' : 'out';
    selectedDiagnoses.value = (reg.icdIds || []).map((id, index) => ({
      icdId: id,
      icdCode: '',
      icdName: reg.icdNames[index]
    }));
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function openMrBandbox() {
  mrSearchOpen.value = true;
  mrSearchResults.value = [];
}

async function searchMrPatients() {
  error.value = '';
  mrSearching.value = true;
  try {
    const params = new URLSearchParams();
    if (mrSearchCode.value) params.set('mrCode', mrSearchCode.value);
    if (mrSearchName.value) params.set('patientName', mrSearchName.value);
    if (mrSearchBirthDate.value) params.set('birthDate', mrSearchBirthDate.value);
    if (mrSearchAddress.value) params.set('address', mrSearchAddress.value);
    mrSearchResults.value = await request(`/mr/diagnose/patients/search?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    mrSearching.value = false;
  }
}

async function pickMrPatient(item) {
  mrCodeInput.value = item.mrCode;
  mrSearchOpen.value = false;
  await lookupMr();
}

// Meniru checkIfDeath(): buka tab SEBAB KEMATIAN bila MENINGGAL
function onCaraKeluarChange() {
  if (caraKeluarList.value === 'Meninggal') {
    activeTab.value = 'cod';
  }
}

function openIcdDialog() {
  showIcdDialog.value = true;
  icdResults.value = [];
}

async function searchIcd() {
  icdSearching.value = true;
  try {
    const params = new URLSearchParams();
    if (icdSearchCode.value) params.set('code', icdSearchCode.value);
    if (icdSearchName.value) params.set('name', icdSearchName.value);
    icdResults.value = await request(`/mr/diagnose/icd/search?${params.toString()}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    icdSearching.value = false;
  }
}

function addIcd(item) {
  if (!selectedDiagnoses.value.some((e) => e.icdId === item.icdId)) {
    selectedDiagnoses.value.push(item);
  }
}

function removeDiagnose(icdId) {
  selectedDiagnoses.value = selectedDiagnoses.value.filter((i) => i.icdId !== icdId);
}

function clearForm() {
  registration.value = null;
  mrCodeInput.value = '';
  diagnoseType.value = 'out';
  conditionList.value = '';
  caraKeluarList.value = '';
  selectedDiagnoses.value = [];
  if (causeOfDeathRef.value) causeOfDeathRef.value.clear();
  activeTab.value = 'diagnose';
}

async function save() {
  error.value = '';
  if (!registration.value) {
    error.value = 'NO MR WAJIB DIISI..';
    return;
  }
  if (!selectedDiagnoses.value.length) {
    error.value = 'DIAGNOSA WAJIB DIISI..';
    return;
  }
  try {
    const body = {
      mrCode: registration.value.mrCode,
      diagnoseType: diagnoseType.value,
      notes: '',
      icdIds: selectedDiagnoses.value.map((d) => d.icdId),
      existingDiagnoseId: registration.value.existingDiagnoseId || null,
      prescriptionLines: []
    };
    await request('/mr/diagnose', { method: 'POST', body: JSON.stringify(body) });
    alert('Sukses Menyimpan Data!');
  } catch (requestError) {
    error.value = requestError.message;
  }
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>📋 Rekam Medis Diagnosa</h2>
      <p class="page-subtitle">Migrasi screen legacy SC0083 — rekam medis diagnosa & sebab kematian</p>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="tabs">
      <button class="tab-button" :class="{ active: activeTab === 'diagnose' }" type="button" @click="activeTab = 'diagnose'">REKAM MEDIS DIAGNOSA</button>
      <button class="tab-button" :class="{ active: activeTab === 'cod' }" type="button" @click="activeTab = 'cod'">SEBAB KEMATIAN</button>
    </div>

    <!-- TAB: REKAM MEDIS DIAGNOSA -->
    <div v-show="activeTab === 'diagnose'">
      <div class="card">
        <h3 class="card-title">DATA PASIEN</h3>
        <div class="form-grid">
          <div class="field">
            <label for="mr">NO. MR</label>
            <div class="bandbox">
              <input
                id="mr"
                v-model="mrCodeInput"
                type="text"
                placeholder="Pilih No. MR"
                readonly
                @focus="mrSearchOpen = true"
              />
              <button class="bandbox-btn" type="button" @click="mrSearchOpen = !mrSearchOpen">▾</button>
            </div>
            <div v-if="mrSearchOpen" class="bandbox-popup">
              <div class="bandbox-search-grid">
                <div class="bandbox-search-field">
                  <label>NO. MR</label>
                  <input v-model="mrSearchCode" type="text" placeholder="No. MR" />
                </div>
                <div class="bandbox-search-field">
                  <label>NAMA</label>
                  <input v-model="mrSearchName" type="text" placeholder="Nama pasien" />
                </div>
                <div class="bandbox-search-field">
                  <label>TGL. LAHIR</label>
                  <input v-model="mrSearchBirthDate" type="date" />
                </div>
                <div class="bandbox-search-field">
                  <label>ALAMAT</label>
                  <input v-model="mrSearchAddress" type="text" placeholder="Alamat" />
                </div>
              </div>
              <div class="bandbox-actions">
                <button class="small-button primary" type="button" :disabled="mrSearching" @click="searchMrPatients">CARI</button>
                <button class="small-button" type="button" @click="mrSearchOpen = false">TUTUP</button>
              </div>
              <table class="table bandbox-table">
                <thead>
                  <tr>
                    <th>NO. MR</th>
                    <th>NAMA</th>
                    <th>TGL. LAHIR</th>
                    <th>ALAMAT</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in mrSearchResults" :key="item.mrCode" @click="pickMrPatient(item)">
                    <td class="strong">{{ item.mrCode }}</td>
                    <td>{{ item.patientName }}</td>
                    <td>{{ item.birthDate }}</td>
                    <td>{{ item.address }}</td>
                  </tr>
                  <tr v-if="!mrSearchResults.length">
                    <td colspan="4" class="empty-state">Isi minimal satu field lalu tekan CARI.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="field">
            <label>PASIEN</label>
            <div class="radio-row">
              <label><input type="radio" value="in" v-model="diagnoseType" /> RAWAT INAP</label>
              <label><input type="radio" value="out" v-model="diagnoseType" /> RAWAT JALAN</label>
            </div>
          </div>
          <div class="field">
            <label>NAMA</label>
            <input class="field-input" :value="registration?.patientName" readonly />
          </div>
          <div class="field">
            <label>JENIS KELAMIN</label>
            <input class="field-input" :value="registration?.gender === 'M' ? 'PRIA' : 'WANITA'" readonly />
          </div>
          <div class="field">
            <label>TANGGAL LAHIR</label>
            <input class="field-input" :value="registration?.birthDate" readonly />
          </div>
          <div class="field">
            <label>DOKTER UTAMA</label>
            <input class="field-input" :value="registration?.doctorName" readonly />
          </div>
          <div class="field">
            <label>DIVISI</label>
            <input class="field-input" :value="registration?.unitLabel" readonly />
          </div>
          <div class="field">
            <label>TIPE PASIEN</label>
            <input class="field-input" :value="registration?.patientTypeDesc" readonly />
          </div>
          <div class="field">
            <label for="condition">KEADAAN KELUAR RS</label>
            <select id="condition" v-model="conditionList">
              <option v-for="opt in KEADAAN_KELUAR" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
          <div class="field">
            <label for="checkout">CARA KELUAR RS</label>
            <select id="checkout" v-model="caraKeluarList" @change="onCaraKeluarChange">
              <option v-for="opt in CARA_KELUAR" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">DATA DIAGNOSA PASIEN</h3>
        <table class="file-table">
          <thead>
            <tr>
              <th>KODE</th>
              <th>TIPE DIAGNOSA</th>
              <th>KETERANGAN</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in selectedDiagnoses" :key="d.icdId">
              <td>{{ d.icdCode }}</td>
              <td>ICD</td>
              <td>{{ d.icdName }}</td>
              <td><button class="small-button" type="button" @click="removeDiagnose(d.icdId)">🗑️</button></td>
            </tr>
            <tr v-if="!selectedDiagnoses.length">
              <td colspan="4" class="empty-state">Belum ada diagnosa.</td>
            </tr>
          </tbody>
        </table>
        <div class="file-actions">
          <button class="small-button" type="button" @click="openIcdDialog">TAMBAH ICD</button>
        </div>
      </div>

      <div class="card">
        <div class="file-actions">
          <button class="small-button primary" type="button" @click="save">💾 SIMPAN</button>
          <button class="small-button" type="button" @click="clearForm">🆕 BARU</button>
        </div>
      </div>
    </div>

    <!-- TAB: SEBAB KEMATIAN -->
    <div v-show="activeTab === 'cod'">
      <CauseOfDeathSection ref="causeOfDeathRef" :api-base-url="apiBaseUrl" @session-expired="emit('session-expired', $event)" />
      <div class="card">
        <div class="file-actions">
          <button class="small-button primary" type="button" @click="save">💾 SIMPAN</button>
          <button class="small-button" type="button" @click="clearForm">🆕 BARU</button>
        </div>
      </div>
    </div>

    <!-- ICD SEARCH DIALOG -->
    <div v-if="showIcdDialog" class="modal-overlay" @click.self="showIcdDialog = false">
      <div class="modal-card">
        <h3 class="card-title">PENCARIAN DATA DIAGNOSA</h3>
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
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.tabs { display: flex; gap: 6px; margin-bottom: 16px; border-bottom: 2px solid #e5e7eb; }
.tab-button { padding: 10px 16px; border: none; background: none; cursor: pointer; font-weight: 600; color: #6b7280; border-bottom: 3px solid transparent; }
.tab-button.active { color: #304b73; border-bottom-color: #304b73; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); margin-bottom: 16px; }
.card-title { margin: 0 0 12px; color: #304b73; font-size: 15px; text-align: center; }

.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.search-form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 12px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 12px; font-weight: 700; color: #304b73; }
.field-input { padding: 8px 10px; border-radius: 8px; border: 1px solid #d1d5db; font-size: 13px; }
.field select, .field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.field input[readonly] { background: #f6f8fb; color: #6b7280; }
.radio-row { display: flex; gap: 14px; align-items: center; font-size: 13px; padding: 10px 0; }

.bandbox { display: flex; align-items: stretch; }
.bandbox input { flex: 1; border-top-right-radius: 0; border-bottom-right-radius: 0; }
.bandbox-btn { padding: 0 12px; border: 1px solid #d1d9e6; border-left: none; border-radius: 0 6px 6px 0; background: #f6f8fb; cursor: pointer; }
.bandbox-popup { border: 1px solid #d1d9e6; border-radius: 8px; padding: 10px; background: #fff; box-shadow: 0 8px 20px rgba(0,0,0,0.12); margin-top: 4px; }
.bandbox-search-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 8px; }
.bandbox-search-field { display: flex; flex-direction: column; gap: 4px; }
.bandbox-search-field label { font-size: 12px; font-weight: 700; color: #304b73; }
.bandbox-search-field input { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 14px; text-transform: uppercase; }
.bandbox-actions { display: flex; gap: 10px; margin-bottom: 10px; }
.bandbox-table { font-size: 13px; width: 100%; border-collapse: collapse; }
.bandbox-table th, .bandbox-table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.bandbox-table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.bandbox-table tbody tr { cursor: pointer; }
.bandbox-table tbody tr:hover { background: #f6f8fb; }
.strong { font-weight: 700; }

.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th { text-align: left; padding: 8px; border-bottom: 2px solid #e5e7eb; color: #304b73; }
.file-table td { padding: 8px; border-bottom: 1px solid #f3f4f6; }
.empty-state { color: #9ca3af; text-align: center; padding: 12px; }

.file-actions { display: flex; gap: 10px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal-card { background: #fff; border-radius: 12px; padding: 20px; width: 820px; max-width: 95vw; max-height: 85vh; overflow-y: auto; }
.modal-actions { display: flex; gap: 10px; margin-bottom: 12px; }
</style>