<script setup>
import { computed, onMounted, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true }
});

const emit = defineEmits(['session-expired', 'close']);

const loading = ref(false);
const error = ref('');
const activeTab = ref('transaksi');

const masters = ref(null);
const patient = ref(null);
const note = ref(null);
const lines = ref([]);
const total = ref(0);
const editMode = ref(false);
const readOnly = ref(false);
const selectedLine = ref(null);

const form = ref({
  unitId: null,
  noteNo: '',
  mrCode: '',
  regNo: '',
  patientName: '',
  gender: '',
  birthDate: '',
  age: '',
  address: '',
  patientTypeId: null,
  escortId: null,
  doctorName: '',
  hall: '',
  bed: ''
});

const showModal = ref('');
const patientSearch = ref({ mrCode: '', name: '', address: '', birthDate: '' });
const patientResults = ref([]);
const noteSearch = ref({ noteNo: '', name: '' });
const noteResults = ref([]);
const doctorSearch = ref({ code: '', name: '' });
const doctorResults = ref([]);
const radiographers = ref([]);
const treatmentDoctor = ref(null);
const treatmentRadiograferId = ref(null);
const treatmentSearch = ref({ code: '', name: '' });
const treatmentResults = ref([]);
const itemSearch = ref({ code: '', name: '' });
const itemResults = ref([]);
const miscForm = ref({ name: '', qty: 1, price: 0 });

const historyMode = ref('divisi');
const historyNotes = ref([]);
const historyGrandTotal = ref(0);
const selectedHistoryNote = ref(null);

const toast = ref({ visible: false, message: '', type: 'success' });
const dialog = ref({ visible: false, mode: 'alert', type: 'warning', title: '', message: '', resolve: null });
const promptDialog = ref({ visible: false, title: '', message: '', value: '', resolve: null });
let toastTimer = null;

function showToast(message, type = 'success') {
  toast.value = { visible: true, message, type };
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.value.visible = false; }, 3500);
}

function showAlert(message, options = {}) {
  return new Promise((resolve) => {
    dialog.value = { visible: true, mode: 'alert', type: options.type || 'warning',
      title: options.title || 'PERHATIAN', message, resolve };
  });
}

function showConfirm(message, options = {}) {
  return new Promise((resolve) => {
    dialog.value = { visible: true, mode: 'confirm', type: options.type || 'confirm',
      title: options.title || 'KONFIRMASI', message, resolve };
  });
}

function showPrompt(message, options = {}) {
  return new Promise((resolve) => {
    promptDialog.value = { visible: true, title: options.title || 'INPUT',
      message, value: options.initial || '', resolve };
  });
}

function closeDialog(result) {
  const resolve = dialog.value.resolve;
  dialog.value.visible = false;
  if (resolve) resolve(result);
}

function closePrompt(result) {
  const resolve = promptDialog.value.resolve;
  promptDialog.value.visible = false;
  if (resolve) resolve(result);
}

const dialogIcon = computed(() => ({
  warning: '⚠️', info: 'ℹ️', error: '❌', success: '✅', confirm: '❓'
}[dialog.value.type] || 'ℹ️'));

const fmtMoney = (v) => Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });

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

function qs(params) {
  const parts = [];
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      parts.push(`${key}=${encodeURIComponent(value)}`);
    }
  });
  return parts.length ? `?${parts.join('&')}` : '';
}

onMounted(async () => {
  try {
    masters.value = await request('/radiology/masters');
    if (masters.value.units.length) {
      form.value.unitId = masters.value.units[0].unitId;
    }
  } catch (requestError) {
    error.value = requestError.message;
  }
});

function currentUnit() {
  if (!masters.value || !form.value.unitId) return null;
  return masters.value.units.find((u) => u.unitId === form.value.unitId) || null;
}

// ================= LINE HELPERS =================

function lineSubtotal(line) {
  const amount = (Number(line.qty) || 0) * (Number(line.price) || 0);
  const disc = Number(line.discAmount) || 0;
  const discValue = line.discType === '%' ? (amount * disc / 100) : disc;
  return amount - discValue;
}

function updateLine(line) {
  line.subtotal = lineSubtotal(line);
}

function calculateTotal() {
  total.value = lines.value.reduce((sum, line) => sum + lineSubtotal(line), 0);
}

function deleteSelectedLine() {
  if (selectedLine.value === null || selectedLine.value < 0 || selectedLine.value >= lines.value.length) {
    showAlert('PILIH BARIS TERLEBIH DAHULU!');
    return;
  }
  lines.value.splice(selectedLine.value, 1);
  selectedLine.value = null;
  calculateTotal();
}

function addLine(line) {
  lines.value.push({ ...line, subtotal: lineSubtotal(line) });
}

// ================= PASIEN =================

