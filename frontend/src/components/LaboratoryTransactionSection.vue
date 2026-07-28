<script setup>
import { computed, onMounted, reactive, ref } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, required: true },
  availableUnits: { type: Array, default: () => [] }
});

const emit = defineEmits(['session-expired']);

const loading = ref(true);
const saving = ref(false);
const searchingPatients = ref(false);
const searchingNotes = ref(false);
const treatmentSearching = ref(false);
const statusMessage = ref('');
const errorMessage = ref('');

const searchMode = ref('patient');
const selectedPatient = ref(null);
const showSearch = ref(true);

const masters = reactive({ units: [], patientTypes: [], escorts: [] });
const registeredPatients = ref([]);
const noteResults = ref([]);
const noteDetail = ref(null);
const treatmentResults = ref([]);

const patientSearch = reactive({ mrCode: '', patientName: '', address: '' });
const noteSearch = reactive({ noteNumber: '', patientName: '' });
const treatmentSearch = reactive({ code: '', name: '' });

// Panel Lab — Button-triggered modal overlay like legacy SIMRS
const panels = ref([]);
const panelsLoading = ref(false);
const expandedPanels = reactive({});
const showPanelLab = ref(false);
const selectedPanelItems = ref({}); // { [treatmentId]: true/false }
const showTreatmentSearch = ref(false);
const showMiscModal = ref(false);
const miscDraft = reactive({ description: '', quantity: 1, unitPrice: 0 });

const form = reactive({
  unitId: '',
  mrCode: '',
  registrationCode: '',
  patientTypeId: '',
  patientName: '',
  gender: '',
  doctorStaffId: '',
  escortId: '',
  noteId: null,
  noteNumber: '',
  noteStatusLabel: '',
  cancelationNote: ''
});

const cart = ref([]);

const selectedUnit = computed(() => masters.units.find(u => String(u.unitId) === String(form.unitId)) || null);
const totalAmount = computed(() => cart.value.reduce((t, l) => t + (l.quantity * l.unitPrice), 0));
const isEditingNote = computed(() => Boolean(form.noteId));

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
    if (!form.unitId && masters.units.length) {
      form.unitId = String(masters.units[0].unitId);
    }
  } catch (e) { errorMessage.value = e.message; }
}

function switchSearchMode(mode) {
  searchMode.value = mode;
  registeredPatients.value = [];
  noteResults.value = [];
  noteDetail.value = null;
  showSearch.value = true;
}

function toggleSearch() {
  showSearch.value = !showSearch.value;
}

async function searchPatients() {
  const params = new URLSearchParams();
  if (patientSearch.mrCode) params.set('mrCode', patientSearch.mrCode);
  if (patientSearch.patientName) params.set('patientName', patientSearch.patientName);
  if (patientSearch.address) params.set('address', patientSearch.address);
  if (!params.toString()) { errorMessage.value = 'Isi minimal satu kriteria pencarian.'; return; }

  searchingPatients.value = true;
  errorMessage.value = '';
  try {
    const res = await request(`/laborat/patients/registered?${params.toString()}`);
    registeredPatients.value = res.data || [];
  } catch (e) { errorMessage.value = e.message; }
  finally { searchingPatients.value = false; }
}

async function selectPatient(mrCode) {
  try {
    const res = await request(`/laborat/patients/${mrCode}`);
    const data = res.data;
    form.mrCode = data.mrCode;
    form.patientName = data.patientName;
    form.gender = data.gender;
    if (data.registrations && data.registrations.length > 0) {
      form.registrationCode = data.registrations[0].registrationCode;
    }
    selectedPatient.value = data;
    showSearch.value = false;
    registeredPatients.value = [];
    statusMessage.value = '';
    errorMessage.value = '';
  } catch (e) { errorMessage.value = e.message; }
}

function resetSearch() {
  patientSearch.mrCode = '';
  patientSearch.patientName = '';
  patientSearch.address = '';
  registeredPatients.value = [];
  selectedPatient.value = null;
  noteResults.value = [];
  noteDetail.value = null;
  cart.value = [];
  treatmentResults.value = [];
  form.mrCode = '';
  form.patientName = '';
  form.gender = '';
  form.registrationCode = '';
  form.patientTypeId = '';
  form.doctorStaffId = '';
  form.escortId = '';
  form.noteId = null;
  form.noteNumber = '';
  form.noteStatusLabel = '';
  statusMessage.value = '';
  errorMessage.value = '';
  showSearch.value = true;
}

async function searchNotes() {
  if (!form.unitId) { errorMessage.value = 'Pilih unit laboratorium terlebih dahulu.'; return; }
  const params = new URLSearchParams();
  if (noteSearch.noteNumber) params.set('noteNumber', noteSearch.noteNumber);
  if (noteSearch.patientName) params.set('patientName', noteSearch.patientName);

  searchingNotes.value = true;
  errorMessage.value = '';
  try {
    const res = await request(`/laborat/units/${form.unitId}/notes?${params.toString()}`);
    noteResults.value = res.data || [];
  } catch (e) { errorMessage.value = e.message; }
  finally { searchingNotes.value = false; }
}

