<script setup>
import { computed, onMounted, reactive, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true },
  availableUnits: { type: Array, default: () => [] }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const saving = ref(false);
const searching = ref(false);
const statusMessage = ref('');
const errorMessage = ref('');

const masters = reactive({ units: [], patientTypes: [], escorts: [] });
const selectedUnitId = ref('');

const searchMode = ref('result');
const selectedResult = ref(null);
const selectedNoteId = ref(null);
const resultDetail = ref(null);
const resultItems = ref([]);
const showSearch = ref(true);

const resultSearch = reactive({ resultCode: '', patientName: '' });
const noteSearch = reactive({ noteNumber: '', patientName: '' });
const patientSearch = reactive({ mrCode: '', patientName: '' });

const searchResults = ref([]);
const noteResults = ref([]);
const patientResults = ref([]);

const headerForm = reactive({
  mrCode: '', patientName: '', registrationCode: '',
  doctorName: '', hall: '', bed: '',
  takeTime: '', escortDoctor: '', laboratNo: '',
  noteNumber: '', resultCode: ''
});

const editedItems = ref([]);
const isEditMode = ref(false);

async function request(path, options = {}) {
  const response = await fetch(`${props.apiBaseUrl}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    emit('session-expired', payload?.message || 'Sesi habis.');
    throw new Error(payload?.message || 'Unauthorized');
  }
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  return payload;
}

async function loadMasters() {
  try {
    const res = await request('/laborat/masters');
    masters.units = res.data.units || [];
    masters.patientTypes = res.data.patientTypes || [];
    masters.escorts = res.data.escorts || [];
    if (!selectedUnitId.value && masters.units.length) {
      selectedUnitId.value = String(masters.units[0].unitId);
    }
  } catch (e) { errorMessage.value = e.message; }
}

function switchSearchMode(mode) {
  searchMode.value = mode;
  searchResults.value = [];
  noteResults.value = [];
  patientResults.value = [];
  resultDetail.value = null;
  resultItems.value = [];
  selectedResult.value = null;
  selectedNoteId.value = null;
  isEditMode.value = false;
  showSearch.value = true;
  errorMessage.value = '';
  statusMessage.value = '';
}

async function searchByResult() {
  const params = new URLSearchParams();
  if (resultSearch.resultCode) params.set('resultCode', resultSearch.resultCode);
  if (resultSearch.patientName) params.set('patientName', resultSearch.patientName);
  searching.value = true;
  errorMessage.value = '';
  try {
    const res = await request(`/laborat/results?${params.toString()}`);
    searchResults.value = res.data || [];
    if (!searchResults.value.length) errorMessage.value = 'Tidak ada hasil ditemukan.';
  } catch (e) { errorMessage.value = e.message; }
  finally { searching.value = false; }
}

async function searchNotes() {
  if (!selectedUnitId.value) { errorMessage.value = 'Pilih unit/lokasi laboratorium terlebih dahulu.'; return; }
  const params = new URLSearchParams();
  if (noteSearch.noteNumber) params.set('noteNumber', noteSearch.noteNumber);
  if (noteSearch.patientName) params.set('patientName', noteSearch.patientName);
  searching.value = true;
  errorMessage.value = '';
  try {
    const res = await request(`/laborat/units/${selectedUnitId.value}/notes?${params.toString()}`);
    noteResults.value = res.data || [];
    if (!noteResults.value.length) errorMessage.value = 'Tidak ada nota ditemukan.';
  } catch (e) { errorMessage.value = e.message; }
  finally { searching.value = false; }
}

async function searchPatients() {
  const params = new URLSearchParams();
  if (patientSearch.mrCode) params.set('mrCode', patientSearch.mrCode);
  if (patientSearch.patientName) params.set('patientName', patientSearch.patientName);
  searching.value = true;
  errorMessage.value = '';
  try {
    const res = await request('/laborat/patients/registered?' + params.toString());
    patientResults.value = res.data || [];
    if (!patientResults.value.length) errorMessage.value = 'Tidak ada pasien ditemukan.';
  } catch (e) { errorMessage.value = e.message; }
  finally { searching.value = false; }
}

async function selectResult(resultId) {
  errorMessage.value = '';
  statusMessage.value = '';
  try {
    const res = await request('/laborat/results/' + resultId);
    resultDetail.value = res.data;
    selectedResult.value = resultId;
    isEditMode.value = true;
    showSearch.value = false;
    const d = res.data;
    headerForm.mrCode = d.mrCode || '';
    headerForm.patientName = d.patientName || '';
    headerForm.registrationCode = d.registrationCode || '';
    headerForm.doctorName = d.doctorName || '';
    headerForm.hall = d.hall || '';
    headerForm.bed = d.bed || '';
    headerForm.takeTime = d.takeTime || '';
    headerForm.escortDoctor = d.escortDoctor || '';
    headerForm.laboratNo = d.laboratNo || '';
    headerForm.noteNumber = d.noteNumber || '';
    headerForm.resultCode = d.resultCode || '';
    editedItems.value = (d.items || []).map(item => ({
      ...item,
      _resultDesc: item.resultDescription || '',
      _rangeMan: item.normalRangeMan || '',
      _rangeWoman: item.normalRangeWoman || '',
      _qty: item.quantityUnit || ''
    }));
  } catch (e) { errorMessage.value = e.message; }
}

async function loadResultItemsForNote(noteId) {
  try {
    const res = await request('/laborat/notes/' + noteId + '/result-items');
    resultItems.value = res.data || [];
    selectedNoteId.value = noteId;
    editedItems.value = (res.data || []).map(item => ({
      ...item,
      _resultDesc: item.resultDescription || '',
      _rangeMan: item.normalRangeMan || '',
      _rangeWoman: item.normalRangeWoman || '',
      _qty: item.quantityUnit || ''
    }));
    isEditMode.value = false;
    showSearch.value = false;
    errorMessage.value = '';
  } catch (e) { errorMessage.value = e.message; }
}

async function selectNote(noteId) {
  try {
    const res = await request('/laborat/notes/' + noteId);
    const d = res.data;
    headerForm.mrCode = d.mrCode || '';
    headerForm.patientName = d.patientName || '';
    headerForm.registrationCode = d.registrationCode || '';
    headerForm.doctorName = d.doctorName || '';
    headerForm.noteNumber = d.noteNumber || '';
    headerForm.resultCode = '';
    const now = new Date();
    headerForm.takeTime = now.getHours().toString().padStart(2,'0') + ':' + now.getMinutes().toString().padStart(2,'0');
    headerForm.escortDoctor = '';
    headerForm.laboratNo = '';
    await loadResultItemsForNote(noteId);
    if (d.mrCode) {
      try {
        const pd = await request('/laborat/patients/' + d.mrCode);
        if (pd.data) {
          headerForm.hall = pd.data.hall || '';
          headerForm.bed = pd.data.bed || '';
        }
      } catch (_) {}
    }
  } catch (e) { errorMessage.value = e.message; }
}

async function selectPatient(mrCode, patientName) {
  try {
    const pd = await request('/laborat/patients/' + mrCode);
    const d = pd.data;
    headerForm.mrCode = d.mrCode || mrCode;
    headerForm.patientName = d.patientName || patientName;
    headerForm.registrationCode = d.registrationCode || '';
    headerForm.doctorName = d.doctorName || '';
    headerForm.hall = d.hall || '';
    headerForm.bed = d.bed || '';
    const now = new Date();
    headerForm.takeTime = now.getHours().toString().padStart(2,'0') + ':' + now.getMinutes().toString().padStart(2,'0');
    headerForm.escortDoctor = '';
    headerForm.laboratNo = '';
    headerForm.noteNumber = d.noteNumber || '';
    if (d.noteId) {
      await loadResultItemsForNote(d.noteId);
    } else {
      errorMessage.value = 'Pasien tidak memiliki nota lab aktif.';
    }
  } catch (e) { errorMessage.value = e.message; }
}

function resetForm() {
  selectedResult.value = null;
  selectedNoteId.value = null;
  resultDetail.value = null;
  resultItems.value = [];
  editedItems.value = [];
  isEditMode.value = false;
  showSearch.value = true;
  headerForm.mrCode = '';
  headerForm.patientName = '';
  headerForm.registrationCode = '';
  headerForm.doctorName = '';
  headerForm.hall = '';
  headerForm.bed = '';
  headerForm.takeTime = '';
  headerForm.escortDoctor = '';
  headerForm.laboratNo = '';
  headerForm.noteNumber = '';
  headerForm.resultCode = '';
  errorMessage.value = '';
  statusMessage.value = '';
}

function getGroupedItems() {
  const groups = {};
  for (const item of editedItems.value) {
    const key = item.groupName || 'LAIN-LAIN';
    if (!groups[key]) groups[key] = [];
    groups[key].push(item);
  }
  return groups;
}

async function saveResult() {
  if (!selectedNoteId.value && !resultDetail.value) {
    errorMessage.value = 'Pilih nota terlebih dahulu.';
    return;
  }
  saving.value = true;
  errorMessage.value = '';
  statusMessage.value = '';
  try {
    const lines = editedItems.value
      .filter(item => item._resultDesc && item._resultDesc.trim())
      .map(item => ({
        detailId: item.detailId,
        treatmentId: item.treatmentId,
        labDetilId: item.labDetilId,
        resultDescription: item._resultDesc,
        normalRangeMan: item._rangeMan,
        normalRangeWoman: item._rangeWoman,
        quantityUnit: item._qty
      }));
    const body = {
      examId: resultDetail.value ? resultDetail.value.examId : selectedNoteId.value,
      mrCode: headerForm.mrCode,
      registrationCode: headerForm.registrationCode,
      takeTime: headerForm.takeTime,
      escortDoctor: headerForm.escortDoctor,
      laboratNo: headerForm.laboratNo,
      lines
    };
    if (isEditMode.value && selectedResult.value) {
      const res = await request('/laborat/results/' + selectedResult.value, {
        method: 'PUT',
        body: JSON.stringify(body)
      });
      statusMessage.value = res.data?.message || 'Hasil lab berhasil diubah.';
      if (res.data?.resultId) await selectResult(res.data.resultId);
    } else {
      const res = await request('/laborat/results', {
        method: 'POST',
        body: JSON.stringify(body)
      });
      statusMessage.value = res.data?.message || 'Hasil lab berhasil disimpan.';
      if (res.data?.resultId) await selectResult(res.data.resultId);
    }
  } catch (e) { errorMessage.value = e.message; }
  finally { saving.value = false; }
}

onMounted(() => {
  loadMasters();
  loading.value = false;
});
</script>

<template>
  <div class="lab-result-section">
    <div v-if="loading" class="loading">Memuat...</div>

    <!-- Location / Unit Selector — Always visible like legacy -->
    <div class="location-bar">
      <label class="location-label">🏥 Lokasi Transaksi</label>
      <select v-model="selectedUnitId" class="location-select">
        <option value="" disabled>— Pilih Unit —</option>
        <option v-for="u in masters.units" :key="u.unitId" :value="String(u.unitId)">
          {{ u.unitCode }}. {{ u.unitName }}
        </option>
      </select>
      <span v-if="!selectedUnitId" class="location-hint">Pilih lokasi/unit laboratorium</span>
    </div>

    <!-- Mode Tabs -->
    <div class="mode-tabs">
      <button class="mode-tab" :class="{ 'mode-tab--active': searchMode === 'result' }" @click="switchSearchMode('result')">
        🔎 Cari Hasil Lab
      </button>
      <button class="mode-tab" :class="{ 'mode-tab--active': searchMode === 'note' }" @click="switchSearchMode('note')">
        📋 Cari Nota
      </button>
      <button class="mode-tab" :class="{ 'mode-tab--active': searchMode === 'patient' }" @click="switchSearchMode('patient')">
        👤 Cari Pasien
      </button>
    </div>

    <div v-if="errorMessage" class="message error">{{ errorMessage }}</div>
    <div v-if="statusMessage" class="message success">{{ statusMessage }}</div>

    <!-- SEARCH BY RESULT -->
    <div v-if="searchMode === 'result'" class="card">
      <h3>
        <span>🔎 Cari Berdasarkan No. Hasil Lab</span>
        <span class="search-actions">
          <button v-if="resultDetail" class="small-button" type="button" @click="resetForm">Baru</button>
        </span>
      </h3>
      <div v-if="!resultDetail" class="search-form">
        <div class="form-row">
          <label>No. Hasil Lab <input v-model="resultSearch.resultCode" placeholder="Ketik No. Hasil" @keyup.enter="searchByResult" /></label>
          <label>Nama Pasien <input v-model="resultSearch.patientName" placeholder="Nama pasien" @keyup.enter="searchByResult" /></label>
        </div>
        <button class="primary-button" :disabled="searching" @click="searchByResult">
          {{ searching ? 'Mencari...' : 'Cari' }}
        </button>
      </div>
      <div v-if="searchResults.length && !resultDetail" class="table-wrap">
        <table class="table">
          <thead><tr><th>No. Hasil</th><th>No. Nota</th><th>Pasien</th><th>No. MR</th><th>Tgl</th><th>Aksi</th></tr></thead>
          <tbody>
            <tr v-for="r in searchResults" :key="r.resultId">
              <td><strong>{{ r.resultCode }}</strong></td>
              <td>{{ r.noteNumber }}</td>
              <td>{{ r.patientName }}</td>
              <td>{{ r.mrCode }}</td>
              <td>{{ r.createdAt }}</td>
              <td><button class="link-button" @click="selectResult(r.resultId)">Pilih</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- SEARCH BY NOTE -->
    <div v-if="searchMode === 'note'" class="card">
      <h3>
        <span>📋 Cari Berdasarkan No. Nota</span>
        <span class="search-actions">
          <button v-if="editedItems.length" class="small-button" type="button" @click="resetForm">Baru</button>
        </span>
      </h3>
      <div v-if="!editedItems.length" class="search-form">
        <div class="form-row">
          <label>No. Nota <input v-model="noteSearch.noteNumber" placeholder="Ketik No. Nota" @keyup.enter="searchNotes" /></label>
          <label>Nama Pasien <input v-model="noteSearch.patientName" placeholder="Nama pasien" @keyup.enter="searchNotes" /></label>
        </div>
        <button class="primary-button" :disabled="searching" @click="searchNotes">
          {{ searching ? 'Mencari...' : 'Cari Nota' }}
        </button>
      </div>
      <div v-if="noteResults.length && !editedItems.length" class="table-wrap">
        <table class="table">
          <thead><tr><th>No. Nota</th><th>Pasien</th><th>Status</th><th>Tgl</th><th>Aksi</th></tr></thead>
          <tbody>
            <tr v-for="n in noteResults" :key="n.noteId">
              <td><strong>{{ n.noteNumber }}</strong></td>
              <td>{{ n.patientName }}</td>
              <td><span class="badge" :class="n.statusLabel === 'VALID' ? 'badge-valid' : 'badge-new'">{{ n.statusLabel }}</span></td>
              <td>{{ n.createdAt }}</td>
              <td><button class="link-button" @click="selectNote(n.noteId)">Pilih & Input Hasil</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- SEARCH BY PATIENT -->
    <div v-if="searchMode === 'patient'" class="card">
      <h3>
        <span>👤 Cari Berdasarkan Pasien</span>
        <span class="search-actions">
          <button v-if="editedItems.length" class="small-button" type="button" @click="resetForm">Baru</button>
        </span>
      </h3>
      <div v-if="!editedItems.length" class="search-form">
        <div class="form-row">
          <label>No. MR <input v-model="patientSearch.mrCode" placeholder="Ketik No. MR" @keyup.enter="searchPatients" /></label>
          <label>Nama Pasien <input v-model="patientSearch.patientName" placeholder="Nama pasien" @keyup.enter="searchPatients" /></label>
        </div>
        <button class="primary-button" :disabled="searching" @click="searchPatients">
          {{ searching ? 'Mencari...' : 'Cari Pasien' }}
        </button>
      </div>
      <div v-if="patientResults.length && !editedItems.length" class="table-wrap">
        <table class="table">
          <thead><tr><th>No. MR</th><th>Nama</th><th>Alamat</th><th>Aksi</th></tr></thead>
          <tbody>
            <tr v-for="p in patientResults" :key="p.mrCode">
              <td><strong>{{ p.mrCode }}</strong></td>
              <td>{{ p.patientName }}</td>
              <td>{{ p.address || '-' }}</td>
              <td><button class="link-button" @click="selectPatient(p.mrCode, p.patientName)">Pilih</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- RESULT INPUT FORM -->
    <div v-if="editedItems.length" class="card">
      <h3>
        <span>📝 Input Hasil Pemeriksaan Lab</span>
        <button class="small-button" @click="resetForm">BARU</button>
      </h3>
      <div class="patient-detail-grid three-col">
        <div class="detail-item"><span class="detail-label">No. MR</span><span class="detail-value">{{ headerForm.mrCode || '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">Nama Pasien</span><span class="detail-value">{{ headerForm.patientName || '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">No. Registrasi</span><span class="detail-value">{{ headerForm.registrationCode || '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">Dokter</span><span class="detail-value">{{ headerForm.doctorName || '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">Ruang / Hall</span><span class="detail-value">{{ headerForm.hall || '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">Bed</span><span class="detail-value">{{ headerForm.bed || '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">No. Nota</span><span class="detail-value">{{ headerForm.noteNumber || '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">No. Hasil Lab</span><span class="detail-value">{{ headerForm.resultCode || '(baru)' }}</span></div>
      </div>
      <div class="form-row meta-row">
        <label>Jam <input v-model="headerForm.takeTime" placeholder="HH:mm" /></label>
        <label>Dokter Pengirim <input v-model="headerForm.escortDoctor" placeholder="Nama dokter" /></label>
        <label>No. Laborat <input v-model="headerForm.laboratNo" placeholder="No. laborat" /></label>
      </div>

      <div v-for="(items, group) in getGroupedItems()" :key="group" class="result-group">
        <h4 class="group-header">{{ group }}</h4>
        <table class="table result-table">
          <thead>
            <tr>
              <th style="width:30%">Pemeriksaan</th>
              <th style="width:22%">Hasil</th>
              <th style="width:18%">Nilai Normal</th>
              <th style="width:12%">Satuan</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in items" :key="idx">
              <td class="item-name">{{ item.detailName || item.treatmentName }}</td>
              <td><input v-model="item._resultDesc" class="result-input" placeholder="Hasil" /></td>
              <td><input v-model="item._rangeMan" class="range-input" placeholder="Nilai normal" /></td>
              <td><input v-model="item._qty" class="qty-input" placeholder="Satuan" /></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="action-bar">
        <button class="primary-button" :disabled="saving" @click="saveResult">
          {{ saving ? 'Menyimpan...' : (isEditMode ? 'UBAH' : 'SIMPAN') }}
        </button>
        <button class="secondary-button" @click="resetForm">BARU</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lab-result-section { max-width: 1200px; margin: 0 auto; }
.loading { padding: 40px; text-align: center; color: #6b7280; }
.mode-tabs { display: flex; gap: 6px; margin-bottom: 16px; flex-wrap: wrap; }
.mode-tab {
  padding: 8px 16px; border: 1px solid #d1d9e6; border-radius: 10px;
  background: #fff; font-weight: 700; font-size: 13px; color: #3d4b63; cursor: pointer;
}
.mode-tab:hover { border-color: #5f83c2; color: #304b73; }
.mode-tab--active { background: #304b73; border-color: #304b73; color: #fff; }
.message { padding: 10px 16px; border-radius: 8px; margin-bottom: 12px; font-weight: 600; font-size: 13px; }
.message.error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
.message.success { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
.card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px;
  padding: 20px; margin-bottom: 16px;
}
.card h3 {
  margin: 0 0 12px; font-size: 15px; color: #304b73;
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px;
}
.card h4 { margin: 12px 0 8px; font-size: 14px; color: #304b73; }
.search-actions { display: flex; gap: 6px; }
.search-form { margin-bottom: 12px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px; }
.form-row label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; }
.meta-row { grid-template-columns: 1fr 1fr 1fr; }
input, select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; background: #fff; }
.result-input { width: 100%; box-sizing: border-box; }
.range-input { width: 100%; box-sizing: border-box; }
.qty-input { width: 100%; box-sizing: border-box; }
.table-wrap { overflow: auto; margin: 8px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.result-table td { vertical-align: middle; }
.item-name { font-weight: 600; color: #3d4b63; }
.patient-detail-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 10px;
  padding: 12px; background: #f6f8fb; border-radius: 8px; margin-bottom: 12px;
}
.patient-detail-grid.three-col { grid-template-columns: 1fr 1fr 1fr; }
.detail-item { display: grid; gap: 2px; }
.detail-label { font-size: 11px; color: #6b7280; text-transform: uppercase; }
.detail-value { font-weight: 700; color: #304b73; }
.result-group { margin-bottom: 16px; border: 1px solid #eef2f7; border-radius: 8px; overflow: hidden; }
.group-header {
  padding: 8px 12px; margin: 0; font-size: 12px; text-transform: uppercase;
  letter-spacing: 0.04em; background: #304b73; color: #fff;
}
.primary-button, .secondary-button, .small-button {
  border: 0; cursor: pointer; padding: 8px 20px; font-weight: 700; border-radius: 8px;
}
.primary-button { background: #304b73; color: #fff; }
.primary-button:disabled { opacity: 0.5; cursor: not-allowed; }
.secondary-button { background: #fff; border: 1px solid #d1d9e6; color: #3d4b63; }
.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
.link-button { background: none; border: 0; color: #5f83c2; font-weight: 700; cursor: pointer; padding: 4px 8px; font-size: 13px; }
.action-bar { display: flex; gap: 8px; margin-top: 16px; padding-top: 12px; border-top: 1px solid #e2e8f0; }
.badge { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; }
.badge-new { background: #dbeafe; color: #1e40af; }
.badge-valid { background: #d1fae5; color: #065f46; }

/* Location bar — always visible seperti legacy */
.location-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px; margin-bottom: 16px;
  background: #f0f4fa; border: 1px solid #d1d9e6;
  border-radius: 10px;
}
.location-label {
  font-weight: 700; font-size: 13px; color: #304b73;
  white-space: nowrap;
}
.location-select {
  padding: 8px 12px; border: 1px solid #d1d9e6;
  border-radius: 8px; font: inherit; font-size: 13px;
  background: #fff; min-width: 250px;
}
.location-hint { font-size: 12px; color: #b84747; font-weight: 600; }
</style>