async function searchPatient() {
  const s = patientSearch.value;
  if (!s.mrCode && !s.name && !s.address && !s.birthDate) {
    await showAlert('Salah satu field pencarian pasien harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    patientResults.value = await request(`/radiology/patients/registered${qs(s)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function selectPatient(result) {
  showModal.value = '';
  try {
    patient.value = await request(`/radiology/patients/${encodeURIComponent(result.mrCode)}`);
    applyPatient(patient.value);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

function applyPatient(p) {
  form.value.mrCode = p.mrCode;
  form.value.regNo = p.registrationNumber || '';
  form.value.patientName = p.patientName;
  form.value.gender = p.gender || '';
  form.value.birthDate = p.birthDate || '';
  form.value.age = p.age || '';
  form.value.address = p.address || '';
  form.value.patientTypeId = p.patientTypeId ?? null;
  form.value.doctorName = p.doctorName || '';
  form.value.hall = p.hall || '';
  form.value.bed = p.bed || '';
}

// ================= NOTA =================

async function searchNote() {
  const s = noteSearch.value;
  if (!s.noteNo && !s.name) {
    await showAlert('Salah satu field pencarian nota harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    noteResults.value = await request(`/radiology/notes${qs({ unitId: form.value.unitId, noteNo: s.noteNo, patientName: s.name })}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function selectNote(result) {
  showModal.value = '';
  try {
    await loadNote(result.noteId);
  } catch (requestError) {
    error.value = requestError.message;
  }
}

async function loadNote(noteId) {
  loading.value = true;
  try {
    note.value = await request(`/radiology/notes/${noteId}`);
    form.value.noteNo = note.value.noteNo;
    form.value.unitId = note.value.unitId ?? form.value.unitId;
    form.value.regNo = note.value.registrationNumber || '';
    form.value.mrCode = note.value.mrCode || '';
    form.value.patientName = note.value.patientName || '';
    form.value.gender = note.value.gender || '';
    form.value.birthDate = note.value.birthDate || '';
    form.value.age = note.value.age || '';
    form.value.address = note.value.address || '';
    form.value.patientTypeId = note.value.patientTypeId ?? null;
    form.value.escortId = note.value.escortId ?? null;
    form.value.doctorName = note.value.doctorName || '';
    form.value.hall = note.value.hall || '';
    form.value.bed = note.value.bed || '';
    lines.value = (note.value.lines || []).map((line) => ({ ...line, subtotal: line.subtotal || 0 }));
    total.value = note.value.total || 0;
    editMode.value = false;
    readOnly.value = !(note.value.canModify || note.value.canValidate);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

// ================= DOKTER / TINDAKAN / ITEM / MISC =================

async function searchDoctor() {
  const s = doctorSearch.value;
  loading.value = true;
  error.value = '';
  try {
    doctorResults.value = await request(`/radiology/doctors${qs(s)}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function selectDoctor(result) {
  form.value.doctorName = `${result.code}-${result.name}`;
  showModal.value = '';
}

// ================= TAMBAH TINDAKAN (DOKTER PEMERIKSA & RADIOGRAFER) =================

async function openTreatmentModal() {
  treatmentDoctor.value = null;
  treatmentRadiograferId.value = null;
  treatmentSearch.value = { code: '', name: '' };
  treatmentResults.value = [];
  if (!radiographers.value.length) {
    try {
      radiographers.value = await request('/radiology/radiographers');
    } catch (requestError) {
      error.value = requestError.message;
    }
  }
  showModal.value = 'treatment';
}

function openTreatmentDoctorSearch() {
  doctorSearch.value = { code: '', name: '' };
  doctorResults.value = [];
  showModal.value = 'doctor';
}

function selectTreatmentDoctor(result) {
  treatmentDoctor.value = result;
  showModal.value = 'treatment';
}

async function searchTreatment() {
  const s = treatmentSearch.value;
  if (!s.code && !s.name) {
    await showAlert('Salah satu field harus diisi!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    treatmentResults.value = await request(`/radiology/treatments${qs({ code: s.code, name: s.name })}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function addTreatment(result) {
  const doctor = treatmentDoctor.value;
  const radiografer = radiographers.value.find(
    (r) => r.staffId === Number(treatmentRadiograferId.value)
  );
  let name = result.name;
  // migrasi legacy: keterangan = nama tindakan - dokter pemeriksa - radiografer
  if (doctor) name = `${name}-${doctor.name}`;
  if (radiografer) name = `${name}-${radiografer.name}`;
  addLine({
    lineType: 'TREATMENT',
    referenceId: result.treatmentFeeId,
    code: result.code,
    name,
    qty: 1,
    unit: '-',
    price: result.price,
    discType: 'RP',
    discAmount: 0,
    doctorId: doctor ? doctor.staffId : null,
    radiograferId: radiografer ? radiografer.staffId : null
  });
  calculateTotal();
}

async function searchItem() {
  const unit = currentUnit();
  if (!unit || !unit.warehouseId) {
    await showAlert('Unit ini belum memiliki gudang!');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    itemResults.value = await request(`/radiology/items${qs({ warehouseId: unit.warehouseId, code: itemSearch.value.code, name: itemSearch.value.name })}`);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function addItem(result) {
  addLine({
    lineType: 'ITEM',
    referenceId: result.itemId,
    code: result.code,
    name: result.name,
    qty: 1,
    unit: result.unit || '-',
    price: result.price || 0,
    discType: 'RP',
    discAmount: 0,
    doctorId: null
  });
  calculateTotal();
}

function addMisc() {
  if (!miscForm.value.name) {
    showAlert('NAMA BIAYA LAIN-LAIN HARUS DIISI!');
    return;
  }
  addLine({
    lineType: 'MISC',
    referenceId: null,
    code: 'MISC-001',
    name: miscForm.value.name,
    qty: Number(miscForm.value.qty) || 1,
    unit: '-',
    price: Number(miscForm.value.price) || 0,
    discType: 'RP',
    discAmount: 0,
    doctorId: null
  });
  miscForm.value = { name: '', qty: 1, price: 0 };
  showModal.value = '';
  calculateTotal();
}

// ================= ACTIONS =================

function newTransaction() {
  note.value = null;
  patient.value = null;
  editMode.value = false;
  readOnly.value = false;
  lines.value = [];
  total.value = 0;
  selectedLine.value = null;
  Object.assign(form.value, {
    noteNo: '', mrCode: '', regNo: '', patientName: '', gender: '', birthDate: '',
    age: '', address: '', patientTypeId: null, escortId: null, doctorName: '', hall: '', bed: ''
  });
}

function modify() {
  if (!note.value) {
    showAlert('PILIH NOTA TERLEBIH DAHULU!');
    return;
  }
  editMode.value = true;
  readOnly.value = false;
  lines.value = (note.value.lines || []).map((line) => ({ ...line, subtotal: line.subtotal || 0 }));
}

async function save() {
  if (!form.value.mrCode) {
    await showAlert('NO. MR HARUS DI ISI!');
    return;
  }
  if (!lines.value.length) {
    await showAlert('TIDAK ADA TRANSAKSI YANG AKAN DISIMPAN!');
    return;
  }
  if (total.value === 0 && !lines.value.every((l) => lineSubtotal(l) === 0)) {
    await showAlert('HITUNG TOTAL TERLEBIH DAHULU!');
    return;
  }
  loading.value = true;
  error.value = '';
  const body = JSON.stringify({
    mrCode: form.value.mrCode,
    unitId: form.value.unitId,
    doctorId: null,
    escortId: form.value.escortId,
    lines: lines.value.map((line) => ({
      lineType: line.lineType,
      referenceId: line.referenceId,
      qty: line.qty,
      price: line.price,
      discType: line.discType,
      discAmount: line.discAmount,
      doctorId: line.doctorId,
      radiograferId: line.radiograferId,
      miscName: line.lineType === 'MISC' ? line.name : undefined
    }))
  });
  try {
    const result = editMode.value && note.value
      ? await request(`/radiology/notes/${note.value.noteId}`, { method: 'PUT', body })
      : await request('/radiology/notes', { method: 'POST', body });
    showToast(result.message || 'Nota berhasil disimpan.', 'success');
    editMode.value = false;
    await loadNote(result.noteId);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function validateNote() {
  if (!note.value) {
    await showAlert('PILIH NOTA TERLEBIH DAHULU!');
    return;
  }
  const ok = await showConfirm(`Validasi nota ${note.value.noteNo}?`, { title: 'VALIDASI NOTA' });
  if (!ok) return;
  loading.value = true;
  error.value = '';
  try {
    const result = await request(`/radiology/notes/${note.value.noteId}/validate`, { method: 'POST' });
    showToast(result.message || 'Nota berhasil divalidasi.', 'success');
    await loadNote(note.value.noteId);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

async function cancelNote() {
  if (!note.value) {
    await showAlert('PILIH NOTA TERLEBIH DAHULU!');
    return;
  }
  const reason = await showPrompt('ALASAN PEMBATALAN NOTA :', { title: 'PEMBATALAN NOTA', initial: '' });
  if (reason === null) return;
  loading.value = true;
  error.value = '';
  try {
    const result = await request(`/radiology/notes/${note.value.noteId}/cancel${qs({ reason })}`, { method: 'POST' });
    showToast(result.message || 'Nota berhasil dibatalkan.', 'success');
    await loadNote(note.value.noteId);
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

function printNote() {
  if (!note.value) {
    showAlert('PILIH NOTA TERLEBIH DAHULU!');
    return;
  }
  window.open(`${props.apiBaseUrl}/radiology/notes/${note.value.noteId}/print`, '_blank');
}

// ================= HISTORY =================

async function loadHistory() {
  if (!form.value.mrCode && !patient.value) {
    await showAlert('PILIH PASIEN TERLEBIH DAHULU DI TAB TRANSAKSI!');
    activeTab.value = 'transaksi';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const data = await request(`/radiology/history${qs({ mrCode: form.value.mrCode || patient.value.mrCode, mode: historyMode.value })}`);
    historyNotes.value = data.notes;
    historyGrandTotal.value = data.grandTotal;
    selectedHistoryNote.value = historyNotes.value.length ? historyNotes.value[0] : null;
  } catch (requestError) {
    error.value = requestError.message;
  } finally {
    loading.value = false;
  }
}

const historyLineCount = computed(() => (selectedHistoryNote.value ? selectedHistoryNote.value.lines.length : 0));

function switchHistoryNote(noteItem) {
  selectedHistoryNote.value = noteItem;
}

function historySubtotal() {
  if (!selectedHistoryNote.value) return 0;
  return selectedHistoryNote.value.lines.reduce((sum, line) => sum + (Number(line.jumlah) || 0), 0);
}
</script>

<template>
  <div class="screen-page">
    <div class="page-header">
      <h2>🔬 TRANSAKSI RADIOLOGI</h2>
    </div>

    <p v-if="error" class="status-banner status-banner--error">{{ error }}</p>

    <div class="card">
      <div class="tabs">
        <button class="tab" :class="{ active: activeTab === 'transaksi' }" type="button" @click="activeTab = 'transaksi'">📝 TRANSAKSI</button>
        <button class="tab" :class="{ active: activeTab === 'history' }" type="button" @click="activeTab = 'history'; loadHistory()">🕘 HISTORY TRANSAKSI</button>
      </div>

      <!-- ==================== TAB TRANSAKSI ==================== -->
      <div v-if="activeTab === 'transaksi'">
        <div class="section-title">DATA PASIEN</div>
        <div class="patient-grid">
          <div class="field">
            <label>LOKASI TRANSAKSI</label>
            <select v-model="form.unitId">
              <option v-for="u in (masters?.units || [])" :key="u.unitId" :value="u.unitId">{{ u.code }}-{{ u.name }}</option>
            </select>
          </div>
          <div class="field">
            <label>NO. NOTA</label>
            <div class="input-row">
              <input v-model="form.noteNo" readonly placeholder="-" />
              <button class="mini primary" type="button" @click="showModal = 'note'">CARI NOTA</button>
            </div>
          </div>
          <div class="field">
            <label>NO. MR</label>
            <div class="input-row">
              <input v-model="form.mrCode" readonly placeholder="-" />
              <button class="mini primary" type="button" @click="showModal = 'patient'">CARI PASIEN</button>
            </div>
          </div>
          <div class="field">
            <label>NO. REGISTRASI</label>
            <input v-model="form.regNo" readonly />
          </div>
          <div class="field">
            <label>NAMA</label>
            <input v-model="form.patientName" readonly />
          </div>
          <div class="field">
            <label>JENIS KELAMIN</label>
            <input :value="form.gender === 'M' ? 'PRIA' : form.gender === 'F' ? 'WANITA' : ''" readonly />
          </div>
          <div class="field">
            <label>TGL LAHIR</label>
            <input v-model="form.birthDate" readonly />
          </div>
          <div class="field">
            <label>UMUR</label>
            <input v-model="form.age" readonly />
          </div>
          <div class="field">
            <label>ALAMAT</label>
            <input v-model="form.address" readonly />
          </div>
          <div class="field">
            <label>TIPE PASIEN</label>
            <select v-model="form.patientTypeId" :disabled="readOnly">
              <option :value="null" />
              <option v-for="t in (masters?.patientTypes || [])" :key="t.patientTypeId" :value="t.patientTypeId">
                {{ t.patientTypeId }}. {{ t.description || t.code }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>DOKTER UTAMA</label>
            <input v-model="form.doctorName" readonly />
          </div>
          <div class="field">
            <label>TIPE PEMBAWA</label>
            <select v-model="form.escortId" :disabled="readOnly">
              <option :value="null" />
              <option v-for="e in (masters?.escorts || [])" :key="e.escortId" :value="e.escortId">
                {{ e.escortId }}. {{ e.type }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>RUANGAN</label>
            <input v-model="form.hall" readonly />
          </div>
          <div class="field">
            <label>BED</label>
            <input v-model="form.bed" readonly />
          </div>
        </div>

        <div class="section-title">DATA TINDAKAN RADIOLOGI</div>
        <div class="status-line">
          <span class="status-label">STATUS NOTA :</span>
          <span class="status-value">{{ note ? note.statusLabel : '-' }}</span>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th style="width:30px"></th>
                <th>KODE</th>
                <th>KETERANGAN</th>
                <th class="num">JUMLAH</th>
                <th>SATUAN</th>
                <th class="num">HARGA</th>
                <th class="num">DISKON</th>
                <th class="num">SUBTOTAL</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(line, index) in lines" :key="index">
                <td><input type="radio" name="lineSelect" :value="index" v-model="selectedLine" /></td>
                <td>{{ line.code }}</td>
                <td>{{ line.name }}</td>
                <td class="num">
                  <input v-if="!readOnly || editMode" v-model.number="line.qty" class="num-input" type="number" min="1" @change="updateLine(line); calculateTotal()" />
                  <span v-else>{{ line.qty }}</span>
                </td>
                <td>{{ line.unit }}</td>
                <td class="num">{{ fmtMoney(line.price) }}</td>
                <td class="num">
                  <template v-if="!readOnly || editMode">
                    <input v-model.number="line.discAmount" class="num-input" type="number" min="0" @change="updateLine(line); calculateTotal()" />
                    <select v-model="line.discType" class="disc-select" @change="updateLine(line); calculateTotal()">
                      <option value="RP">RP</option>
                      <option value="%">%</option>
                    </select>
                  </template>
                  <span v-else>{{ fmtMoney(line.discAmount) }} {{ line.discType }}</span>
                </td>
                <td class="num strong">{{ fmtMoney(line.subtotal) }}</td>
              </tr>
              <tr v-if="!lines.length">
                <td colspan="8" class="empty-state">Belum ada tindakan. Tambahkan tindakan, O-BM, atau biaya lain-lain.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="total-line">
          <span class="total-label">TOTAL</span>
          <span class="total-colon">:</span>
          <input class="total-input" :value="fmtMoney(total)" readonly />
        </div>

        <div class="action-bar">
          <button class="small-button" type="button" :disabled="readOnly && !editMode" @click="calculateTotal">🧮 HITUNG</button>
          <button class="small-button" type="button" :disabled="readOnly && !editMode" @click="deleteSelectedLine">🗑️ HAPUS</button>
          <button class="small-button primary" type="button" :disabled="readOnly && !editMode" @click="openTreatmentModal">➕ TAMBAH TINDAKAN</button>
          <button class="small-button primary" type="button" :disabled="readOnly && !editMode" @click="showModal = 'item'">💊 TAMBAH O-BM</button>
          <button class="small-button primary" type="button" :disabled="readOnly && !editMode" @click="showModal = 'misc'">💲 BIAYA LAIN-LAIN</button>
        </div>

        <div class="action-bar">
          <button class="small-button primary" type="button" :disabled="loading" @click="save">💾 SIMPAN</button>
          <button class="small-button" type="button" :disabled="!note || note.status === 2" @click="modify">✏️ UBAH</button>
          <button class="small-button" type="button" @click="newTransaction">🆕 BARU</button>
          <button class="small-button danger" type="button" :disabled="!note" @click="cancelNote">🚫 PEMBATALAN NOTA</button>
          <button class="small-button success" type="button" :disabled="!note || note.status !== 1" @click="validateNote">✅ VALIDASI</button>
          <button class="small-button" type="button" :disabled="!note" @click="printNote">🖨️ CETAK</button>
          <button class="small-button" type="button" @click="emit('close')">✅ SELESAI</button>
        </div>
      </div>

      <!-- ==================== TAB HISTORY ==================== -->
      <div v-else>
        <div class="section-title">
          <span>{{ form.patientName ? `${form.patientName} (${form.mrCode})` : 'HISTORY TRANSAKSI' }}</span>
        </div>
        <div class="period-bar">
          <span class="period-label">OPSI</span>
          <label><input v-model="historyMode" type="radio" value="divisi" /> PER DIVISI</label>
          <label><input v-model="historyMode" type="radio" value="global" /> GLOBAL</label>
          <button class="small-button primary" type="button" @click="loadHistory">🔍 CARI</button>
        </div>

        <div v-if="historyNotes.length" class="history-layout">
          <div class="history-notes">
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr><th>NO. NOTA</th><th>STATUS</th><th>TANGGAL</th><th class="num">TOTAL</th></tr>
                </thead>
                <tbody>
                  <tr v-for="n in historyNotes" :key="n.noteId" :class="{ selected: selectedHistoryNote && selectedHistoryNote.noteId === n.noteId }" @click="switchHistoryNote(n)">
                    <td class="strong">{{ n.noteNo }}</td>
                    <td>{{ n.statusLabel }}</td>
                    <td>{{ n.date }}</td>
                    <td class="num">{{ fmtMoney(n.total) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="history-lines">
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr><th>KETERANGAN</th><th>SUB DIVISI</th><th>TANGGAL</th><th class="num">JUMLAH</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(line, index) in (selectedHistoryNote ? selectedHistoryNote.lines : [])" :key="index">
                    <td>{{ line.keterangan }}</td>
                    <td>{{ line.subDivisi }}</td>
                    <td>{{ line.tanggal }}</td>
                    <td class="num">{{ fmtMoney(line.jumlah) }}</td>
                  </tr>
                  <tr v-if="!historyLineCount"><td colspan="4" class="empty-state">Pilih nota di sebelah kiri.</td></tr>
                </tbody>
              </table>
            </div>
            <div class="total-line">
              <span class="total-label">TOTAL NOTA</span>
              <span class="total-colon">:</span>
              <input class="total-input" :value="fmtMoney(historySubtotal())" readonly />
            </div>
          </div>
        </div>

        <div class="grand-total">GRAND TOTAL : <strong>{{ fmtMoney(historyGrandTotal) }}</strong></div>
      </div>
    </div>

    <!-- ==================== MODAL: CARI PASIEN ==================== -->
    <div v-if="showModal === 'patient'" class="modal-overlay" @click.self="showModal = ''">
      <div class="modal">
        <div class="modal-header">CARI DATA PASIEN</div>
        <div class="modal-body">
          <div class="field"><label>NO. MR</label><input v-model="patientSearch.mrCode" /></div>
          <div class="field"><label>NAMA</label><input v-model="patientSearch.name" /></div>
          <div class="field"><label>TGL. LAHIR</label><input v-model="patientSearch.birthDate" type="date" /></div>
          <div class="field"><label>ALAMAT</label><input v-model="patientSearch.address" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchPatient">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>NO. MR</th><th>NAMA</th><th>TIPE PASIEN</th><th>ALAMAT</th></tr></thead>
              <tbody>
                <tr v-for="r in patientResults" :key="r.mrId" @click="selectPatient(r)">
                  <td class="strong">{{ r.mrCode }}</td>
                  <td>{{ r.patientName }}</td>
                  <td>{{ r.patientType }}</td>
                  <td>{{ r.address }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showModal = ''">TUTUP</button>
        </div>
      </div>
    </div>

    <!-- ==================== MODAL: CARI NOTA ==================== -->
    <div v-if="showModal === 'note'" class="modal-overlay" @click.self="showModal = ''">
      <div class="modal">
        <div class="modal-header">CARI NOTA</div>
        <div class="modal-body">
          <div class="field"><label>NO. NOTA</label><input v-model="noteSearch.noteNo" /></div>
          <div class="field"><label>NAMA</label><input v-model="noteSearch.name" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchNote">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>NO. NOTA</th><th>NAMA</th><th>STATUS NOTA</th></tr></thead>
              <tbody>
                <tr v-for="r in noteResults" :key="r.noteId" @click="selectNote(r)">
                  <td class="strong">{{ r.noteNo }}</td>
                  <td>{{ r.patientName }}</td>
                  <td>{{ r.statusLabel }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showModal = ''">TUTUP</button>
        </div>
      </div>
    </div>

    <!-- ==================== MODAL: TAMBAH TINDAKAN ==================== -->
    <div v-if="showModal === 'treatment'" class="modal-overlay" @click.self="showModal = ''">
      <div class="modal">
        <div class="modal-header">FORM TAMBAH TINDAKAN</div>
        <div class="modal-body">
          <div class="field">
            <label>DOKTER PEMERIKSA</label>
            <div class="input-row">
              <input
                :value="treatmentDoctor ? `${treatmentDoctor.code}-${treatmentDoctor.name}` : ''"
                readonly
                placeholder="-"
              />
              <button class="mini primary" type="button" @click="openTreatmentDoctorSearch">CARI</button>
            </div>
          </div>
          <div class="field">
            <label>RADIOGRAFER</label>
            <select v-model="treatmentRadiograferId">
              <option :value="null">--PILIH--</option>
              <option v-for="r in radiographers" :key="r.staffId" :value="r.staffId">
                {{ r.code }} - {{ r.name }}
              </option>
            </select>
          </div>
          <div class="field"><label>KODE</label><input v-model="treatmentSearch.code" /></div>
          <div class="field"><label>NAMA</label><input v-model="treatmentSearch.name" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchTreatment">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>KODE</th><th>NAMA</th><th class="num">HARGA</th></tr></thead>
              <tbody>
                <tr v-for="r in treatmentResults" :key="r.treatmentFeeId" @click="addTreatment(r)">
                  <td class="strong">{{ r.code }}</td>
                  <td>{{ r.name }}</td>
                  <td class="num">{{ fmtMoney(r.price) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showModal = ''">SELESAI</button>
        </div>
      </div>
    </div>

    <!-- ==================== MODAL: CARI DOKTER (DOKTER PEMERIKSA) ==================== -->
    <div v-if="showModal === 'doctor'" class="modal-overlay" @click.self="showModal = 'treatment'">
      <div class="modal">
        <div class="modal-header">PENCARIAN DATA DOKTER</div>
        <div class="modal-body">
          <div class="field"><label>KODE</label><input v-model="doctorSearch.code" @keyup.enter="searchDoctor" /></div>
          <div class="field"><label>NAMA</label><input v-model="doctorSearch.name" @keyup.enter="searchDoctor" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchDoctor">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>KODE</th><th>NAMA</th></tr></thead>
              <tbody>
                <tr v-for="r in doctorResults" :key="r.staffId" @click="selectTreatmentDoctor(r)">
                  <td class="strong">{{ r.code }}</td>
                  <td>{{ r.name }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showModal = 'treatment'">TUTUP</button>
        </div>
      </div>
    </div>

    <!-- ==================== MODAL: TAMBAH O-BM ==================== -->
    <div v-if="showModal === 'item'" class="modal-overlay" @click.self="showModal = ''">
      <div class="modal">
        <div class="modal-header">FORM TAMBAH O-BM</div>
        <div class="modal-body">
          <div class="field"><label>KODE</label><input v-model="itemSearch.code" /></div>
          <div class="field"><label>NAMA</label><input v-model="itemSearch.name" /></div>
          <button class="small-button primary" type="button" :disabled="loading" @click="searchItem">🔍 CARI</button>
          <div class="table-wrap modal-list">
            <table class="table">
              <thead><tr><th>KODE</th><th>NAMA</th><th>SATUAN</th><th class="num">HARGA</th></tr></thead>
              <tbody>
                <tr v-for="r in itemResults" :key="r.itemId" @click="addItem(r)">
                  <td class="strong">{{ r.code }}</td>
                  <td>{{ r.name }}</td>
                  <td>{{ r.unit }}</td>
                  <td class="num">{{ fmtMoney(r.price) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="small-button" type="button" @click="showModal = ''">SELESAI</button>
        </div>
      </div>
    </div>

    <!-- ==================== MODAL: BIAYA LAIN-LAIN ==================== -->
    <div v-if="showModal === 'misc'" class="modal-overlay" @click.self="showModal = ''">
      <div class="modal">
        <div class="modal-header">FORM TRANSAKSI LAIN-LAIN</div>
        <div class="modal-body">
          <div class="field"><label>NAMA</label><input v-model="miscForm.name" /></div>
          <div class="field"><label>JUMLAH</label><input v-model.number="miscForm.qty" type="number" min="1" /></div>
          <div class="field"><label>HARGA SATUAN</label><input v-model.number="miscForm.price" type="number" min="0" /></div>
        </div>
        <div class="modal-footer">
          <button class="small-button primary" type="button" @click="addMisc">💾 SIMPAN</button>
          <button class="small-button" type="button" @click="showModal = ''">SELESAI</button>
        </div>
      </div>
    </div>

    <!-- ==================== DIALOG / TOAST ==================== -->
    <transition name="dialog-fade">
      <div v-if="dialog.visible" class="modal-overlay" @click.self="closeDialog(false)">
        <div class="dialog-box" :class="'dialog-box--' + dialog.type">
          <div class="dialog-icon">{{ dialogIcon }}</div>
          <div class="dialog-title">{{ dialog.title }}</div>
          <div class="dialog-message">{{ dialog.message }}</div>
          <div class="dialog-buttons">
            <template v-if="dialog.mode === 'confirm'">
              <button class="small-button danger" type="button" @click="closeDialog(false)">✖ TIDAK</button>
              <button class="small-button primary" type="button" @click="closeDialog(true)">✔ YA</button>
            </template>
            <template v-else>
              <button class="small-button primary" type="button" @click="closeDialog(true)">✔ OK</button>
            </template>
          </div>
        </div>
      </div>
    </transition>

    <transition name="dialog-fade">
      <div v-if="promptDialog.visible" class="modal-overlay" @click.self="closePrompt(null)">
        <div class="dialog-box">
          <div class="dialog-icon">✍️</div>
          <div class="dialog-title">{{ promptDialog.title }}</div>
          <div class="dialog-message">{{ promptDialog.message }}</div>
          <input v-model="promptDialog.value" class="dialog-input" type="text" placeholder="Tulis alasan..." @keyup.enter="closePrompt(promptDialog.value)" />
          <div class="dialog-buttons">
            <button class="small-button" type="button" @click="closePrompt(null)">✖ BATAL</button>
            <button class="small-button primary" type="button" @click="closePrompt(promptDialog.value)">✔ SIMPAN</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="toast-fade">
      <div v-if="toast.visible" class="toast" :class="'toast--' + toast.type">
        <span class="toast-icon">{{ toast.type === 'success' ? '✅' : toast.type === 'error' ? '❌' : 'ℹ️' }}</span>
        <span class="toast-message">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.screen-page { padding: 16px; }
.page-header { margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 0; color: #6b7280; font-size: 14px; }

.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }

.tabs { display: flex; gap: 8px; margin-bottom: 16px; border-bottom: 2px solid #eef2f7; padding-bottom: 8px; }
.tab { padding: 8px 18px; border: 1px solid #d1d9e6; border-radius: 8px; background: #fff; cursor: pointer; font-weight: 700; font-size: 13px; color: #304b73; }
.tab.active { background: #304b73; color: #fff; border-color: #304b73; }

.section-title { font-weight: 800; color: #304b73; font-size: 14px; margin: 14px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eef2f7; }

.patient-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px 18px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; }
.field input, .field select { padding: 7px 9px; border: 1px solid #d1d9e6; border-radius: 6px; font-size: 13px; width: 100%; box-sizing: border-box; }
.field input[readonly] { background: #f3f5f8; color: #4b5563; }
.input-row { display: flex; gap: 6px; align-items: center; }
.input-row input { flex: 1; }
.mini { padding: 6px 10px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 12px; white-space: nowrap; }
.mini.primary { background: #304b73; color: #fff; border-color: #304b73; }

.status-line { display: flex; gap: 6px; align-items: center; margin-bottom: 8px; }
.status-label, .status-value { font-weight: 700; color: #a32943; font-size: 13px; }

.table-wrap { overflow: auto; margin: 10px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 7px 9px; border-bottom: 1px solid #eef2f7; text-align: left; white-space: nowrap; }
.table th { background: #f6f8fb; color: #304b73; }
.table tbody tr:hover { background: #f6f8fb; }
.table tbody tr.selected { background: #e8eef8; }
.num { text-align: right; }
.strong { font-weight: 700; }
.empty-state { color: #9ca3af; text-align: center; padding: 16px; }
.num-input { width: 70px; padding: 4px 6px; border: 1px solid #d1d9e6; border-radius: 4px; text-align: right; }
.disc-select { padding: 4px; border: 1px solid #d1d9e6; border-radius: 4px; }

.total-line { display: flex; align-items: center; justify-content: flex-end; gap: 8px; margin: 8px 0; }
.total-label { font-weight: 800; color: #304b73; font-size: 13px; }
.total-colon { font-weight: 800; }
.total-input { width: 180px; padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 6px; font-weight: 800; text-align: right; background: #f3f5f8; }

.action-bar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 12px; }
.small-button { padding: 8px 14px; border-radius: 8px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-weight: 600; font-size: 13px; }
.small-button.primary { background: #304b73; color: #fff; border-color: #304b73; }
.small-button.success { background: #e7f6ec; color: #177245; border-color: #177245; }
.small-button.danger { background: #fde8ea; color: #a32943; border-color: #a32943; }
.small-button:disabled { opacity: 0.5; cursor: default; }

.period-bar { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin: 10px 0; }
.period-label { font-weight: 700; color: #304b73; font-size: 13px; }
.period-bar label { font-size: 13px; display: flex; align-items: center; gap: 4px; }

.history-layout { display: grid; grid-template-columns: 320px 1fr; gap: 14px; }
.grand-total { text-align: right; font-size: 15px; color: #304b73; margin-top: 10px; }
.grand-total strong { font-size: 18px; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.45); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal { background: #fff; border-radius: 12px; width: 640px; max-width: 94vw; max-height: 88vh; display: flex; flex-direction: column; box-shadow: 0 20px 40px rgba(0,0,0,0.2); }
.modal-header { padding: 14px 18px; background: #304b73; color: #fff; font-weight: 800; border-radius: 12px 12px 0 0; }
.modal-body { padding: 14px 18px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
.modal-list { max-height: 300px; }
.modal-footer { padding: 12px 18px; border-top: 1px solid #eef2f7; display: flex; justify-content: flex-end; gap: 10px; border-radius: 0 0 12px 12px; }

.toast { position: fixed; bottom: 24px; right: 24px; z-index: 100; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-radius: 12px; font-weight: 700; font-size: 14px; color: #fff; box-shadow: 0 10px 30px rgba(15,23,42,0.25); max-width: 420px; }
.toast--success { background: linear-gradient(135deg, #177245, #1f9d5c); }
.toast--error { background: linear-gradient(135deg, #a32943, #d64567); }
.toast--info { background: linear-gradient(135deg, #304b73, #5f83c2); }
.toast-icon { font-size: 18px; }
.toast-message { flex: 1; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateY(16px); }

.dialog-fade-enter-active, .dialog-fade-leave-active { transition: all 0.25s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.92); }
.dialog-box { background: #fff; border-radius: 16px; width: 380px; max-width: 92vw; padding: 26px 28px; text-align: center; box-shadow: 0 24px 50px rgba(15,23,42,0.3); border-top: 5px solid #5f83c2; }
.dialog-box--warning { border-top-color: #e6a23c; }
.dialog-box--error { border-top-color: #d64567; }
.dialog-box--success { border-top-color: #1f9d5c; }
.dialog-box--confirm { border-top-color: #5f83c2; }
.dialog-icon { font-size: 44px; margin-bottom: 8px; }
.dialog-title { font-size: 17px; font-weight: 800; color: #304b73; margin-bottom: 8px; }
.dialog-message { font-size: 14px; color: #4b5563; line-height: 1.5; margin-bottom: 18px; white-space: pre-line; }
.dialog-input { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1px solid #d1d9e6; border-radius: 8px; font-size: 14px; margin-bottom: 16px; outline: none; }
.dialog-input:focus { border-color: #5f83c2; box-shadow: 0 0 0 3px rgba(95,131,194,0.2); }
.dialog-buttons { display: flex; justify-content: center; gap: 12px; }
.dialog-buttons .small-button { min-width: 110px; }

@media (max-width: 960px) {
  .patient-grid { grid-template-columns: 1fr; }
  .history-layout { grid-template-columns: 1fr; }
}
</style>