async function selectNote(noteId) {
  try {
    const res = await request(`/laborat/notes/${noteId}`);
    const data = res.data;
    noteDetail.value = data;
    form.noteId = data.noteId;
    form.noteNumber = data.noteNumber;
    form.noteStatusLabel = data.statusLabel;
    form.mrCode = data.mrCode;
    form.patientName = data.patientName;
    form.registrationCode = data.registrationCode;
    form.patientTypeId = String(data.patientTypeId);
    // Load lines into cart
    cart.value = (data.lines || []).map(l => ({
      lineType: l.lineType,
      refId: l.refId,
      code: l.code,
      description: l.description,
      quantity: l.quantity,
      unitPrice: l.unitPrice,
      discountAmount: l.discountAmount,
      discountType: l.discountType
    }));
    statusMessage.value = '';
    errorMessage.value = '';
  } catch (e) { errorMessage.value = e.message; }
}

async function searchTreatments() {
  if (!form.unitId) { errorMessage.value = 'Pilih unit laboratorium terlebih dahulu.'; return; }
  const params = new URLSearchParams();
  if (form.unitId) params.set('unitId', form.unitId);
  if (treatmentSearch.code) params.set('code', treatmentSearch.code);
  if (treatmentSearch.name) params.set('name', treatmentSearch.name);
  const tariffClass = selectedPatient.value
    ? (selectedPatient.value.tariffClass || 'KELAS II')
    : 'KELAS II';
  params.set('tariffClass', tariffClass);

  treatmentSearching.value = true;
  try {
    const res = await request(`/laborat/treatments?${params.toString()}`);
    treatmentResults.value = res.data || [];
  } catch (e) { errorMessage.value = e.message; }
  finally { treatmentSearching.value = false; }
}

async function loadPanels() {
  panelsLoading.value = true;
  try {
    const res = await request('/laborat/panels');
    panels.value = res.data || [];
    // Auto-expand all categories
    (res.data || []).forEach(p => { expandedPanels[p.panelName] = true; });
  } catch (e) { /* ignore */ }
  finally { panelsLoading.value = false; }
}

function togglePanel(name) {
  expandedPanels[name] = !expandedPanels[name];
}

function isPanelExpanded(name) {
  return expandedPanels[name] === true;
}

// ── Button Panel Lab (modal overlay, like legacy tambahTindakan.zul) ──
async function openPanelLab() {
  selectedPanelItems.value = {};
  panelsLoading.value = true;
  showPanelLab.value = true;
  // Tentukan kelas tarif: rawat inap pakai tariffClass pasien, rawat jalan default KELAS II
  const tariffClass = selectedPatient.value
    ? (selectedPatient.value.tariffClass || 'KELAS II')
    : 'KELAS II';
  try {
    const res = await request(`/laborat/panels?tariffClass=${encodeURIComponent(tariffClass)}`);
    panels.value = res.data || [];
    (res.data || []).forEach(p => { expandedPanels[p.panelName] = true; });
  } catch (e) { /* ignore */ }
  finally { panelsLoading.value = false; }
}

function closePanelLab() {
  showPanelLab.value = false;
  selectedPanelItems.value = {};
}

function togglePanelItem(treatmentId) {
  if (selectedPanelItems.value[treatmentId]) {
    delete selectedPanelItems.value[treatmentId];
  } else {
    selectedPanelItems.value[treatmentId] = true;
  }
}

function isPanelItemSelected(treatmentId) {
  return selectedPanelItems.value[treatmentId] === true;
}

function addSelectedPanelItems() {
  // Gather all selected treatments across all panels
  const selected = [];
  panels.value.forEach(panel => {
    panel.treatments.forEach(t => {
      if (selectedPanelItems.value[t.treatmentId]) {
        selected.push(t);
      }
    });
  });
  if (selected.length === 0) return;
  selected.forEach(t => addTreatment(t));
  closePanelLab();
}

function countSelectedPanelItems() {
  let count = 0;
  // eslint-disable-next-line no-unused-vars
  for (const _k in selectedPanelItems.value) { count++; }
  return count;
}

// ── Button Tambah Pemeriksaan (treatment search modal) ──
function openTreatmentSearch() {
  treatmentSearch.code = '';
  treatmentSearch.name = '';
  treatmentResults.value = [];
  showTreatmentSearch.value = true;
}

function closeTreatmentSearch() {
  showTreatmentSearch.value = false;
  treatmentResults.value = [];
}

function addTreatmentToCart(t) {
  const tariffClass = selectedPatient.value
    ? (selectedPatient.value.tariffClass || 'KELAS II')
    : 'KELAS II';
  const existing = cart.value.find(l => l.lineType === 'TREATMENT' && l.refId === t.treatmentId);
  if (existing) { existing.quantity += 1; return; }
  cart.value.push({
    lineType: 'TREATMENT', refId: t.treatmentId, code: t.code,
    description: t.name, quantity: 1, unitPrice: t.tariff,
    discountAmount: 0, discountType: 'RP'
  });
}

// ── Button Biaya Lain-Lain (misc modal) ──
function openMiscModal() {
  miscDraft.description = '';
  miscDraft.quantity = 1;
  miscDraft.unitPrice = 0;
  showMiscModal.value = true;
}

function closeMiscModal() {
  showMiscModal.value = false;
  miscDraft.description = '';
  miscDraft.quantity = 1;
  miscDraft.unitPrice = 0;
}

function addMiscCost() {
  if (!miscDraft.description || !miscDraft.quantity || !miscDraft.unitPrice) {
    errorMessage.value = 'Lengkapi nama, jumlah, dan harga biaya lain-lain.';
    return;
  }
  cart.value.push({
    lineType: 'MISC', refId: null, code: 'MISC-001',
    description: miscDraft.description,
    quantity: Number(miscDraft.quantity),
    unitPrice: Number(miscDraft.unitPrice),
    discountAmount: 0, discountType: 'RP'
  });
  closeMiscModal();
}



function addTreatment(t) {
  const existing = cart.value.find(l => l.lineType === 'TREATMENT' && l.refId === t.treatmentId);
  if (existing) { existing.quantity += 1; return; }
  cart.value.push({
    lineType: 'TREATMENT', refId: t.treatmentId, code: t.code,
    description: t.name, quantity: 1, unitPrice: t.tariff,
    discountAmount: 0, discountType: 'RP'
  });
}

function removeCartItem(idx) { cart.value.splice(idx, 1); }

function formatCurrency(v) {
  if (v == null || isNaN(v)) return '0';
  return Number(v).toLocaleString('id-ID');
}

function formatDate(dateStr) {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

function calculateAge(birthDateStr) {
  if (!birthDateStr) return '';
  const birthDate = new Date(birthDateStr);
  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }
  return age + ' Thn';
}

function statusBadgeClass(statusLabel) {
  if (!statusLabel) return '';
  const s = statusLabel.toUpperCase();
  if (s === 'VALID' || s === 'VALIDATED') return 'badge-valid';
  if (s === 'CANCEL' || s === 'CANCELED') return 'badge-cancel';
  return 'badge-draft';
}

async function saveNote() {
  if (!form.mrCode) { errorMessage.value = 'Pilih pasien terlebih dahulu.'; return; }
  if (cart.value.length === 0) { errorMessage.value = 'Tambahkan minimal satu tindakan/barang.'; return; }

  saving.value = true;
  errorMessage.value = '';
  statusMessage.value = '';

  const body = {
    mrCode: form.mrCode,
    registrationCode: form.registrationCode || null,
    unitId: Number(form.unitId),
    patientTypeId: Number(form.patientTypeId) || 1,
    doctorStaffId: form.doctorStaffId || null,
    escortId: form.escortId ? Number(form.escortId) : null,
    referencePatient: false,
    treatments: cart.value.filter(l => l.lineType === 'TREATMENT').map(l => ({
      lineType: l.lineType, refId: l.refId, code: l.code,
      description: l.description, quantity: l.quantity, unitPrice: l.unitPrice,
      discountAmount: l.discountAmount || 0, discountType: l.discountType || 'RP'
    })),
    items: cart.value.filter(l => l.lineType === 'ITEM').map(l => ({
      lineType: l.lineType, refId: l.refId, code: l.code,
      description: l.description, quantity: l.quantity, unitPrice: l.unitPrice,
      discountAmount: 0, discountType: 'RP'
    }))
  };

  try {
    let res;
    if (isEditingNote.value) {
      res = await request(`/laborat/notes/${form.noteId}`, { method: 'PUT', body: JSON.stringify(body) });
      statusMessage.value = `Nota berhasil diubah. No. Nota: ${form.noteNumber}`;
    } else {
      res = await request('/laborat/notes', { method: 'POST', body: JSON.stringify(body) });
      form.noteId = res.data.noteId;
      form.noteNumber = res.data.noteNumber;
      statusMessage.value = `Nota berhasil disimpan. No. Nota: ${res.data.noteNumber}`;
    }
  } catch (e) { errorMessage.value = e.message; }
  finally { saving.value = false; }
}

async function validateNote() {
  if (!form.noteId) return;
  try {
    await request(`/laborat/notes/${form.noteId}/validate`, { method: 'POST' });
    statusMessage.value = `Nota ${form.noteNumber} divalidasi.`;
    form.noteStatusLabel = 'VALID';
  } catch (e) { errorMessage.value = e.message; }
}

function resetForm() {
  resetSearch();
}

onMounted(async () => {
  loading.value = true;
  await loadMasters();
  loading.value = false;
});
</script>

<template>
  <div class="lab-page">
    <div class="page-header">
      <h2>🧪 Transaksi Laboratorium</h2>
      <p class="page-subtitle">SC0041 — Pencarian pasien dan input transaksi lab</p>
    </div>

    <!-- Unit Selector -->
    <div class="unit-bar">
      <label class="unit-label">LOKASI TRANSAKSI</label>
      <select v-model="form.unitId" class="unit-select">
        <option value="">Pilih unit</option>
        <option v-for="u in masters.units" :key="u.unitId" :value="String(u.unitId)">
          {{ u.unitCode }}. {{ u.unitName }}
        </option>
      </select>
    </div>

    <!-- Notifications -->
    <p v-if="errorMessage" class="status-banner status-banner--error">{{ errorMessage }}</p>
    <p v-else-if="statusMessage" class="status-banner status-banner--success">{{ statusMessage }}</p>

    <div v-if="loading" class="loading">Memuat master laboratorium...</div>

    <template v-else>
      <!-- Mode Tabs -->
      <div class="mode-tabs">
        <button class="mode-tab" :class="{ 'mode-tab--active': searchMode === 'patient' }" @click="switchSearchMode('patient')">
          🏥 Cari Pasien
        </button>
        <button class="mode-tab" :class="{ 'mode-tab--active': searchMode === 'note' }" @click="switchSearchMode('note')">
          📋 Cari Nota Aktif
        </button>
      </div>

      <!-- ======================== PATIENT SEARCH MODE ======================== -->
      <div v-if="searchMode === 'patient'" class="card search-card">
        <h3>
          <span>Pencarian Pasien</span>
          <span class="search-actions">
            <button v-if="selectedPatient && !showSearch" class="small-button" type="button" @click="toggleSearch">Cari Pasien Lain</button>
            <button v-if="registeredPatients.length || selectedPatient" class="small-button" type="button" @click="resetSearch">Reset</button>
          </span>
        </h3>

        <div v-if="showSearch" class="search-form">
          <div class="form-row">
            <label>No. MR <input v-model="patientSearch.mrCode" placeholder="Ketik No. MR" @keyup.enter="searchPatients" /></label>
            <label>Nama Pasien <input v-model="patientSearch.patientName" placeholder="Nama lengkap" @keyup.enter="searchPatients" /></label>
          </div>
          <div class="form-row">
            <label class="wide">Alamat <input v-model="patientSearch.address" placeholder="Alamat" @keyup.enter="searchPatients" /></label>
          </div>
          <button class="primary-button" :disabled="searchingPatients" @click="searchPatients">
            {{ searchingPatients ? 'Mencari...' : 'Cari Pasien' }}
          </button>
        </div>

        <div v-if="showSearch && registeredPatients.length" class="table-wrap">
          <table class="table">
            <thead><tr><th>No. MR</th><th>Nama Pasien</th><th>Tgl. Lahir</th><th>Alamat</th><th>Aksi</th></tr></thead>
            <tbody>
              <tr v-for="p in registeredPatients" :key="p.patientId">
                <td><strong>{{ p.mrCode }}</strong></td>
                <td>{{ p.patientName }}</td>
                <td>{{ p.birthDate || '-' }}</td>
                <td>{{ p.address || '-' }}</td>
                <td><button class="link-button" @click="selectPatient(p.mrCode)">Pilih</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="selectedPatient" class="selected-patient">
          <h4>Detail Pasien Terpilih</h4>
          <div class="patient-detail-grid three-col">
            <div class="detail-item">
              <span class="detail-label">No. MR</span>
              <span class="detail-value">{{ selectedPatient.mrCode }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">No. Registrasi</span>
              <span class="detail-value">{{ form.registrationCode || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Nama</span>
              <span class="detail-value">{{ selectedPatient.patientName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Jenis Kelamin</span>
              <span class="detail-value">{{ selectedPatient.gender === 'M' ? 'Laki-Laki' : 'Perempuan' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Tgl. Lahir / Umur</span>
              <span class="detail-value">{{ formatDate(selectedPatient.birthDate) }} / {{ calculateAge(selectedPatient.birthDate) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Tipe Pasien</span>
              <span class="detail-value">{{ selectedPatient.patientTypeName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Dokter Utama</span>
              <span class="detail-value">{{ selectedPatient.doctorName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Tipe Pembawa</span>
              <span class="detail-value">{{ selectedPatient.escortName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Alamat</span>
              <span class="detail-value">{{ selectedPatient.address || '-' }}</span>
            </div>
          </div>

          <!-- Cart / Lines -->
          <div class="lines-section">
            <h4>DAFTAR TINDAKAN</h4>

            <!-- Panel Lab & Action Buttons (like legacy SIMRS) -->
            <div class="panel-lab-button-bar">
              <button class="panel-lab-btn" type="button" @click="openPanelLab()">
                <span class="panel-lab-btn-icon">📋</span>
                <span class="panel-lab-btn-label">PANEL LAB</span>
              </button>
              <button class="panel-lab-btn" type="button" @click="openTreatmentSearch()">
                <span class="panel-lab-btn-icon">➕</span>
                <span class="panel-lab-btn-label">TAMBAH PEMERIKSAAN</span>
              </button>
              <button class="panel-lab-btn" type="button" @click="openMiscModal()">
                <span class="panel-lab-btn-icon">💰</span>
                <span class="panel-lab-btn-label">BIAYA LAIN-LAIN</span>
              </button>
            </div>

            <!-- Cart Table -->
            <div v-if="cart.length" class="table-wrap">
              <table class="table">
                <thead><tr><th>Kode</th><th>Nama</th><th>Qty</th><th>Harga</th><th>Subtotal</th><th>Aksi</th></tr></thead>
                <tbody>
                  <tr v-for="(line, idx) in cart" :key="idx">
                    <td>{{ line.code }}</td>
                    <td>{{ line.description }}</td>
                    <td><input type="number" v-model.number="line.quantity" min="1" class="qty-input" /></td>
                    <td class="amount">{{ formatCurrency(line.unitPrice) }}</td>
                    <td class="amount">{{ formatCurrency(line.quantity * line.unitPrice) }}</td>
                    <td><button class="link-button danger" @click="removeCartItem(idx)">✕</button></td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div v-if="cart.length" class="total-bar">
              <strong>TOTAL: Rp {{ formatCurrency(totalAmount) }}</strong>
            </div>

            <!-- Action Buttons -->
            <div v-if="cart.length || isEditingNote" class="action-bar">
              <button class="primary-button" :disabled="saving || !cart.length" @click="saveNote">
                {{ saving ? 'Menyimpan...' : (isEditingNote ? 'UBAH' : 'SIMPAN') }}
              </button>
              <button class="secondary-button" @click="resetForm">BARU</button>
              <button v-if="isEditingNote && form.noteStatusLabel !== 'VALID'" class="success-button" @click="validateNote">VALIDASI</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ======================== NOTE SEARCH MODE ======================== -->
      <div v-if="searchMode === 'note'" class="card">
        <h3>
          <span>🔍 Pencarian Nota Aktif</span>
          <span class="search-actions">
            <button v-if="noteDetail" class="small-button" type="button" @click="noteDetail = null; noteResults = []">Reset</button>
          </span>
        </h3>

        <div v-if="!noteDetail" class="search-form">
          <div class="form-row">
            <label>No. Nota <input v-model="noteSearch.noteNumber" placeholder="Ketik No. Nota" @keyup.enter="searchNotes" /></label>
            <label>Nama Pasien <input v-model="noteSearch.patientName" placeholder="Nama pasien" @keyup.enter="searchNotes" /></label>
          </div>
          <button class="primary-button" :disabled="searchingNotes" @click="searchNotes">
            {{ searchingNotes ? 'Mencari...' : 'Cari Nota' }}
          </button>
        </div>

        <div v-if="noteResults.length" class="table-wrap">
          <table class="table">
            <thead><tr><th>No. Nota</th><th>Pasien</th><th>Status</th><th>Tgl</th><th>Aksi</th></tr></thead>
            <tbody>
              <tr v-for="n in noteResults" :key="n.noteId">
                <td><strong>{{ n.noteNumber }}</strong></td>
                <td>{{ n.patientName }}</td>
                <td><span class="badge" :class="statusBadgeClass(n.statusLabel)">{{ n.statusLabel }}</span></td>
                <td>{{ n.createdAt }}</td>
                <td><button class="link-button" @click="selectNote(n.noteId)">Pilih & Edit</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="noteDetail" class="selected-note">
          <h4>Detail Nota</h4>
          <div class="patient-detail-grid">
            <div class="detail-item"><span class="detail-label">No. Nota</span><span class="detail-value">{{ noteDetail.noteNumber }}</span></div>
            <div class="detail-item"><span class="detail-label">Status</span><span class="detail-value">{{ noteDetail.statusLabel }}</span></div>
            <div class="detail-item"><span class="detail-label">Pasien</span><span class="detail-value">{{ noteDetail.patientName }}</span></div>
            <div class="detail-item"><span class="detail-label">Total</span><span class="detail-value">Rp {{ formatCurrency(noteDetail.totalAmount) }}</span></div>
          </div>

          <div v-if="noteDetail.lines && noteDetail.lines.length" class="table-wrap">
            <table class="table">
              <thead><tr><th>Kode</th><th>Nama</th><th>Harga</th><th>Qty</th><th>Subtotal</th></tr></thead>
              <tbody>
                <tr v-for="(line, idx) in noteDetail.lines" :key="idx">
                  <td>{{ line.code }}</td><td>{{ line.description }}</td>
                  <td class="amount">{{ formatCurrency(line.unitPrice) }}</td>
                  <td>{{ line.quantity }}</td>
                  <td class="amount">{{ formatCurrency(line.subtotal) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="action-bar">
            <button v-if="noteDetail.statusCode === 0" class="primary-button" @click="searchMode = 'patient'; selectedPatient = { mrCode: noteDetail.mrCode, patientName: noteDetail.patientName }; form.mrCode = noteDetail.mrCode; form.patientName = noteDetail.patientName; form.noteId = noteDetail.noteId; form.noteNumber = noteDetail.noteNumber; form.noteStatusLabel = noteDetail.statusLabel">
              Edit Nota
            </button>
          </div>
        </div>
      </div>

      <!-- ======================== PANEL LAB MODAL OVERLAY (legacy-style) ======================== -->
      <Teleport to="body">
        <div v-if="showPanelLab" class="panel-lab-overlay" @click.self="closePanelLab">
          <div class="panel-lab-modal">
            <div class="panel-lab-modal-header">
              <h3>📋 FORM PANEL LAB</h3>
              <button class="panel-lab-close-btn" type="button" @click="closePanelLab" title="Tutup">&times;</button>
            </div>

            <div class="panel-lab-modal-body">
              <div v-if="panelsLoading" class="hint">Memuat panel...</div>
              <div v-else-if="!panels.length" class="hint">Tidak ada data panel.</div>
              <div v-else class="panel-lab-grid">
                <div v-for="panel in panels" :key="panel.panelName" class="panel-lab-col">
                  <div class="panel-lab-col-header">
                    {{ panel.panelName }}
                  </div>
                  <div class="panel-lab-col-body">
                    <div
                      v-for="t in panel.treatments"
                      :key="t.treatmentId"
                      class="panel-lab-item"
                      :class="{ 'panel-lab-item--selected': isPanelItemSelected(t.treatmentId) }"
                      @click="togglePanelItem(t.treatmentId)"
                    >
                      <span class="panel-lab-item-check">
                        {{ isPanelItemSelected(t.treatmentId) ? '☑' : '☐' }}
                      </span>
                      <span class="panel-lab-item-code">{{ t.code }}</span>
                      <span class="panel-lab-item-name">{{ t.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="panel-lab-modal-footer">
              <span class="panel-lab-selected-count">
                {{ countSelectedPanelItems() }} item dipilih
              </span>
              <div class="panel-lab-footer-actions">
                <button
                  class="panel-lab-add-btn"
                  type="button"
                  :disabled="countSelectedPanelItems() === 0"
                  @click="addSelectedPanelItems"
                >
                  + TAMBAH
                </button>
                <button class="panel-lab-close-btn-bottom" type="button" @click="closePanelLab">
                  SELESAI
                </button>
              </div>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- ======================== TAMBAH PEMERIKSAAN MODAL ======================== -->
      <Teleport to="body">
        <div v-if="showTreatmentSearch" class="panel-lab-overlay" @click.self="closeTreatmentSearch">
          <div class="panel-lab-modal" style="max-width: 700px;">
            <div class="panel-lab-modal-header">
              <h3>➕ TAMBAH PEMERIKSAAN</h3>
              <button class="panel-lab-close-btn" type="button" @click="closeTreatmentSearch" title="Tutup">&times;</button>
            </div>

            <div class="panel-lab-modal-body">
              <div class="search-compact">
                <div class="compact-row">
                  <input v-model="treatmentSearch.code" placeholder="Kode pemeriksaan" @keyup.enter="searchTreatments" />
                  <input v-model="treatmentSearch.name" placeholder="Nama pemeriksaan" @keyup.enter="searchTreatments" />
                  <button class="small-button" :disabled="treatmentSearching" @click="searchTreatments">🔍 Cari</button>
                </div>
              </div>

              <div v-if="treatmentSearching" class="hint">Mencari...</div>
              <div v-else-if="!treatmentResults.length" class="hint">Cari pemeriksaan dengan mengisi kode atau nama di atas.</div>
              <div v-else class="compact-results" style="max-height: 400px;">
                <div v-for="t in treatmentResults" :key="t.treatmentId" class="compact-item" @click="addTreatmentToCart(t)">
                  <span class="item-code">{{ t.code }}</span>
                  <span class="item-name">{{ t.name }}</span>
                  <span class="item-add">+</span>
                </div>
              </div>
            </div>

            <div class="panel-lab-modal-footer">
              <span style="font-size: 13px; color: #6b7280;">Klik item untuk menambahkan ke daftar tindakan</span>
              <button class="panel-lab-close-btn-bottom" type="button" @click="closeTreatmentSearch">TUTUP</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- ======================== BIAYA LAIN-LAIN MODAL ======================== -->
      <Teleport to="body">
        <div v-if="showMiscModal" class="panel-lab-overlay" @click.self="closeMiscModal">
          <div class="panel-lab-modal" style="max-width: 500px;">
            <div class="panel-lab-modal-header">
              <h3>💰 BIAYA LAIN-LAIN</h3>
              <button class="panel-lab-close-btn" type="button" @click="closeMiscModal" title="Tutup">&times;</button>
            </div>

            <div class="panel-lab-modal-body">
              <div class="misc-form">
                <label class="misc-label">
                  NAMA BIAYA
                  <input v-model="miscDraft.description" placeholder="Nama biaya" class="misc-input" />
                </label>
                <label class="misc-label">
                  JUMLAH
                  <input v-model.number="miscDraft.quantity" type="number" min="1" placeholder="Jumlah" class="misc-input" />
                </label>
                <label class="misc-label">
                  HARGA SATUAN (Rp)
                  <input v-model.number="miscDraft.unitPrice" type="number" min="0" placeholder="Harga" class="misc-input" />
                </label>
              </div>
            </div>

            <div class="panel-lab-modal-footer">
              <span style="font-size: 13px; color: #6b7280;">
                {{ miscDraft.description ? 'Total: Rp ' + formatCurrency(miscDraft.quantity * miscDraft.unitPrice) : '' }}
              </span>
              <div class="panel-lab-footer-actions">
                <button class="panel-lab-add-btn" type="button" @click="addMiscCost">TAMBAH</button>
                <button class="panel-lab-close-btn-bottom" type="button" @click="closeMiscModal">BATAL</button>
              </div>
            </div>
          </div>
        </div>
      </Teleport>
    </template>
  </div>
</template>

<style scoped>
.lab-page { padding: 16px; }
.page-header h2 { margin: 0; color: #304b73; font-size: 20px; }
.page-subtitle { margin: 4px 0 0; color: #6b7280; font-size: 14px; }
.loading { padding: 24px; text-align: center; color: #9ca3af; }
.status-banner { padding: 12px 16px; border-radius: 10px; font-weight: 600; margin-bottom: 12px; }
.status-banner--success { background: #e6f5ea; color: #1d6b3a; }
.status-banner--error { background: #fde8ea; color: #a32943; }

.unit-bar {
  display: flex; align-items: center; gap: 10px; margin-bottom: 16px;
  padding: 10px 16px; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px;
}
.unit-label { font-size: 12px; font-weight: 700; color: #304b73; white-space: nowrap; }
.unit-select { min-width: 280px; padding: 6px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; font-size: 13px; background: #fff; }

.mode-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.mode-tab {
  flex: 1; padding: 12px 20px; border: 2px solid #d1d9e6; border-radius: 10px;
  background: #fff; font-weight: 700; font-size: 14px; color: #3d4b63; cursor: pointer;
}
.mode-tab:hover { border-color: #5f83c2; color: #304b73; }
.mode-tab--active { background: #304b73; border-color: #304b73; color: #fff; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.card h3 { margin: 0 0 12px; font-size: 16px; color: #304b73; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.card h4 { margin: 12px 0 8px; font-size: 14px; color: #304b73; }

.search-actions { display: flex; gap: 6px; }
.search-form { margin-bottom: 12px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px; }
.form-row label { display: grid; gap: 4px; font-size: 13px; color: #3d4b63; }
.form-row .wide { grid-column: 1 / -1; }
input, select { padding: 8px 10px; border: 1px solid #d1d9e6; border-radius: 8px; font: inherit; background: #fff; }
.qty-input { width: 60px; text-align: center; }

.table-wrap { overflow: auto; margin: 8px 0; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #eef2f7; text-align: left; }
.table th { background: #f6f8fb; color: #304b73; white-space: nowrap; }
.amount { text-align: right; font-family: 'Courier New', monospace; }

.primary-button, .secondary-button, .success-button {
  border: 0; cursor: pointer; padding: 8px 20px; font-weight: 700; border-radius: 8px;
}
.primary-button { background: #304b73; color: #fff; }
.primary-button:disabled { opacity: 0.5; cursor: not-allowed; }
.secondary-button { background: #fff; border: 1px solid #d1d9e6; color: #3d4b63; }
.success-button { background: #1d6b3a; color: #fff; }
.small-button { padding: 6px 12px; font-size: 12px; background: #eef3fb; border: 1px solid #d1d9e6; border-radius: 6px; cursor: pointer; }
.link-button {
  background: none; border: 0; color: #5f83c2; font-weight: 700;
  cursor: pointer; padding: 4px 8px; font-size: 13px;
}
.link-button.danger { color: #b84747; }

.selected-patient, .selected-note { margin-top: 12px; }
.patient-detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 12px; background: #f6f8fb; border-radius: 8px; margin-bottom: 12px; }
.patient-detail-grid.three-col { grid-template-columns: 1fr 1fr 1fr; }
.detail-item { display: grid; gap: 2px; }
.detail-label { font-size: 11px; color: #6b7280; text-transform: uppercase; }
.detail-value { font-weight: 700; color: #304b73; }

.lines-section { margin-top: 16px; border-top: 1px solid #e2e8f0; padding-top: 12px; }

.search-compact { margin-bottom: 8px; }
.compact-row { display: flex; gap: 6px; align-items: center; }
.compact-row input { flex: 1; padding: 6px 8px; font-size: 12px; }
.compact-results {
  max-height: 150px; overflow-y: auto; border: 1px solid #eef2f7;
  border-radius: 8px; margin-top: 4px;
}
.compact-item {
  display: flex; align-items: center; gap: 8px; padding: 6px 10px;
  cursor: pointer; font-size: 12px; border-bottom: 1px solid #f6f8fb;
}
.compact-item:hover { background: #eef3fb; }
.compact-item .item-code { font-weight: 700; color: #5f83c2; min-width: 60px; }
.compact-item .item-name { flex: 1; }
.compact-item .item-price { color: #374151; font-family: monospace; }
.compact-item .item-add { color: #1d6b3a; font-weight: 700; font-size: 16px; }

/* Panel Lab — Button & Modal overlay (legacy-style) */
.panel-lab-button-bar {
  display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap;
}
.panel-lab-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border: 1px solid #d1d9e6; border-radius: 8px;
  background: #fff; cursor: pointer; font-weight: 700; font-size: 12px;
  color: #304b73; transition: all 0.15s;
}
.panel-lab-btn:hover {
  background: #eef3fb; border-color: #5f83c2;
}
.panel-lab-btn-icon { font-size: 16px; }
.panel-lab-btn-label { text-transform: uppercase; letter-spacing: 0.03em; }

/* Modal Overlay */
.panel-lab-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.45); z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.panel-lab-modal {
  background: #fff; border-radius: 12px; max-width: 1100px; width: 100%;
  max-height: 85vh; display: flex; flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.panel-lab-modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid #e2e8f0;
  background: #f6f8fb; border-radius: 12px 12px 0 0;
}
.panel-lab-modal-header h3 { margin: 0; font-size: 15px; color: #304b73; }
.panel-lab-close-btn {
  width: 32px; height: 32px; border: 1px solid #d1d9e6; background: #fff;
  border-radius: 50%; font-size: 20px; line-height: 1; cursor: pointer;
  color: #6b7280; display: flex; align-items: center; justify-content: center;
}
.panel-lab-close-btn:hover { background: #fee2e2; color: #991b1b; border-color: #fca5a5; }

.panel-lab-modal-body {
  padding: 16px 20px; overflow-y: auto; flex: 1; min-height: 300px;
}

/* Grid of panel columns (like legacy side-by-side listboxes) */
.panel-lab-grid {
  display: flex; flex-wrap: wrap; gap: 12px;
}
.panel-lab-col {
  flex: 1 1 200px; min-width: 180px; max-width: 260px;
  border: 1px solid #eef2f7; border-radius: 8px; overflow: hidden;
  background: #fff;
}
.panel-lab-col-header {
  padding: 8px 10px; font-weight: 700; font-size: 11px; text-transform: uppercase;
  letter-spacing: 0.04em; background: #304b73; color: #fff;
  text-align: center;
}
.panel-lab-col-body {
  max-height: 400px; overflow-y: auto;
}
.panel-lab-item {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px; cursor: pointer; font-size: 12px;
  border-bottom: 1px solid #f6f8fb; transition: background 0.1s;
}
.panel-lab-item:hover { background: #eef3fb; }
.panel-lab-item--selected { background: rgba(48, 75, 115, 0.08); }
.panel-lab-item-check { font-size: 14px; flex-shrink: 0; }
.panel-lab-item-code { font-weight: 700; color: #5f83c2; font-size: 10px; min-width: 35px; }
.panel-lab-item-name { flex: 1; color: #3d4b63; }

.panel-lab-modal-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; border-top: 1px solid #e2e8f0;
  background: #fafafa; border-radius: 0 0 12px 12px;
}
.panel-lab-selected-count { font-size: 13px; color: #6b7280; font-weight: 600; }
.panel-lab-footer-actions { display: flex; gap: 8px; }
.panel-lab-add-btn {
  padding: 8px 20px; border: 0; background: #304b73; color: #fff;
  font-weight: 700; border-radius: 8px; cursor: pointer; font-size: 13px;
}
.panel-lab-add-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.panel-lab-add-btn:hover:not(:disabled) { background: #1d3461; }
.panel-lab-close-btn-bottom {
  padding: 8px 20px; border: 1px solid #d1d9e6; background: #fff;
  color: #3d4b63; font-weight: 700; border-radius: 8px; cursor: pointer; font-size: 13px;
}
.panel-lab-close-btn-bottom:hover { background: #f3f4f6; }

.hint { padding: 12px; text-align: center; color: #9ca3af; font-size: 12px; }

/* Misc Form */
.misc-form { display: grid; gap: 14px; padding: 8px 0; }
.misc-label { display: grid; gap: 4px; font-weight: 700; color: #304b73; font-size: 13px; }
.misc-input {
  height: 38px; border: 1px solid #d1d9e6; border-radius: 6px;
  padding: 0 10px; font: inherit; font-size: 14px;
}
.misc-input:focus { outline: none; border-color: #5f83c2; box-shadow: 0 0 0 2px rgba(95,131,194,0.15); }

.total-bar { padding: 12px 16px; background: #f6f8fb; border-radius: 8px; margin: 12px 0; text-align: right; font-size: 16px; color: #304b73; }
.action-bar { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.badge { padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.badge-draft { background: #fef3c7; color: #92400e; }
.badge-valid { background: #d1fae5; color: #065f46; }
.badge-cancel { background: #fee2e2; color: #991b1b; }

.empty-state { padding: 24px; text-align: center; color: #9ca3af; }
</style>
